/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Range;
import org.springframework.lang.Nullable;

/**
 * Datastructure that analyses a String to determine the parts of the String that are quoted and offers an API to query
 * that information.
 *
 * @author Jens Schauder
 */
class QuotationMap {

	private static final Set<Character> QUOTING_CHARACTERS = new HashSet<>(Arrays.asList('"', '\''));
	private List<Range<Integer>> quotedRanges = new ArrayList<>();

	QuotationMap(@Nullable String query) {

		if (query == null)
			return;

		Character inQuotation = null;
		int start = 0;

		for (int i = 0; i < query.length(); i++) {

			char currentChar = query.charAt(i);
			if (QUOTING_CHARACTERS.contains(currentChar)) {

				if (inQuotation == null) {

					inQuotation = currentChar;
					start = i;
				} else if (currentChar == inQuotation) {

					inQuotation = null;
					quotedRanges.add(Range.of(Range.Bound.inclusive(start), Range.Bound.inclusive(i)));
				}
			}
		}

		if (inQuotation != null) {
			throw new IllegalArgumentException(
					String.format("The string <%s> starts a quoted range at %d, but never ends it.", query, start));
		}
	}

	/**
	 * @param index to check if it is part of a quoted range.
	 * @return whether the query contains a quoted range at {@literal index}.
	 */
	public boolean isQuoted(int index) {
		return quotedRanges.stream().anyMatch(r -> r.contains(index));
	}
}
