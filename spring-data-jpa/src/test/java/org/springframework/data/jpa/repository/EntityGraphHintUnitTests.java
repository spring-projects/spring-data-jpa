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
package org.springframework.data.jpa.repository;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.data.core.TypedPropertyPath;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;

/**
 * Unit tests for {@link EntityGraphHint}.
 *
 * @author YeongJae Min
 */
class EntityGraphHintUnitTests {

	@BeforeEach
	void setUp() {
		System.setProperty("spring.data.lambda-reader.filter-stacktrace", "false");
	}

	@Test // GH-4175
	void createsFetchGraphFromPropertyReferences() {

		EntityGraphHint<User> hint = EntityGraphHint.fetch(User::getFirstname, User::getRoles);

		assertThat(hint.getType()).isEqualTo(EntityGraphType.FETCH);
		assertThat(hint.getAttributePaths()).containsExactly("firstname", "roles");
	}

	@Test // GH-4175
	void createsLoadGraphFromNestedPropertyPath() {

		EntityGraphHint<User> hint = EntityGraphHint
				.load(TypedPropertyPath.of(User::getManager).then(User::getLastname));

		assertThat(hint.getType()).isEqualTo(EntityGraphType.LOAD);
		assertThat(hint.getAttributePaths()).containsExactly("manager.lastname");
	}

	@Test // GH-4175
	void createsNamedGraphHint() {

		EntityGraphHint<User> hint = EntityGraphHint.fetch("User.overview");

		assertThat(hint.getType()).isEqualTo(EntityGraphType.FETCH);
		assertThat(hint.getName()).isEqualTo("User.overview");
		assertThat(hint.getAttributePaths()).isEmpty();
	}

	@Test // GH-4175
	void rejectsEmptyPropertyPaths() {
		assertThatIllegalArgumentException().isThrownBy(EntityGraphHint::fetch);
	}
}
