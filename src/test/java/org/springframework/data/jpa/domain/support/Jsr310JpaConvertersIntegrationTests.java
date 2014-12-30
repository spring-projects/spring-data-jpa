/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.jpa.domain.support;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;
import static org.springframework.data.jpa.support.EntityManagerTestUtils.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link Jsr310JpaConverters}.
 * 
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
public class Jsr310JpaConvertersIntegrationTests {

	@Configuration
	static class Config {

		@Bean
		public LocalContainerEntityManagerFactoryBean entityManagerFactory() {

			AbstractJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
			vendorAdapter.setDatabase(Database.HSQL);
			vendorAdapter.setGenerateDdl(true);

			LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
			factory.setDataSource(dataSource());
			factory.setPackagesToScan(getClass().getPackage().getName(), User.class.getPackage().getName());
			factory.setJpaVendorAdapter(vendorAdapter);

			return factory;
		}

		public @Bean PlatformTransactionManager transactionManager() {
			return new JpaTransactionManager();
		}

		public @Bean DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL).build();
		}
	}

	@PersistenceContext EntityManager em;

	/**
	 * @see DATAJPA-650
	 */
	@Test
	public void usesJsr310JpaConverters() {

		assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

		DateTimeSample sample = new DateTimeSample();

		sample.instant = Instant.now();
		sample.localDate = LocalDate.now();
		sample.localTime = LocalTime.now();
		sample.localDateTime = LocalDateTime.now();

		em.persist(sample);
		em.clear();

		DateTimeSample result = em.find(DateTimeSample.class, sample.id);

		assertThat(result, is(notNullValue()));
		assertThat(result.instant, is(sample.instant));
		assertThat(result.localDate, is(sample.localDate));
		assertThat(result.localTime, is(sample.localTime));
		assertThat(result.localDateTime, is(sample.localDateTime));
	}
}
