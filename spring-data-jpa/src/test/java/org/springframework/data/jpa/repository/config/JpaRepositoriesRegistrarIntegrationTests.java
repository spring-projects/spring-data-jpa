/*
 * Copyright 2012-2023 the original author or authors.
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

import jakarta.persistence.EntityManagerFactory;

import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Integration test for {@link JpaRepositoriesRegistrar}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Erik Pellizzon
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
class JpaRepositoriesRegistrarIntegrationTests {

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
	void foo() {
		assertThat(repository).isNotNull();
	}

	@Test // DATAJPA-330
	void doesNotProxyPlainAtRepositoryBeans() {

		assertThat(sampleRepository).isNotNull();
		assertThat(AopUtils.isCglibProxy(sampleRepository)).isFalse();

		assertExceptionTranslationActive(repository);
	}

	@Repository
	static class SampleRepository {

	}

	public static void assertExceptionTranslationActive(Object repository) {

		if (repository == null) {
			return;
		}

		assertThat(repository).isInstanceOf(Advised.class);
		List<Advisor> advisors = Arrays.asList(((Advised) repository).getAdvisors());

		assertThat(advisors) //
				.extracting("advice") //
				.hasAtLeastOneElementOfType(PersistenceExceptionTranslationInterceptor.class);
	}
}
