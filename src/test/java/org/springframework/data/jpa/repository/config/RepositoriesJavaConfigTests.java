/*
 * Copyright 2013-2019 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.data.repository.support.Repositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration test for the combination of JavaConfig and an {@link Repositories} wrapper.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class RepositoriesJavaConfigTests {

	@Autowired Repositories repositories;

	@Test // DATAJPA-323
	public void foo() {
		assertThat(repositories.hasRepositoryFor(User.class)).isTrue();
	}

	@Configuration
	@EnableJpaRepositories(basePackageClasses = UserRepository.class)
	@ImportResource("classpath:infrastructure.xml")
	static class Config {

		@Autowired ApplicationContext context;

		@Bean
		public Repositories repositories() {
			return new Repositories(context);
		}
	}
}
