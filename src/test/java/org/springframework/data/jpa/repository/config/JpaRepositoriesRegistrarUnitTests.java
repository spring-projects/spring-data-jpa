/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository.config;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.data.jpa.repository.sample.UserRepository;

/**
 * Unit test for {@link JpaRepositoriesRegistrar}.
 *
 * @author Oliver Gierke
 */
public class JpaRepositoriesRegistrarUnitTests {

	BeanDefinitionRegistry registry;
	AnnotationMetadata metadata;

	@Before
	public void setUp() {

		metadata = new StandardAnnotationMetadata(Config.class, true);
		registry = new DefaultListableBeanFactory();
	}

	@Test
	public void configuresRepositoriesCorrectly() {

		JpaRepositoriesRegistrar registrar = new JpaRepositoriesRegistrar();
		registrar.setResourceLoader(new DefaultResourceLoader());
		registrar.setEnvironment(new StandardEnvironment());
		registrar.registerBeanDefinitions(metadata, registry);

		Iterable<String> names = Arrays.asList(registry.getBeanDefinitionNames());
		assertThat(names, hasItems("userRepository", "auditableUserRepository", "roleRepository"));
	}

	@EnableJpaRepositories(basePackageClasses = UserRepository.class)
	class Config {

	}
}
