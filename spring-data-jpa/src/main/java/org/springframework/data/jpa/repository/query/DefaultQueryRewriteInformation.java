/*
 * Copyright 2024-present the original author or authors.
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

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.ReturnedType;

/**
 * Default {@link org.springframework.data.jpa.repository.query.QueryEnhancer.QueryRewriteInformation} implementation.
 *
 * @author Mark Paluch
 */
record DefaultQueryRewriteInformation(Sort sort,
		ReturnedType returnedType) implements QueryEnhancer.QueryRewriteInformation {
	@Override
	public Sort getSort() {
		return sort();
	}

	@Override
	public ReturnedType getReturnedType() {
		return returnedType();
	}
}
