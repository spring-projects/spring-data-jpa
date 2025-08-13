/*
 * Copyright 2025 the original author or authors.
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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.ImportResource;
import org.springframework.test.context.ContextConfiguration;

/**
 * Testcase to run {@link RepositoryWithCompositeKeyTests} integration tests on top of EclipseLink.
 *
 * @author Mark Paluch
 */
@ContextConfiguration
class EclipselinkRepositoryWithCompositeKeyTests extends RepositoryWithCompositeKeyTests {

	@ImportResource({ "classpath:infrastructure.xml", "classpath:eclipselink.xml" })
	static class TestConfig extends RepositoryWithIdClassKeyTests.Config {}

	@Override
	@Test
	@Disabled("Eclipselink doesn't support batch delete by id with IdClass")
	void shouldSupportDeleteAllByIdInBatchWithIdClass() {}

	@Override
	@Test
	@Disabled("Eclipselink doesn't support derived identities with IdClass")
	void shouldSupportSavingEntitiesWithCompositeKeyClassesWithEmbeddedIdsAndDerivedIdentities() {}

}
