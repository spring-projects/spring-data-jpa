/*
 * Copyright 2014-2025 the original author or authors.
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
package org.springframework.data.jpa.convert.threeten;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.springframework.data.jpa.support.EntityManagerTestUtils.currentEntityManagerIsAJpa21EntityManager;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.domain.support.AbstractAttributeConverterIntegrationTests;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link Jsr310JpaConverters}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Mark Paluch
 */
@ContextConfiguration
@Transactional
class Jsr310JpaConvertersIntegrationTests extends AbstractAttributeConverterIntegrationTests {

	@PersistenceContext EntityManager em;

	@Test // DATAJPA-650, DATAJPA-1631
	void usesJsr310JpaConverters() {

		assumeThat(currentEntityManagerIsAJpa21EntityManager(em)).isTrue();

		DateTimeSample sample = new DateTimeSample();

		sample.instant = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		sample.localDate = LocalDate.now();
		sample.localTime = LocalTime.now().truncatedTo(ChronoUnit.MILLIS);
		sample.localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
		sample.zoneId = ZoneId.of("Europe/Berlin");

		em.persist(sample);
		em.flush();
		em.clear();

		DateTimeSample result = em.find(DateTimeSample.class, sample.id);

		assertThat(result).isNotNull();
		assertThat(result.instant).isEqualTo(sample.instant);
		assertThat(result.localDate).isEqualTo(sample.localDate);
		assertThat(result.localTime).isEqualTo(sample.localTime);
		assertThat(result.localDateTime).isEqualTo(sample.localDateTime);
		assertThat(result.zoneId).isEqualTo(sample.zoneId);
	}

	@Configuration
	static class Config extends InfrastructureConfig {

		@Override
		protected String getPackageName() {
			return getClass().getPackage().getName();
		}
	}
}
