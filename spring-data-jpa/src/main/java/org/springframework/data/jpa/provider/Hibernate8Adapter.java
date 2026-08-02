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

import java.lang.reflect.Method;

import org.hibernate.query.SelectionQuery;
import org.jspecify.annotations.Nullable;

import org.springframework.util.ReflectionUtils;

/**
 * {@link HibernateAdapter} for the Hibernate 8 contracts.
 *
 * @author Oscar Fanchin
 * @since 4.2
 */
final class Hibernate8Adapter extends AbstractHibernateAdapter {

	static final String SQM_STATEMENT_ACCESS = "org.hibernate.query.sqm.spi.SqmStatementAccess";

	private final @Nullable Class<?> mutationOrSelectionQuery;
	private final @Nullable Method isSelectionQuery;
	private final @Nullable Method asSelectionQuery;

	Hibernate8Adapter(ClassLoader classLoader) {

		super(classLoader, SQM_STATEMENT_ACCESS, "org.hibernate.query.named.spi.NamedSqmQueryMemento",
				"org.hibernate.query.named.spi.NamedNativeQueryMemento");

		this.mutationOrSelectionQuery = loadClass("org.hibernate.query.MutationOrSelectionQuery", classLoader);
		this.isSelectionQuery = findMethod(mutationOrSelectionQuery, "isSelectionQuery");
		this.asSelectionQuery = findMethod(mutationOrSelectionQuery, "asSelectionQuery");
	}

	@Override
	public @Nullable SelectionQuery<?> asSelectionQuery(Query query) {

		if (mutationOrSelectionQuery == null || isSelectionQuery == null || asSelectionQuery == null
				|| !mutationOrSelectionQuery.isInstance(query)
				|| !Boolean.TRUE.equals(ReflectionUtils.invokeMethod(isSelectionQuery, query))) {
			return null;
		}

		Object selectionQuery = ReflectionUtils.invokeMethod(asSelectionQuery, query);

		return selectionQuery instanceof SelectionQuery<?> candidate ? candidate : null;
	}

	private static @Nullable Method findMethod(@Nullable Class<?> type, String methodName) {
		return type != null ? ReflectionUtils.findMethod(type, methodName) : null;
	}
}
