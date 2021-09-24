/*
 * Copyright 2008-2021 the original author or authors.
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
package org.springframework.data.jpa.repository.query;

import java.lang.reflect.Method;

import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Query lookup strategy to execute finders.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 * @author Réda Housni Alaoui
 */
public final class JpaQueryLookupStrategy {

	private static final Log LOG = LogFactory.getLog(JpaQueryLookupStrategy.class);

	/**
	 * Private constructor to prevent instantiation.
	 */
	private JpaQueryLookupStrategy() {}

	/**
	 * Base class for {@link QueryLookupStrategy} implementations that need access to an {@link EntityManager}.
	 *
	 * @author Oliver Gierke
	 * @author Thomas Darimont
	 */
	private abstract static class AbstractQueryLookupStrategy implements QueryLookupStrategy {

		private final EntityManager em;
		private final JpaQueryMethodFactory queryMethodFactory;

		/**
		 * Creates a new {@link AbstractQueryLookupStrategy}.
		 *
		 * @param em must not be {@literal null}.
		 * @param queryMethodFactory must not be {@literal null}.
		 */
		public AbstractQueryLookupStrategy(EntityManager em, JpaQueryMethodFactory queryMethodFactory) {

			Assert.notNull(em, "EntityManager must not be null!");
			Assert.notNull(queryMethodFactory, "JpaQueryMethodFactory must not be null!");

			this.em = em;
			this.queryMethodFactory = queryMethodFactory;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.query.QueryLookupStrategy#resolveQuery(java.lang.reflect.Method, org.springframework.data.repository.core.RepositoryMetadata, org.springframework.data.projection.ProjectionFactory, org.springframework.data.repository.core.NamedQueries)
		 */
		@Override
		public final RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
				NamedQueries namedQueries) {
			return resolveQuery(queryMethodFactory.build(method, metadata, factory), em, namedQueries);
		}

		protected abstract RepositoryQuery resolveQuery(JpaQueryMethod method, EntityManager em, NamedQueries namedQueries);
	}

	/**
	 * {@link QueryLookupStrategy} to create a query from the method name.
	 *
	 * @author Oliver Gierke
	 * @author Thomas Darimont
	 */
	private static class CreateQueryLookupStrategy extends AbstractQueryLookupStrategy {

		private final EscapeCharacter escape;

		public CreateQueryLookupStrategy(EntityManager em, JpaQueryMethodFactory queryMethodFactory,
				EscapeCharacter escape) {

			super(em, queryMethodFactory);

			this.escape = escape;
		}

		@Override
		protected RepositoryQuery resolveQuery(JpaQueryMethod method, EntityManager em, NamedQueries namedQueries) {
			return new PartTreeJpaQuery(method, em, escape);
		}
	}

	/**
	 * {@link QueryLookupStrategy} that tries to detect a declared query declared via {@link Query} annotation followed by
	 * a JPA named query lookup.
	 *
	 * @author Oliver Gierke
	 * @author Thomas Darimont
	 * @author Jens Schauder
	 */
	private static class DeclaredQueryLookupStrategy extends AbstractQueryLookupStrategy {

		private final QueryMethodEvaluationContextProvider evaluationContextProvider;

		/**
		 * Creates a new {@link DeclaredQueryLookupStrategy}.
		 *
		 * @param em must not be {@literal null}.
		 * @param queryMethodFactory must not be {@literal null}.
		 * @param evaluationContextProvider must not be {@literal null}.
		 */
		public DeclaredQueryLookupStrategy(EntityManager em, JpaQueryMethodFactory queryMethodFactory,
				QueryMethodEvaluationContextProvider evaluationContextProvider) {

			super(em, queryMethodFactory);

			this.evaluationContextProvider = evaluationContextProvider;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.query.JpaQueryLookupStrategy.AbstractQueryLookupStrategy#resolveQuery(org.springframework.data.jpa.repository.query.JpaQueryMethod, javax.persistence.EntityManager, org.springframework.data.repository.core.NamedQueries)
		 */
		@Override
		protected RepositoryQuery resolveQuery(JpaQueryMethod method, EntityManager em, NamedQueries namedQueries) {

			if (method.isProcedureQuery()) {
				return JpaQueryFactory.INSTANCE.fromProcedureAnnotation(method, em);
			}

			if (StringUtils.hasText(method.getAnnotatedQuery())) {

				if (method.hasAnnotatedQueryName()) {
					LOG.warn(String.format(
							"Query method %s is annotated with both, a query and a query name. Using the declared query.", method));
				}

				return JpaQueryFactory.INSTANCE.fromMethodWithQueryString(method, em, method.getRequiredAnnotatedQuery(),
						getCountQuery(method, namedQueries, em),
						evaluationContextProvider);
			}

			String name = method.getNamedQueryName();
			if (namedQueries.hasQuery(name)) {
				return JpaQueryFactory.INSTANCE.fromMethodWithQueryString(method, em, namedQueries.getQuery(name), getCountQuery(method, namedQueries, em),
						evaluationContextProvider);
			}

			RepositoryQuery query = NamedQuery.lookupFrom(method, em);

			if (null != query) {
				return query;
			}

			throw new IllegalStateException(
					String.format("Did neither find a NamedQuery nor an annotated query for method %s!", method));
		}

		@Nullable
		private String getCountQuery(JpaQueryMethod method, NamedQueries namedQueries, EntityManager em) {

			if (StringUtils.hasText(method.getCountQuery())) {
				return method.getCountQuery();
			}

			String queryName = method.getNamedCountQueryName();

			if (!StringUtils.hasText(queryName)) {
				return method.getCountQuery();
			}

			if (namedQueries.hasQuery(queryName)) {
				return namedQueries.getQuery(queryName);
			}

			boolean namedQuery = NamedQuery.hasNamedQuery(em, queryName);

			if (namedQuery) {
				return method.getQueryExtractor().extractQueryString(em.createNamedQuery(queryName));
			}

			return null;
		}
	}

	/**
	 * {@link QueryLookupStrategy} to try to detect a declared query first (
	 * {@link org.springframework.data.jpa.repository.Query}, JPA named query). In case none is found we fall back on
	 * query creation.
	 *
	 * @author Oliver Gierke
	 * @author Thomas Darimont
	 */
	private static class CreateIfNotFoundQueryLookupStrategy extends AbstractQueryLookupStrategy {

		private final DeclaredQueryLookupStrategy lookupStrategy;
		private final CreateQueryLookupStrategy createStrategy;

		/**
		 * Creates a new {@link CreateIfNotFoundQueryLookupStrategy}.
		 *
		 * @param em must not be {@literal null}.
		 * @param queryMethodFactory must not be {@literal null}.
		 * @param createStrategy must not be {@literal null}.
		 * @param lookupStrategy must not be {@literal null}.
		 */
		public CreateIfNotFoundQueryLookupStrategy(EntityManager em, JpaQueryMethodFactory queryMethodFactory,
				CreateQueryLookupStrategy createStrategy, DeclaredQueryLookupStrategy lookupStrategy) {

			super(em, queryMethodFactory);

			Assert.notNull(createStrategy, "CreateQueryLookupStrategy must not be null!");
			Assert.notNull(lookupStrategy, "DeclaredQueryLookupStrategy must not be null!");

			this.createStrategy = createStrategy;
			this.lookupStrategy = lookupStrategy;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.query.JpaQueryLookupStrategy.AbstractQueryLookupStrategy#resolveQuery(org.springframework.data.jpa.repository.query.JpaQueryMethod, javax.persistence.EntityManager, org.springframework.data.repository.core.NamedQueries)
		 */
		@Override
		protected RepositoryQuery resolveQuery(JpaQueryMethod method, EntityManager em, NamedQueries namedQueries) {

			try {
				return lookupStrategy.resolveQuery(method, em, namedQueries);
			} catch (IllegalStateException e) {
				return createStrategy.resolveQuery(method, em, namedQueries);
			}
		}
	}

	/**
	 * Creates a {@link QueryLookupStrategy} for the given {@link EntityManager} and {@link Key}.
	 *
	 * @param em must not be {@literal null}.
	 * @param queryMethodFactory must not be {@literal null}.
	 * @param key may be {@literal null}.
	 * @param evaluationContextProvider must not be {@literal null}.
	 * @param escape must not be {@literal null}.
	 */
	public static QueryLookupStrategy create(EntityManager em, JpaQueryMethodFactory queryMethodFactory,
			@Nullable Key key, QueryMethodEvaluationContextProvider evaluationContextProvider, EscapeCharacter escape) {

		Assert.notNull(em, "EntityManager must not be null!");
		Assert.notNull(evaluationContextProvider, "EvaluationContextProvider must not be null!");

		switch (key != null ? key : Key.CREATE_IF_NOT_FOUND) {
			case CREATE:
				return new CreateQueryLookupStrategy(em, queryMethodFactory, escape);
			case USE_DECLARED_QUERY:
				return new DeclaredQueryLookupStrategy(em, queryMethodFactory, evaluationContextProvider);
			case CREATE_IF_NOT_FOUND:
				return new CreateIfNotFoundQueryLookupStrategy(em, queryMethodFactory,
						new CreateQueryLookupStrategy(em, queryMethodFactory, escape),
						new DeclaredQueryLookupStrategy(em, queryMethodFactory, evaluationContextProvider));
			default:
				throw new IllegalArgumentException(String.format("Unsupported query lookup strategy %s!", key));
		}
	}
}
