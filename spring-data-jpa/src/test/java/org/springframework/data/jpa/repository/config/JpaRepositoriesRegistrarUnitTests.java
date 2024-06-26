/*
 * Copyright 2012-2024 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.jpa.repository.sample.UserRepository;

/**
 * Unit test for {@link JpaRepositoriesRegistrar}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Erik Pellizzon
 * @author Christoph Strobl
 */
class JpaRepositoriesRegistrarUnitTests {

	private BeanDefinitionRegistry registry;

	@BeforeEach
	void setUp() {
		registry = new DefaultListableBeanFactory();
	}


	@ParameterizedTest // GH-499, GH-3440
	@MethodSource(value = { "args" })
	void configuresRepositoriesCorrectly(AnnotationMetadata metadata, String[] beanNames) {

		JpaRepositoriesRegistrar registrar = new JpaRepositoriesRegistrar();
		registrar.setResourceLoader(new DefaultResourceLoader());
		registrar.setEnvironment(new StandardEnvironment());
		registrar.registerBeanDefinitions(metadata, registry);

		Iterable<String> names = Arrays.asList(registry.getBeanDefinitionNames());
		assertThat(names).contains(beanNames);
	}

	static Stream<Arguments> args() {
		return Stream.of(
				Arguments.of(AnnotationMetadata.introspect(Config.class),
						new String[] { "userRepository", "auditableUserRepository", "roleRepository" }),
				Arguments.of(AnnotationMetadata.introspect(ConfigWithBeanNameGenerator.class),
						new String[] { "userREPO", "auditableUserREPO", "roleREPO" }));
	}

	@EnableJpaRepositories(basePackageClasses = UserRepository.class)
	private class Config {

	}

	@EnableJpaRepositories(basePackageClasses = UserRepository.class, nameGenerator = MyBeanNameGenerator.class)
	private class ConfigWithBeanNameGenerator {

	}

	static class MyBeanNameGenerator extends AnnotationBeanNameGenerator {

		@Override
		public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
			return super.generateBeanName(definition, registry).replaceAll("Repository", "REPO");
		}
	}
}
