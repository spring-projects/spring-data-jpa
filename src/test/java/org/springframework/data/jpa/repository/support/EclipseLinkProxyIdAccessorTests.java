/*
 * Copyright 2014-2019 the original author or authors.
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
package org.springframework.data.jpa.repository.support;

import org.junit.Ignore;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.jpa.provider.PersistenceProviderIntegrationTests;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Oliver Gierke
 */
@ContextConfiguration(classes = EclipseLinkProxyIdAccessorTests.EclipseLinkConfig.class)
public class EclipseLinkProxyIdAccessorTests extends PersistenceProviderIntegrationTests {

	@Configuration
	@ImportResource("classpath:eclipselink.xml")
	static class EclipseLinkConfig {}

	/**
	 * Do not execute the test as EclipseLink does not create a lazy-loading proxy as expected.
	 */
	@Override
	@Ignore
	public void testname() {}
}
