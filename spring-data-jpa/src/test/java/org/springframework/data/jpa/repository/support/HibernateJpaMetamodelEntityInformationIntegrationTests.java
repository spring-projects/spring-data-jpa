/*
 * Copyright 2011-2024 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.jpa.domain.sample.SampleWithPrimitiveVersion;
import org.springframework.data.jpa.util.DisabledOnHibernate61;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hibernate execution for {@link JpaMetamodelEntityInformationIntegrationTests}.
 *
 * @author Greg Turnquist
 * @author Yanming Zhou
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:infrastructure.xml")
class HibernateJpaMetamodelEntityInformationIntegrationTests extends JpaMetamodelEntityInformationIntegrationTests {

	@Override
	String getMetadadataPersistenceUnitName() {
		return "metadata-id-handling";
	}

	@DisabledOnHibernate61
	@Test
	@Override
	void correctlyDeterminesIdValueForNestedIdClassesWithNonPrimitiveNonManagedType() {
		super.correctlyDeterminesIdValueForNestedIdClassesWithNonPrimitiveNonManagedType();
	}

	@DisabledOnHibernate61
	@Test
	@Override
	void prefersPrivateGetterOverFieldAccess() {
		super.prefersPrivateGetterOverFieldAccess();
	}

	@DisabledOnHibernate61
	@Test
	@Override
	void findsIdClassOnMappedSuperclass() {
		super.findsIdClassOnMappedSuperclass();
	}

	@Test
	void negativeVersionedEntityIsNew() {
		EntityInformation<SampleWithPrimitiveVersion, Long> information = new JpaMetamodelEntityInformation<>(SampleWithPrimitiveVersion.class,
				em.getMetamodel(), em.getEntityManagerFactory().getPersistenceUnitUtil());

		SampleWithPrimitiveVersion entity = new SampleWithPrimitiveVersion();
		entity.setId(23L); // assigned
		assertThat(information.isNew(entity)).isTrue();
	}
}
