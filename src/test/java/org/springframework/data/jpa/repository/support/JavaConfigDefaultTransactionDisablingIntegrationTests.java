/*
 * Copyright 2015-2018 the original author or authors.
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
package org.springframework.data.jpa.repository.support;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Integration tests for disabled default configurations via JavaConfig.
 *
 * @author Oliver Gierke
 * @soundtrack The Intersphere - Live in Mannheim
 */
@ContextConfiguration
public class JavaConfigDefaultTransactionDisablingIntegrationTests extends DefaultTransactionDisablingIntegrationTests {

	@Configuration
	@EnableJpaRepositories(basePackageClasses = UserRepository.class, enableDefaultTransactions = false)
	@EnableTransactionManagement
	@ImportResource({ "classpath:infrastructure.xml", "classpath:tx-manager.xml" })
	static class Config {}
}
