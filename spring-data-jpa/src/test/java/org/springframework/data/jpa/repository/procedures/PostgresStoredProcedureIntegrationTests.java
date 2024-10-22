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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureParameter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.hibernate.dialect.PostgreSQLDialect;
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
import org.springframework.data.jpa.util.DisabledOnHibernate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Testcase to verify {@link org.springframework.jdbc.object.StoredProcedure}s work with Postgres.
 *
 * @author Gabriel Basilio
 * @author Greg Turnquist
 * @author Yanming Zhou
 * @author Thorben Janssen
 * @author Mark Paluch
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

	@DisabledOnHibernate("6")
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

	@Test // GH-3460
	void testPositionalInOutParameter() {

		Map results = repository.positionalInOut(1, 2);

		assertThat(results.get("2")).isEqualTo(2);
		assertThat(results.get("3")).isEqualTo(3);
	}

	@Test // GH-3460
	void supportsMultipleOutParameters() {

		Map<String, Object> results = repository.multiple_out(5);

		assertThat(results).containsEntry("result1", 5).containsEntry("result2", 10);
		assertThat(results).containsKey("some_cursor");
	}

	@Test // GH-3081
	@DisabledOnHibernate(value = "6.2",
			disabledReason = "Hibernate 6.2 does not support stored procedures with array types")
	void supportsArrayTypes() {

		String result = repository.accept_array(new String[] { "one", "two" });

		assertThat(result).isEqualTo("[1:2]");
	}

	@Entity
	@NamedStoredProcedureQuery( //
			name = "get_employees_postgres", //
			procedureName = "get_employees", //
			parameters = { @StoredProcedureParameter(mode = ParameterMode.REF_CURSOR, type = void.class) }, //
			resultClasses = Employee.class)

	@NamedStoredProcedureQuery( //
			name = "Employee.noResultSet", //
			procedureName = "get_employees_count", //
			parameters = { @StoredProcedureParameter(mode = ParameterMode.OUT, name = "results", type = Integer.class) })
	@NamedStoredProcedureQuery( //
			name = "Employee.multiple_out", //
			procedureName = "multiple_out", //
			parameters = { @StoredProcedureParameter(mode = ParameterMode.IN, name = "someNumber", type = Integer.class),
					@StoredProcedureParameter(mode = ParameterMode.REF_CURSOR, name = "some_cursor", type = void.class),
					@StoredProcedureParameter(mode = ParameterMode.OUT, name = "result1", type = Integer.class),
					@StoredProcedureParameter(mode = ParameterMode.OUT, name = "result2", type = Integer.class) })
	@NamedStoredProcedureQuery( //
			name = "Employee.accept_array", //
			procedureName = "accept_array", //
			parameters = { @StoredProcedureParameter(mode = ParameterMode.IN, name = "some_chars", type = String[].class),
					@StoredProcedureParameter(mode = ParameterMode.OUT, name = "dims", type = String.class) })
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

		@Procedure(value = "accept_array")
		String accept_array(String[] some_chars);

		@Procedure(value = "multiple_out")
		Map<String, Object> multiple_out(int someNumber);

		@Procedure(name = "get_employees_postgres", refCursor = true)
		List<Employee> entityListFromNamedProcedure();

		@Procedure(name = "positional_inout")
		Map positionalInOut(Integer in, Integer inout);
	}

	@EnableJpaRepositories(considerNestedRepositories = true,
			includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = EmployeeRepositoryWithRefCursor.class))
	@EnableTransactionManagement
	static class Config extends StoredProcedureConfigSupport {

		public Config() {
			super(PostgreSQLDialect.class, new ClassPathResource("scripts/postgres-stored-procedures.sql"));
		}

		@SuppressWarnings("resource")
		@Bean(initMethod = "start", destroyMethod = "stop")
		public PostgreSQLContainer<?> container() {

			return new PostgreSQLContainer<>("postgres:15.3") //
					.withUsername("postgres");
		}
	}
}
