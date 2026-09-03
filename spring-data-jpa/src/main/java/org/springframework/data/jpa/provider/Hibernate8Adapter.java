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
import java.util.List;

import org.hibernate.query.SelectionQuery;
import org.jspecify.annotations.Nullable;
import org.springframework.util.ReflectionUtils;

/**
 * {@link HibernateAdapter} for the Hibernate 8 contracts.
 *
 * @author Oscar Fanchin
 * @author Christoph Strobl
 * @since 4.2
 */
final class Hibernate8Adapter extends AbstractHibernateAdapter {

	static final String SQM_STATEMENT_ACCESS = "org.hibernate.query.sqm.spi.SqmStatementAccess";

	private final Class<?> mutationOrSelectionQuery;
	private final Method isSelectionQuery;
	private final Method asSelectionQuery;

	Hibernate8Adapter(ClassLoader classLoader) {

		// the mementos moved from org.hibernate.query.named to org.hibernate.query.named.spi after 8.0.0.Beta1
		super(classLoader, List.of(SQM_STATEMENT_ACCESS),
				List.of("org.hibernate.query.named.spi.NamedSqmQueryMemento", "org.hibernate.query.named.NamedSqmQueryMemento"),
				List.of("org.hibernate.query.named.spi.NamedNativeQueryMemento",
						"org.hibernate.query.named.NamedNativeQueryMemento"));

		this.mutationOrSelectionQuery = requireClass(List.of("org.hibernate.query.MutationOrSelectionQuery"), classLoader);
		this.isSelectionQuery = requireMethod(mutationOrSelectionQuery, "isSelectionQuery");
		this.asSelectionQuery = requireMethod(mutationOrSelectionQuery, "asSelectionQuery");
	}

	@Override
	public @Nullable SelectionQuery<?> asSelectionQuery(Query query) {

		// mutations legitimately answer null here, the contract itself is required and resolved upfront
		if (!mutationOrSelectionQuery.isInstance(query)
				|| !Boolean.TRUE.equals(ReflectionUtils.invokeMethod(isSelectionQuery, query))) {
			return null;
		}

		Object selectionQuery = ReflectionUtils.invokeMethod(asSelectionQuery, query);

		return selectionQuery instanceof SelectionQuery<?> candidate ? candidate : null;
	}
}
