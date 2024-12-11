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
package org.springframework.data.jpa.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.IOException;
import java.util.Collections;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.sample.SampleEvaluationContextExtension;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.data.jpa.repository.sample.UserRepositoryImpl;
import org.springframework.data.jpa.repository.support.DefaultJpaContext;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.support.PropertiesBasedNamedQueries;
import org.springframework.data.spel.ExtensionAwareEvaluationContextProvider;
import org.springframework.data.spel.spi.EvaluationContextExtension;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * Integration test for {@link UserRepository} using JavaConfig.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
@ContextConfiguration(inheritLocations = false, loader = AnnotationConfigContextLoader.class)
class JavaConfigUserRepositoryTests extends UserRepositoryTests {

	@Configuration
	@ImportResource("classpath:infrastructure.xml")
	static class Config {

		@PersistenceContext EntityManager entityManager;
		@Autowired ApplicationContext applicationContext;

		@Bean
		public EvaluationContextExtension sampleEvaluationContextExtension() {
			return new SampleEvaluationContextExtension();
		}

		@Bean
		public UserRepository userRepository() throws Exception {

			ExtensionAwareEvaluationContextProvider evaluationContextProvider = new ExtensionAwareEvaluationContextProvider(
					applicationContext);

			JpaRepositoryFactoryBean<UserRepository, User, Integer> factory = new JpaRepositoryFactoryBean<>(
					UserRepository.class);
			factory.setEntityManager(entityManager);
			factory.setBeanFactory(applicationContext);
			factory
					.setCustomImplementation(new UserRepositoryImpl(new DefaultJpaContext(Collections.singleton(entityManager))));
			factory.setNamedQueries(namedQueries());
			factory.setEvaluationContextProvider(evaluationContextProvider);
			factory.afterPropertiesSet();

			return factory.getObject();
		}

		@Bean
		public GreetingsFrom greetingsFrom() {
			return new GreetingsFrom();
		}

		private NamedQueries namedQueries() throws IOException {

			PropertiesFactoryBean factory = new PropertiesFactoryBean();
			factory.setLocation(new ClassPathResource("META-INF/jpa-named-queries.properties"));
			factory.afterPropertiesSet();

			return new PropertiesBasedNamedQueries(factory.getObject());
		}
	}

	@Test // DATAJPA-317
	void doesNotPickUpJpaRepository() {

		try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(JpaRepositoryConfig.class)) {
			Assertions.assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
					.isThrownBy(() -> context.getBean("jpaRepository"));
			context.close();
		}
	}

	@Configuration
	@EnableJpaRepositories(basePackageClasses = UserRepository.class)
	@ImportResource("classpath:infrastructure.xml")
	static class JpaRepositoryConfig {}
}
