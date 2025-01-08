/*
 * Copyright 2024-2025 the original author or authors.
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

/**
 * Interface defining a token. Tokens are atomic elements from which queries are built. Tokens can be inline tokens that
 * do not require spacing or expressions that must be separated by spaces, commas, etc.
 *
 * @author Christoph Strobl
 * @since 3.4
 */
interface QueryToken {

	/**
	 * @return the token value (i.e. its content).
	 */
	String value();

	/**
	 * @return {@code true} if the token represents an expression.
	 */
	default boolean isExpression() {
		return false;
	}

}
