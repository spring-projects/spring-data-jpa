/*
 * Copyright 2022-present the original author or authors.
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

import java.util.List;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import org.springframework.util.ObjectUtils;

/**
 * A {@link BaseErrorListener} that will throw a {@link BadJpqlGrammarException} if the query is invalid.
 *
 * @author Greg Turnquist
 * @since 3.1
 */
class BadJpqlGrammarErrorListener extends BaseErrorListener {

	private final String query;

	private final String grammar;

	BadJpqlGrammarErrorListener(String query) {
		this(query, "JPQL");
	}

	BadJpqlGrammarErrorListener(String query, String grammar) {
		this.query = query;
		this.grammar = grammar;
	}

	@Override
	public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
			String msg, RecognitionException e) {
		throw new BadJpqlGrammarException(formatMessage(offendingSymbol, line, charPositionInLine, msg, e, query), grammar,
				query, null);
	}

	/**
	 * Rewrite the error message.
	 */
	private static String formatMessage(Object offendingSymbol, int line, int charPositionInLine, String message,
			RecognitionException e, String query) {

		String errorText = "At " + line + ":" + charPositionInLine;

		if (offendingSymbol instanceof CommonToken ct) {

			String token = ct.getText();
			if (!ObjectUtils.isEmpty(token)) {
				errorText += " and token '" + token + "'";
			}
		}
		errorText += ", ";

		if (e instanceof NoViableAltException) {

			errorText += message.substring(0, message.indexOf('\''));
			if (query.isEmpty()) {
				errorText += "'*' (empty query string)";
			} else {

				List<String> list = query.lines().toList();
				String lineText = list.get(line - 1);
				String text = lineText.substring(0, charPositionInLine) + "*" + lineText.substring(charPositionInLine);
				errorText += "'" + text + "'";
			}

		} else if (e instanceof InputMismatchException) {
			errorText += message.substring(0, message.length() - 1).replace(" expecting {",
					", expecting one of the following tokens: ");
		} else {
			errorText += message;
		}

		return errorText;
	}

}
