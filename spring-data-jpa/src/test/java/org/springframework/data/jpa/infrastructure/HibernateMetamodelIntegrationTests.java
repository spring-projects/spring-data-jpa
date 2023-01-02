/*
 * Copyright 2015-2023 the original author or authors.
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

/**
 * Hibernate-specific integration test using the JPA metamodel.
 *
 * @author Oliver Gierke
 * @soundtrack Umphrey's McGee - Intentions Clear (Safety In Numbers)
 */
class HibernateMetamodelIntegrationTests extends MetamodelIntegrationTests {

	@Test
	@Disabled
	@Override
	void pathToEntityIsOfBindableTypeEntityType() {}

	@Test
	@Disabled
	@Override
	void considersOneToOneAttributeAnAssociation() {}

	/**
	 * @see <a href="https://hibernate.atlassian.net/browse/HHH-10341">HHH-10341</a>
	 */
	@Test
	@Disabled
	@Override
	void doesNotExposeAliasForTupleIfNoneDefined() {}
}
