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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.jpa.repository.aot.NullComparisons.Replacement;

/**
 * Unit tests for {@link NullComparisons}.
 *
 * @author Christoph Strobl
 */
class NullComparisonsUnitTests {

	@Test
	void rewritesNamedPlaceholder() {

		assertThat(rewrite("SELECT u FROM User u WHERE u.lastname = :lastname", ":lastname"))
				.isEqualTo("SELECT u FROM User u WHERE u.lastname IS NULL");
	}

	@Test
	void rewritesIndexPlaceholder() {

		assertThat(rewrite("SELECT u FROM User u WHERE u.firstname = ?1 AND u.lastname = ?2", "?2"))
				.isEqualTo("SELECT u FROM User u WHERE u.firstname = ?1 AND u.lastname IS NULL");
	}

	@Test
	void rewritesNegatedComparison() {

		assertThat(rewrite("SELECT u FROM User u WHERE u.lastname <> ?1", "?1"))
				.isEqualTo("SELECT u FROM User u WHERE u.lastname IS NOT NULL");
		assertThat(rewrite("SELECT u FROM User u WHERE u.lastname != ?1", "?1"))
				.isEqualTo("SELECT u FROM User u WHERE u.lastname IS NOT NULL");
	}

	@Test
	void findsReplacementOffsets() {

		String query = "SELECT u FROM User u WHERE u.firstname = :firstname AND u.lastname = :lastname";

		assertThat(NullComparisons.find(query, ":lastname")).containsExactly(new Replacement(56, 78, "u.lastname", false));
	}

	@Test
	void doesNotConfuseIndexPlaceholderWithLongerIndex() {

		assertThat(rewrite("SELECT u FROM User u WHERE u.age = ?10 AND u.lastname = ?1", "?1"))
				.isEqualTo("SELECT u FROM User u WHERE u.age = ?10 AND u.lastname IS NULL");
	}

	@ParameterizedTest
	@CsvSource({ "last, lastname", "stra, straße", "员工, 员工编号", "Співробітник, Співробітники", "Jose, Jose\u0301",
			"निर्माण, निर्माणतिथि", "amount, amount$", "price, price€", "na, na\u00ADme" })
	void doesNotConfusePlaceholderWithLongerName(String shorter, String longer) {

		String query = "SELECT e FROM Employee e WHERE e.%1$s = :%1$s AND e.%2$s = :%2$s".formatted(longer, shorter);

		assertThat(rewrite(query, ":" + shorter))
				.isEqualTo("SELECT e FROM Employee e WHERE e.%1$s = :%1$s AND e.%2$s IS NULL".formatted(longer, shorter));
		assertThat(rewrite(query, ":" + longer))
				.isEqualTo("SELECT e FROM Employee e WHERE e.%1$s IS NULL AND e.%2$s = :%2$s".formatted(longer, shorter));
		assertThat(NullComparisons.usesPlaceholder("SELECT e FROM Employee e WHERE e.%1$s LIKE :%1$s".formatted(longer),
				":" + shorter)).isFalse();
	}

	@Test
	void detectsPlaceholderOnlyOnExactMatch() {

		assertThat(NullComparisons.usesPlaceholder("SELECT u FROM User u WHERE u.lastname LIKE :lastname", ":lastname"))
				.isTrue();
		assertThat(NullComparisons.usesPlaceholder("SELECT u FROM User u WHERE u.lastname LIKE ?1", "?1")).isTrue();
		assertThat(NullComparisons.usesPlaceholder("SELECT u FROM User u WHERE u.age = ?10", "?1")).isFalse();
		assertThat(NullComparisons.usesPlaceholder("SELECT u FROM User u WHERE u.lastname IS NULL", "?1")).isFalse();
	}

	@Test
	void rewritesFunctionWrappedComparison() {

		assertThat(rewrite("SELECT u FROM User u WHERE UPPER(u.lastname) = UPPER(:lastname)", ":lastname"))
				.isEqualTo("SELECT u FROM User u WHERE u.lastname IS NULL");
		assertThat(rewrite("SELECT u FROM User u WHERE LOWER(u.lastname) <> LOWER(:lastname)", ":lastname"))
				.isEqualTo("SELECT u FROM User u WHERE u.lastname IS NOT NULL");
	}

	@ParameterizedTest
	@ValueSource(strings = { "员工编号", "Αναγνωριστικό_εργαζομένου", "Співробітники", "Çalışanlar", "José", "निर्माणतिथि",
			"Jose\u0301", "วันที่", "$amount", "amount$", "price€", "£", "¥en", "na\u00ADme" })
	void rewritesUnicodeIdentifiers(String source) {

		assertThat(rewrite("SELECT e FROM Employee e WHERE e.%1$s = :%1$s".formatted(source), ":" + source))
				.isEqualTo("SELECT e FROM Employee e WHERE e.%s IS NULL".formatted(source));
		assertThat(rewrite("SELECT e FROM Employee e WHERE UPPER(e.%1$s) != UPPER(:%1$s)".formatted(source), ":" + source))
				.isEqualTo("SELECT e FROM Employee e WHERE e.%s IS NOT NULL".formatted(source));
	}

	@Test
	void findsNothingForNonComparisonUsage() {
		assertThat(NullComparisons.find("SELECT u FROM User u WHERE u.lastname LIKE ?1", "?1")).isEmpty();
	}

	@Test
	void rejectsOverlappingReplacements() {

		assertThatIllegalArgumentException().isThrownBy(() -> NullComparisons.apply("SELECT u FROM User u",
				List.of(new Replacement(0, 10, "a", false), new Replacement(5, 12, "b", false))));
	}

	private static String rewrite(String query, String placeholder) {
		return NullComparisons.apply(query, NullComparisons.find(query, placeholder));
	}

}
