/*
 * Copyright 2015-2019 the original author or authors.
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

import org.junit.Ignore;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.context.ContextConfiguration;

/**
 * Test case to run {@link StoredProcedureIntegrationTests} integration tests on top of OpenJpa. This is currently not
 * supported since, the OpenJPA tests need to be executed with hsqldb1 which doesn't supported stored procedures.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 */
@Ignore
@ContextConfiguration(classes = { StoredProcedureIntegrationTests.Config.class })
public class OpenJpaStoredProcedureIntegrationTests extends StoredProcedureIntegrationTests {

	@ImportResource({ "classpath:infrastructure.xml", "classpath:openjpa.xml" })
	static class TestConfig extends Config {}
}
