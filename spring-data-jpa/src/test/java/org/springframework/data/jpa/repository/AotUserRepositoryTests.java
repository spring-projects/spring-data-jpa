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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.IOException;
import java.util.Collections;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.aot.AotFragmentTestConfigurationSupport;
import org.springframework.data.jpa.repository.sample.SampleConfig;
import org.springframework.data.jpa.repository.sample.SampleEvaluationContextExtension;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.data.jpa.repository.sample.UserRepositoryImpl;
import org.springframework.data.jpa.repository.support.DefaultJpaContext;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.support.PropertiesBasedNamedQueries;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.spel.ExtensionAwareEvaluationContextProvider;
import org.springframework.data.spel.spi.EvaluationContextExtension;
import org.springframework.test.context.ContextConfiguration;

/**
 * Integration test for {@link UserRepository} using JavaConfig with mounted AOT-generated repository methods.
 *
 * @author Mark Paluch
 */
@ContextConfiguration(classes = AotUserRepositoryTests.Config.class, inheritLocations = false)
class AotUserRepositoryTests extends UserRepositoryTests {

	@Configuration
	@ImportResource("classpath:hibernate-infrastructure.xml")
	static class Config {

		@PersistenceContext EntityManager entityManager;
		@Autowired ApplicationContext applicationContext;

		@Bean
		public EvaluationContextExtension sampleEvaluationContextExtension() {
			return new SampleEvaluationContextExtension();
		}

		@Bean
		static AotFragmentTestConfigurationSupport aot() {
			return new AotFragmentTestConfigurationSupport(UserRepository.class, SampleConfig.class, false,
					UserRepositoryImpl.class);
		}

		@Bean
		public UserRepository userRepository(BeanFactory beanFactory) throws Exception {

			ExtensionAwareEvaluationContextProvider evaluationContextProvider = new ExtensionAwareEvaluationContextProvider(
					applicationContext);

			JpaRepositoryFactoryBean<UserRepository, User, Integer> factory = new JpaRepositoryFactoryBean<>(
					UserRepository.class);
			factory.setEntityManager(entityManager);
			factory.setBeanFactory(applicationContext);
			factory
					.setCustomImplementation(new UserRepositoryImpl(new DefaultJpaContext(Collections.singleton(entityManager))));

			factory.setRepositoryFragments(RepositoryComposition.RepositoryFragments.just(beanFactory.getBean("fragment")));

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

}
