/*
 * Copyright 2026 the original author or authors.
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
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.jpa.repository.query.QueryEnhancerSelector;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.ValueExpressionDelegate;

/**
 * Tests for {@link AotRepositoryFragmentSupport}.
 *
 * @author Christoph Strobl
 */
class AotRepositoryFragmentSupportUnitTests {

	StubAotRepo repoSupport = new StubAotRepo();

	@Test
	void rewritesNamedPlaceholderForNullValue() {

		assertThat(
				repoSupport.conditionalNullComparison("SELECT u FROM User u WHERE u.lastname = :lastname", ":lastname", null))
				.isEqualTo("SELECT u FROM User u WHERE u.lastname IS NULL");
	}

	@Test
	void rewritesIndexPlaceholdersForNullValues() {

		assertThat(repoSupport.conditionalNullComparison("SELECT u FROM User u WHERE u.firstname = ?1 AND u.lastname = ?2",
				"?1", null, "?2", null)).isEqualTo("SELECT u FROM User u WHERE u.firstname IS NULL AND u.lastname IS NULL");
	}

	@Test
	void rewritesOnlyNullIndexPlaceholders() {

		assertThat(repoSupport.conditionalNullComparison("SELECT u FROM User u WHERE u.firstname = ?1 AND u.lastname = ?2",
				"?1", "Oliver", "?2", null)).isEqualTo("SELECT u FROM User u WHERE u.firstname = ?1 AND u.lastname IS NULL");
	}

	@Test
	void rewritesNegatedIndexPlaceholder() {

		assertThat(repoSupport.conditionalNullComparison("SELECT u FROM User u WHERE u.lastname <> ?1", "?1", null))
				.isEqualTo("SELECT u FROM User u WHERE u.lastname IS NOT NULL");
		assertThat(repoSupport.conditionalNullComparison("SELECT u FROM User u WHERE u.lastname != ?1", "?1", null))
				.isEqualTo("SELECT u FROM User u WHERE u.lastname IS NOT NULL");
	}

	@Test
	void doesNotConfuseIndexPlaceholderWithLongerIndex() {

		assertThat(
				repoSupport.conditionalNullComparison("SELECT u FROM User u WHERE u.age = ?10 AND u.lastname = ?1", "?1", null))
				.isEqualTo("SELECT u FROM User u WHERE u.age = ?10 AND u.lastname IS NULL");
	}

	@ParameterizedTest
	@CsvSource({ "last, lastname", "stra, straße", "员工, 员工编号", "Співробітник, Співробітники", "Jose, Jose\u0301",
			"निर्माण, निर्माणतिथि", "amount, amount$", "price, price€", "na, na\u00ADme" })
	void doesNotConfusePlaceholderWithLongerName(String shorter, String longer) {

		String query = "SELECT e FROM Employee e WHERE e.%1$s = :%1$s AND e.%2$s = :%2$s".formatted(longer, shorter);

		assertThat(repoSupport.conditionalNullComparison(query, ":" + shorter, null))
				.isEqualTo("SELECT e FROM Employee e WHERE e.%1$s = :%1$s AND e.%2$s IS NULL".formatted(longer, shorter));
		assertThat(repoSupport.conditionalNullComparison(query, ":" + longer, null))
				.isEqualTo("SELECT e FROM Employee e WHERE e.%1$s IS NULL AND e.%2$s = :%2$s".formatted(longer, shorter));
		assertThat(repoSupport.usesParameter("SELECT e FROM Employee e WHERE e.%1$s LIKE :%1$s".formatted(longer),
				":" + shorter)).isFalse();
	}

	@Test
	void detectsNamedPlaceholderOnlyOnExactMatch() {

		assertThat(repoSupport.usesParameter("SELECT u FROM User u WHERE u.lastname LIKE :lastname", ":last")).isFalse();
		assertThat(repoSupport.usesParameter("SELECT u FROM User u WHERE u.lastname LIKE :lastname", ":lastname")).isTrue();
	}

	@Test
	void rewritesFunctionWrappedComparison() {

		assertThat(repoSupport.conditionalNullComparison("SELECT u FROM User u WHERE UPPER(u.lastname) = UPPER(:lastname)",
				":lastname", null)).isEqualTo("SELECT u FROM User u WHERE u.lastname IS NULL");
		assertThat(repoSupport.conditionalNullComparison("SELECT u FROM User u WHERE LOWER(u.lastname) <> LOWER(:lastname)",
				":lastname", null)).isEqualTo("SELECT u FROM User u WHERE u.lastname IS NOT NULL");
	}

	@ParameterizedTest
	@ValueSource(strings = { "员工编号", "Αναγνωριστικό_εργαζομένου", "Співробітники", "Çalışanlar", "José",
			"निर्माणतिथि", "Jose\u0301", "วันที่", "$amount", "amount$", "price€", "£", "¥en", "na\u00ADme" })
	void rewritesUnicodeIdentifiers(String source) {

		assertThat(repoSupport.conditionalNullComparison("SELECT e FROM Employee e WHERE e.%1$s = :%1$s".formatted(source),
				":" + source, null)).isEqualTo("SELECT e FROM Employee e WHERE e.%s IS NULL".formatted(source));
		assertThat(repoSupport.conditionalNullComparison(
				"SELECT e FROM Employee e WHERE UPPER(e.%1$s) != UPPER(:%1$s)".formatted(source), ":" + source, null))
				.isEqualTo("SELECT e FROM Employee e WHERE e.%s IS NOT NULL".formatted(source));
	}

	@Test
	void keepsQueryWhenNoValueIsNull() {

		String query = "SELECT u FROM User u WHERE u.lastname = ?1";

		assertThat(repoSupport.conditionalNullComparison(query, "?1", "Gierke")).isSameAs(query);
	}

	@Test
	void leavesNonComparisonUsageUntouched() {

		String query = "SELECT u FROM User u WHERE u.lastname LIKE ?1";

		assertThat(repoSupport.conditionalNullComparison(query, "?1", null)).isEqualTo(query);
	}

	@Test
	void detectsRemainingIndexPlaceholder() {

		assertThat(repoSupport.usesParameter("SELECT u FROM User u WHERE u.lastname LIKE ?1", "?1")).isTrue();
		assertThat(repoSupport.usesParameter("SELECT u FROM User u WHERE u.age = ?10", "?1")).isFalse();
		assertThat(repoSupport.usesParameter("SELECT u FROM User u WHERE u.lastname IS NULL", "?1")).isFalse();
	}

	@Test
	void rejectsUnevenPlaceholderValuePairs() {

		assertThatIllegalArgumentException()
				.isThrownBy(() -> repoSupport.conditionalNullComparison("SELECT u FROM User u WHERE u.lastname = ?1", "?1"));
	}

	static class StubAotRepo extends AotRepositoryFragmentSupport {

		StubAotRepo() {
			super(QueryEnhancerSelector.DEFAULT_SELECTOR, mock(RepositoryMetadata.class), mock(ValueExpressionDelegate.class),
					new SpelAwareProxyProjectionFactory());
		}
	}
}
