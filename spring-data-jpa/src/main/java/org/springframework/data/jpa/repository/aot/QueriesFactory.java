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

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;
import org.springframework.data.jpa.repository.query.*;
import org.springframework.data.jpa.repository.support.JpqlQueryTemplates;
import org.springframework.data.repository.aot.generate.AotQueryMethodGenerationContext;
import org.springframework.data.repository.config.PropertiesBasedNamedQueriesFactoryBean;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.PropertiesBasedNamedQueries;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Factory for {@link AotQueries}.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 4.0
 */
class QueriesFactory {

	private final EntityManagerFactory entityManagerFactory;
	private final NamedQueries namedQueries;
	private final Metamodel metamodel;
	private final EscapeCharacter escapeCharacter;
	private final JpqlQueryTemplates templates = JpqlQueryTemplates.UPPER;

	public QueriesFactory(RepositoryConfigurationSource configurationSource, EntityManagerFactory entityManagerFactory,
			ClassLoader classLoader) {
		this(configurationSource, entityManagerFactory, entityManagerFactory.getMetamodel(), classLoader);
	}

	public QueriesFactory(RepositoryConfigurationSource configurationSource, EntityManagerFactory entityManagerFactory,
			Metamodel metamodel, ClassLoader classLoader) {

		this.metamodel = metamodel;
		this.namedQueries = getNamedQueries(configurationSource, classLoader);
		this.entityManagerFactory = entityManagerFactory;

		Optional<Character> escapeCharacter = configurationSource.getAttribute("escapeCharacter", Character.class);
		this.escapeCharacter = escapeCharacter.map(EscapeCharacter::of).orElse(EscapeCharacter.DEFAULT);
	}

	private NamedQueries getNamedQueries(@Nullable RepositoryConfigurationSource configSource, ClassLoader classLoader) {

		String location = configSource != null ? configSource.getNamedQueryLocation().orElse(null) : null;

		if (location == null) {
			location = new JpaRepositoryConfigExtension().getDefaultNamedQueryLocation();
		}

		if (StringUtils.hasText(location)) {

			try {

				PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classLoader);

				PropertiesBasedNamedQueriesFactoryBean factoryBean = new PropertiesBasedNamedQueriesFactoryBean();
				factoryBean.setLocations(resolver.getResources(location));
				factoryBean.afterPropertiesSet();
				return Objects.requireNonNull(factoryBean.getObject());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		return new PropertiesBasedNamedQueries(new Properties());
	}

	/**
	 * Creates the {@link AotQueries} used within a specific {@link JpaQueryMethod}.
	 *
	 * @param repositoryInformation
	 * @param returnedType
	 * @param selector
	 * @param query
	 * @param queryMethod
	 * @return
	 */
	public AotQueries createQueries(RepositoryInformation repositoryInformation, ReturnedType returnedType,
			QueryEnhancerSelector selector, MergedAnnotation<Query> query, JpaQueryMethod queryMethod) {

		if (query.isPresent() && StringUtils.hasText(query.getString("value"))) {
			return buildStringQuery(returnedType, selector, query, queryMethod);
		}

		String queryName = queryMethod.getNamedQueryName();
		if (hasNamedQuery(returnedType, queryName)) {
			return buildNamedQuery(returnedType, selector, queryName, query, queryMethod);
		}

		return buildPartTreeQuery(repositoryInformation, returnedType, selector, query, queryMethod);
	}

	private boolean hasNamedQuery(ReturnedType returnedType, String queryName) {
		return namedQueries.hasQuery(queryName) || getNamedQuery(returnedType, queryName) != null;
	}

	private AotQueries buildStringQuery(ReturnedType returnedType, QueryEnhancerSelector selector,
			MergedAnnotation<Query> query, JpaQueryMethod queryMethod) {

		UnaryOperator<String> operator = s -> s.replaceAll("#\\{#entityName}", queryMethod.getEntityInformation().getEntityName());
		boolean isNative = query.getBoolean("nativeQuery");
		Function<String, DeclaredQuery> queryFunction = isNative ? DeclaredQuery::nativeQuery : DeclaredQuery::jpqlQuery;
		queryFunction = operator.andThen(queryFunction);

		String queryString = query.getString("value");

		EntityQuery entityQuery = EntityQuery.create(queryFunction.apply(queryString), selector);
		StringAotQuery aotStringQuery = StringAotQuery.of(entityQuery);
		String countQuery = query.getString("countQuery");

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
			return AotQueries.from(aotStringQuery, StringAotQuery.of(queryFunction.apply(countQuery)));
		}

		if (hasNamedQuery(returnedType, queryMethod.getNamedCountQueryName())) {
			return AotQueries.from(aotStringQuery,
					createNamedAotQuery(returnedType, selector, queryMethod.getNamedCountQueryName(), queryMethod, isNative));
		}

		if (queryMethod.isModifyingQuery()) {

		}

		String countProjection = query.getString("countProjection");
		return AotQueries.withDerivedCountQuery(aotStringQuery, StringAotQuery::getQuery, countProjection, selector);
	}

	private AotQueries buildNamedQuery(ReturnedType returnedType, QueryEnhancerSelector selector, String queryName,
			MergedAnnotation<Query> query, JpaQueryMethod queryMethod) {

		boolean nativeQuery = query.isPresent() && query.getBoolean("nativeQuery");
		AotQuery aotQuery = createNamedAotQuery(returnedType, selector, queryName, queryMethod, nativeQuery);
		String countQuery = query.isPresent() ? query.getString("countQuery") : null;

		if (StringUtils.hasText(countQuery)) {
			return AotQueries.from(aotQuery,
					StringAotQuery
							.of(aotQuery.isNative() ? DeclaredQuery.nativeQuery(countQuery) : DeclaredQuery.jpqlQuery(countQuery)));
		}

		if (hasNamedQuery(returnedType, queryMethod.getNamedCountQueryName())) {
			return AotQueries.from(aotQuery,
					createNamedAotQuery(returnedType, selector, queryMethod.getNamedCountQueryName(), queryMethod, nativeQuery));
		}

		String countProjection = query.isPresent() ? query.getString("countProjection") : null;
		return AotQueries.withDerivedCountQuery(aotQuery, it -> {

			if (it instanceof StringAotQuery sq) {
				return sq.getQuery();
			}

			return ((NamedAotQuery) aotQuery).getQuery();
		}, countProjection, selector);
	}

	private AotQuery createNamedAotQuery(ReturnedType returnedType, QueryEnhancerSelector selector, String queryName,
			JpaQueryMethod queryMethod, boolean isNative) {

		if (namedQueries.hasQuery(queryName)) {

			String queryString = namedQueries.getQuery(queryName);

			DeclaredQuery query = isNative ? DeclaredQuery.nativeQuery(queryString) : DeclaredQuery.jpqlQuery(queryString);
			return StringAotQuery.named(queryName, EntityQuery.create(query, selector));
		}

		TypedQueryReference<?> namedQuery = getNamedQuery(returnedType, queryName);

		Assert.state(namedQuery != null, "Native named query must not be null");

		return createNamedAotQuery(namedQuery, selector, isNative, queryMethod);
	}

	private AotQuery createNamedAotQuery(TypedQueryReference<?> namedQuery, QueryEnhancerSelector selector,
			boolean isNative, JpaQueryMethod queryMethod) {

		QueryExtractor queryExtractor = queryMethod.getQueryExtractor();
		String queryString = queryExtractor.extractQueryString(namedQuery);

		if (!isNative) {
			isNative = queryExtractor.isNativeQuery(namedQuery);
		}

		Assert.hasText(queryString, () -> "Cannot extract Query from named query [%s]".formatted(namedQuery.getName()));

		DeclaredQuery query = isNative ? DeclaredQuery.nativeQuery(queryString) : DeclaredQuery.jpqlQuery(queryString);

		return NamedAotQuery.named(namedQuery.getName(), EntityQuery.create(query, selector));
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

	private AotQueries buildPartTreeQuery(RepositoryInformation repositoryInformation, ReturnedType returnedType,
			QueryEnhancerSelector selector,
			MergedAnnotation<Query> query, JpaQueryMethod queryMethod) {

		PartTree partTree = new PartTree(queryMethod.getName(), repositoryInformation.getDomainType());
		AotQuery aotQuery = createQuery(partTree, returnedType, queryMethod.getParameters(), templates,
				queryMethod.getEntityInformation());

		if (query.isPresent() && StringUtils.hasText(query.getString("countQuery"))) {
			return AotQueries.from(aotQuery, StringAotQuery.of(DeclaredQuery.jpqlQuery(query.getString("countQuery"))));
		}

		if (hasNamedQuery(returnedType, queryMethod.getNamedCountQueryName())) {
			return AotQueries.from(aotQuery,
					createNamedAotQuery(returnedType, selector, queryMethod.getNamedCountQueryName(), queryMethod, false));
		}

		AotQuery partTreeCountQuery = createCountQuery(partTree, returnedType, queryMethod.getParameters(), templates,
				queryMethod.getEntityInformation());
		return AotQueries.from(aotQuery, partTreeCountQuery);
	}

	private AotQuery createQuery(PartTree partTree, ReturnedType returnedType, JpaParameters parameters,
			JpqlQueryTemplates templates, JpaEntityMetadata<?> entityMetadata) {

		ParameterMetadataProvider metadataProvider = new ParameterMetadataProvider(parameters, escapeCharacter, templates);
		JpaQueryCreator queryCreator = new JpaQueryCreator(partTree, false, returnedType, metadataProvider, templates,
				entityMetadata, metamodel);

		return StringAotQuery.jpqlQuery(queryCreator.createQuery(), metadataProvider.getBindings(),
				partTree.getResultLimit(), partTree.isDelete(), partTree.isExistsProjection());
	}

	private AotQuery createCountQuery(PartTree partTree, ReturnedType returnedType, JpaParameters parameters,
			JpqlQueryTemplates templates, JpaEntityMetadata<?> entityMetadata) {

		ParameterMetadataProvider metadataProvider = new ParameterMetadataProvider(parameters, escapeCharacter, templates);
		JpaQueryCreator queryCreator = new JpaCountQueryCreator(partTree, returnedType, metadataProvider, templates,
				entityMetadata, metamodel);

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

		if (returnedType.isProjecting()) {

			if (returnedType.getReturnedType().isInterface()) {

				if (query.hasConstructorExpressionOrDefaultProjection()) {
					return result;
				}

				return Tuple.class;
			}

			return returnedType.getReturnedType();
		}

		return result;
	}

}
