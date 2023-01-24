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
package org.springframework.data.jpa.repository.query;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;

import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.domain.sample.EmployeeWithName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.Param;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.lang.Nullable;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verify that {@literal LIKE}s mixed with {@literal NULL}s work properly.
 *
 * @author Greg Turnquist
 * @author Yuriy Tsarkov
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = QueryWithNullLikeHibernateIntegrationTests.Config.class)
@Transactional
class QueryWithNullLikeHibernateIntegrationTests {

	@Autowired EmployeeWithNullLikeRepository repository;

	@BeforeEach
	void setUp() {
		repository.saveAllAndFlush(List.of( //
				new EmployeeWithName("Frodo Baggins"), //
				new EmployeeWithName("Bilbo Baggins")));
	}

	@Test
	void customQueryWithMultipleMatch() {

		List<EmployeeWithName> Employees = repository.customQueryWithNullableParam("Baggins");

		assertThat(Employees).extracting(EmployeeWithName::getName).containsExactlyInAnyOrder("Frodo Baggins",
				"Bilbo Baggins");
	}

	@Test
	void customQueryWithSingleMatch() {

		List<EmployeeWithName> Employees = repository.customQueryWithNullableParam("Frodo");

		assertThat(Employees).extracting(EmployeeWithName::getName).containsExactlyInAnyOrder("Frodo Baggins");
	}

	@Test
	void customQueryWithEmptyStringMatch() {

		List<EmployeeWithName> Employees = repository.customQueryWithNullableParam("");

		assertThat(Employees).extracting(EmployeeWithName::getName).containsExactlyInAnyOrder("Frodo Baggins",
				"Bilbo Baggins");
	}

	@Test
	void customQueryWithNullMatch() {

		List<EmployeeWithName> Employees = repository.customQueryWithNullableParam(null);

		assertThat(Employees).extracting(EmployeeWithName::getName).isEmpty();
	}

	@Test
	void derivedQueryStartsWithSingleMatch() {

		List<EmployeeWithName> Employees = repository.findByNameStartsWith("Frodo");

		assertThat(Employees).extracting(EmployeeWithName::getName).containsExactlyInAnyOrder("Frodo Baggins");
	}

	@Test
	void derivedQueryStartsWithNoMatch() {

		List<EmployeeWithName> Employees = repository.findByNameStartsWith("Baggins");

		assertThat(Employees).extracting(EmployeeWithName::getName).isEmpty();
	}

	@Test
	void derivedQueryStartsWithWithEmptyStringMatch() {

		List<EmployeeWithName> Employees = repository.findByNameStartsWith("");

		assertThat(Employees).extracting(EmployeeWithName::getName).containsExactlyInAnyOrder("Frodo Baggins",
				"Bilbo Baggins");
	}

	@Test
	void derivedQueryStartsWithWithNullMatch() {

		List<EmployeeWithName> Employees = repository.findByNameStartsWith(null);

		assertThat(Employees).extracting(EmployeeWithName::getName).isEmpty();
	}

	@Test
	void derivedQueryEndsWithWithMultipleMatch() {

		List<EmployeeWithName> Employees = repository.findByNameEndsWith("Baggins");

		assertThat(Employees).extracting(EmployeeWithName::getName).containsExactlyInAnyOrder("Frodo Baggins",
				"Bilbo Baggins");
	}

	@Test
	void derivedQueryEndsWithWithSingleMatch() {

		List<EmployeeWithName> Employees = repository.findByNameEndsWith("Frodo");

		assertThat(Employees).extracting(EmployeeWithName::getName).isEmpty();
	}

	@Test
	void derivedQueryEndsWithWithEmptyStringMatch() {

		List<EmployeeWithName> Employees = repository.findByNameEndsWith("");

		assertThat(Employees).extracting(EmployeeWithName::getName).containsExactlyInAnyOrder("Frodo Baggins",
				"Bilbo Baggins");
	}

	@Test
	void derivedQueryEndsWithWithNullMatch() {

		List<EmployeeWithName> Employees = repository.findByNameEndsWith(null);

		assertThat(Employees).extracting(EmployeeWithName::getName).isEmpty();
	}

	@Test
	void derivedQueryContainsWithMultipleMatch() {

		List<EmployeeWithName> Employees = repository.findByNameContains("Baggins");

		assertThat(Employees).extracting(EmployeeWithName::getName).containsExactlyInAnyOrder("Frodo Baggins",
				"Bilbo Baggins");
	}

	@Test
	void derivedQueryContainsWithSingleMatch() {

		List<EmployeeWithName> Employees = repository.findByNameContains("Frodo");

		assertThat(Employees).extracting(EmployeeWithName::getName).containsExactly("Frodo Baggins");
	}

	@Test
	void derivedQueryContainsWithEmptyStringMatch() {

		List<EmployeeWithName> Employees = repository.findByNameContains("");

		assertThat(Employees).extracting(EmployeeWithName::getName).containsExactlyInAnyOrder("Frodo Baggins",
				"Bilbo Baggins");
	}

	@Test
	void derivedQueryContainsWithNullMatch() {

		List<EmployeeWithName> Employees = repository.findByNameContains(null);

		assertThat(Employees).extracting(EmployeeWithName::getName).isEmpty();
	}

	@Test
	void derivedQueryLikeWithMultipleMatch() {

		List<EmployeeWithName> Employees = repository.findByNameLike("%Baggins%");

		assertThat(Employees).extracting(EmployeeWithName::getName).containsExactlyInAnyOrder("Frodo Baggins",
				"Bilbo Baggins");
	}

	@Test
	void derivedQueryLikeWithSingleMatch() {

		List<EmployeeWithName> Employees = repository.findByNameLike("%Frodo%");

		assertThat(Employees).extracting(EmployeeWithName::getName).containsExactly("Frodo Baggins");
	}

	@Test
	void derivedQueryLikeWithEmptyStringMatch() {

		List<EmployeeWithName> Employees = repository.findByNameLike("%%");

		assertThat(Employees).extracting(EmployeeWithName::getName).containsExactlyInAnyOrder("Frodo Baggins",
				"Bilbo Baggins");
	}

	@Transactional
	public interface EmployeeWithNullLikeRepository extends JpaRepository<EmployeeWithName, Integer> {

		@Query("select e from EmployeeWithName e where e.name like %:partialName%")
		List<EmployeeWithName> customQueryWithNullableParam(@Nullable @Param("partialName") String partialName);

		List<EmployeeWithName> findByNameStartsWith(@Nullable String partialName);

		List<EmployeeWithName> findByNameEndsWith(@Nullable String partialName);

		List<EmployeeWithName> findByNameContains(@Nullable String partialName);

		List<EmployeeWithName> findByNameLike(@Nullable String partialName);
	}

	@EnableJpaRepositories(considerNestedRepositories = true, //
			includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = EmployeeWithNullLikeRepository.class))
	@EnableTransactionManagement
	static class Config {

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().generateUniqueName(true).build();
		}

		@Bean
		AbstractEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {

			LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
			factoryBean.setDataSource(dataSource);
			factoryBean.setPersistenceUnitName("spring-data-jpa");
			factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

			Properties properties = new Properties();
			properties.setProperty("hibernate.hbm2ddl.auto", "create");
			factoryBean.setJpaProperties(properties);

			return factoryBean;
		}

		@Bean
		PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
			return new JpaTransactionManager(emf);
		}
	}
}
