/*
 * Copyright 2018-2025 the original author or authors.
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

import java.util.Collections;
import java.util.List;

import org.springframework.lang.Nullable;

/**
 * NULL-Object pattern implementation for {@link DeclaredQuery}.
 *
 * @author Jens Schauder
 * @since 2.0.3
 */
class EmptyDeclaredQuery implements DeclaredQuery {

	/**
	 * An implementation implementing the NULL-Object pattern for situations where there is no query.
	 */
	static final DeclaredQuery EMPTY_QUERY = new EmptyDeclaredQuery();

	@Override
	public boolean hasNamedParameter() {
		return false;
	}

	@Override
	public String getQueryString() {
		return "";
	}

	@Override
	public String getAlias() {
		return null;
	}

	@Override
	public boolean hasConstructorExpression() {
		return false;
	}

	@Override
	public boolean isDefaultProjection() {
		return false;
	}

	@Override
	public List<ParameterBinding> getParameterBindings() {
		return Collections.emptyList();
	}

	@Override
	public DeclaredQuery deriveCountQuery(@Nullable String countQueryProjection) {
		return EMPTY_QUERY;
	}

	@Override
	public boolean usesJdbcStyleParameters() {
		return false;
	}
}
