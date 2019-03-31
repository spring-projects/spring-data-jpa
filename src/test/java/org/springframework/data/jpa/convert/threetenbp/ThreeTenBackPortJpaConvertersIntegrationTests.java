/*
 * Copyright 2015-2019 the original author or authors.
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
package org.springframework.data.jpa.convert.threetenbp;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;
import static org.springframework.data.jpa.support.EntityManagerTestUtils.*;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.domain.support.AbstractAttributeConverterIntegrationTests;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneId;

/**
 * Integration tests for {@link ThreeTenBackPortJpaConverters}.
 *
 * @author Oliver Gierke
 * @since 1.8
 */
@ContextConfiguration
@Transactional
public class ThreeTenBackPortJpaConvertersIntegrationTests extends AbstractAttributeConverterIntegrationTests {

	@Configuration
	static class Config extends InfrastructureConfig {

		@Override
		protected String getPackageName() {
			return getClass().getPackage().getName();
		}
	}

	@PersistenceContext EntityManager em;

	@Test // DATAJPA-650
	public void usesThreeTenBackPortJpaConverters() {

		assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

		DateTimeSample sample = new DateTimeSample();

		sample.instant = Instant.now();
		sample.localDate = LocalDate.now();
		sample.localTime = LocalTime.now();
		sample.localDateTime = LocalDateTime.now();
		sample.zoneId = ZoneId.of("Europe/Berlin");

		em.persist(sample);
		em.flush();
		em.clear();

		DateTimeSample result = em.find(DateTimeSample.class, sample.id);

		assertThat(result, is(notNullValue()));
		assertThat(result.instant, is(sample.instant));
		assertThat(result.localDate, is(sample.localDate));
		assertThat(result.localTime, is(sample.localTime));
		assertThat(result.localDateTime, is(sample.localDateTime));
		assertThat(result.zoneId, is(sample.zoneId));
	}
}
