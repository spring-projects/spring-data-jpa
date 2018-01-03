/*
 * Copyright 2008-2018 the original author or authors.
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
package org.springframework.data.jpa.repository.config;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.data.jpa.repository.sample.AuditableUserRepository;
import org.springframework.data.jpa.repository.sample.RoleRepository;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Abstract base class for integration test for namespace configuration.
 *
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class AbstractRepositoryConfigTests {

	@Autowired(required = false) UserRepository userRepository;
	@Autowired(required = false) RoleRepository roleRepository;
	@Autowired(required = false) AuditableUserRepository auditableUserRepository;

	@Autowired JpaMetamodelMappingContext mappingContext;

	/**
	 * Asserts that context creation detects 3 repository beans.
	 */
	@Test
	public void testContextCreation() {

		assertNotNull(userRepository);
		assertNotNull(roleRepository);
		assertNotNull(auditableUserRepository);
	}

	@Test // DATAJPA-330
	public void repositoriesHaveExceptionTranslationApplied() {

		JpaRepositoriesRegistrarIntegrationTests.assertExceptionTranslationActive(userRepository);
		JpaRepositoriesRegistrarIntegrationTests.assertExceptionTranslationActive(roleRepository);
		JpaRepositoriesRegistrarIntegrationTests.assertExceptionTranslationActive(auditableUserRepository);
	}

	@Test // DATAJPA-484
	public void exposesJpaMappingContext() {
		assertNotNull(mappingContext);
	}
}
