/*
 * Copyright 2012-2019 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.support.PersistenceExceptionTranslationInterceptor;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.orm.jpa.JpaDialect;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ClassUtils;

/**
 * Integration test for {@link JpaRepositoriesRegistrar}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class JpaRepositoriesRegistrarIntegrationTests {

	@Autowired UserRepository repository;

	@Autowired SampleRepository sampleRepository;

	@Configuration
	@EnableJpaRepositories(basePackages = "org.springframework.data.jpa.repository.sample")
	static class Config {

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().generateUniqueName(true).build();
		}

		@Bean
		public EntityManagerFactory entityManagerFactory() {
			LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
			factory.setDataSource(dataSource());
			factory.setPersistenceUnitName("spring-data-jpa");
			factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
			factory.afterPropertiesSet();
			return factory.getObject();
		}

		@Bean
		public JpaDialect jpaDialect() {
			return new HibernateJpaDialect();
		}

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new JpaTransactionManager(entityManagerFactory());
		}

		@Bean
		public SampleRepository sampleRepository() {
			return new SampleRepository();
		}
	}

	@Test
	public void foo() {
		assertThat(repository, is(notNullValue()));
	}

	@Test // DATAJPA-330
	public void doesNotProxyPlainAtRepositoryBeans() {

		assertThat(sampleRepository, is(notNullValue()));
		assertThat(ClassUtils.isCglibProxy(sampleRepository), is(false));

		assertExceptionTranslationActive(repository);
	}

	@Repository
	static class SampleRepository {

	}

	public static void assertExceptionTranslationActive(Object repository) {

		if (repository == null) {
			return;
		}

		assertThat(repository, is(instanceOf(Advised.class)));
		List<Advisor> advisors = Arrays.asList(((Advised) repository).getAdvisors());
		assertThat(advisors, Matchers.<Advisor> hasItem(Matchers.<Advisor> hasProperty("advice",
				instanceOf(PersistenceExceptionTranslationInterceptor.class))));
	}
}
