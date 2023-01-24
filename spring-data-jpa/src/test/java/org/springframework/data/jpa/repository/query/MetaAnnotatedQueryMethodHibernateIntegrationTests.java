/*
 * Copyright 2013-2023 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;
import java.util.Properties;
import java.util.function.Predicate;

import javax.sql.DataSource;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.repository.Meta;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.sample.RoleRepositoryWithMeta;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.orm.jpa.JpaDialect;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verify that {@link Meta}-annotated methods properly embed comments into Hibernate queries.
 *
 * @author Greg Turnquist
 * @since 3.0
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
@Transactional
class MetaAnnotatedQueryMethodHibernateIntegrationTests {

	@Autowired RoleRepositoryWithMeta repository;

	Logger testLogger = (Logger) LoggerFactory.getLogger("org.hibernate.SQL");
	ListAppender<ILoggingEvent> testAppender;

	@BeforeEach
	void setUp() {

		testAppender = new ListAppender<>();
		testAppender.start();
		testLogger.setLevel(Level.DEBUG);
		testLogger.addAppender(testAppender);
	}

	@AfterEach
	void clearUp() {
		testLogger.detachAppender(testAppender);
	}

	@Test // GH-775
	void findAllShouldLogAComment() {

		repository.findAll();

		assertAtLeastOneComment();
	}

	@Test // GH-775
	void findByIdShouldNotLogAComment() {

		repository.findById(0);

		assertNoComments();
	}

	@Test // GH-775
	void existsByIdShouldLogAComment() {

		repository.existsById(0);

		assertAtLeastOneComment();
	}

	@Test // GH-775
	void customFinderShouldLogAComment() {

		repository.findByName("name");

		assertAtLeastOneComment();
	}

	@Test // GH-775
	void findOneWithExampleShouldLogAComment() {

		repository.findOne(Example.of(new Role()));

		assertAtLeastOneComment();
	}

	@Test // GH-775
	void findAllWithExampleShouldLogAComment() {

		repository.findAll(Example.of(new Role()));

		assertAtLeastOneComment();
	}

	@Test // GH-775
	void findAllWithExampleAndSortShouldLogAComment() {

		repository.findAll(Example.of(new Role()), Sort.by("name"));

		assertAtLeastOneComment();
	}

	@Test // GH-775
	void findByFluentDslWithExampleShouldLogAComment() {

		repository.findBy(Example.of(new Role()), FluentQuery.FetchableFluentQuery::all);

		assertAtLeastOneComment();
	}

	@Test // GH-775
	void existsByExampleShouldLogAComment() {

		repository.exists(Example.of(new Role()));

		assertAtLeastOneComment();
	}

	@Test // GH-775
	void countShouldLogAComment() {

		repository.count();

		assertAtLeastOneComment();
	}

	@Test // GH-775
	void customCountShouldLogAComment() {

		repository.countByName("name");

		assertAtLeastOneComment();
	}

	@Test // GH-775
	void deleteAllByIdInBatchShouldLogAComment() {

		repository.deleteAllByIdInBatch(List.of(0, 1, 2));

		assertAtLeastOneComment();
	}

	@Test // GH-775
	void deleteAllInBatchShouldLogAComment() {

		repository.deleteAllInBatch();

		assertAtLeastOneComment();
	}

	private final static Predicate<String> hasComment = s -> s.startsWith("/* foobar */");

	private void assertAtLeastOneComment() {
		assertThat(testAppender.list).extracting(ILoggingEvent::getFormattedMessage)
				.haveAtLeastOne(new Condition<String>(hasComment, "SQL contains a comment"));
	}

	private void assertNoComments() {
		assertThat(testAppender.list).extracting(ILoggingEvent::getFormattedMessage).noneMatch(hasComment);
	}

	@Configuration
	@EnableJpaRepositories(basePackages = "org.springframework.data.jpa.repository.sample")
	static class Config {

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().generateUniqueName(true).build();
		}

		@Bean
		public Properties jpaProperties() {

			Properties properties = new Properties();
			properties.setProperty("hibernate.use_sql_comments", "true");
			return properties;
		}

		@Bean
		public AbstractJpaVendorAdapter vendorAdaptor() {

			HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
			vendorAdapter.setGenerateDdl(true);
			vendorAdapter.setDatabase(Database.HSQL);
			return vendorAdapter;
		}

		@Bean
		public EntityManagerFactory entityManagerFactory() {

			LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
			factory.setDataSource(dataSource());
			factory.setPersistenceUnitName("spring-data-jpa");
			factory.setJpaVendorAdapter(vendorAdaptor());
			factory.setJpaProperties(jpaProperties());
			factory.afterPropertiesSet();
			return factory.getObject();
		}

		@Bean
		public JpaDialect jpaDialect() {
			return new HibernateJpaDialect();
		}

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new JpaTransactionManager(entityManagerFactory());
		}
	}
}
