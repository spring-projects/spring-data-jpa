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

import jakarta.persistence.EntityManagerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Properties;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
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
import org.springframework.orm.jpa.vendor.EclipseLinkJpaDialect;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;

/**
 * Verify that {@link Meta}-annotated methods properly embed comments into EclipseLink queries.
 *
 * @author Greg Turnquist
 * @since 3.0
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
@Transactional
class MetaAnnotatedQueryMethodEclipseLinkIntegrationTests {

	@Autowired RoleRepositoryWithMeta repository;

	private static final ResourceLoader RESOURCE_LOADER = new DefaultResourceLoader();
	private static final String LOG_FILE = "test-eclipselink-meta.log";

	@BeforeEach
	void cleanoutLogfile() throws IOException {
		new FileOutputStream(LOG_FILE).close();
	}

	@AfterAll
	static void deleteLogfile() throws IOException {
		FileSystemUtils.deleteRecursively(Path.of(LOG_FILE));
	}

	@Test // GH-775
	void findAllShouldLogAComment() {

		repository.findAll();

		assertAtLeastOneComment();
	}

	@Test // GH-775
	void findByIdShouldLogAComment() {

		repository.findById(0);

		assertAtLeastOneComment();
	}

	@Test // GH-775
	void existsByIdShouldLogAComment() {

		repository.existsById(0);

		assertAtLeastOneComment();
	}

	@Test // GH-775
	void customFinderShouldLogAComment() throws Exception {

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
	void deleteAllInBatchShouldLogAComment() {

		repository.deleteAllInBatch();

		assertAtLeastOneComment();
	}

	void assertAtLeastOneComment() {

		try (Reader reader = new InputStreamReader(RESOURCE_LOADER.getResource("file:" + LOG_FILE).getInputStream(),
				StandardCharsets.UTF_8)) {

			String logFileOutput = FileCopyUtils.copyToString(reader);
			assertThat(logFileOutput).contains("/* foobar */");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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
			properties.put("eclipselink.weaving", "false");
			properties.put("eclipselink.logging.level.sql", "FINE");
			properties.put("eclipselink.logging.file", LOG_FILE);
			return properties;
		}

		@Bean
		public AbstractJpaVendorAdapter vendorAdaptor() {

			EclipseLinkJpaVendorAdapter vendorAdapter = new EclipseLinkJpaVendorAdapter();
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
			return new EclipseLinkJpaDialect();
		}

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new JpaTransactionManager(entityManagerFactory());
		}
	}
}
