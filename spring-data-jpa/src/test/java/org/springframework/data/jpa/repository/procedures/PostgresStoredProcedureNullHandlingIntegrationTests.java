/*
 * Copyright 2015-2023 the original author or authors.
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
package org.springframework.data.jpa.repository.procedures;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Properties;
import java.util.UUID;

import javax.sql.DataSource;

import org.hibernate.dialect.PostgreSQL91Dialect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Temporal;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Testcase to verify {@link org.springframework.jdbc.object.StoredProcedure}s properly handle null values.
 *
 * @author Greg Turnquist
 */
@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PostgresStoredProcedureNullHandlingIntegrationTests.Config.class)
class PostgresStoredProcedureNullHandlingIntegrationTests {

	@Autowired TestModelRepository repository;

	@Test // 2544
	void invokingNullOnNonTemporalStoredProcedureParameterShouldWork() {
		repository.countUuid(null);
	}

	@Test // 2544
	void invokingNullOnTemporalStoredProcedureParameterShouldWork() {
		repository.countLocalDate(null);
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Entity
	class TestModel {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO) private long id;
		private UUID uuid;
		private Date date;
	}

	@Transactional
	public interface TestModelRepository extends JpaRepository<TestModel, Long> {

		@Procedure("countByUuid")
		void countUuid(UUID this_uuid);

		@Procedure("countByLocalDate")
		void countLocalDate(@Temporal Date localDate);
	}

	@EnableJpaRepositories(considerNestedRepositories = true,
			includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TestModelRepository.class))
	@EnableTransactionManagement
	static class Config {

		@Bean(initMethod = "start", destroyMethod = "stop")
		public PostgreSQLContainer<?> container() {

			return new PostgreSQLContainer<>("postgres:9.6.12") //
					.withUsername("postgres");
		}

		@Bean
		public DataSource dataSource(PostgreSQLContainer<?> container) {

			PGSimpleDataSource dataSource = new PGSimpleDataSource();
			dataSource.setUrl(container.getJdbcUrl());
			dataSource.setUser(container.getUsername());
			dataSource.setPassword(container.getPassword());

			return dataSource;
		}

		@Bean
		public AbstractEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {

			LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
			factoryBean.setDataSource(dataSource);
			factoryBean.setPersistenceUnitRootLocation("simple-persistence");
			factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
			factoryBean.setPackagesToScan(this.getClass().getPackage().getName());

			Properties properties = new Properties();
			properties.setProperty("hibernate.hbm2ddl.auto", "create");
			properties.setProperty("hibernate.dialect", PostgreSQL91Dialect.class.getCanonicalName());
			properties.setProperty("hibernate.proc.param_null_passing", "true");
			properties.setProperty("hibernate.globally_quoted_identifiers", "true");
			properties.setProperty("hibernate.globally_quoted_identifiers_skip_column_definitions", "true");
			factoryBean.setJpaProperties(properties);

			return factoryBean;
		}

		@Bean
		PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
			return new JpaTransactionManager(entityManagerFactory);
		}

		@Bean
		DataSourceInitializer initializer(DataSource dataSource) {

			DataSourceInitializer initializer = new DataSourceInitializer();
			initializer.setDataSource(dataSource);

			ClassPathResource script = new ClassPathResource("scripts/postgres-nullable-stored-procedures.sql");
			ResourceDatabasePopulator populator = new ResourceDatabasePopulator(script);
			populator.setSeparator(";;");
			initializer.setDatabasePopulator(populator);

			return initializer;
		}
	}
}
