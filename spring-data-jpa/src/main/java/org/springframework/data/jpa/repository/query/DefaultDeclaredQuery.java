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

import org.springframework.util.ObjectUtils;

/**
 * @author Mark Paluch
 */
class DefaultDeclaredQuery implements DeclaredQuery {

	private final String query;
	private final boolean nativeQuery;

	DefaultDeclaredQuery(String query, boolean nativeQuery) {
		this.query = query;
		this.nativeQuery = nativeQuery;
	}

	@Override
	public String getQueryString() {
		return query;
	}

	@Override
	public boolean isNativeQuery() {
		return nativeQuery;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof DefaultDeclaredQuery that)) {
			return false;
		}
		if (nativeQuery != that.nativeQuery) {
			return false;
		}
		return ObjectUtils.nullSafeEquals(query, that.query);
	}

	@Override
	public int hashCode() {
		int result = ObjectUtils.nullSafeHashCode(query);
		result = 31 * result + (nativeQuery ? 1 : 0);
		return result;
	}

	@Override
	public String toString() {
		return (isNativeQuery() ? "[native] " : "[JPQL] ") + getQueryString();
	}
}
