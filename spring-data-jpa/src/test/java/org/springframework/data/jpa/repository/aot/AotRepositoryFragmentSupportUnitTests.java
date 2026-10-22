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
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.jpa.repository.aot.AotRepositoryFragmentSupport.NullCompare;
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

	static final String QUERY = "SELECT u FROM User u WHERE u.firstname = :firstname AND UPPER(u.lastname) != UPPER(:lastname)";

	@Test
	void appliesOnlyMatchingComparisons() {

		assertThat(repoSupport.conditionalNullComparison(QUERY, firstname(true), lastname(false)))
				.isEqualTo("SELECT u FROM User u WHERE u.firstname IS NULL AND UPPER(u.lastname) != UPPER(:lastname)");
		assertThat(repoSupport.conditionalNullComparison(QUERY, firstname(false), lastname(true)))
				.isEqualTo("SELECT u FROM User u WHERE u.firstname = :firstname AND u.lastname IS NOT NULL");
		assertThat(repoSupport.conditionalNullComparison(QUERY, firstname(true), lastname(true)))
				.isEqualTo("SELECT u FROM User u WHERE u.firstname IS NULL AND u.lastname IS NOT NULL");
	}

	@Test
	void returnsQueryIfNoComparisonApplies() {

		assertThat(repoSupport.conditionalNullComparison(QUERY, firstname(false), lastname(false))).isSameAs(QUERY);
		assertThat(repoSupport.conditionalNullComparison(QUERY)).isSameAs(QUERY);
	}

	@Test
	void rejectsUnorderedComparisons() {

		assertThatIllegalArgumentException()
				.isThrownBy(() -> repoSupport.conditionalNullComparison(QUERY, lastname(true), firstname(true)));
	}

	@ParameterizedTest
	@ValueSource(strings = { "u.lastname", "e.员工编号", "e.price€", "e.$amount", "e.Jose\u0301", "e.\uD840\uDC00" })
	void acceptsJavaIdentifierPaths(String path) {

		assertThat(repoSupport.conditionalNullComparison("x = :p", NullCompare.of(path).inRange(0, 6).isNullIf(true)))
				.isEqualTo(path + " IS NULL");
	}

	private static NullCompare firstname(boolean isNull) {

		NullComparisons.Replacement replacement = NullComparisons.find(QUERY, ":firstname").get(0);
		return NullCompare.of(replacement.path()).inRange(replacement.start(), replacement.end()).isNullIf(isNull);
	}

	private static NullCompare lastname(boolean isNull) {

		NullComparisons.Replacement replacement = NullComparisons.find(QUERY, ":lastname").get(0);
		return NullCompare.of(replacement.path()).inRange(replacement.start(), replacement.end()).isNotNullIf(isNull);
	}

	static class StubAotRepo extends AotRepositoryFragmentSupport {

		StubAotRepo() {
			super(QueryEnhancerSelector.DEFAULT_SELECTOR, mock(RepositoryMetadata.class), mock(ValueExpressionDelegate.class),
					new SpelAwareProxyProjectionFactory());
		}
	}
}
