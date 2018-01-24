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

import static org.assertj.core.api.Assertions.*;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.springframework.data.jpa.repository.query.StringQuery.QuotationMap;

/**
 * Unit tests for {@link QuotationMap}.
 *
 * @author Jens Schauder
 */
public class QuotationMapUnitTests {

	SoftAssertions softly = new SoftAssertions();

	@Test // DATAJPA-1235
	public void emptyStringDoesNotContainQuotes() {
		isNotQuoted("", "empty String", -1, 0, 1);
	}

	@Test // DATAJPA-1235
	public void nullStringDoesNotContainQuotes() {
		isNotQuoted(null, "null String", -1, 0, 1);
	}

	@Test // DATAJPA-1235
	public void simpleStringDoesNotContainQuotes() {
		String query = "something";
		isNotQuoted(query, "simple String", -1, 0, query.length() - 1, query.length(), query.length() + 1);
	}

	@Test // DATAJPA-1235
	public void fullySingleQuotedStringDoesContainQuotes() {

		String query = "'something'";
		isNotQuoted(query, "quoted String", -1, query.length());
		isQuoted(query, "quoted String", 0, 1, 5, query.length() - 1);
	}

	@Test // DATAJPA-1235
	public void fullyDoubleQuotedStringDoesContainQuotes() {

		String query = "\"something\"";
		isNotQuoted(query, "double quoted String", -1, query.length());
		isQuoted(query, "double quoted String", 0, 1, 5, query.length() - 1);
	}

	@Test // DATAJPA-1235
	public void stringWithEmptyQuotes() {

		String query = "abc''def";
		isNotQuoted(query, "zero length quote", -1, 0, 1, 2, 5, 6, 7);
		isQuoted(query, "zero length quote", 3, 4);
	}

	@Test // DATAJPA-1235
	public void doubleInSingleQuotes() {

		String query = "abc'\"'def";
		isNotQuoted(query, "double inside single quote", -1, 0, 1, 2, 6, 7, 8);
		isQuoted(query, "double inside single quote", 3, 4, 5);
	}

	@Test // DATAJPA-1235
	public void singleQuotesInDoubleQuotes() {

		String query = "abc\"'\"def";
		isNotQuoted(query, "single inside double quote", -1, 0, 1, 2, 6, 7, 8);
		isQuoted(query, "single inside double quote", 3, 4, 5);
	}

	@Test // DATAJPA-1235
	public void escapedQuotes() {

		String query = "a'b''cd''e'f";
		isNotQuoted(query, "escaped quote", -1, 0, 11, 12);
		isQuoted(query, "escaped quote", 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
	}

	@Test // DATAJPA-1235
	public void openEndedQuoteThrowsException() {

		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new QuotationMap("a'b"));
	}

	public void isNotQuoted(String query, Object label, int... indexes) {

		QuotationMap quotationMap = new QuotationMap(query);

		for (int index : indexes) {

			assertThat(quotationMap.isQuoted(index))
					.describedAs(String.format("(%s) %s does not contain a quote at %s", label, query, index)) //
					.isFalse();
		}
	}

	public void isQuoted(String query, Object label, int... indexes) {

		QuotationMap quotationMap = new QuotationMap(query);

		for (int index : indexes) {

			assertThat(quotationMap.isQuoted(index))
					.describedAs(String.format("(%s) %s does contain a quote at %s", label, query, index)).isTrue();
		}
	}
}
