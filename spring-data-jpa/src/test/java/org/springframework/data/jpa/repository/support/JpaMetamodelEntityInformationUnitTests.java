/*
 * Copyright 2012-2023 the original author or authors.
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

import static java.util.Arrays.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.Type;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.sample.PersistableWithIdClass;
import org.springframework.data.jpa.domain.sample.PersistableWithIdClassPK;

/**
 * Unit tests for {@link JpaMetamodelEntityInformation}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JpaMetamodelEntityInformationUnitTests {

	@Mock EntityManager em;
	@Mock EntityManagerFactory entityManagerFactory;
	@Mock PersistenceUnitUtil persistenceUnit;
	@Mock Metamodel metamodel;

	@Mock IdentifiableType<PersistableWithIdClass> type;
	@Mock SingularAttribute<PersistableWithIdClass, ?> first, second;

	@Mock
	@SuppressWarnings("rawtypes") Type idType;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {

		when(em.getMetamodel()).thenReturn(metamodel);
		when(em.getEntityManagerFactory()).thenReturn(entityManagerFactory);
		when(entityManagerFactory.getPersistenceUnitUtil()).thenReturn(persistenceUnit);

		when(first.getName()).thenReturn("first");
		when(second.getName()).thenReturn("second");
		Set<SingularAttribute<? super PersistableWithIdClass, ?>> attributes = new HashSet<>(asList(first, second));

		when(type.getIdClassAttributes()).thenReturn(attributes);

		when(metamodel.managedType(Object.class)).thenThrow(IllegalArgumentException.class);
		when(metamodel.managedType(PersistableWithIdClass.class)).thenReturn(type);

		when(type.getIdType()).thenReturn(idType);
		when(idType.getJavaType()).thenReturn(PersistableWithIdClassPK.class);
	}

	@Test // DATAJPA-50
	void doesNotCreateIdIfAllPartialAttributesAreNull() {

		JpaMetamodelEntityInformation<PersistableWithIdClass, Serializable> information = new JpaMetamodelEntityInformation<>(
				PersistableWithIdClass.class, em.getMetamodel(), em.getEntityManagerFactory().getPersistenceUnitUtil());

		PersistableWithIdClass entity = new PersistableWithIdClass(null, null);
		assertThat(information.getId(entity)).isNull();

		entity = new PersistableWithIdClass(2L, null);
		when(persistenceUnit.getIdentifier(entity)).thenReturn(2L);
		assertThat(information.getId(entity)).isNotNull();
	}
}
