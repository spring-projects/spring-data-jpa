/*
 * Copyright 2014-2019 the original author or authors.
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
package org.springframework.data.jpa.repository.support;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration tests for {@link EntityManagerBeanDefinitionRegistrarPostProcessor}.
 *
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class EntityManagerBeanDefinitionRegistrarPostProcessorIntegrationTests {

	@Configuration
	@ImportResource("classpath:infrastructure.xml")
	@ComponentScan(includeFilters = @Filter(TestComponent.class), useDefaultFilters = false)
	static class Config {

		@Autowired DataSource dataSource;
		@Autowired JpaVendorAdapter vendorAdapter;

		@Bean
		public static EntityManagerBeanDefinitionRegistrarPostProcessor processor() {
			return new EntityManagerBeanDefinitionRegistrarPostProcessor();
		}

		private LocalContainerEntityManagerFactoryBean emf() {

			LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
			factoryBean.setPersistenceUnitName("spring-data-jpa");
			factoryBean.setDataSource(dataSource);
			factoryBean.setJpaVendorAdapter(vendorAdapter);

			return factoryBean;
		}

		@Bean
		LocalContainerEntityManagerFactoryBean firstEmf() {
			return emf();
		}

		@Bean
		LocalContainerEntityManagerFactoryBean secondEmf() {
			return emf();
		}
	}

	@Autowired EntityManagerInjectionTarget target;

	@Test // DATAJPA-445
	public void injectsEntityManagerIntoConstructors() {

		assertThat(target, is(notNullValue()));
		assertThat(target.em, is(notNullValue()));
	}

	@TestComponent
	static class EntityManagerInjectionTarget {

		private final EntityManager em;

		@Autowired
		public EntityManagerInjectionTarget(@Qualifier("firstEmf") EntityManager em) {
			this.em = em;
		}
	}

	/**
	 * Annotation to demarcate test components.
	 *
	 * @author Oliver Gierke
	 */
	@Component
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	static @interface TestComponent {

	}
}
