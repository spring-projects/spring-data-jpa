/*
 * Copyright 2008-2023 the original author or authors.
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

import jakarta.persistence.EntityManager;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryRewriter;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethod;
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
 * @author Greg Turnquist
 */
public final class JpaQueryLookupStrategy {

	private static final Log LOG = LogFactory.getLog(JpaQueryLookupStrategy.class);

	/**
	 * A null-value instance used to signal if no declared query could be found. It checks many different formats before
	 * falling through to this value object.
	 */
	private static final RepositoryQuery NO_QUERY = new NoQuery();

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
		private final QueryRewriterProvider queryRewriterProvider;

		/**
		 * Creates a new {@link AbstractQueryLookupStrategy}.
		 *
		 * @param em must not be {@literal null}.
		 * @param queryMethodFactory must not be {@literal null}.
		 */
		public AbstractQueryLookupStrategy(EntityManager em, JpaQueryMethodFactory queryMethodFactory,
				QueryRewriterProvider queryRewriterProvider) {

			Assert.notNull(em, "EntityManager must not be null");
			Assert.notNull(queryMethodFactory, "JpaQueryMethodFactory must not be null");

			this.em = em;
			this.queryMethodFactory = queryMethodFactory;
			this.queryRewriterProvider = queryRewriterProvider;
		}

		@Override
		public final RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
				NamedQueries namedQueries) {
			JpaQueryMethod queryMethod = queryMethodFactory.build(method, metadata, factory);
			return resolveQuery(queryMethod, queryRewriterProvider.getQueryRewriter(queryMethod), em, namedQueries);
		}

		protected abstract RepositoryQuery resolveQuery(JpaQueryMethod method, QueryRewriter queryRewriter,
				EntityManager em, NamedQueries namedQueries);

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
				QueryRewriterProvider queryRewriterProvider, EscapeCharacter escape) {

			super(em, queryMethodFactory, queryRewriterProvider);

			this.escape = escape;
		}

		@Override
		protected RepositoryQuery resolveQuery(JpaQueryMethod method, QueryRewriter queryRewriter, EntityManager em,
				NamedQueries namedQueries) {
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
				QueryMethodEvaluationContextProvider evaluationContextProvider, QueryRewriterProvider queryRewriterProvider) {

			super(em, queryMethodFactory, queryRewriterProvider);

			this.evaluationContextProvider = evaluationContextProvider;
		}

		@Override
		protected RepositoryQuery resolveQuery(JpaQueryMethod method, QueryRewriter queryRewriter, EntityManager em,
				NamedQueries namedQueries) {

			if (method.isProcedureQuery()) {
				return JpaQueryFactory.INSTANCE.fromProcedureAnnotation(method, em);
			}

			if (StringUtils.hasText(method.getAnnotatedQuery())) {

				if (method.hasAnnotatedQueryName()) {
					LOG.warn(String.format(
							"Query method %s is annotated with both, a query and a query name; Using the declared query", method));
				}

				return JpaQueryFactory.INSTANCE.fromMethodWithQueryString(method, em, method.getRequiredAnnotatedQuery(),
						getCountQuery(method, namedQueries, em), queryRewriter, evaluationContextProvider);
			}

			String name = method.getNamedQueryName();
			if (namedQueries.hasQuery(name)) {
				return JpaQueryFactory.INSTANCE.fromMethodWithQueryString(method, em, namedQueries.getQuery(name),
						getCountQuery(method, namedQueries, em), queryRewriter, evaluationContextProvider);
			}

			RepositoryQuery query = NamedQuery.lookupFrom(method, em);

			return query != null //
					? query //
					: NO_QUERY;
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
				CreateQueryLookupStrategy createStrategy, DeclaredQueryLookupStrategy lookupStrategy,
				QueryRewriterProvider queryRewriterProvider) {

			super(em, queryMethodFactory, queryRewriterProvider);

			Assert.notNull(createStrategy, "CreateQueryLookupStrategy must not be null");
			Assert.notNull(lookupStrategy, "DeclaredQueryLookupStrategy must not be null");

			this.createStrategy = createStrategy;
			this.lookupStrategy = lookupStrategy;
		}

		@Override
		protected RepositoryQuery resolveQuery(JpaQueryMethod method, QueryRewriter queryRewriter, EntityManager em,
				NamedQueries namedQueries) {

			RepositoryQuery lookupQuery = lookupStrategy.resolveQuery(method, queryRewriter, em, namedQueries);

			if (lookupQuery != NO_QUERY) {
				return lookupQuery;
			}

			return createStrategy.resolveQuery(method, queryRewriter, em, namedQueries);
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
			@Nullable Key key, QueryMethodEvaluationContextProvider evaluationContextProvider,
			QueryRewriterProvider queryRewriterProvider, EscapeCharacter escape) {

		Assert.notNull(em, "EntityManager must not be null");
		Assert.notNull(evaluationContextProvider, "EvaluationContextProvider must not be null");

		switch (key != null ? key : Key.CREATE_IF_NOT_FOUND) {
			case CREATE:
				return new CreateQueryLookupStrategy(em, queryMethodFactory, queryRewriterProvider, escape);
			case USE_DECLARED_QUERY:
				return new DeclaredQueryLookupStrategy(em, queryMethodFactory, evaluationContextProvider,
						queryRewriterProvider);
			case CREATE_IF_NOT_FOUND:
				return new CreateIfNotFoundQueryLookupStrategy(em, queryMethodFactory,
						new CreateQueryLookupStrategy(em, queryMethodFactory, queryRewriterProvider, escape),
						new DeclaredQueryLookupStrategy(em, queryMethodFactory, evaluationContextProvider, queryRewriterProvider),
						queryRewriterProvider);
			default:
				throw new IllegalArgumentException(String.format("Unsupported query lookup strategy %s", key));
		}
	}

	/**
	 * A null value type that represents the lack of a defined query.
	 */
	static class NoQuery implements RepositoryQuery {

		@Override
		public Object execute(Object[] parameters) {
			throw new IllegalStateException("NoQuery should not be executed!");
		}

		@Override
		public QueryMethod getQueryMethod() {
			throw new IllegalStateException("NoQuery does not have a QueryMethod!");
		}
	}
}
