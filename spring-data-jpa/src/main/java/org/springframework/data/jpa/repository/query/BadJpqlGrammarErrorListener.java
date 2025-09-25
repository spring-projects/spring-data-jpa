/*
 * Copyright 2022-2025 the original author or authors.
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
    private static String formatMessage(Object offendingSymbol,
            int line,
            int charPositionInLine,
            String message,
            RecognitionException e,
            String query) {

        StringBuilder errorText = new StringBuilder(256)
                .append("At ")
                .append(line)
                .append(":")
                .append(charPositionInLine);

        if (offendingSymbol instanceof CommonToken ct) {
            String token = ct.getText();
            if (!ObjectUtils.isEmpty(token)) {
                errorText.append(" and token '").append(token).append("'");
            }
        }
        errorText.append(", ");

        if (e instanceof NoViableAltException) {
            int idx = message.indexOf('\'');
            if (idx >= 0) {
                errorText.append(message, 0, idx);
            } else {
                errorText.append(message);
            }

            if (query.isEmpty()) {
                errorText.append("'*' (empty query string)");
            } else {
                String lineText = getLineAt(query, line);

                StringBuilder lineSb = new StringBuilder(lineText.length() + 1); // 삽입 고려
                lineSb.append(lineText);
                if (charPositionInLine >= 0 && charPositionInLine <= lineSb.length()) {
                    lineSb.insert(charPositionInLine, '*');
                } else {
                    lineSb.append('*');
                }

                errorText.append("'").append(lineSb).append("'");
            }

        } else if (e instanceof InputMismatchException) {
            errorText.append(
                    message.substring(0, message.length() - 1)
                            .replace(" expecting {", ", expecting one of the following tokens: ")
            );
        } else {
            errorText.append(message);
        }

        return errorText.toString();
    }

    private static String getLineAt(String query, int line) {
        if (query == null || query.isEmpty()) {
            return "";
        }

        String[] lines = query.split("\\r?\\n");
        int lineIndex = line - 1; // 라인 번호는 1-based이므로 0-based 인덱스로 변환

        if (lineIndex >= 0 && lineIndex < lines.length) {
            return lines[lineIndex];
        }

        return ""; // 라인 번호가 범위를 벗어나면 빈 문자열 반환
    }
}
