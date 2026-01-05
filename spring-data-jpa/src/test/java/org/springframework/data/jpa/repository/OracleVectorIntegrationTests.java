/*
 * Copyright 2015-present the original author or authors.
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
package org.springframework.data.jpa.repository;

import java.net.URL;
import java.util.List;

import org.hibernate.dialect.OracleDialect;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.support.TestcontainerConfigSupport;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Testcase to verify Vector Search work with Oracle.
 *
 * @author Mark Paluch
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = OracleVectorIntegrationTests.Config.class)
class OracleVectorIntegrationTests extends AbstractVectorIntegrationTests {

	@EnableJpaRepositories(considerNestedRepositories = true,
			includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = VectorSearchRepository.class))
	@EnableTransactionManagement
	static class Config extends TestcontainerConfigSupport {

		public Config() {
			super(OracleDialect.class, new ClassPathResource("scripts/oracle-vector.sql"));
		}

		@Override
		protected String getSchemaAction() {
			return "none";
		}

		@Override
		protected PersistenceManagedTypes getManagedTypes() {
			return new PersistenceManagedTypes() {
				@Override
				public List<String> getManagedClassNames() {
					return List.of(WithVector.class.getName());
				}

				@Override
				public List<String> getManagedPackages() {
					return List.of();
				}

				@Override
				public @Nullable URL getPersistenceUnitRootUrl() {
					return null;
				}

			};
		}

		@SuppressWarnings("resource")
		@Bean(initMethod = "start", destroyMethod = "start")
		public OracleContainer container() {

			return new OracleContainer("gvenzl/oracle-free:slim-faststart") //
					.withReuse(true)
					.withCopyFileToContainer(MountableFile.forClasspathResource("/scripts/oracle-vector-initialize.sql"),
							"/container-entrypoint-initdb.d/initialize.sql");
		}

	}

}
