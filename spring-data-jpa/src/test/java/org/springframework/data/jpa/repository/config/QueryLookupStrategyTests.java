/*
 * Copyright 2008-2025 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.util.ReflectionTestUtils.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Integration test for XML configuration of {@link QueryLookupStrategy.Key}s.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Jens Schauder
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:config/lookup-strategies-context.xml")
class QueryLookupStrategyTests {

	@Autowired ApplicationContext context;

	/**
	 * Assert that {@link Key#CREATE_IF_NOT_FOUND} is being set on the factory if configured.
	 */
	@Test
	void shouldUseExplicitlyConfiguredQueryLookUpStrategy() {

		JpaRepositoryFactoryBean<?, ?, ?> factory = context.getBean("&roleRepository", JpaRepositoryFactoryBean.class);

		assertThat(getField(factory, "queryLookupStrategyKey")).isEqualTo(Key.CREATE_IF_NOT_FOUND);
	}
}
