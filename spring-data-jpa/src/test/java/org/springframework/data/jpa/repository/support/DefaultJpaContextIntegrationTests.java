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
package org.springframework.data.jpa.repository.support;

import static org.assertj.core.api.Assertions.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.domain.sample.Category;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.infrastructure.HibernateTestUtils;
import org.springframework.data.jpa.repository.JpaContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.stereotype.Component;

/**
 * Integration tests for {@link DefaultJpaContext}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @soundtrack Marcus Miller - Papa Was A Rolling Stone (Afrodeezia)
 */
class DefaultJpaContextIntegrationTests {

	private static EntityManagerFactory firstEmf;
	private static EntityManagerFactory secondEmf;
	private EntityManager firstEm;
	private EntityManager secondEm;
	private JpaContext jpaContext;

	@BeforeAll
	static void bootstrapJpa() {

		firstEmf = createEntityManagerFactory("spring-data-jpa");
		secondEmf = createEntityManagerFactory("querydsl");
	}

	private static final LocalContainerEntityManagerFactoryBean createEntityManagerFactoryBean(
			String persistenceUnitName) {

		LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
		factoryBean.setPersistenceProvider(HibernateTestUtils.getPersistenceProvider());
		factoryBean.setDataSource(
				new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL).generateUniqueName(true).build());
		factoryBean.setPersistenceUnitName(persistenceUnitName);

		return factoryBean;
	}

	private static final EntityManagerFactory createEntityManagerFactory(String persistenceUnitName) {

		LocalContainerEntityManagerFactoryBean factoryBean = createEntityManagerFactoryBean(persistenceUnitName);
		factoryBean.afterPropertiesSet();

		return factoryBean.getObject();
	}

	@BeforeEach
	void createEntityManagers() {

		this.firstEm = firstEmf.createEntityManager();
		this.secondEm = secondEmf.createEntityManager();

		this.jpaContext = new DefaultJpaContext(new HashSet<>(Arrays.asList(firstEm, secondEm)));
	}

	@Test // DATAJPA-669
	void rejectsUnmanagedType() {

		assertThatIllegalArgumentException().isThrownBy(() -> jpaContext.getEntityManagerByManagedType(Object.class))
				.withMessageContaining(Object.class.getSimpleName());
	}

	@Test // DATAJPA-669
	void returnsEntitymanagerForUniqueType() {
		assertThat(jpaContext.getEntityManagerByManagedType(Category.class)).isEqualTo(firstEm);
	}

	@Test // DATAJPA-669
	void rejectsRequestForTypeManagedByMultipleEntityManagers() {

		assertThatIllegalArgumentException().isThrownBy(() -> jpaContext.getEntityManagerByManagedType(User.class))
				.withMessageContaining(User.class.getSimpleName());
	}

	@Test // DATAJPA-813, DATAJPA-956
	void bootstrapsDefaultJpaContextInSpringContainer() {

		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
		ApplicationComponent component = context.getBean(ApplicationComponent.class);

		assertThat(component.context).isNotNull();

		context.close();
	}

	@EnableJpaRepositories
	@ComponentScan(includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, value = ApplicationComponent.class),
			useDefaultFilters = false)
	static class Config {

		@Bean
		public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
			return createEntityManagerFactoryBean("spring-data-jpa");
		}

	}

	@Component
	static class ApplicationComponent {

		JpaContext context;

		@Autowired
		public ApplicationComponent(JpaContext context) {
			this.context = context;
		}
	}
}
