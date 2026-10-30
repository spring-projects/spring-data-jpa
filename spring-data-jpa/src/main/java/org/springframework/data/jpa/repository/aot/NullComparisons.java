/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.data.jpa.repository.aot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper for AOT {@code <path> = <placeholder>} and {@code <path> <> <placeholder>} rewrite for {@code IS [NOT] NULL}.
 *
 * @author Christoph Strobl
 * @since 4.2
 */
final class NullComparisons {

	private static final String IDENTIFIER_PART = "\\p{javaJavaIdentifierPart}";
	private static final String PATH = "[" + IDENTIFIER_PART + ".]+";
	private static final String FUNCTION = IDENTIFIER_PART + "+\\(";

	private NullComparisons() {}

	/**
	 * Find comparisons of {@code placeholder} that need to be rewritten if the bound value is {@literal null}. Function
	 * wrappers such as {@code UPPER(…)} are dropped from the replacement as they are {@literal null}-preserving.
	 *
	 * @param query the query string.
	 * @param placeholder the parameter placeholder, e.g. {@code :lastname}.
	 * @return the replacements ordered by their position within the query.
	 */
	static List<Replacement> find(String query, String placeholder) {

		String quoted = Pattern.quote(placeholder);
		Pattern pattern = Pattern.compile("(?:" + FUNCTION + "(" + PATH + ")\\)|(" + PATH + "))\\s*(=|<>|!=)\\s*(?:"
				+ FUNCTION + "\\s*" + quoted + "\\s*\\)|" + quoted + "(?!" + IDENTIFIER_PART + "))");

		List<Replacement> replacements = new ArrayList<>();
		Matcher matcher = pattern.matcher(query);
		while (matcher.find()) {

			String path = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
			replacements.add(new Replacement(matcher.start(), matcher.end(), path, !"=".equals(matcher.group(3))));
		}
		return replacements;
	}

	/**
	 * @return {@literal true} if the {@code placeholder} occurs in the query string.
	 */
	static boolean usesPlaceholder(String query, String placeholder) {
		return Pattern.compile(Pattern.quote(placeholder) + "(?!" + IDENTIFIER_PART + ")").matcher(query).find();
	}

	/**
	 * Apply the given replacements to the query.
	 *
	 * @param query the query string.
	 * @param replacements the replacements to apply.
	 * @return the rewritten query.
	 * @throws IllegalArgumentException if replacements overlap.
	 */
	static String apply(String query, List<Replacement> replacements) {

		List<Replacement> sorted = new ArrayList<>(replacements);
		sorted.sort(Comparator.comparingInt(Replacement::start));

		StringBuilder builder = new StringBuilder(query.length());
		int last = 0;
		for (Replacement replacement : sorted) {

			if (replacement.start() < last) {
				throw new IllegalArgumentException("Overlapping replacements %s".formatted(sorted));
			}
			builder.append(query, last, replacement.start()).append(replacement.text());
			last = replacement.end();
		}
		return builder.append(query, last, query.length()).toString();
	}

	/**
	 * Replacement of the query region {@code [start, end)} with {@code <path> IS [NOT] NULL}.
	 */
	record Replacement(int start, int end, String path, boolean negated) {

		String text() {
			return path + (negated ? " IS NOT NULL" : " IS NULL");
		}
	}

}
