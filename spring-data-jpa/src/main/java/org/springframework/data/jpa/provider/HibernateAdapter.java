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

import java.util.function.Supplier;

import jakarta.persistence.Query;

import org.hibernate.Version;
import org.hibernate.query.SelectionQuery;
import org.jspecify.annotations.Nullable;
import org.springframework.util.ClassUtils;

/**
 * Adapter bridging the contracts between hibernate 7 and 8.
 *
 * @author Oscar Fanchin
 * @author Christoph Strobl
 * @since 4.2
 */
interface HibernateAdapter {

	String HIBERNATE_8_SQM_STATEMENT_ACCESS = "org.hibernate.query.sqm.spi.SqmStatementAccess";

	/**
	 * Select the adapter matching the Hibernate version on the classpath. Detection is based on type presence rather than
	 * on the declared version, which is not reliable in shaded or repackaged distributions.
	 */
	static HibernateAdapter create() {

		ClassLoader classLoader = HibernateAdapter.class.getClassLoader();

		return isHibernate8(() -> classLoader) ? new Hibernate8Adapter(classLoader) //
				: new Hibernate7Adapter(classLoader);
	}

	boolean isSqmQuery(Object query);

	boolean isNamedSqmQuery(Object query);

	boolean isNamedNativeQuery(Object query);

	String getQueryString(Object query);

	String getHqlString(Object query);

	String getSqlString(Object query);

	String getSqmStatement(Object query);

	/**
	 * Return the {@link SelectionQuery} the given query represents, or {@literal null} if it does not represent one.
	 * Hibernate 8 no longer lets selection queries implement {@link SelectionQuery} directly, exposing them through
	 * {@code MutationOrSelectionQuery} instead.
	 */
	@Nullable
	SelectionQuery<?> asSelectionQuery(Query query);

	static boolean isHibernate8(Supplier<ClassLoader> classLoader) {

		if (Version.getVersionString().startsWith("8.")) {
			return true;
		}
		return ClassUtils.isPresent(HIBERNATE_8_SQM_STATEMENT_ACCESS, classLoader.get());
	}
}
