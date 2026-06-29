/*
 * Copyright 2025-present the original author or authors.
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

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.data.jpa.domain.sample.AuditableUser;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.repository.config.AotRepositoryContext;

/**
 * Unit tests for {@link JpaAnnotationMetadata}.
 *
 * @author LordKay-sudo
 */
class JpaAnnotationMetadataUnitTests {

	@Test // GH-4166
	void collectsNamedQueriesFromEntityAnnotations() {

		JpaAnnotationMetadata metadata = JpaAnnotationMetadata.from(List.of(User.class, AuditableUser.class));

		assertThat(metadata.findNamedQuery("User.findByEmailAddress")).hasValueSatisfying(query -> {
			assertThat(query.nativeQuery()).isFalse();
			assertThat(query.query()).isEqualTo("SELECT u FROM User u WHERE u.emailAddress = ?1");
		});

		assertThat(metadata.findNamedQuery("AuditableUser.findByFirstname")).hasValueSatisfying(query -> {
			assertThat(query.nativeQuery()).isFalse();
			assertThat(query.query()).isEqualTo("SELECT u FROM AuditableUser u WHERE u.firstname = ?1");
		});

		assertThat(metadata.findNamedQuery("User.findByNativeNamedQueryWithPageable")).hasValueSatisfying(query -> {
			assertThat(query.nativeQuery()).isTrue();
			assertThat(query.query()).contains("SD_USER");
		});
	}

	@Test // GH-4166
	void collectsNamedEntityGraphsFromEntityAnnotations() {

		JpaAnnotationMetadata metadata = JpaAnnotationMetadata.from(List.of(User.class));

		assertThat(metadata.hasNamedEntityGraph(User.class, "User.overview")).isTrue();
		assertThat(metadata.hasNamedEntityGraph(User.class, "User.detail")).isTrue();
		assertThat(metadata.hasNamedEntityGraph(User.class, "User.missing")).isFalse();
	}

	@Test // GH-4166
	void emptyMetadataDoesNotExposeQueriesOrGraphs() {

		JpaAnnotationMetadata metadata = JpaAnnotationMetadata.empty();

		assertThat(metadata.findNamedQuery("User.findByEmailAddress")).isEmpty();
		assertThat(metadata.hasNamedEntityGraph(User.class, "User.overview")).isFalse();
	}

	@Test // GH-4166
	void collectsMetadataFromAotRepositoryContext() {

		AotRepositoryContext context = org.mockito.Mockito.mock(AotRepositoryContext.class);
		org.mockito.Mockito.when(context.getResolvedTypes()).thenReturn(Set.of(User.class, String.class));

		JpaAnnotationMetadata metadata = JpaAnnotationMetadata.from(context);

		assertThat(metadata.findNamedQuery("User.findByEmailAddress")).isPresent();
		assertThat(metadata.hasNamedEntityGraph(User.class, "User.detail")).isTrue();
	}

}
