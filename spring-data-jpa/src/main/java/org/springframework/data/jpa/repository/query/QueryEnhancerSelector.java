/*
 * Copyright 2024 the original author or authors.
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

import org.springframework.data.jpa.provider.PersistenceProvider;

/**
 * Interface declaring a strategy to select a {@link QueryEnhancer} for a given {@link DeclaredQuery query}.
 * <p>
 * Enhancers are selected when introspecting a query to determine their selection, joins, aliases and other information
 * so that query methods can derive count queries, apply sorting and perform other transformations.
 *
 * @author Mark Paluch
 */
public interface QueryEnhancerSelector {

	/**
	 * Default selector strategy.
	 */
	QueryEnhancerSelector DEFAULT_SELECTOR = new DefaultQueryEnhancerSelector();

	/**
	 * Select a {@link QueryEnhancer} for a {@link DeclaredQuery query}.
	 *
	 * @param query
	 * @return
	 */
	QueryEnhancerFactory select(DeclaredQuery query);

	/**
	 * Default {@link QueryEnhancerSelector} implementation using class-path information to determine enhancer
	 * availability. Subclasses may provide a different configuration by using the protected constructor.
	 */
	class DefaultQueryEnhancerSelector implements QueryEnhancerSelector {

		protected static QueryEnhancerFactory DEFAULT_NATIVE;
		protected static QueryEnhancerFactory DEFAULT_JPQL;

		static {

			DEFAULT_NATIVE = QueryEnhancerFactories.jSqlParserPresent ? QueryEnhancerFactories.jsqlparser()
					: QueryEnhancerFactories.fallback();

			if (PersistenceProvider.HIBERNATE.isPresent()) {
				DEFAULT_JPQL = QueryEnhancerFactories.hql();
			} else if (PersistenceProvider.ECLIPSELINK.isPresent()) {
				DEFAULT_JPQL = QueryEnhancerFactories.eql();
			} else {
				DEFAULT_JPQL = QueryEnhancerFactories.jpql();
			}
		}

		private final QueryEnhancerFactory nativeQuery;
		private final QueryEnhancerFactory jpql;

		public DefaultQueryEnhancerSelector() {
			this(DEFAULT_NATIVE, DEFAULT_JPQL);
		}

		protected DefaultQueryEnhancerSelector(QueryEnhancerFactory nativeQuery, QueryEnhancerFactory jpql) {
			this.nativeQuery = nativeQuery;
			this.jpql = jpql;
		}

		/**
		 * Returns the default JPQL {@link QueryEnhancerFactory} based on class path presence of Hibernate and EclipseLink.
		 *
		 * @return the default JPQL {@link QueryEnhancerFactory}.
		 */
		public static QueryEnhancerFactory jpql() {
			return DEFAULT_JPQL;
		}

		@Override
		public QueryEnhancerFactory select(DeclaredQuery query) {
			return jpql.supports(query) ? jpql : nativeQuery;
		}

	}
}
