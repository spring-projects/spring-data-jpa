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
package org.springframework.data.jpa.domain.support;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Auditable;
import org.springframework.data.jpa.domain.sample.AnnotatedAuditableUser;
import org.springframework.data.jpa.domain.sample.AuditableRole;
import org.springframework.data.jpa.domain.sample.AuditableUser;
import org.springframework.data.jpa.domain.sample.AuditorAwareStub;
import org.springframework.data.jpa.repository.sample.AnnotatedAuditableUserRepository;
import org.springframework.data.jpa.repository.sample.AuditableUserRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for {@link AuditingEntityListener}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:auditing/auditing-entity-listener.xml")
@Transactional
@DirtiesContext
public class AuditingEntityListenerTests {

	@Autowired AuditableUserRepository repository;
	@Autowired AnnotatedAuditableUserRepository annotatedUserRepository;

	@Autowired AuditorAwareStub auditorAware;

	AuditableUser user;

	@Before
	public void setUp() {

		user = new AuditableUser();
		auditorAware.setAuditor(user);

		repository.saveAndFlush(user);
	}

	@Test
	public void auditsRootEntityCorrectly() {

		assertDatesSet(user);
		assertUserIsAuditor(user, user);
	}

	@Test // DATAJPA-303
	public void updatesLastModifiedDates() throws Exception {

		Thread.sleep(200);
		user.setFirstname("Oliver");

		user = repository.saveAndFlush(user);

		assertThat(user.getCreatedDate().get().isBefore(user.getLastModifiedDate().get()), is(true));
	}

	@Test
	public void auditsTransitiveEntitiesCorrectly() {

		AuditableRole role = new AuditableRole();
		role.setName("ADMIN");

		user.addRole(role);

		repository.saveAndFlush(user);

		role = user.getRoles().iterator().next();

		assertDatesSet(user);
		assertDatesSet(role);
		assertUserIsAuditor(user, user);
		assertUserIsAuditor(user, role);
	}

	@Test // DATAJPA-501
	public void usesAnnotationMetadata() {

		AnnotatedAuditableUser auditableUser = annotatedUserRepository.save(new AnnotatedAuditableUser());

		assertThat(auditableUser.getCreateAt(), is(notNullValue()));
		assertThat(auditableUser.getLastModifiedBy(), is(notNullValue()));
	}

	private static void assertDatesSet(Auditable<?, ?, LocalDateTime> auditable) {

		assertThat(auditable.getCreatedDate().isPresent(), is(true));
		assertThat(auditable.getLastModifiedDate().isPresent(), is(true));
	}

	private static void assertUserIsAuditor(AuditableUser user, Auditable<AuditableUser, ?, LocalDateTime> auditable) {

		assertThat(auditable.getCreatedBy(), is(Optional.of(user)));
		assertThat(auditable.getLastModifiedBy(), is(Optional.of(user)));
	}
}
