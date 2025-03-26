/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository.aot.generated;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQueryReference;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.jpa.repository.query.DeclaredQuery;
import org.springframework.data.jpa.repository.query.EntityQuery;
import org.springframework.data.jpa.repository.query.EscapeCharacter;
import org.springframework.data.jpa.repository.query.JpaCountQueryCreator;
import org.springframework.data.jpa.repository.query.JpaParameters;
import org.springframework.data.jpa.repository.query.JpaQueryCreator;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.data.jpa.repository.query.ParameterMetadataProvider;
import org.springframework.data.jpa.repository.query.QueryEnhancerSelector;
import org.springframework.data.jpa.repository.support.JpqlQueryTemplates;
import org.springframework.data.repository.aot.generate.AotQueryMethodGenerationContext;
import org.springframework.data.repository.aot.generate.AotRepositoryConstructorBuilder;
import org.springframework.data.repository.aot.generate.AotRepositoryFragmentMetadata;
import org.springframework.data.repository.aot.generate.MethodContributor;
import org.springframework.data.repository.aot.generate.RepositoryContributor;
import org.springframework.data.repository.config.AotRepositoryContext;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.TypeName;
import org.springframework.javapoet.TypeSpec;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * JPA-specific {@link RepositoryContributor} contributing an AOT repository fragment using the {@link EntityManager}
 * directly to run queries.
 * <p>
 * The underlying {@link jakarta.persistence.metamodel.Metamodel} requires Hibernate to build metamodel information.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 4.0
 */
public class JpaRepositoryContributor extends RepositoryContributor {

	private final AotMetamodel metaModel;
	private final PersistenceProvider persistenceProvider;

	public JpaRepositoryContributor(AotRepositoryContext repositoryContext) {
		super(repositoryContext);
		this.metaModel = new AotMetamodel(repositoryContext.getResolvedTypes());
		this.persistenceProvider = PersistenceProvider.fromEntityManagerFactory(metaModel.getEntityManagerFactory());
	}

	@Override
	protected void customizeClass(RepositoryInformation information, AotRepositoryFragmentMetadata metadata,
			TypeSpec.Builder builder) {
		builder.superclass(TypeName.get(AotRepositoryFragmentSupport.class));
	}

	@Override
	protected void customizeConstructor(AotRepositoryConstructorBuilder constructorBuilder) {

		constructorBuilder.addParameter("entityManager", EntityManager.class);
		constructorBuilder.addParameter("context", RepositoryFactoryBeanSupport.FragmentCreationContext.class);

		// TODO: Pick up the configured QueryEnhancerSelector
		constructorBuilder.customize((repositoryInformation, builder) -> {
			builder.addStatement("super($T.DEFAULT_SELECTOR, context)", QueryEnhancerSelector.class);
		});
	}

	@Override
	protected @Nullable MethodContributor<? extends QueryMethod> contributeQueryMethod(Method method,
			RepositoryInformation repositoryInformation) {

		JpaQueryMethod queryMethod = new JpaQueryMethod(method, repositoryInformation, getProjectionFactory(),
				persistenceProvider);

		// meh!
		QueryEnhancerSelector selector = QueryEnhancerSelector.DEFAULT_SELECTOR;

		// no stored procedures for now.
		if (queryMethod.isProcedureQuery()) {
			return null;
		}

		// no KeysetScrolling for now.
		if (queryMethod.getParameters().hasScrollPositionParameter()) {
			return null;
		}

		if (queryMethod.isModifyingQuery()) {

			Class<?> returnType = repositoryInformation.getReturnType(method).getType();
			if (!ClassUtils.isVoidType(returnType)
					&& !JpaCodeBlocks.QueryExecutionBlockBuilder.returnsModifying(returnType)) {
				return null;
			}
		}

		return MethodContributor.forQueryMethod(queryMethod).contribute(context -> {

			CodeBlock.Builder body = CodeBlock.builder();

			MergedAnnotation<Query> query = context.getAnnotation(Query.class);
			MergedAnnotation<NativeQuery> nativeQuery = context.getAnnotation(NativeQuery.class);
			MergedAnnotation<QueryHints> queryHints = context.getAnnotation(QueryHints.class);
			MergedAnnotation<Modifying> modifying = context.getAnnotation(Modifying.class);
			ReturnedType returnedType = context.getReturnedType();

			body.add(context.codeBlocks().logDebug("invoking [%s]".formatted(context.getMethod().getName())));

			AotQueries aotQueries = getQueries(context, query, selector, queryMethod, returnedType);

			body.add(JpaCodeBlocks.queryBuilder(context, queryMethod).filter(aotQueries)
					.queryReturnType(getQueryReturnType(aotQueries.result(), returnedType, context)).query(query)
					.nativeQuery(nativeQuery).queryHints(queryHints).build());

			body.add(
					JpaCodeBlocks.executionBuilder(context, queryMethod).modifying(modifying).query(aotQueries.result()).build());

			return body.build();
		});
	}

	private AotQueries getQueries(AotQueryMethodGenerationContext context, MergedAnnotation<Query> query,
			QueryEnhancerSelector selector, JpaQueryMethod queryMethod, ReturnedType returnedType) {

		if (query.isPresent() && StringUtils.hasText(query.getString("value"))) {
			return buildStringQuery(context.getRepositoryInformation().getDomainType(), returnedType, selector, query,
					queryMethod);
		}

		TypedQueryReference<?> namedQuery = getNamedQuery(returnedType, queryMethod.getNamedQueryName());
		if (namedQuery != null) {
			return buildNamedQuery(returnedType, selector, namedQuery, query, queryMethod);
		}

		return buildPartTreeQuery(returnedType, context, query, queryMethod);
	}

	private AotQueries buildStringQuery(Class<?> domainType, ReturnedType returnedType, QueryEnhancerSelector selector,
			MergedAnnotation<Query> query, JpaQueryMethod queryMethod) {

		UnaryOperator<String> operator = s -> s.replaceAll("#\\{#entityName}", domainType.getName());
		boolean isNative = query.getBoolean("nativeQuery");
		Function<String, StringAotQuery> queryFunction = isNative ? StringAotQuery::nativeQuery
				: StringAotQuery::jpqlQuery;
		queryFunction = operator.andThen(queryFunction);

		String queryString = query.getString("value");

		StringAotQuery aotStringQuery = queryFunction.apply(queryString);
		String countQuery = query.getString("countQuery");

		EntityQuery entityQuery = EntityQuery.create(aotStringQuery.getQuery(), selector);
		if (entityQuery.hasConstructorExpression() || entityQuery.isDefaultProjection()) {
			aotStringQuery = aotStringQuery.withReturnsDeclaredMethodType();
		}

		if (StringUtils.hasText(countQuery)) {
			return AotQueries.from(aotStringQuery, queryFunction.apply(countQuery));
		}

		String namedCountQueryName = queryMethod.getNamedCountQueryName();
		TypedQueryReference<?> namedCountQuery = getNamedQuery(returnedType, namedCountQueryName);
		if (namedCountQuery != null) {
			return AotQueries.from(aotStringQuery, buildNamedAotQuery(namedCountQuery, queryMethod, isNative));
		}

		String countProjection = query.getString("countProjection");
		return AotQueries.from(aotStringQuery, countProjection, selector);
	}

	private AotQueries buildNamedQuery(ReturnedType returnedType, QueryEnhancerSelector selector,
			TypedQueryReference<?> namedQuery, MergedAnnotation<Query> query, JpaQueryMethod queryMethod) {

		NamedAotQuery aotQuery = buildNamedAotQuery(namedQuery, queryMethod,
				query.isPresent() && query.getBoolean("nativeQuery"));

		String countQuery = query.isPresent() ? query.getString("countQuery") : null;
		if (StringUtils.hasText(countQuery)) {
			return AotQueries.from(aotQuery,
					aotQuery.isNative() ? StringAotQuery.nativeQuery(countQuery) : StringAotQuery.jpqlQuery(countQuery));
		}

		TypedQueryReference<?> namedCountQuery = getNamedQuery(returnedType, queryMethod.getNamedCountQueryName());

		if (namedCountQuery != null) {
			return AotQueries.from(aotQuery, buildNamedAotQuery(namedCountQuery, queryMethod, aotQuery.isNative()));
		}

		String countProjection = query.isPresent() ? query.getString("countProjection") : null;
		return AotQueries.from(aotQuery, it -> {
			return StringAotQuery.of(aotQuery.getQueryString()).getQuery();
		}, countProjection, selector);
	}

	private NamedAotQuery buildNamedAotQuery(TypedQueryReference<?> namedQuery, JpaQueryMethod queryMethod,
			boolean isNative) {

		QueryExtractor queryExtractor = queryMethod.getQueryExtractor();
		String queryString = queryExtractor.extractQueryString(namedQuery);

		if (!isNative) {
			isNative = queryExtractor.isNativeQuery(namedQuery);
		}

		Assert.hasText(queryString, () -> "Cannot extract Query from named query [%s]".formatted(namedQuery.getName()));

		return NamedAotQuery.named(namedQuery.getName(),
				isNative ? DeclaredQuery.nativeQuery(queryString) : DeclaredQuery.jpqlQuery(queryString));
	}

	private @Nullable TypedQueryReference<?> getNamedQuery(ReturnedType returnedType, String queryName) {

		List<Class<?>> candidates = Arrays.asList(Object.class, returnedType.getDomainType(),
				returnedType.getReturnedType(), returnedType.getTypeToRead(), void.class, null, Long.class, Integer.class,
				Long.TYPE, Integer.TYPE, Number.class);

		EntityManagerFactory emf = metaModel.getEntityManagerFactory();

		for (Class<?> candidate : candidates) {

			Map<String, ? extends TypedQueryReference<?>> namedQueries = emf.getNamedQueries(candidate);

			if (namedQueries.containsKey(queryName)) {
				return namedQueries.get(queryName);
			}
		}

		return null;
	}

	private AotQueries buildPartTreeQuery(ReturnedType returnedType, AotQueryMethodGenerationContext context,
			MergedAnnotation<Query> query, JpaQueryMethod queryMethod) {

		PartTree partTree = new PartTree(context.getMethod().getName(), context.getRepositoryInformation().getDomainType());
		// TODO make configurable
		JpqlQueryTemplates templates = JpqlQueryTemplates.UPPER;

		AotQuery aotQuery = createQuery(partTree, returnedType, queryMethod.getParameters(), templates);

		if (query.isPresent() && StringUtils.hasText(query.getString("countQuery"))) {
			return AotQueries.from(aotQuery, StringAotQuery.jpqlQuery(query.getString("countQuery")));
		}

		TypedQueryReference<?> namedCountQuery = getNamedQuery(returnedType, queryMethod.getNamedCountQueryName());
		if (namedCountQuery != null) {
			return AotQueries.from(aotQuery, buildNamedAotQuery(namedCountQuery, queryMethod, false));
		}

		AotQuery partTreeCountQuery = createCountQuery(partTree, returnedType, queryMethod.getParameters(), templates);
		return AotQueries.from(aotQuery, partTreeCountQuery);
	}

	private AotQuery createQuery(PartTree partTree, ReturnedType returnedType, JpaParameters parameters,
			JpqlQueryTemplates templates) {

		ParameterMetadataProvider metadataProvider = new ParameterMetadataProvider(parameters, EscapeCharacter.DEFAULT,
				templates);
		JpaQueryCreator queryCreator = new JpaQueryCreator(partTree, returnedType, metadataProvider, templates, metaModel);

		return StringAotQuery.jpqlQuery(queryCreator.createQuery(), metadataProvider.getBindings(),
				partTree.getResultLimit(), partTree.isDelete(), partTree.isExistsProjection());
	}

	private AotQuery createCountQuery(PartTree partTree, ReturnedType returnedType, JpaParameters parameters,
			JpqlQueryTemplates templates) {

		ParameterMetadataProvider metadataProvider = new ParameterMetadataProvider(parameters, EscapeCharacter.DEFAULT,
				templates);
		JpaQueryCreator queryCreator = new JpaCountQueryCreator(partTree, returnedType, metadataProvider, templates,
				metaModel);

		return StringAotQuery.jpqlQuery(queryCreator.createQuery(), metadataProvider.getBindings(), null, false, false);
	}

	private static @Nullable Class<?> getQueryReturnType(AotQuery query, ReturnedType returnedType,
			AotQueryMethodGenerationContext context) {

		Method method = context.getMethod();
		RepositoryInformation repositoryInformation = context.getRepositoryInformation();

		Class<?> methodReturnType = repositoryInformation.getReturnedDomainClass(method);
		boolean queryForEntity = repositoryInformation.getDomainType().isAssignableFrom(methodReturnType);

		Class<?> result = queryForEntity ? returnedType.getDomainType() : null;

		if (query instanceof StringAotQuery sq && sq.returnsDeclaredMethodType()) {
			return result;
		}

		if (returnedType.isProjecting()) {

			if (returnedType.getReturnedType().isInterface()) {
				return Tuple.class;
			}

			return returnedType.getReturnedType();
		}

		return result;
	}

}
