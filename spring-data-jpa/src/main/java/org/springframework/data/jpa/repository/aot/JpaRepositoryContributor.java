/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.data.jpa.repository.aot;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.metamodel.Metamodel;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.EntityGraph;
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
import org.springframework.data.util.TypeInformation;
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

	private final EntityManagerFactory emf;
	private final Metamodel metaModel;
	private final PersistenceProvider persistenceProvider;

	public JpaRepositoryContributor(AotRepositoryContext repositoryContext) {
		super(repositoryContext);
		AotMetamodel amm = new AotMetamodel(repositoryContext.getResolvedTypes());
		this.metaModel = amm;
		this.emf = amm.getEntityManagerFactory();
		this.persistenceProvider = PersistenceProvider.fromEntityManagerFactory(amm.getEntityManagerFactory());
	}

	public JpaRepositoryContributor(AotRepositoryContext repositoryContext, EntityManagerFactory entityManagerFactory) {
		super(repositoryContext);
		this.emf = entityManagerFactory;
		this.metaModel = entityManagerFactory.getMetamodel();
		this.persistenceProvider = PersistenceProvider.fromEntityManagerFactory(entityManagerFactory);
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

		ReturnedType returnedType = queryMethod.getResultProcessor().getReturnedType();

		// no interface/dynamic projections for now.
		if (returnedType.isProjecting() && returnedType.getReturnedType().isInterface()) {
			return null;
		}

		if (queryMethod.getParameters().hasDynamicProjection()) {
			return null;
		}

		// no KeysetScrolling for now.
		if (queryMethod.getParameters().hasScrollPositionParameter()) {
			return null;
		}

		if (queryMethod.isModifyingQuery()) {

			TypeInformation<?> returnType = repositoryInformation.getReturnType(method);

			boolean returnsCount = JpaCodeBlocks.QueryExecutionBlockBuilder.returnsModifying(returnType.getType());

			boolean isVoid = ClassUtils.isVoidType(returnType.getType());

			if (!returnsCount && !isVoid) {
				return null;
			}
		}

		return MethodContributor.forQueryMethod(queryMethod).contribute(context -> {

			CodeBlock.Builder body = CodeBlock.builder();

			MergedAnnotation<Query> query = context.getAnnotation(Query.class);
			MergedAnnotation<NativeQuery> nativeQuery = context.getAnnotation(NativeQuery.class);
			MergedAnnotation<QueryHints> queryHints = context.getAnnotation(QueryHints.class);
			MergedAnnotation<EntityGraph> entityGraph = context.getAnnotation(EntityGraph.class);
			MergedAnnotation<Modifying> modifying = context.getAnnotation(Modifying.class);

			body.add(context.codeBlocks().logDebug("invoking [%s]".formatted(context.getMethod().getName())));

			AotQueries aotQueries = getQueries(context, query, selector, queryMethod, returnedType);
			AotEntityGraph aotEntityGraph = getAotEntityGraph(entityGraph, repositoryInformation, returnedType, queryMethod);

			body.add(JpaCodeBlocks.queryBuilder(context, queryMethod).filter(aotQueries)
					.queryReturnType(getQueryReturnType(aotQueries.result(), returnedType, context)).nativeQuery(nativeQuery)
					.queryHints(queryHints).entityGraph(aotEntityGraph).build());

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
		Function<String, StringAotQuery> queryFunction = isNative ? StringAotQuery::nativeQuery : StringAotQuery::jpqlQuery;
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

	@SuppressWarnings("unchecked")
	private @Nullable AotEntityGraph getAotEntityGraph(MergedAnnotation<EntityGraph> entityGraph,
			RepositoryInformation information, ReturnedType returnedType, JpaQueryMethod queryMethod) {

		if (!entityGraph.isPresent()) {
			return null;
		}

		EntityGraph.EntityGraphType type = entityGraph.getEnum("type", EntityGraph.EntityGraphType.class);
		String[] attributePaths = entityGraph.getStringArray("attributePaths");
		Collection<String> entityGraphNames = getEntityGraphNames(entityGraph, information, queryMethod);
		List<Class<?>> candidates = Arrays.asList(returnedType.getDomainType(), returnedType.getReturnedType(),
				returnedType.getTypeToRead());

		for (Class<?> candidate : candidates) {

			Map<String, jakarta.persistence.EntityGraph<?>> namedEntityGraphs = emf
					.getNamedEntityGraphs(Class.class.cast(candidate));

			if (namedEntityGraphs.isEmpty()) {
				continue;
			}

			for (String entityGraphName : entityGraphNames) {
				if (namedEntityGraphs.containsKey(entityGraphName)) {
					return new AotEntityGraph(entityGraphName, type, Collections.emptyList());
				}
			}
		}

		if (attributePaths.length > 0) {
			return new AotEntityGraph(null, type, Arrays.asList(attributePaths));
		}

		return null;
	}

	private Set<String> getEntityGraphNames(MergedAnnotation<EntityGraph> entityGraph, RepositoryInformation information,
			JpaQueryMethod queryMethod) {

		Set<String> entityGraphNames = new LinkedHashSet<>();
		String value = entityGraph.getString("value");

		if (StringUtils.hasText(value)) {
			entityGraphNames.add(value);
		}
		entityGraphNames.add(queryMethod.getNamedQueryName());
		entityGraphNames.add(getFallbackEntityGraphName(information, queryMethod));
		return entityGraphNames;
	}

	private String getFallbackEntityGraphName(RepositoryInformation information, JpaQueryMethod queryMethod) {

		Class<?> domainType = information.getDomainType();
		Entity entity = AnnotatedElementUtils.findMergedAnnotation(domainType, Entity.class);
		String entityName = entity != null && StringUtils.hasText(entity.name()) ? entity.name()
				: domainType.getSimpleName();

		return entityName + "." + queryMethod.getName();
	}

}
