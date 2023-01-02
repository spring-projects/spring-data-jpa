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
package org.springframework.data.envers;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;

import javax.sql.DataSource;

import org.hibernate.envers.strategy.ValidityAuditStrategy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.envers.repository.config.EnableEnversRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring JavaConfig configuration for general infrastructure.
 *
 * @author Oliver Gierke
 */
@Configuration
@EnableEnversRepositories
public class Config {

	@Bean
	public DataSource dataSource() throws SQLException {
		return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
	}

	@Bean
	public PlatformTransactionManager transactionManager() throws SQLException {
		return new JpaTransactionManager();
	}

	@Bean
	public AbstractEntityManagerFactoryBean entityManagerFactory() throws SQLException {

		HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
		jpaVendorAdapter.setDatabase(Database.H2);
		jpaVendorAdapter.setGenerateDdl(true);

		LocalContainerEntityManagerFactoryBean bean = new LocalContainerEntityManagerFactoryBean();
		bean.setJpaVendorAdapter(jpaVendorAdapter);
		bean.setPackagesToScan(Config.class.getPackage().getName());
		bean.setDataSource(dataSource());
		bean.setJpaPropertyMap(jpaProperties());

		return bean;
	}

	private Map<String, String> jpaProperties() {
		return Collections.singletonMap("org.hibernate.envers.audit_strategy", ValidityAuditStrategy.class.getName());
	}
}
