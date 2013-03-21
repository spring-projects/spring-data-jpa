/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.data.jpa.auditing;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.Assert.*;

/**
 * Integration tests for enabling auditing via java configuration.
 *
 * @author Ranie Jade Ramiso
 */
public class JpaAuditingRegistrarIntegrationTests {

	private ApplicationContext context = new AnnotationConfigApplicationContext(SampleConfig.class);

	private PrincipalRepository principalRepository;

	private DomainRepository domainRepository;

	@Before
	public void setup() {
		principalRepository = context.getBean(PrincipalRepository.class);
		domainRepository = context.getBean(DomainRepository.class);
		SampleConfig.PRINCIPAL = principalRepository.save(SampleConfig.PRINCIPAL);
	}

	@Test
	public void testAuditNew() {
		Domain domain = new Domain();

		domainRepository.save(domain);

		domain = domainRepository.findOne(domain.getId());
		assertNotNull(domain);

		assertNotNull(domain.getCreatedBy());
		assertNotNull(domain.getCreatedDate());
		assertEquals(SampleConfig.PRINCIPAL, domain.getCreatedBy());
		assertNull(domain.getLastModifiedBy());
		assertNull(domain.getLastModifiedDate());
	}

	@Test
	public void testAuditUpdate() {
		Domain domain = new Domain();

		// first save
		domain = domainRepository.save(domain);

		// update
		domain.setName("a very simple name");
		domainRepository.save(domain);

		domain = domainRepository.findOne(domain.getId());
		assertNotNull(domain);

		assertNotNull(domain.getCreatedBy());
		assertNotNull(domain.getCreatedDate());
		assertEquals(SampleConfig.PRINCIPAL, domain.getCreatedBy());

		assertNotNull(domain.getLastModifiedBy());
		assertNotNull(domain.getLastModifiedDate());
		assertEquals(SampleConfig.PRINCIPAL, domain.getLastModifiedBy());
	}

	@Configuration
	@EnableJpaAuditing(auditorAwareRef = "principalService", setDates = true, modifyOnCreate = false)
	@EnableJpaRepositories(basePackages = "org.springframework.data.jpa.auditing")
	static class SampleConfig {
		public static Principal PRINCIPAL = new Principal();

		@Bean(name = "principalService")
		public AuditorAware<Principal> getAuditorAware() {
			return new AuditorAware<Principal>() {
				@Resource
				private PrincipalRepository principalRepository;

				public Principal getCurrentAuditor() {
					return principalRepository.findOne(PRINCIPAL.getId());
				}
			};
		}

		@Bean
		public DataSource dataSource() {
			EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
			return builder.setType(EmbeddedDatabaseType.HSQL).build();
		}

		@Bean
		public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
			HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
			vendorAdapter.setDatabase(Database.HSQL);
			vendorAdapter.setGenerateDdl(true);

			LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
			factory.setPersistenceUnitName("audit");
			factory.setDataSource(dataSource());
			factory.setJpaVendorAdapter(vendorAdapter);

			return factory;
		}


		@Bean
		public PlatformTransactionManager transactionManager() {

			JpaTransactionManager txManager = new JpaTransactionManager();
			txManager.setEntityManagerFactory(entityManagerFactory().getObject());
			return txManager;
		}
	}
}
