/*
 * Copyright 2017-2023 the original author or authors.
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
package org.springframework.data.jpa.repository.projections;

import static org.assertj.core.api.Assertions.*;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.projections.ProjectionsIntegrationTests.Config;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
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
 * Integration tests for the behavior of projections.
 *
 * @author Jens Schauder
 */
@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Config.class)
class ProjectionsIntegrationTests {

	@Autowired DummyEntityWithCollectionRepository repository;

	@BeforeEach
	void setup() {

		DummyEntityWithCollection entity = new DummyEntityWithCollection();
		entity.setName("A Name");
		entity.getSubs().add(createSubEntity(1));
		entity.getSubs().add(createSubEntity(2));

		repository.save(entity);
	}

	@Test // DATAJPA-1173
	void findAllFindsTheSingleEntity() {
		assertThat(repository.findAll()).hasSize(1);
	}

	@Test // DATAJPA-1173
	void findAllProjectedFindsTheSingleEntity() {
		assertThat(repository.findAllProjectedBy()).hasSize(1);
	}

	private SubEntity createSubEntity(int index) {

		SubEntity entity = new SubEntity();
		entity.setName("sub-" + index);
		return entity;
	}

	@Data
	@Entity(name = "Dummy")
	@Table(name = "DummyEntity")
	static class DummyEntityWithCollection {

		@GeneratedValue @Id Long id;

		String name;

		@OneToMany(cascade = CascadeType.ALL) @JoinColumn(name = "subs") List<SubEntity> subs = new ArrayList<>();

		String otherAttribute;
	}

	@Data
	@Entity
	@Table(name = "SubEntity")
	static class SubEntity {

		@GeneratedValue @Id Long id;
		String name;
		String otherAttribute;
	}

	interface DummyEntityProjection {

		String getName();

		List<SubEntityProjection> getSubs();
	}

	interface SubEntityProjection {
		String getName();
	}

	interface DummyEntityWithCollectionRepository extends JpaRepository<DummyEntityWithCollection, Long> {
		List<DummyEntityProjection> findAllProjectedBy();
	}

	@EnableJpaRepositories(considerNestedRepositories = true)
	@EnableTransactionManagement
	static class Config {

		@Bean
		DataSource dataSource() {

			return new EmbeddedDatabaseBuilder() //
					.generateUniqueName(true) //
					.setType(EmbeddedDatabaseType.HSQL) //
					.setScriptEncoding("UTF-8") //
					.ignoreFailedDrops(true) //
					.build();
		}

		@Bean
		AbstractEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {

			LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
			factoryBean.setDataSource(dataSource);
			factoryBean.setPersistenceUnitRootLocation("simple-persistence");
			factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

			factoryBean.setPackagesToScan(this.getClass().getPackage().getName());

			Properties properties = new Properties();
			properties.setProperty("hibernate.hbm2ddl.auto", "create");
			properties.setProperty("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
			factoryBean.setJpaProperties(properties);

			return factoryBean;
		}

		@Bean
		PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
			return new JpaTransactionManager(emf);
		}
	}
}
