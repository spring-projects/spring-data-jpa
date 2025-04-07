/*
 * Copyright 2025 the original author or authors.
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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.*;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link UserRepository} JSON metadata.
 *
 * @author Mark Paluch
 */
@SpringJUnitConfig(classes = JpaRepositoryMetadataIntegrationTests.JpaRepositoryContributorConfiguration.class)
@Transactional
class JpaRepositoryMetadataIntegrationTests {

	@Autowired AbstractApplicationContext context;

	@Configuration
	static class JpaRepositoryContributorConfiguration extends AotFragmentTestConfigurationSupport {
		public JpaRepositoryContributorConfiguration() {
			super(UserRepository.class);
		}
	}

	@Test // GH-3830
	void shouldDocumentBase() throws IOException {

		Resource resource = getResource();

		assertThat(resource).isNotNull();
		assertThat(resource.exists()).isTrue();

		String json = resource.getContentAsString(StandardCharsets.UTF_8);

		assertThatJson(json).isObject() //
				.containsEntry("name", UserRepository.class.getName()) //
				.containsEntry("module", "") // TODO: JPA should be here
				.containsEntry("type", "IMPERATIVE");
	}

	@Test // GH-3830
	void shouldDocumentDerivedQuery() throws IOException {

		Resource resource = getResource();

		assertThat(resource).isNotNull();
		assertThat(resource.exists()).isTrue();

		String json = resource.getContentAsString(StandardCharsets.UTF_8);

		assertThatJson(json).inPath("$.methods[0]").isObject().containsEntry("name", "countUsersByLastname");
		assertThatJson(json).inPath("$.methods[0].query").isObject().containsEntry("query",
				"SELECT COUNT(u) FROM org.springframework.data.jpa.domain.sample.User u WHERE u.lastname = ?1");
	}

	@Test // GH-3830
	void shouldDocumentPagedQuery() throws IOException {

		Resource resource = getResource();

		assertThat(resource).isNotNull();
		assertThat(resource.exists()).isTrue();

		String json = resource.getContentAsString(StandardCharsets.UTF_8);

		assertThatJson(json).inPath("$.methods[?(@.name == 'findAndApplyQueryRewriter')].query").isArray().element(1)
				.isObject().containsEntry("query", "select u from OTHER u where u.emailAddress = ?1")
				.containsEntry("count-query", "select count(u) from OTHER u where u.emailAddress = ?1");
	}

	@Test // GH-3830
	void shouldDocumentQueryWithExpression() throws IOException {

		Resource resource = getResource();

		assertThat(resource).isNotNull();
		assertThat(resource.exists()).isTrue();

		String json = resource.getContentAsString(StandardCharsets.UTF_8);

		assertThatJson(json).inPath("$.methods[?(@.name == 'findValueExpressionNamedByEmailAddress')].query").isArray()
				.first().isObject().containsEntry("query", "select u from User u where u.emailAddress = :__$synthetic$__1");
	}

	@Test // GH-3830
	void shouldDocumentNamedQuery() throws IOException {

		Resource resource = getResource();

		assertThat(resource).isNotNull();
		assertThat(resource.exists()).isTrue();

		String json = resource.getContentAsString(StandardCharsets.UTF_8);

		assertThatJson(json).inPath("$.methods[?(@.name == 'findPagedWithNamedCountByEmailAddress')].query").isArray()
				.first().isObject().containsEntry("name", "User.findByEmailAddress")
				.containsEntry("query", "SELECT u FROM User u WHERE u.emailAddress = ?1")
				.containsEntry("count-name", "User.findByEmailAddress.count-provided")
				.containsEntry("count-query", "SELECT count(u) FROM User u WHERE u.emailAddress = ?1");
	}

	@Test // GH-3830
	void shouldDocumentNamedProcedure() throws IOException {

		Resource resource = getResource();

		assertThat(resource).isNotNull();
		assertThat(resource.exists()).isTrue();

		String json = resource.getContentAsString(StandardCharsets.UTF_8);

		assertThatJson(json).inPath("$.methods[?(@.name == 'namedProcedure')].query").isArray().first().isObject()
				.containsEntry("procedure-name", "User.plus1IO");
	}

	@Test // GH-3830
	void shouldDocumentProvidedProcedure() throws IOException {

		Resource resource = getResource();

		assertThat(resource).isNotNull();
		assertThat(resource.exists()).isTrue();

		String json = resource.getContentAsString(StandardCharsets.UTF_8);

		assertThatJson(json).inPath("$.methods[?(@.name == 'providedProcedure')].query").isArray().first().isObject()
				.containsEntry("procedure", "sp_add");
	}

	@Test // GH-3830
	void shouldDocumentBaseFragment() throws IOException {

		Resource resource = getResource();

		assertThat(resource).isNotNull();
		assertThat(resource.exists()).isTrue();

		String json = resource.getContentAsString(StandardCharsets.UTF_8);

		assertThatJson(json).inPath("$.methods[?(@.name == 'existsById')].fragment").isArray().first().isObject()
				.containsEntry("fragment", "org.springframework.data.jpa.repository.support.SimpleJpaRepository");
	}

	private Resource getResource() {

		String location = UserRepository.class.getPackageName().replace('.', '/') + "/"
				+ UserRepository.class.getSimpleName() + ".json";
		return new UrlResource(context.getBeanFactory().getBeanClassLoader().getResource(location));
	}

}
