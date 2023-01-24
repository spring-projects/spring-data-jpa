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

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedStoredProcedureQuery;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.hibernate.dialect.MySQL8Dialect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.JpaRepository;
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
import org.testcontainers.containers.MySQLContainer;

import com.mysql.cj.jdbc.MysqlDataSource;

/**
 * Testcase to verify {@link org.springframework.jdbc.object.StoredProcedure}s work with MySQL.
 *
 * @author Gabriel Basilio
 * @author Greg Turnquist
 * @author Yanming Zhou
 */
@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MySqlStoredProcedureIntegrationTests.Config.class)
class MySqlStoredProcedureIntegrationTests {

	@Autowired EmployeeRepositoryWithNoCursor repository;

	@Test // #2256
	void testGenericSingleObjectFromResultSet() {

		Object[] employee = repository.genericSingleObjectFromResultSet();

		assertThat(employee).containsExactly( //
				new Object[] { 3, "Fanny" }, //
				new Object[] { 4, "Gabriel" });
	}

	@Test // #2256
	void testGenericObjectsFromResultSet() {

		List<Object[]> employees = repository.genericObjectsFromResultSet();

		assertThat(employees).containsExactly( //
				new Object[] { 3, "Fanny" }, //
				new Object[] { 4, "Gabriel" });
	}

	@Test // #2256
	void testEntityListFromResultSet() {

		List<Employee> employees = repository.entityListFromResultSet();

		assertThat(employees).containsExactly( //
				new Employee(3, "Fanny"), //
				new Employee(4, "Gabriel"));
	}

	@Test // #2256
	void testNamedOutputParameter() {

		List<Employee> employees = repository.namedOutputParameter();

		assertThat(employees).containsExactly( //
				new Employee(3, "Fanny"), //
				new Employee(4, "Gabriel"));
	}

	@Test // #2256
	void testSingleEntityFromResultSet() {

		Employee employee = repository.singleEntityFromResultSet();

		assertThat(employee).isEqualTo(new Employee(3, "Fanny"));
	}

	@Test // #2256
	void testEntityListFromSingleRowResultSet() {

		List<Employee> employees = repository.entityListFromSingleRowResultSet();

		assertThat(employees).containsExactly(new Employee(3, "Fanny"));
	}

	@Test // #2256
	void testNoResultSet() {

		int count = repository.noResultSet();

		assertThat(count).isEqualTo(2);
	}

	@Test // #2256
	void testEntityListFromNamedProcedure() {

		List<Employee> employees = repository.entityListFromNamedProcedure();

		assertThat(employees).containsExactly( //
				new Employee(3, "Fanny"), //
				new Employee(4, "Gabriel"));
	}

	@Data
	@Entity
	@AllArgsConstructor
	@NoArgsConstructor
	@NamedStoredProcedureQuery(name = "get_employees_mysql", procedureName = "get_employees",
			resultClasses = Employee.class)
	public static class Employee {

		@Id
		@GeneratedValue private Integer id;
		private String name;
	}

	@Transactional
	public interface EmployeeRepositoryWithNoCursor extends JpaRepository<Employee, Integer> {

		@Procedure(value = "get_employees")
		Object[] genericSingleObjectFromResultSet();

		@Procedure(value = "get_employees")
		List<Object[]> genericObjectsFromResultSet();

		@Procedure(value = "get_employees")
		List<Employee> entityListFromResultSet();

		@Procedure(value = "get_employees", outputParameterName = "p_employees")
		List<Employee> namedOutputParameter();

		@Procedure(value = "get_single_employee")
		Employee singleEntityFromResultSet();

		@Procedure(value = "get_single_employee")
		List<Employee> entityListFromSingleRowResultSet();

		@Procedure(value = "get_employees_count")
		Integer noResultSet();

		@Procedure(name = "get_employees_mysql")
		List<Employee> entityListFromNamedProcedure();
	}

	@EnableJpaRepositories( //
			considerNestedRepositories = true, //
			basePackageClasses = Config.class, //
			includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = EmployeeRepositoryWithNoCursor.class))
	@EnableTransactionManagement
	static class Config {

		@SuppressWarnings("resource")
		@Bean(initMethod = "start", destroyMethod = "stop")
		public MySQLContainer<?> container() {

			return new MySQLContainer<>("mysql:8.0.24") //
					.withUsername("test") //
					.withPassword("test") //
					.withConfigurationOverride("");
		}

		@Bean
		public DataSource dataSource(MySQLContainer<?> container) {

			MysqlDataSource dataSource = new MysqlDataSource();
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
			properties.setProperty("hibernate.dialect", MySQL8Dialect.class.getCanonicalName());
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

			ClassPathResource script = new ClassPathResource("scripts/mysql-stored-procedures.sql");
			ResourceDatabasePopulator populator = new ResourceDatabasePopulator(script);
			populator.setSeparator(";;");
			initializer.setDatabasePopulator(populator);

			return initializer;
		}
	}
}
