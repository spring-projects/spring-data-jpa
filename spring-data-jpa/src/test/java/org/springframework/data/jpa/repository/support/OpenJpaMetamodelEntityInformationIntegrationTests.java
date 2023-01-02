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
package org.springframework.data.jpa.repository.support;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * OpenJpa execution for {@link JpaMetamodelEntityInformationIntegrationTests}.
 *
 * @author Oliver Gierke
 * @author Greg Turnquist
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration({ "classpath:infrastructure.xml", "classpath:openjpa.xml" })
class OpenJpaMetamodelEntityInformationIntegrationTests extends JpaMetamodelEntityInformationIntegrationTests {

	@Override
	String getMetadadataPersistenceUnitName() {
		return "metadata_oj";
	}

	/**
	 * Re-activate test.
	 */
	@Test
	void reactivatedDetectsIdTypeForMappedSuperclass() {
		super.detectsIdTypeForMappedSuperclass();
	}

	/**
	 * Ignore as it fails with weird {@link NoClassDefFoundError}.
	 */
	@Override
	@Disabled
	void findsIdClassOnMappedSuperclass() {}

	/**
	 * Re-activate test for DATAJPA-820.
	 */
	@Test
	@Override
	void detectsVersionPropertyOnMappedSuperClass() {
		super.detectsVersionPropertyOnMappedSuperClass();
	}
}
