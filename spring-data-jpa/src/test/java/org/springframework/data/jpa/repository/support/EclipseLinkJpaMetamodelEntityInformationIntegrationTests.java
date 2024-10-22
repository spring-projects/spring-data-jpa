/*
 * Copyright 2013-2024 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.jpa.repository.support.JpaEntityInformationSupport.*;

import java.io.Serializable;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.data.jpa.domain.sample.SampleWithPrimitiveVersion;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * EclipseLink execution for {@link JpaMetamodelEntityInformationIntegrationTests}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Greg Turnquist
 * @author Yanming Zhou
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration({ "classpath:infrastructure.xml", "classpath:eclipselink.xml" })
class EclipseLinkJpaMetamodelEntityInformationIntegrationTests extends JpaMetamodelEntityInformationIntegrationTests {

	@Override
	String getMetadadataPersistenceUnitName() {
		return "metadata_el";
	}

	/**
	 * Change to check for {@link String} as EclipseLink defaults {@link Serializable}s to {@link String}.
	 */
	@Override
	void detectsIdTypeForMappedSuperclass() {

		JpaEntityInformation<?, ?> information = getEntityInformation(AbstractPersistable.class, em);
		assertThat(information.getIdType()).isEqualTo(String.class);
	}

	/**
	 * Ignored due to https://bugs.eclipse.org/bugs/show_bug.cgi?id=411231.
	 */
	@Override
	@Disabled
	void findsIdClassOnMappedSuperclass() {}

	/**
	 * Ignored due to https://bugs.eclipse.org/bugs/show_bug.cgi?id=415027
	 */
	@Override
	@Disabled
	void detectsNewStateForEntityWithPrimitiveId() {}

	/**
	 * This test fails due to https://bugs.eclipse.org/bugs/show_bug.cgi?id=531528 IdentifiableType.hasSingleIdAttribute()
	 * returns true when IdClass references an inner class. This bug is supposedly fixed, but the test still fails.
	 */
	@Disabled
	@Test
	@Override
	void correctlyDeterminesIdValueForNestedIdClassesWithNonPrimitiveNonManagedType() {}

	@Override
	@Disabled
	void prefersPrivateGetterOverFieldAccess() {}

	@Override
	@Disabled
	// superseded by #nonPositiveVersionedEntityIsNew()
	void considersEntityAsNotNewWhenHavingIdSetAndUsingPrimitiveTypeForVersionProperty() {}

	@Test
	void nonPositiveVersionedEntityIsNew() {
		EntityInformation<SampleWithPrimitiveVersion, Long> information = new JpaMetamodelEntityInformation<>(SampleWithPrimitiveVersion.class,
				em.getMetamodel(), em.getEntityManagerFactory().getPersistenceUnitUtil());

		SampleWithPrimitiveVersion entity = new SampleWithPrimitiveVersion();
		entity.setId(23L); // assigned
		assertThat(information.isNew(entity)).isTrue();
		entity.setVersion(0);
		assertThat(information.isNew(entity)).isTrue();
	}
}
