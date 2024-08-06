/*
 * Copyright 2008-2024 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.sample.AuditableEmbeddable;
import org.springframework.data.jpa.domain.sample.AuditableEntity;
import org.springframework.data.jpa.repository.sample.AuditableEntityRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Integration test for {@link AuditingEntityListener}.
 *
 * @author Greg Turnquist
 * @author Oliver Drotbohm
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:auditing/auditing-entity-with-embeddable-listener.xml")
class AuditingEntityWithEmbeddableListenerTests {

	@Autowired AuditableEntityRepository repository;

	private AuditableEntity entity;
	private AuditableEmbeddable auditDetails;

	@BeforeEach
	void setUp() {

		entity = new AuditableEntity();
		entity.setData("original value");

		auditDetails = new AuditableEmbeddable();
		entity.setAuditDetails(auditDetails);
	}

	@Test
	void auditsEmbeddedCorrectly() {

		// when
		repository.saveAndFlush(entity);

		// then

		assertThat(repository.findById(1L)).hasValueSatisfying(it -> {

			assertThat(it.getData()).isEqualTo("original value");

			AuditableEmbeddable details = it.getAuditDetails();

			assertThat(details.getDateCreated()).isNotNull();
			assertThat(details.getDateUpdated()).isNotNull();

			it.setData("updated value");
			repository.saveAndFlush(it);

			assertThat(repository.findById(1L)).hasValueSatisfying(revised -> {

				assertThat(revised.getData()).isEqualTo("updated value");

				AuditableEmbeddable revisedDetails = revised.getAuditDetails();

				assertThat(revisedDetails.getDateCreated()).isEqualTo(details.getDateCreated());
				assertThat(revisedDetails.getDateUpdated()).isAfter(details.getDateUpdated());
			});
		});
	}
}
