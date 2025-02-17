/*
 * Copyright 2025 the original author or authors.
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

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Hibernate-specific query details capturing common table expression details.
 *
 * @author Mark Paluch
 * @since 3.5
 */
class HibernateQueryInformation extends QueryInformation {

	private final boolean hasCte;

	public HibernateQueryInformation(@Nullable String alias, List<QueryToken> projection,
			boolean hasConstructorExpression, boolean hasCte) {
		super(alias, projection, hasConstructorExpression);
		this.hasCte = hasCte;
	}

	public boolean hasCte() {
		return hasCte;
	}
}
