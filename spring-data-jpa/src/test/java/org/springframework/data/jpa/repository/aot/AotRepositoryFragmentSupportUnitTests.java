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

	@Test
	void doesNotConfuseNamedPlaceholderWithLongerName() {

		String query = "SELECT u FROM User u WHERE u.lastname = :lastname AND u.last = :last";

		assertThat(repoSupport.conditionalNullComparison(query, ":last", null))
				.isEqualTo("SELECT u FROM User u WHERE u.lastname = :lastname AND u.last IS NULL");
		assertThat(repoSupport.conditionalNullComparison(query, ":lastname", null))
				.isEqualTo("SELECT u FROM User u WHERE u.lastname IS NULL AND u.last = :last");
	}

	@Test
	void detectsNamedPlaceholderOnlyOnExactMatch() {

		assertThat(repoSupport.usesParameter("SELECT u FROM User u WHERE u.lastname LIKE :lastname", ":last")).isFalse();
		assertThat(repoSupport.usesParameter("SELECT u FROM User u WHERE u.lastname LIKE :lastname", ":lastname")).isTrue();
	}

	@Test
	void rewritesFunctionWrappedComparison() {

		assertThat(repoSupport.conditionalNullComparison("SELECT u FROM User u WHERE UPPER(u.lastname) = UPPER(:lastname)",
				":lastname", null)).isEqualTo("SELECT u FROM User u WHERE UPPER(u.lastname) IS NULL");
		assertThat(repoSupport.conditionalNullComparison("SELECT u FROM User u WHERE LOWER(u.lastname) <> LOWER(:lastname)",
				":lastname", null)).isEqualTo("SELECT u FROM User u WHERE LOWER(u.lastname) IS NOT NULL");
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
