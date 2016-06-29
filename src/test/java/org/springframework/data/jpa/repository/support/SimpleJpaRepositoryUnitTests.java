/*
 * Copyright 2011-2016 the original author or authors.
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
package org.springframework.data.jpa.repository.support;

import static java.util.Collections.*;
import static org.mockito.Mockito.*;

import java.io.Serializable;
import java.util.Arrays;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.repository.CrudRepository;

/**
 * Unit tests for {@link SimpleJpaRepository}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class SimpleJpaRepositoryUnitTests {

	SimpleJpaRepository<User, Integer> repo;

	@Mock EntityManager em;
	@Mock CriteriaBuilder builder;
	@Mock CriteriaQuery<User> criteriaQuery;
	@Mock CriteriaQuery<Long> countCriteriaQuery;
	@Mock TypedQuery<User> query;
	@Mock TypedQuery<Long> countQuery;
	@Mock JpaEntityInformation<User, Long> information;
	@Mock CrudMethodMetadata metadata;
	@Mock EntityGraph<User> entityGraph;
	@Mock org.springframework.data.jpa.repository.EntityGraph entityGraphAnnotation;

	@Before
	public void setUp() {

		when(em.getDelegate()).thenReturn(em);

		when(information.getJavaType()).thenReturn(User.class);
		when(em.getCriteriaBuilder()).thenReturn(builder);

		when(builder.createQuery(User.class)).thenReturn(criteriaQuery);
		when(builder.createQuery(Long.class)).thenReturn(countCriteriaQuery);

		when(em.createQuery(criteriaQuery)).thenReturn(query);
		when(em.createQuery(countCriteriaQuery)).thenReturn(countQuery);

		repo = new SimpleJpaRepository<User, Integer>(information, em);
		repo.setRepositoryMethodMetadata(metadata);
	}

	/**
	 * @see DATAJPA-124
	 * @see DATAJPA-912
	 */
	@Test
	public void retrieveObjectsForPageableOutOfRange() {

		when(countQuery.getSingleResult()).thenReturn(20L);
		repo.findAll(new PageRequest(2, 10));

		verify(query).getResultList();
	}

	/**
	 * @see DATAJPA-912
	 */
	@Test
	public void doesNotRetrieveCountWithoutOffsetAndResultsWithinPageSize() {

		when(query.getResultList()).thenReturn(Arrays.asList(new User(), new User()));

		repo.findAll(new PageRequest(0, 10));

		verify(countQuery, never()).getSingleResult();
	}

	/**
	 * @see DATAJPA-912
	 */
	@Test
	public void doesNotRetrieveCountWithOffsetAndResultsWithinPageSize() {

		when(query.getResultList()).thenReturn(Arrays.asList(new User(), new User()));

		repo.findAll(new PageRequest(2, 10));

		verify(countQuery, never()).getSingleResult();
	}

	/**
	 * @see DATAJPA-177
	 */
	@Test(expected = EmptyResultDataAccessException.class)
	public void throwsExceptionIfEntityToDeleteDoesNotExist() {

		repo.delete(4711);
	}

	/**
	 * @see DATAJPA-689
	 * @see DATAJPA-696
	 */
	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void shouldPropagateConfiguredEntityGraphToFindOne() throws Exception {

		String entityGraphName = "User.detail";
		when(entityGraphAnnotation.value()).thenReturn(entityGraphName);
		when(entityGraphAnnotation.type()).thenReturn(EntityGraphType.LOAD);
		when(metadata.getEntityGraph()).thenReturn(entityGraphAnnotation);
		when(em.getEntityGraph(entityGraphName)).thenReturn((EntityGraph) entityGraph);
		when(information.getEntityName()).thenReturn("User");
		when(metadata.getMethod()).thenReturn(CrudRepository.class.getMethod("findOne", Serializable.class));

		Integer id = 0;
		repo.findOne(id);

		verify(em).find(User.class, id, singletonMap(EntityGraphType.LOAD.getKey(), (Object) entityGraph));
	}
}
