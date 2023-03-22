/*
 * Copyright 2023 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CollectionUtils}.
 *
 * @author Mark Paluch
 */
class CollectionUtilsUnitTests {

	@Test // GH-2878
	void shouldReturnFirstItems() {

		assertThat(CollectionUtils.getFirst(2, List.of(1, 2, 3))).containsExactly(1, 2);
		assertThat(CollectionUtils.getFirst(2, List.of(1, 2))).containsExactly(1, 2);
		assertThat(CollectionUtils.getFirst(2, List.of(1))).containsExactly(1);
	}

	@Test // GH-2878
	void shouldReturnLastItems() {

		assertThat(CollectionUtils.getLast(2, List.of(1, 2, 3))).containsExactly(2, 3);
		assertThat(CollectionUtils.getLast(2, List.of(1, 2))).containsExactly(1, 2);
		assertThat(CollectionUtils.getLast(2, List.of(1))).containsExactly(1);
	}
}
