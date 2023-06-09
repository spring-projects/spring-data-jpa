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

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

/**
 * A {@link BaseErrorListener} that will throw a {@link BadJpqlGrammarException} if the query is invalid.
 *
 * @author Greg Turnquist
 * @since 3.1
 */
class BadJpqlGrammarErrorListener extends BaseErrorListener {

	private final String query;

	BadJpqlGrammarErrorListener(String query) {
		this.query = query;
	}

	@Override
	public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
			String msg, RecognitionException e) {
		throw new BadJpqlGrammarException("Line " + line + ":" + charPositionInLine + " " + msg, query, null);
	}

}
