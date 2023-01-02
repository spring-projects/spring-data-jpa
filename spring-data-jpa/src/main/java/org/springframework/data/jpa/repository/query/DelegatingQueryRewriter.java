/*
 * Copyright 2022-2023 the original author or authors.
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

import java.util.function.Supplier;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.QueryRewriter;

/**
 * Delegating {@link QueryRewriter} that delegates rewrite calls to a {@link QueryRewriter delegate} provided by a
 * {@link Supplier}.
 *
 * @author Mark Paluch
 * @since 3.0
 */
public class DelegatingQueryRewriter implements QueryRewriter {

	private final Supplier<QueryRewriter> delegate;

	public DelegatingQueryRewriter(Supplier<QueryRewriter> delegate) {
		this.delegate = delegate;
	}

	@Override
	public String rewrite(String query, Sort sort) {
		return delegate.get().rewrite(query, sort);
	}

	@Override
	public String rewrite(String query, Pageable pageRequest) {
		return delegate.get().rewrite(query, pageRequest);
	}
}
