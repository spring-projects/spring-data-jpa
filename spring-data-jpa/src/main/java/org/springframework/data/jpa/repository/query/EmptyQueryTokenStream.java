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

import java.util.Collections;
import java.util.Iterator;

/**
 * Empty QueryTokenStream.
 *
 * @author Mark Paluch
 * @since 3.4
 */
class EmptyQueryTokenStream implements QueryTokenStream {

	static final EmptyQueryTokenStream INSTANCE = new EmptyQueryTokenStream();

	private EmptyQueryTokenStream() {}

	@Override
	public QueryToken getFirst() {
		return null;
	}

	@Override
	public QueryToken getLast() {
		return null;
	}

	@Override
	public boolean isExpression() {
		return false;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public Iterator<QueryToken> iterator() {
		return Collections.emptyIterator();
	}
}
