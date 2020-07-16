/*
 * Copyright 2017-2020 the original author or authors.
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
package org.springframework.data.jpa.repository.support;

import java.util.function.BiConsumer;

import javax.persistence.EntityManager;

/**
 * QueryHints provides access to query hints defined via {@link CrudMethodMetadata#getQueryHints()} QueryHintList()} by
 * default excluding JPA {@link javax.persistence.EntityGraph}. The object allows to switch between query hints for
 * count queries with or without fetch graph hints.
 *
 * @author Christoph Strobl
 * @author Oliver Gierke
 * @author Jens Schauder
 * @since 2.0
 */
interface QueryHints {

	/**
	 * Creates and returns a new {@link QueryHints} instance including {@link javax.persistence.EntityGraph}.
	 *
	 * @param em must not be {@literal null}.
	 * @return new instance of {@link QueryHints}.
	 */
	QueryHints withFetchGraphs(EntityManager em);

	/**
	 * Creates and returns a new {@link QueryHints} instance that will contain only those hints applicable for count
	 * queries.
	 *
	 * @return new instance of {@link QueryHints}.
	 * @since 2.2
	 */
	QueryHints forCounts();

	/**
	 * Passes each query hint to the consumer. Query hint keys might appear more than once.
	 *
	 * @param consumer to process query hints consisting of a key and a value.
	 * @since 2.4
	 */
	void forEach(BiConsumer<String, Object> consumer);

	/**
	 * Null object implementation of {@link QueryHints}.
	 *
	 * @author Oliver Gierke
	 * @since 2.0
	 */
	enum NoHints implements QueryHints {

		INSTANCE;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.support.QueryHints#withFetchGraphs(javax.persistence.EntityManager)
		 */
		@Override
		public QueryHints withFetchGraphs(EntityManager em) {
			return this;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.support.QueryHints#forCounts(javax.persistence.EntityManager)
		 */
		@Override
		public QueryHints forCounts() {
			return this;
		}

		@Override
		public void forEach(BiConsumer<String, Object> consumer) {}
	}
}
