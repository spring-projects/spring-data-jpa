/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.data.jpa.provider;

import jakarta.persistence.Query;

import org.hibernate.Version;
import org.hibernate.query.SelectionQuery;
import org.jspecify.annotations.Nullable;

import org.springframework.util.ClassUtils;

/**
 * Adapter over the differences across Hibernate version changes that differ between Hibernate 7 and 8.
 * <p>
 * Implementations resolve the version-specific methods and classes reflectively when created, so an unsupported
 * Hibernate version fails early instead of on first use.
 *
 * @author Oscar Fanchin
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 4.2
 * @see HibernateUtils
 */
public interface HibernateAdapter {

	/**
	 * Type introduced in Hibernate 8 whose presence identifies that generation.
	 */
	String HIBERNATE_8_SQM_STATEMENT_ACCESS = "org.hibernate.query.sqm.spi.SqmStatementAccess";

	/**
	 * Whether {@code query} is an SQM query, that is, an HQL or criteria query.
	 */
	boolean isSqmQuery(Object query);

	/**
	 * Whether {@code query} is the memento of a named HQL or criteria query.
	 */
	boolean isNamedSqmQuery(Object query);

	/**
	 * Whether {@code query} is the memento of a named native query.
	 */
	boolean isNamedNativeQuery(Object query);

	/**
	 * Return the query string of an SQM query. A criteria query reports the placeholder {@code <criteria>} instead of
	 * HQL. Use {@link #getSqmStatement(Object)} to render it.
	 */
	String getQueryString(Object query);

	/**
	 * Return the HQL string of a named SQM query memento.
	 */
	String getHqlString(Object query);

	/**
	 * Return the SQL string of a named native query memento.
	 */
	String getSqlString(Object query);

	/**
	 * Render the SQM statement of an SQM query or named SQM query memento as HQL. This recovers the HQL of criteria
	 * queries, whose query string is only a placeholder.
	 *
	 * @throws IllegalStateException if the query carries no SQM statement.
	 */
	String getSqmStatement(Object query);

	/**
	 * Return the {@link SelectionQuery} that {@code query} exposes indirectly, or {@literal null} if it exposes none.
	 * Hibernate 8 no longer lets selection queries implement {@link SelectionQuery} directly and exposes them through
	 * {@code MutationOrSelectionQuery} instead. Queries that implement {@link SelectionQuery} themselves, as on Hibernate
	 * 7, answer {@literal null}. Callers test for those first.
	 */
	@Nullable
	SelectionQuery<?> asSelectionQuery(Query query);

	/**
	 * Create the adapter for the Hibernate version on the classpath.
	 *
	 * @throws IllegalStateException if the Hibernate version is not supported.
	 * @see #isHibernate8(ClassLoader)
	 */
	static HibernateAdapter create() {
		ClassLoader classLoader = HibernateAdapter.class.getClassLoader();
		return isHibernate8(classLoader) ? new Hibernate8Adapter(classLoader) //
				: new Hibernate7Adapter(classLoader);
	}

	/**
	 * Detect whether Hibernate 8 is on the classpath. The reported Hibernate version is checked first. Type presence
	 * serves as fallback because the reported version is not reliable in shaded or repackaged distributions.
	 *
	 * @param classLoader class loader for the presence check. 8.
	 */
	static boolean isHibernate8(ClassLoader classLoader) {

		if (Version.getVersionString().startsWith("8.")) {
			return true;
		}
		return ClassUtils.isPresent(HIBERNATE_8_SQM_STATEMENT_ACCESS, classLoader);
	}

}
