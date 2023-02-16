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

import org.antlr.v4.runtime.misc.ParseCancellationException;

/**
 * An exception to throw if a JPQL query is invalid.
 *
 * @author Greg Turnquist
 * @since 3.1
 */
class QueryParsingSyntaxError extends ParseCancellationException {

	public QueryParsingSyntaxError() {}

	public QueryParsingSyntaxError(String message) {
		super(message);
	}

	public QueryParsingSyntaxError(Throwable cause) {
		super(cause);
	}

	public QueryParsingSyntaxError(String message, Throwable cause) {
		super(message, cause);
	}
}
