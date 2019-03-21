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

import static org.mockito.Mockito.*;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.domain.sample.AuditableUser;
import org.springframework.test.context.ContextConfiguration;

/**
 * Integration tests for auditing via Java config with default configuration.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 */
@ContextConfiguration
public class DefaultAuditingViaJavaConfigRepositoriesTests extends AbstractAuditingViaJavaConfigRepositoriesTests {

	@Configuration
	@EnableJpaAuditing
	@Import(TestConfig.class)
	static class Config {

		@Bean
		@SuppressWarnings("unchecked")
		public AuditorAware<AuditableUser> auditorProvider() {
			return mock(AuditorAware.class);
		}
	}
}
