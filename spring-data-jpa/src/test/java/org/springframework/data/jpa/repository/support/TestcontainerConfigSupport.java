/*
 * Copyright 2025 the original author or authors.
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

import jakarta.persistence.EntityManagerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.ManagedClassNameFilter;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import org.testcontainers.containers.JdbcDatabaseContainer;

/**
 * Support class for integration testing with Testcontainers
 *
 * @author Mark Paluch
 */
public class TestcontainerConfigSupport {

	private final Class<?> dialect;
	private final Resource initScript;

	protected TestcontainerConfigSupport(Class<?> dialect, Resource initScript) {
		this.dialect = dialect;
		this.initScript = initScript;
	}

	@Bean
	DataSource dataSource(JdbcDatabaseContainer<?> container) {

		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setUrl(container.getJdbcUrl());
		dataSource.setUsername(container.getUsername());
		dataSource.setPassword(container.getPassword());

		return dataSource;
	}

	@Bean
	AbstractEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {

		LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
		factoryBean.setDataSource(dataSource);
		factoryBean.setPersistenceUnitRootLocation("simple-persistence");
		factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

		factoryBean.setManagedTypes(getManagedTypes());
		factoryBean.setPackagesToScan(getPackagesToScan().toArray(new String[0]));
		factoryBean.setManagedClassNameFilter(getManagedClassNameFilter());

		Properties properties = new Properties();
		properties.setProperty("hibernate.hbm2ddl.auto", getSchemaAction());
		properties.setProperty("hibernate.dialect", dialect.getCanonicalName());

		factoryBean.setJpaProperties(properties);

		return factoryBean;
	}

	protected String getSchemaAction() {
		return "create";
	}

	protected PersistenceManagedTypes getManagedTypes() {
		return null;
	}

	protected Collection<String> getPackagesToScan() {
		return Collections.emptyList();
	}

	protected ManagedClassNameFilter getManagedClassNameFilter() {
		return className -> true;
	}

	@Bean
	PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
		return new JpaTransactionManager(entityManagerFactory);
	}

	@Bean
	DataSourceInitializer initializer(DataSource dataSource) {

		DataSourceInitializer initializer = new DataSourceInitializer();
		initializer.setDataSource(dataSource);

		ResourceDatabasePopulator populator = new ResourceDatabasePopulator(initScript);
		populator.setSeparator(";;");
		initializer.setDatabasePopulator(populator);

		return initializer;
	}

}
