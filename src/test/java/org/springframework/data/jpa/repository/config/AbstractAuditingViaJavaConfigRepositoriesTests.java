/*
 * Copyright 2013 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.domain.sample.AuditableUser;
import org.springframework.data.jpa.repository.sample.AuditableUserRepository;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for auditing via Java config.
 * 
 * @author Thomas Darimont
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public abstract class AbstractAuditingViaJavaConfigRepositoriesTests {

	@Autowired AuditableUserRepository auditableUserRepository;
	@Autowired AuditorAware<AuditableUser> auditorAware;
	AuditableUser auditor;

	@Configuration
	@Import(InfrastructureConfig.class)
	@EnableJpaRepositories(basePackageClasses = AuditableUserRepository.class)
	static class TestConfig {}

	@Before
	public void setup() {
		AuditableUser auditor = new AuditableUser(null);
		auditor.setFirstname("auditor");
		this.auditor = this.auditableUserRepository.save(auditor);

		doReturn(this.auditor).when(this.auditorAware).getCurrentAuditor();
	}

	@After
	public void teardown() {
		auditableUserRepository.delete(this.auditor);
	}

	@Test
	@Transactional
	public void basicAuditing() throws Exception {

		AuditableUser user = new AuditableUser(null);
		user.setFirstname("user");

		AuditableUser savedUser = auditableUserRepository.save(user);
		TimeUnit.MILLISECONDS.sleep(10);

		assertThat(savedUser.getCreatedDate(), is(notNullValue()));
		assertThat(savedUser.getCreatedDate().isBeforeNow(), is(true));

		AuditableUser createdBy = savedUser.getCreatedBy();
		assertThat(createdBy, is(notNullValue()));
		assertThat(createdBy.getFirstname(), is(this.auditor.getFirstname()));
	}
}
