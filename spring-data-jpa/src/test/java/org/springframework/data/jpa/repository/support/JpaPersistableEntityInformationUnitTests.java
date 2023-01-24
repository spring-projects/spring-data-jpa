/*
 * Copyright 2011-2023 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.Type;
import lombok.Getter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Persistable;
import org.springframework.data.repository.core.EntityInformation;

/**
 * Unit tests for {@link JpaPersistableEntityInformation}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Krzysztof Krason
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JpaPersistableEntityInformationUnitTests {

	@Mock EntityManager em;
	@Mock EntityManagerFactory entityManagerFactory;
	@Mock Metamodel metamodel;
	@Mock PersistenceUnitUtil persistenceUnitUtil;

	@Mock EntityType<Foo> type;

	@Mock
	@SuppressWarnings("rawtypes") Type idType;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {

		when(em.getMetamodel()).thenReturn(metamodel);
		when(em.getEntityManagerFactory()).thenReturn(entityManagerFactory);
		when(entityManagerFactory.getPersistenceUnitUtil()).thenReturn(persistenceUnitUtil);
		when(metamodel.managedType(Object.class)).thenThrow(IllegalArgumentException.class);
		when(metamodel.managedType(Foo.class)).thenReturn(type);
		when(type.getIdType()).thenReturn(idType);
	}

	@Test
	void usesPersistableMethodsForIsNewAndGetId() {

		EntityInformation<Foo, Long> entityInformation = new JpaPersistableEntityInformation<>(Foo.class, em.getMetamodel(),
				em.getEntityManagerFactory().getPersistenceUnitUtil());

		Foo foo = new Foo();
		assertThat(entityInformation.isNew(foo)).isFalse();
		assertThat(entityInformation.getId(foo)).isNull();

		foo.id = 1L;
		assertThat(entityInformation.isNew(foo)).isTrue();
		assertThat(entityInformation.getId(foo)).isOne();
	}

	@SuppressWarnings("serial")
	class Foo implements Persistable<Long> {

		@Getter Long id;

		@Override
		public boolean isNew() {
			return id != null;
		}
	}
}
