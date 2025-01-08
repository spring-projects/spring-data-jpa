/*
 * Copyright 2012-2025 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.core.support.RepositoryFactoryInformation;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author Jens Schauder
 * @author Krzysztof Krason
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:config/namespace-application-context.xml")
class SPR8954Tests {

	@Autowired ApplicationContext context;

	@Test
	@SuppressWarnings("rawtypes")
	void canAccessRepositoryFactoryInformationFactoryBeans() {

		Map<String, RepositoryFactoryInformation> repoFactories = context
				.getBeansOfType(RepositoryFactoryInformation.class);

		assertThat(repoFactories).isNotEmpty();
		assertThat(repoFactories.keySet()).contains("&userRepository");
		assertThat(repoFactories.get("&userRepository")).isInstanceOf(JpaRepositoryFactoryBean.class);
		assertThat(context.getBeanNamesForType(UserRepository.class)).contains("userRepository");
	}
}
