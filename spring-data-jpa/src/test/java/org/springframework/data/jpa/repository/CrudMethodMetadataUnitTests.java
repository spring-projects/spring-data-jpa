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
package org.springframework.data.jpa.repository;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.metamodel.Metamodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.data.jpa.domain.sample.QRole;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.repository.sample.RoleRepository;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

/**
 * Integration test for lock support.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CrudMethodMetadataUnitTests {

	@Mock EntityManager em;
	@Mock EntityManagerFactory emf;
	@Mock CriteriaBuilder builder;
	@Mock CriteriaQuery<Role> criteriaQuery;
	@Mock JpaEntityInformation<Role, Integer> information;
	@Mock TypedQuery<Role> typedQuery;
	@Mock jakarta.persistence.Query query;
	@Mock Metamodel metamodel;

	private RoleRepository repository;

	@BeforeEach
	void setUp() {

		when(information.getJavaType()).thenReturn(Role.class);

		when(em.getMetamodel()).thenReturn(metamodel);
		when(em.getDelegate()).thenReturn(em);
		when(em.getEntityManagerFactory()).thenReturn(emf);
		when(emf.createEntityManager()).thenReturn(em);

		JpaRepositoryFactory factory = new JpaRepositoryFactory(em) {
			@Override
			@SuppressWarnings("unchecked")
			public <T, ID> JpaEntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
				return (JpaEntityInformation<T, ID>) information;
			}
		};

		repository = factory.getRepository(RoleRepository.class);
	}

	@Test // DATAJPA-73, DATAJPA-173
	void usesLockInformationAnnotatedAtRedeclaredMethod() {

		when(em.getCriteriaBuilder()).thenReturn(builder);
		when(builder.createQuery(Role.class)).thenReturn(criteriaQuery);
		when(em.createQuery(criteriaQuery)).thenReturn(typedQuery);
		when(typedQuery.setLockMode(any(LockModeType.class))).thenReturn(typedQuery);

		repository.findAll();

		verify(typedQuery).setLockMode(LockModeType.READ);
		verify(typedQuery).setHint("foo", "bar");
	}

	@Test // DATAJPA-359, DATAJPA-173
	void usesMetadataAnnotatedAtRedeclaredFindOne() {

		repository.findById(1);

		Map<String, Object> expectedLinks = Collections.singletonMap("foo", (Object) "bar");
		LockModeType expectedLockModeType = LockModeType.READ;

		verify(em).find(Role.class, 1, expectedLockModeType, expectedLinks);
	}

	@Test // DATAJPA-574
	void appliesLockModeAndQueryHintsToQuerydslQuery() {

		when(em.getDelegate()).thenReturn(mock(EntityManager.class));
		when(em.createQuery(anyString())).thenReturn(query);

		repository.findOne(QRole.role.name.eq("role"));

		verify(query).setLockMode(LockModeType.READ);
		verify(query).setHint("foo", "bar");
	}
}
