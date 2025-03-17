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

import org.jspecify.annotations.Nullable;

/**
 * NULL-Object pattern implementation for {@link ParametrizedQuery}.
 *
 * @author Jens Schauder
 * @author Mark Paluch
 * @since 2.0.3
 */
enum EmptyIntrospectedQuery implements EntityQuery {

	INSTANCE;

	EmptyIntrospectedQuery() {}

	@Override
	public boolean hasParameterBindings() {
		return false;
	}

	@Override
	public boolean usesJdbcStyleParameters() {
		return false;
	}

	@Override
	public boolean hasNamedParameter() {
		return false;
	}

	@Override
	public List<ParameterBinding> getParameterBindings() {
		return Collections.emptyList();
	}

	public @Nullable String getAlias() {
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
	public String getQueryString() {
		return "";
	}

	@Override
	public ParametrizedQuery deriveCountQuery(@Nullable String countQueryProjection) {
		return INSTANCE;
	}

	@Override
	public QueryProvider rewrite(QueryEnhancer.QueryRewriteInformation rewriteInformation) {
		return this;
	}

	@Override
	public String toString() {
		return "<EMPTY>";
	}

}
