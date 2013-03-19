/*
 * Copyright 2012-2013 the original author or authors.
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
package org.springframework.data.jpa.repository;

import java.io.IOException;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.data.jpa.repository.sample.UserRepositoryImpl;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.support.PropertiesBasedNamedQueries;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * Integration test for {@link UserRepository} using JavaConfig.
 * 
 * @author Oliver Gierke
 */
@ContextConfiguration(inheritLocations = false, loader = AnnotationConfigContextLoader.class)
public class JavaConfigUserRepositoryTests extends UserRepositoryTests {

	@Configuration
	@ImportResource("classpath:infrastructure.xml")
	static class Config {

		@PersistenceContext
		EntityManager entityManager;
		@Autowired
		BeanFactory beanFactory;

		@Bean
		public UserRepository userRepository() throws IOException {

			JpaRepositoryFactoryBean<UserRepository, User, Integer> factory = new JpaRepositoryFactoryBean<UserRepository, User, Integer>();
			factory.setEntityManager(entityManager);
			factory.setBeanFactory(beanFactory);
			factory.setRepositoryInterface(UserRepository.class);
			factory.setCustomImplementation(new UserRepositoryImpl());
			factory.setNamedQueries(namedQueries());
			factory.afterPropertiesSet();

			return factory.getObject();
		}

		private NamedQueries namedQueries() throws IOException {

			PropertiesFactoryBean factory = new PropertiesFactoryBean();
			factory.setLocation(new ClassPathResource("META-INF/jpa-named-queries.properties"));
			factory.afterPropertiesSet();

			return new PropertiesBasedNamedQueries(factory.getObject());
		}
	}

	/**
	 * @see DATAJPA-317
	 */
	@Test(expected = NoSuchBeanDefinitionException.class)
	public void doesNotPickUpJpaRepository() {

		ApplicationContext context = new AnnotationConfigApplicationContext(JpaRepositoryConfig.class);
		context.getBean("jpaRepository");
	}

	@Configuration
	@EnableJpaRepositories
	@ImportResource("classpath:infrastructure.xml")
	static class JpaRepositoryConfig {

	}
}
