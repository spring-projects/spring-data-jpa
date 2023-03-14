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

import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.lang.Nullable;

/**
 * An exception thrown if the JPQL query is invalid.
 *
 * @author Greg Turnquist
 * @author Mark Paluch
 * @since 3.1
 */
public class BadJpqlGrammarException extends InvalidDataAccessResourceUsageException {

	private final String jpql;

	public BadJpqlGrammarException(String message, String jpql, @Nullable Throwable cause) {
		super(message + "; Bad JPQL grammar [" + jpql + "]", cause);
		this.jpql = jpql;
	}

	public String getJpql() {
		return this.jpql;
	}

}
