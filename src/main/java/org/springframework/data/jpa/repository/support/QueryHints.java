package org.springframework.data.jpa.repository.support;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.EntityManager;

/**
 * QueryHints provides access to query hints defined via {@link CrudMethodMetadata#getQueryHints()} by default excluding
 * JPA {@link javax.persistence.EntityGraph}.
 *
 * @author Christoph Strobl
 * @author Oliver Gierke
 * @since 2.0
 */
interface QueryHints extends Iterable<Entry<String, Object>> {

	/**
	 * Creates and returns a new {@link QueryHints} instance including {@link javax.persistence.EntityGraph}.
	 *
	 * @param em must not be {@literal null}.
	 * @return new instance of {@link QueryHints}.
	 */
	QueryHints withFetchGraphs(EntityManager em);

	/**
	 * Get the query hints as a {@link Map}.
	 *
	 * @return never {@literal null}.
	 */
	Map<String, Object> asMap();

	/**
	 * Null object implementation of {@link QueryHints}.
	 *
	 * @author Oliver Gierke
	 * @since 2.0
	 */
	static enum NoHints implements QueryHints {

		INSTANCE;

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.support.QueryHints#asMap()
		 */
		@Override
		public Map<String, Object> asMap() {
			return Collections.emptyMap();
		}

		/* 
		 * (non-Javadoc)
		 * @see java.lang.Iterable#iterator()
		 */
		@Override
		public Iterator<Entry<String, Object>> iterator() {
			return Collections.emptyIterator();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.support.QueryHints#withFetchGraphs(javax.persistence.EntityManager)
		 */
		@Override
		public QueryHints withFetchGraphs(EntityManager em) {
			return this;
		}
	}
}
