/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository.config;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.domain.sample.AuditableUser;
import org.springframework.data.jpa.repository.sample.AuditableUserRepository;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for auditing via Java config.
 * 
 * @author Thomas Darimont
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
public class AuditingViaJavaConfigRepositoriesTests {

	@Autowired AuditableUserRepository auditableUserRepository;
	@Autowired AuditorAware<AuditableUser> auditorAware;
	AuditableUser auditor;

	@Configuration
	@EnableTransactionManagement
	static class InfrastructureConfig {

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().setName("auditingtests").setType(EmbeddedDatabaseType.HSQL).build();
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
			em.setDataSource(dataSource());
			em.setJpaVendorAdapter(jpaVendorAdapter());
			em.setPackagesToScan("purejavaconfig");

			return em;
		}
	}

	@Configuration
	@EnableJpaAuditing
	@Import(InfrastructureConfig.class)
	@EnableJpaRepositories(basePackageClasses = AuditableUserRepository.class)
	static class Config {

		@Bean
		public AuditorAware<AuditableUser> auditorProvider() {
			return mock(AuditorAware.class);
		}
	}

	@Before
	public void setup() {
		this.auditor = new AuditableUser(null);
		this.auditor.setFirstname("auditor");
	}

	@Test
	public void basicAuditing() {

		this.auditor = auditableUserRepository.save(auditor);

		doReturn(this.auditor).when(this.auditorAware).getCurrentAuditor();

		AuditableUser user = new AuditableUser(null);
		user.setFirstname("user");

		AuditableUser savedUser = auditableUserRepository.save(user);
		System.out.println(savedUser);

		AuditableUser createdBy = savedUser.getCreatedBy();
		assertThat(createdBy, is(notNullValue()));
		assertThat(createdBy.getFirstname(), is(this.auditor.getFirstname()));
	}
}
