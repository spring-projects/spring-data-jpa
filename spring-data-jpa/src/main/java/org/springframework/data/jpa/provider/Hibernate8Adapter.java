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

import java.util.ArrayList;
import java.util.List;

import org.hibernate.query.SelectionQuery;
import org.jspecify.annotations.Nullable;

import org.springframework.data.jpa.util.ReflectiveMethod;

/**
 * {@link HibernateAdapter} for the Hibernate 8.
 *
 * @author Oscar Fanchin
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 4.2
 */
class Hibernate8Adapter extends AbstractHibernateAdapter {

	static final String SQM_STATEMENT_ACCESS = "org.hibernate.query.sqm.spi.SqmStatementAccess";

	private final Class<?> mutationOrSelectionQuery;
	private final ReflectiveMethod isSelectionQuery;
	private final ReflectiveMethod asSelectionQuery;

	Hibernate8Adapter(ClassLoader classLoader) {

		// the mementos moved from org.hibernate.query.named to org.hibernate.query.named.spi after 8.0.0.Beta1
		super(classLoader, ClassNames.of(SQM_STATEMENT_ACCESS),
				ClassNames.of("org.hibernate.query.named.spi.NamedSqmQueryMemento",
						"org.hibernate.query.named.NamedSqmQueryMemento"),
				ClassNames.of("org.hibernate.query.named.spi.NamedNativeQueryMemento",
						"org.hibernate.query.named.NamedNativeQueryMemento"));

		this.mutationOrSelectionQuery = ClassNames.of("org.hibernate.query.MutationOrSelectionQuery").getClass(classLoader);
		this.isSelectionQuery = getMethod(mutationOrSelectionQuery, "isSelectionQuery");
		this.asSelectionQuery = getMethod(mutationOrSelectionQuery, "asSelectionQuery");
	}

	@Override
	public @Nullable SelectionQuery<?> asSelectionQuery(Query query) {

		// mutations legitimately answer null here, the contract itself is required and resolved upfront
		if (!mutationOrSelectionQuery.isInstance(query)
				|| !Boolean.TRUE.equals(isSelectionQuery.invoke(query))) {
			return null;
		}

		Object selectionQuery = asSelectionQuery.invoke(query);
		return selectionQuery instanceof SelectionQuery<?> candidate ? candidate : null;
	}

	@Override
	public List<ReflectiveMethod> getReflectiveMethods() {
		List<ReflectiveMethod> methods = new ArrayList<>(super.getReflectiveMethods());
		methods.add(isSelectionQuery);
		methods.add(asSelectionQuery);
		return methods;
	}
}
