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
package org.springframework.data.jpa.infrastructure;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

/**
 * Metamodel tests using OpenJPA.
 *
 * @author Oliver Gierke
 */
@ContextConfiguration("classpath:openjpa.xml")
class OpenJpaMetamodelIntegrationTests extends MetamodelIntegrationTests {

	@Test
	@Disabled
	@Override
	void canAccessParametersByIndexForNativeQueries() {}

	/**
	 * TODO: Remove once https://issues.apache.org/jira/browse/OPENJPA-2618 is fixed.
	 */
	@Test
	@Disabled
	@Override
	void doesNotExposeAliasForTupleIfNoneDefined() {}
}
