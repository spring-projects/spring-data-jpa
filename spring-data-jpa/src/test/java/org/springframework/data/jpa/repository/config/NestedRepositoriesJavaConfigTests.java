/*
 * Copyright 2013-2025 the original author or authors.
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
package org.springframework.data.jpa.repository.config;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.jpa.repository.sample.ClassWithNestedRepository.NestedUserRepository;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.data.repository.support.Repositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Integration test for the combination of JavaConfig and an {@link Repositories} wrapper.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Jens Schauder
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
class NestedRepositoriesJavaConfigTests {

	@Autowired NestedUserRepository nestedUserRepository;

	@Test // DATAJPA-416
	void shouldSupportNestedRepositories() {
		assertThat(nestedUserRepository).isNotNull();
	}

	@Configuration
	@EnableJpaRepositories(basePackageClasses = UserRepository.class, considerNestedRepositories = true)
	@ImportResource("classpath:infrastructure.xml")
	static class Config {}
}
