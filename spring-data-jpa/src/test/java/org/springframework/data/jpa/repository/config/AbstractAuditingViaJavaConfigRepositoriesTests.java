/*
 * Copyright 2013-2025 the original author or authors.
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
package org.springframework.data.jpa.repository.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.domain.sample.AuditableUser;
import org.springframework.data.jpa.repository.sample.AuditableUserRepository;
import org.springframework.data.jpa.repository.sample.SampleEvaluationContextExtension;
import org.springframework.data.jpa.repository.sample.SampleEvaluationContextExtension.SampleSecurityContextHolder;
import org.springframework.data.jpa.util.FixedDate;
import org.springframework.data.spel.spi.EvaluationContextExtension;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for auditing via Java config.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Krzysztof Krason
 * @author Christoph Strobl
 */
@ExtendWith(SpringExtension.class)
@Transactional
@DirtiesContext
public abstract class AbstractAuditingViaJavaConfigRepositoriesTests {

	@Autowired AuditableUserRepository auditableUserRepository;
	@Autowired AuditorAware<AuditableUser> auditorAware;
	private AuditableUser auditor;

	@Autowired EntityManager em;

	@BeforeEach
	void setup() {

		AuditableUser auditor = new AuditableUser(null);
		auditor.setFirstname("auditor");

		when(this.auditorAware.getCurrentAuditor()).thenReturn(Optional.empty());
		this.auditor = this.auditableUserRepository.save(auditor);
		when(this.auditorAware.getCurrentAuditor()).thenReturn(Optional.of(this.auditor));
	}

	@AfterEach
	void teardown() {
		Mockito.reset(this.auditorAware);
	}

	@Test
	void basicAuditing() throws Exception {

		AuditableUser user = new AuditableUser(null);
		user.setFirstname("user");

		AuditableUser savedUser = auditableUserRepository.save(user);
		TimeUnit.MILLISECONDS.sleep(10);

		assertThat(savedUser.getCreatedDate()).isNotNull();
		assertThat(savedUser.getCreatedDate().get().isBefore(LocalDateTime.now())).isTrue();

		AuditableUser createdBy = savedUser.getCreatedBy().get();
		assertThat(createdBy).isNotNull();
		assertThat(createdBy.getFirstname()).isEqualTo(this.auditor.getFirstname());
	}

	@Test // DATAJPA-382
	void shouldAllowUseOfDynamicSpelParametersInUpdateQueries() {

		AuditableUser oliver = auditableUserRepository.save(new AuditableUser(null, "oliver"));
		AuditableUser christoph = auditableUserRepository.save(new AuditableUser(null, "christoph"));
		AuditableUser thomas = auditableUserRepository.save(new AuditableUser(null, "thomas"));

		em.detach(oliver);
		em.detach(christoph);
		em.detach(thomas);
		em.detach(auditor);

		FixedDate.INSTANCE.setDate(Instant.now());

		SampleSecurityContextHolder.getCurrent().setPrincipal(thomas);
		auditableUserRepository.updateAllNamesToUpperCase();

		// DateTime now = new DateTime(FixedDate.INSTANCE.getDate());
		LocalDateTime now = LocalDateTime.ofInstant(FixedDate.INSTANCE.getDate(), ZoneId.systemDefault());
		List<AuditableUser> users = auditableUserRepository.findAll();

		for (AuditableUser user : users) {

			assertThat(user.getFirstname()).isEqualTo(user.getFirstname().toUpperCase());
			assertThat(user.getLastModifiedBy()).contains(thomas);
			assertThat(user.getLastModifiedDate()).contains(now);
		}
	}

	@Configuration
	@Import(InfrastructureConfig.class)
	@EnableJpaRepositories(basePackageClasses = AuditableUserRepository.class)
	static class TestConfig {

		@Bean
		EvaluationContextExtension sampleEvaluationContextExtension() {
			return new SampleEvaluationContextExtension();
		}
	}
}
