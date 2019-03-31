/*
 * Copyright 2015-2019 the original author or authors.
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
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.HashSet;

import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.jpa.domain.sample.Category;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.infrastructure.HibernateTestUtils;
import org.springframework.data.jpa.repository.JpaContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.mock.jndi.ExpectedLookupTemplate;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.stereotype.Component;

/**
 * Integration tests for {@link DefaultJpaContext}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @soundtrack Marcus Miller - Papa Was A Rolling Stone (Afrodeezia)
 */
public class DefaultJpaContextIntegrationTests {

	public @Rule ExpectedException exception = ExpectedException.none();

	static EntityManagerFactory firstEmf, secondEmf;

	EntityManager firstEm, secondEm;
	JpaContext jpaContext;

	@BeforeClass
	public static void bootstrapJpa() {

		firstEmf = createEntityManagerFactory("spring-data-jpa");
		secondEmf = createEntityManagerFactory("querydsl");
	}

	@Before
	public void createEntityManagers() {

		this.firstEm = firstEmf.createEntityManager();
		this.secondEm = secondEmf.createEntityManager();

		this.jpaContext = new DefaultJpaContext(new HashSet<EntityManager>(Arrays.asList(firstEm, secondEm)));
	}

	@Test // DATAJPA-669
	public void rejectsUnmanagedType() {

		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(Object.class.getSimpleName());

		jpaContext.getEntityManagerByManagedType(Object.class);
	}

	@Test // DATAJPA-669
	public void returnsEntitymanagerForUniqueType() {
		assertThat(jpaContext.getEntityManagerByManagedType(Category.class), is(firstEm));
	}

	@Test // DATAJPA-669
	public void rejectsRequestForTypeManagedByMultipleEntityManagers() {

		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(User.class.getSimpleName());

		jpaContext.getEntityManagerByManagedType(User.class);
	}

	@Test // DATAJPA-813, DATAJPA-956
	public void bootstrapsDefaultJpaContextInSpringContainer() {

		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
		ApplicationComponent component = context.getBean(ApplicationComponent.class);

		assertThat(component.context, is(notNullValue()));

		context.close();
	}

	@Test // DATAJPA-813
	public void bootstrapsDefaultJpaContextInSpringContainerWithEntityManagerFromJndi() throws Exception {

		SimpleNamingContextBuilder builder = SimpleNamingContextBuilder.emptyActivatedContextBuilder();
		builder.bind("some/EMF", createEntityManagerFactory("spring-data-jpa"));
		builder.bind("some/other/Component", new Object());

		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext("config/jpa-context-with-jndi.xml");
		ApplicationComponent component = context.getBean(ApplicationComponent.class);

		assertThat(component.context, is(notNullValue()));

		context.close();
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

	@EnableJpaRepositories
	@ComponentScan(includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, value = ApplicationComponent.class),
			useDefaultFilters = false)
	static class Config {

		@Bean
		public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
			return createEntityManagerFactoryBean("spring-data-jpa");
		}

		// A non-EntityManagerFactory JNDI object to make sure the detection doesn't include it
		// see DATAJPA-956
		@Bean
		public JndiObjectFactoryBean jndiObject() throws NamingException {

			JndiObjectFactoryBean bean = new JndiObjectFactoryBean();

			bean.setJndiName("some/DataSource");
			bean.setJndiTemplate(new ExpectedLookupTemplate("some/DataSource", mock(DataSource.class)));
			bean.setExpectedType(DataSource.class);

			return bean;
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
