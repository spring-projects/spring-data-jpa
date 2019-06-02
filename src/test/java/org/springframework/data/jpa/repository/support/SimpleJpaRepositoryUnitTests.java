/*
 * Copyright 2011-2019 the original author or authors.
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

import static java.util.Collections.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Optional;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
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
 * @author Jens Schauder
 */
@RunWith(MockitoJUnitRunner.Silent.class)
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

	@Test // DATAJPA-124, DATAJPA-912
	public void retrieveObjectsForPageableOutOfRange() {

		when(countQuery.getSingleResult()).thenReturn(20L);
		repo.findAll(PageRequest.of(2, 10));

		verify(query).getResultList();
	}

	@Test // DATAJPA-912
	public void doesNotRetrieveCountWithoutOffsetAndResultsWithinPageSize() {

		when(query.getResultList()).thenReturn(Arrays.asList(new User(), new User()));

		repo.findAll(PageRequest.of(0, 10));

		verify(countQuery, never()).getSingleResult();
	}

	@Test // DATAJPA-912
	public void doesNotRetrieveCountWithOffsetAndResultsWithinPageSize() {

		when(query.getResultList()).thenReturn(Arrays.asList(new User(), new User()));

		repo.findAll(PageRequest.of(2, 10));

		verify(countQuery, never()).getSingleResult();
	}

	@Test(expected = EmptyResultDataAccessException.class) // DATAJPA-177
	public void throwsExceptionIfEntityToDeleteDoesNotExist() {

		repo.deleteById(4711);
	}

	@Test // DATAJPA-689, DATAJPA-696
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void shouldPropagateConfiguredEntityGraphToFindOne() throws Exception {

		String entityGraphName = "User.detail";
		when(entityGraphAnnotation.value()).thenReturn(entityGraphName);
		when(entityGraphAnnotation.type()).thenReturn(EntityGraphType.LOAD);
		when(metadata.getEntityGraph()).thenReturn(Optional.of(entityGraphAnnotation));
		when(em.getEntityGraph(entityGraphName)).thenReturn((EntityGraph) entityGraph);
		when(information.getEntityName()).thenReturn("User");
		when(metadata.getMethod()).thenReturn(CrudRepository.class.getMethod("findById", Object.class));

		Integer id = 0;
		repo.findById(id);

		verify(em).find(User.class, id, singletonMap(EntityGraphType.LOAD.getKey(), (Object) entityGraph));
	}

	@Test // DATAJPA-931
	public void mergeGetsCalledWhenDetached() {

		User detachedUser = new User();

		when(em.contains(detachedUser)).thenReturn(false);

		repo.save(detachedUser);

		verify(em).merge(detachedUser);
	}

	@Test // DATAJPA-931, DATAJPA-1261
	public void mergeGetsCalledWhenAttached() {

		User attachedUser = new User();

		when(em.contains(attachedUser)).thenReturn(true);

		repo.save(attachedUser);

		verify(em).merge(attachedUser);
	}

	@Test // DATAJPA-1535
	public void doNothingWhenNewInstanceGetsDeleted() {

		User newUser = new User();
		newUser.setId(null);

		repo.delete(newUser);

		verify(em, never()).find(any(Class.class), any(Object.class));
		verify(em, never()).remove(newUser);
		verify(em, never()).merge(newUser);
	}

	@Test // DATAJPA-1535
	public void doNothingWhenNonExistentInstanceGetsDeleted() {

		User newUser = new User();
		newUser.setId(23);

		when(information.isNew(newUser)).thenReturn(false);
		when(em.find(User.class,23)).thenReturn(null);

		repo.delete(newUser);

		verify(em, never()).remove(newUser);
		verify(em, never()).merge(newUser);
	}

}
