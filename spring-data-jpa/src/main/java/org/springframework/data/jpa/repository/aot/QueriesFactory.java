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

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.metamodel.Metamodel;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.*;
import org.springframework.data.jpa.repository.support.JpqlQueryTemplates;
import org.springframework.data.repository.aot.generate.AotQueryMethodGenerationContext;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Factory for {@link AotQueries}.
 *
 * @author Mark Paluch
 * @since 4.0
 */
class QueriesFactory {

	private final EntityManagerFactory entityManagerFactory;
	private final Metamodel metamodel;

	public QueriesFactory(EntityManagerFactory entityManagerFactory) {
		this(entityManagerFactory, entityManagerFactory.getMetamodel());
	}

	public QueriesFactory(EntityManagerFactory entityManagerFactory, Metamodel metamodel) {
		this.metamodel = metamodel;
		this.entityManagerFactory = entityManagerFactory;
	}

	/**
	 * Creates the {@link AotQueries} used within a specific {@link JpaQueryMethod}.
	 *
	 * @param context
	 * @param query
	 * @param selector
	 * @param queryMethod
	 * @param returnedType
	 * @return
	 */
	public AotQueries createQueries(AotQueryMethodGenerationContext context, MergedAnnotation<Query> query,
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
			aotStringQuery = aotStringQuery.withConstructorExpressionOrDefaultProjection();
		}

		if (returnedType.isProjecting() && returnedType.hasInputProperties()
				&& !returnedType.getReturnedType().isInterface()) {

			QueryProvider rewritten = entityQuery.rewrite(new QueryEnhancer.QueryRewriteInformation() {
				@Override
				public Sort getSort() {
					return Sort.unsorted();
				}

				@Override
				public ReturnedType getReturnedType() {
					return returnedType;
				}
			});

			aotStringQuery = aotStringQuery.rewrite(rewritten);
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

			Map<String, ? extends TypedQueryReference<?>> namedQueries = entityManagerFactory.getNamedQueries(candidate);

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
		JpaQueryCreator queryCreator = new JpaQueryCreator(partTree, returnedType, metadataProvider, templates, metamodel);

		return StringAotQuery.jpqlQuery(queryCreator.createQuery(), metadataProvider.getBindings(),
				partTree.getResultLimit(), partTree.isDelete(), partTree.isExistsProjection());
	}

	private AotQuery createCountQuery(PartTree partTree, ReturnedType returnedType, JpaParameters parameters,
			JpqlQueryTemplates templates) {

		ParameterMetadataProvider metadataProvider = new ParameterMetadataProvider(parameters, EscapeCharacter.DEFAULT,
				templates);
		JpaQueryCreator queryCreator = new JpaCountQueryCreator(partTree, returnedType, metadataProvider, templates,
				metamodel);

		return StringAotQuery.jpqlQuery(queryCreator.createQuery(), metadataProvider.getBindings(), Limit.unlimited(),
				false, false);
	}

	public static @Nullable Class<?> getQueryReturnType(AotQuery query, ReturnedType returnedType,
			AotQueryMethodGenerationContext context) {

		Method method = context.getMethod();
		RepositoryInformation repositoryInformation = context.getRepositoryInformation();

		Class<?> methodReturnType = repositoryInformation.getReturnedDomainClass(method);
		boolean queryForEntity = repositoryInformation.getDomainType().isAssignableFrom(methodReturnType);

		Class<?> result = queryForEntity ? returnedType.getDomainType() : null;

		if (query instanceof StringAotQuery sq && sq.hasConstructorExpressionOrDefaultProjection()) {
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
