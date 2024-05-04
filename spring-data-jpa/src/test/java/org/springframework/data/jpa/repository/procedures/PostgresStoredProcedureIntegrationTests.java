/*
 * Copyright 2015-2024 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureParameter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import javax.sql.DataSource;

import org.hibernate.dialect.PostgreSQLDialect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.jpa.util.DisabledOnHibernate61;
import org.springframework.data.jpa.util.DisabledOnHibernate62;
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
 * Testcase to verify {@link org.springframework.jdbc.object.StoredProcedure}s work with Postgres.
 *
 * @author Gabriel Basilio
 * @author Greg Turnquist
 * @author Yanming Zhou
 */
@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PostgresStoredProcedureIntegrationTests.Config.class)
class PostgresStoredProcedureIntegrationTests {

	@Autowired EmployeeRepositoryWithRefCursor repository;

	@Test // 2256
	void testGenericSingleObjectFromResultSet() {

		Object[] employee = repository.genericSingleObjectFromResultSet();

		assertThat(employee).containsExactly( //
				new Object[] { new BigDecimal("3"), "Fanny" }, //
				new Object[] { new BigDecimal("4"), "Gabriel" });
	}

	@Test // 2256
	void testGenericObjectsFromResultSet() {

		List<Object[]> employees = repository.genericObjectsFromResultSet();

		assertThat(employees).containsExactly( //
				new Object[] { new BigDecimal("3"), "Fanny" }, //
				new Object[] { new BigDecimal("4"), "Gabriel" });
	}

	@Test // 2256
	void testEntityListFromResultSet() {

		List<Employee> employees = repository.entityListFromResultSet();

		assertThat(employees).containsExactly( //
				new Employee(3, "Fanny"), //
				new Employee(4, "Gabriel"));
	}

	@Test // 2256
	void testNamedOutputParameter() {

		List<Employee> employees = repository.namedOutputParameter();

		assertThat(employees).containsExactly( //
				new Employee(3, "Fanny"), //
				new Employee(4, "Gabriel"));
	}

	@DisabledOnHibernate62
	@Test // 2256
	void testSingleEntityFromResultSet() {

		Employee employee = repository.singleEntityFromResultSet();

		assertThat(employee).isEqualTo(new Employee(3, "Fanny"));
	}

	@Test // 2256
	void testEntityListFromSingleRowResultSet() {

		List<Employee> employees = repository.entityListFromSingleRowResultSet();

		assertThat(employees).containsExactly(new Employee(3, "Fanny"));
	}

	@Test // 2256
	void testNoResultSet() {

		int count = repository.noResultSet();

		assertThat(count).isEqualTo(2);
	}

	@Test // 2256
	void testEntityListFromNamedProcedure() {

		List<Employee> employees = repository.entityListFromNamedProcedure();

		assertThat(employees).containsExactly( //
				new Employee(3, "Fanny"), //
				new Employee(4, "Gabriel"));
	}

	@Test // 3460
	void testPositionalInOutParameter() {

		Map results = repository.positionalInOut(1, 2);

		// TODO: Check result format
		assertThat(results.get("2")).isEqualTo(2);
		assertThat(results.get("3")).isEqualTo(3);
	}

	@Entity
	@NamedStoredProcedureQuery( //
			name = "get_employees_postgres", //
			procedureName = "get_employees", //
			parameters = { @StoredProcedureParameter(mode = ParameterMode.REF_CURSOR, type = void.class) }, //
			resultClasses = Employee.class)
	@NamedStoredProcedureQuery( //
			name = "positional_inout", //
			procedureName = "positional_inout_parameter_issue3460", //
			parameters = { @StoredProcedureParameter(mode = ParameterMode.IN, type = Integer.class),
							@StoredProcedureParameter(mode = ParameterMode.INOUT, type = Integer.class),
							@StoredProcedureParameter(mode = ParameterMode.OUT, type = Integer.class) }, //
			resultClasses = Employee.class)	
	public static class Employee {

		@Id
		@GeneratedValue //
		private Integer id;
		private String name;

		public Employee(Integer id, String name) {

			this.id = id;
			this.name = name;
		}

		public Employee() {}

		public Integer getId() {
			return this.id;
		}

		public String getName() {
			return this.name;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public boolean equals(Object o) {

			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			Employee employee = (Employee) o;
			return Objects.equals(id, employee.id) && Objects.equals(name, employee.name);
		}

		@Override
		public int hashCode() {
			return Objects.hash(id, name);
		}

		public String toString() {
			return "PostgresStoredProcedureIntegrationTests.Employee(id=" + this.getId() + ", name=" + this.getName() + ")";
		}
	}

	@Transactional
	public interface EmployeeRepositoryWithRefCursor extends JpaRepository<Employee, Integer> {

		@Procedure(value = "get_employees", refCursor = true)
		Object[] genericSingleObjectFromResultSet();

		@Procedure(value = "get_employees", refCursor = true)
		List<Object[]> genericObjectsFromResultSet();

		@Procedure(value = "get_employees", refCursor = true)
		List<Employee> entityListFromResultSet();

		@Procedure(value = "get_employees", outputParameterName = "p_employees", refCursor = true)
		List<Employee> namedOutputParameter();

		@Procedure(value = "get_single_employee", refCursor = true)
		Employee singleEntityFromResultSet();

		@Procedure(value = "get_single_employee", refCursor = true)
		List<Employee> entityListFromSingleRowResultSet();

		@Procedure(value = "get_employees_count")
		Integer noResultSet();

		@Procedure(name = "get_employees_postgres", refCursor = true)
		List<Employee> entityListFromNamedProcedure();

		@Procedure(name = "positional_inout")
		Map positionalInOut(Integer in, Integer inout);
	}

	@EnableJpaRepositories(considerNestedRepositories = true,
			includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = EmployeeRepositoryWithRefCursor.class))
	@EnableTransactionManagement
	static class Config {

		@SuppressWarnings("resource")
		@Bean(initMethod = "start", destroyMethod = "stop")
		public PostgreSQLContainer<?> container() {

			return new PostgreSQLContainer<>("postgres:15.3") //
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
			properties.setProperty("hibernate.dialect", PostgreSQLDialect.class.getCanonicalName());
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

			ClassPathResource script = new ClassPathResource("scripts/postgres-stored-procedures.sql");
			ResourceDatabasePopulator populator = new ResourceDatabasePopulator(script);
			populator.setSeparator(";;");
			initializer.setDatabasePopulator(populator);

			return initializer;
		}
	}
}
