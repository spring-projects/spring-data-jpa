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
package org.springframework.data.jpa.repository.aot.generated;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.query.ParameterBinding;
import org.springframework.data.jpa.repository.query.ParameterBindingParser;
import org.springframework.data.jpa.repository.query.ParameterBindingParser.Metadata;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * @author Christoph Strobl
 * @since 2025/01
 */
class AotStringQuery {

	private final String raw;
	private final String sanitized;
	private @Nullable String countQuery;
	private final List<ParameterBinding> parameterBindings;
	private final Metadata parameterMetadata;
	private Limit limit;
	private boolean nativeQuery;

	public AotStringQuery(String raw, String sanitized, List<ParameterBinding> parameterBindings,
			Metadata parameterMetadata) {
		this.raw = raw;
		this.sanitized = sanitized;
		this.parameterBindings = parameterBindings;
		this.parameterMetadata = parameterMetadata;
	}

	static AotStringQuery of(String raw) {

		List<ParameterBinding> bindings = new ArrayList<>();
		Metadata metadata = new Metadata();
		String targetQuery = ParameterBindingParser.INSTANCE
				.parseParameterBindingsOfQueryIntoBindingsAndReturnCleanedQuery(raw, bindings, metadata);

		return new AotStringQuery(raw, targetQuery, bindings, metadata);
	}

	static AotStringQuery nativeQuery(String raw) {
		AotStringQuery q = of(raw);
		q.nativeQuery = true;
		return q;
	}

	static AotStringQuery bindable(String query, List<ParameterBinding> bindings) {
		return new AotStringQuery(query, query, bindings, new Metadata());
	}

	public String getQueryString() {
		return sanitized;
	}

	public String getCountQuery(@Nullable String projection) {

		if (StringUtils.hasText(countQuery)) {
			return countQuery;
		}
		return QueryUtils.createCountQueryFor(sanitized, StringUtils.hasText(projection) ? projection : null, nativeQuery);
	}

	public List<ParameterBinding> parameterBindings() {
		return this.parameterBindings;
	}

	boolean isLimited() {
		return limit != null && limit.isLimited();
	}

	Limit getLimit() {
		return limit;
	}

	public void setLimit(Limit limit) {
		this.limit = limit;
	}

	public boolean isNativeQuery() {
		return nativeQuery;
	}

	public void setCountQuery(@Nullable String countQuery) {
		this.countQuery = StringUtils.hasText(countQuery) ? countQuery : null;
	}
}
