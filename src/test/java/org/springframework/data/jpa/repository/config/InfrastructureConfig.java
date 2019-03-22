/*
 * Copyright 2013-2019 the original author or authors.
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

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Reusable config class for testing pure java based Spring configuration.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 */
@Configuration
@EnableTransactionManagement
public class InfrastructureConfig {

	@Bean
	public DataSource dataSource() {
		return new EmbeddedDatabaseBuilder().setName("data").setType(EmbeddedDatabaseType.HSQL).build();
	}

	@Bean
	public JpaTransactionManager transactionManager(EntityManagerFactory emf) {
		return new JpaTransactionManager(emf);
	}

	@Bean
	public HibernateJpaVendorAdapter jpaVendorAdapter() {

		HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
		adapter.setGenerateDdl(true);
		adapter.setDatabase(Database.HSQL);

		return adapter;
	}

	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory() {

		LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
		em.setPersistenceUnitName("spring-data-jpa");
		em.setDataSource(dataSource());
		em.setJpaVendorAdapter(jpaVendorAdapter());
		em.setPackagesToScan("purejavaconfig");

		return em;
	}
}
