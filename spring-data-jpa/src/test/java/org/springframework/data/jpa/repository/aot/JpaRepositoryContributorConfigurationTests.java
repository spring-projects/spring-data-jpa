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

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.UrlResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Integration tests for the {@link UserRepository} AOT fragment.
 *
 * @author Mark Paluch
 */
class JpaRepositoryContributorConfigurationTests {

	@Configuration
	static class JpaRepositoryContributorConfiguration extends AotFragmentTestConfigurationSupport {
		public JpaRepositoryContributorConfiguration() {
			super(UserRepository.class, MyConfiguration.class);
		}

		@EnableJpaRepositories(escapeCharacter = 'ö', /* avoid creating repository instances */ includeFilters = {
				@ComponentScan.Filter(value = EnableJpaRepositories.class) })
		static class MyConfiguration {

		}
	}

	@Test // GH-3838
	void shouldConsiderConfiguration() throws IOException {

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(JpaRepositoryContributorConfiguration.class);
		context.refreshForAotProcessing(new RuntimeHints());

		String location = UserRepository.class.getPackageName().replace('.', '/') + "/"
				+ UserRepository.class.getSimpleName() + ".json";
		UrlResource resource = new UrlResource(context.getBeanFactory().getBeanClassLoader().getResource(location));

		assertThat(resource).isNotNull();
		assertThat(resource.exists()).isTrue();

		String json = resource.getContentAsString(StandardCharsets.UTF_8);

		assertThatJson(json).inPath("$.methods[?(@.name == 'streamByLastnameLike')].query").isArray().first().isObject()
				.containsEntry("query",
						"SELECT u FROM User u WHERE u.lastname LIKE :lastname ESCAPE 'ö'");
	}

}
