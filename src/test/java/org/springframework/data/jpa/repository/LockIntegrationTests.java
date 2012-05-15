/*
 * Copyright 2011 the original author or authors.
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
package org.springframework.data.jpa.repository;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.Serializable;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.repository.sample.RoleRepository;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

/**
 * Integratio test for lock support.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class LockIntegrationTests {

	@Mock
	EntityManager em;
	@Mock
	CriteriaBuilder builder;
	@Mock
	CriteriaQuery<Role> criteriaQuery;
	@Mock
	JpaEntityInformation<Role, Integer> information;
	@Mock
	TypedQuery<Role> query;

	/**
	 * @see DATAJPA-73
	 */
	@Test
	public void usesLockInformationAnnotatedAtRedeclaredMethod() {

		when(information.getJavaType()).thenReturn(Role.class);
		when(em.getCriteriaBuilder()).thenReturn(builder);
		when(builder.createQuery(Role.class)).thenReturn(criteriaQuery);
		when(em.createQuery(criteriaQuery)).thenReturn(query);
		when(query.setLockMode(any(LockModeType.class))).thenReturn(query);

		JpaRepositoryFactory factory = new JpaRepositoryFactory(em) {
			@Override
			@SuppressWarnings("unchecked")
			public <T, ID extends Serializable> JpaEntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
				return (JpaEntityInformation<T, ID>) information;
			}
		};

		RoleRepository repository = factory.getRepository(RoleRepository.class);

		repository.findAll();

		verify(query).setLockMode(LockModeType.READ);
	}
}
