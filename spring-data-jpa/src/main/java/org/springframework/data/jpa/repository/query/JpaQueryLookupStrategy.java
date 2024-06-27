/*
 * Copyright 2008-2025 the original author or authors.
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
import org.jspecify.annotations.Nullable;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryCreationException;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ValueExpressionDelegate;
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
		private final JpaQueryConfiguration configuration;

		/**
		 * Creates a new {@link AbstractQueryLookupStrategy}.
		 *
		 * @param em must not be {@literal null}.
		 * @param queryMethodFactory must not be {@literal null}.
		 * @param configuration must not be {@literal null}.
		 */
		public AbstractQueryLookupStrategy(EntityManager em, JpaQueryMethodFactory queryMethodFactory,
				JpaQueryConfiguration configuration) {

			this.em = em;
			this.queryMethodFactory = queryMethodFactory;
			this.configuration = configuration;
		}

		@Override
		public final RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
				NamedQueries namedQueries) {
			JpaQueryMethod queryMethod = queryMethodFactory.build(method, metadata, factory);
			return resolveQuery(queryMethod, configuration, em, namedQueries);
		}

		protected abstract RepositoryQuery resolveQuery(JpaQueryMethod method, JpaQueryConfiguration configuration,
				EntityManager em, NamedQueries namedQueries);

	}

	/**
	 * {@link QueryLookupStrategy} to create a query from the method name.
	 *
	 * @author Oliver Gierke
	 * @author Thomas Darimont
	 */
	private static class CreateQueryLookupStrategy extends AbstractQueryLookupStrategy {

		public CreateQueryLookupStrategy(EntityManager em, JpaQueryMethodFactory queryMethodFactory,
				JpaQueryConfiguration configuration) {

			super(em, queryMethodFactory, configuration);
		}

		@Override
		protected RepositoryQuery resolveQuery(JpaQueryMethod method, JpaQueryConfiguration configuration, EntityManager em,
				NamedQueries namedQueries) {
			return new PartTreeJpaQuery(method, em, configuration.getEscapeCharacter());
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
	static class DeclaredQueryLookupStrategy extends AbstractQueryLookupStrategy {

		/**
		 * Creates a new {@link DeclaredQueryLookupStrategy}.
		 *
		 * @param em must not be {@literal null}.
		 * @param queryMethodFactory must not be {@literal null}.
		 * @param configuration must not be {@literal null}.
		 */
		public DeclaredQueryLookupStrategy(EntityManager em, JpaQueryMethodFactory queryMethodFactory,
				JpaQueryConfiguration configuration) {

			super(em, queryMethodFactory, configuration);
		}

		@Override
		protected RepositoryQuery resolveQuery(JpaQueryMethod method, JpaQueryConfiguration configuration, EntityManager em,
				NamedQueries namedQueries) {

			if (method.isProcedureQuery()) {
				return createProcedureQuery(method, em);
			}

			if (StringUtils.hasText(method.getAnnotatedQuery())) {

				if (method.hasAnnotatedQueryName()) {
					LOG.warn(String.format(
							"Query method %s is annotated with both, a query and a query name; Using the declared query", method));
				}

				return createStringQuery(method, em, method.getRequiredAnnotatedQuery(),
						getCountQuery(method, namedQueries, em), configuration);
			}

			String name = method.getNamedQueryName();
			if (namedQueries.hasQuery(name)) {
				return createStringQuery(method, em, namedQueries.getQuery(name), getCountQuery(method, namedQueries, em),
						configuration);
			}

			RepositoryQuery query = NamedQuery.lookupFrom(method, em, configuration.getSelector());

			return query != null //
					? query //
					: NO_QUERY;
		}

		private @Nullable String getCountQuery(JpaQueryMethod method, NamedQueries namedQueries, EntityManager em) {

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

		/**
		 * Creates a {@link RepositoryQuery} from the given {@link String} query.
		 *
		 * @param method must not be {@literal null}.
		 * @param em must not be {@literal null}.
		 * @param queryString must not be {@literal null}.
		 * @param countQueryString must not be {@literal null}.
		 * @param configuration must not be {@literal null}.
		 * @return
		 */
		static AbstractJpaQuery createStringQuery(JpaQueryMethod method, EntityManager em, String queryString,
				@Nullable String countQueryString, JpaQueryConfiguration configuration) {

			if (method.isScrollQuery()) {
				throw QueryCreationException.create(method, "Scroll queries are not supported using String-based queries");
			}

			return method.isNativeQuery() ? new NativeJpaQuery(method, em, queryString, countQueryString, configuration)
					: new SimpleJpaQuery(method, em, queryString, countQueryString, configuration);
		}

		/**
		 * Creates a {@link StoredProcedureJpaQuery} from the given {@link JpaQueryMethod} query.
		 *
		 * @param method must not be {@literal null}.
		 * @param em must not be {@literal null}.
		 * @return
		 */
		static StoredProcedureJpaQuery createProcedureQuery(JpaQueryMethod method, EntityManager em) {

			if (method.isScrollQuery()) {
				throw QueryCreationException.create(method, "Scroll queries are not supported using stored procedures");
			}

			return new StoredProcedureJpaQuery(method, em);
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
		 * @param configuration must not be {@literal null}.
		 */
		public CreateIfNotFoundQueryLookupStrategy(EntityManager em, JpaQueryMethodFactory queryMethodFactory,
				CreateQueryLookupStrategy createStrategy, DeclaredQueryLookupStrategy lookupStrategy,
				JpaQueryConfiguration configuration) {

			super(em, queryMethodFactory, configuration);

			this.createStrategy = createStrategy;
			this.lookupStrategy = lookupStrategy;
		}

		@Override
		protected RepositoryQuery resolveQuery(JpaQueryMethod method, JpaQueryConfiguration configuration, EntityManager em,
				NamedQueries namedQueries) {

			RepositoryQuery lookupQuery = lookupStrategy.resolveQuery(method, configuration, em, namedQueries);

			if (lookupQuery != NO_QUERY) {
				return lookupQuery;
			}

			return createStrategy.resolveQuery(method, configuration, em, namedQueries);
		}
	}

	/**
	 * Creates a {@link QueryLookupStrategy} for the given {@link EntityManager} and {@link Key}.
	 *
	 * @param em must not be {@literal null}.
	 * @param queryMethodFactory must not be {@literal null}.
	 * @param key may be {@literal null}.
	 * @param configuration must not be {@literal null}.
	 */
	public static QueryLookupStrategy create(EntityManager em, JpaQueryMethodFactory queryMethodFactory,
			@Nullable Key key, JpaQueryConfiguration configuration) {

		Assert.notNull(em, "EntityManager must not be null");
		Assert.notNull(configuration, "JpaQueryConfiguration must not be null");

		return switch (key != null ? key : Key.CREATE_IF_NOT_FOUND) {
			case CREATE -> new CreateQueryLookupStrategy(em, queryMethodFactory, configuration);
			case USE_DECLARED_QUERY -> new DeclaredQueryLookupStrategy(em, queryMethodFactory, configuration);
			case CREATE_IF_NOT_FOUND -> new CreateIfNotFoundQueryLookupStrategy(em, queryMethodFactory,
					new CreateQueryLookupStrategy(em, queryMethodFactory, configuration),
					new DeclaredQueryLookupStrategy(em, queryMethodFactory, configuration), configuration);
			default -> throw new IllegalArgumentException(String.format("Unsupported query lookup strategy %s", key));
		};
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
