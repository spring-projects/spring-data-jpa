/*
 * Copyright 2008-2019 the original author or authors.
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

import javax.persistence.EntityManagerFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.sample.AuditableUser;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.sample.AuditableUserRepository;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assures the injected repository instances are wired to the customly configured {@link EntityManagerFactory}.
 *
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:multiple-entity-manager-integration-context.xml")
public class EntityManagerFactoryRefTests {

	@Autowired UserRepository userRepository;
	@Autowired AuditableUserRepository auditableUserRepository;

	@Test
	@Transactional
	public void useUserRepository() throws Exception {
		userRepository.saveAndFlush(new User("firstname", "lastname", "foo@bar.de"));
	}

	@Test
	@Transactional("transactionManager-2")
	public void useAuditableUserRepository() throws Exception {
		auditableUserRepository.saveAndFlush(new AuditableUser());
	}
}
