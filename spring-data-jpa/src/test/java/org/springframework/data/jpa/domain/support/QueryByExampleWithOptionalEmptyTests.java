/*
 * Copyright 2008-2023 the original author or authors.
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
package org.springframework.data.jpa.domain.support;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.domain.sample.UserWithOptionalField;
import org.springframework.data.jpa.domain.sample.UserWithOptionalFieldRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Integration test for {@link org.springframework.data.repository.query.QueryByExampleExecutor} involving
 * {@link Optional#empty()}.
 *
 * @author Greg Turnquist
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
class QueryByExampleWithOptionalEmptyTests {

	@Autowired UserWithOptionalFieldRepository repository;
	UserWithOptionalField user;

	@Test
	void queryByExampleTreatsEmptyOptionalsLikeNulls() {

		UserWithOptionalField user = new UserWithOptionalField();
		user.setName("Greg");
		repository.saveAndFlush(user);

		UserWithOptionalField probe = new UserWithOptionalField();
		probe.setName("Greg");
		Example<UserWithOptionalField> example = Example.of(probe);

		List<UserWithOptionalField> results = repository.findAll(example);

		assertThat(results).hasSize(1);
		assertThat(results).extracting(UserWithOptionalField::getName).containsExactly("Greg");
	}

	@Configuration
	@EnableJpaRepositories(basePackageClasses = UserWithOptionalFieldRepository.class)
	@ImportResource("classpath:infrastructure.xml")
	static class JpaRepositoryConfig {}

}
