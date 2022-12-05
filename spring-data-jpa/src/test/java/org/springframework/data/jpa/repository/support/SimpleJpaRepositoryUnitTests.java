/*
 * Copyright 2011-2022 the original author or authors.
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

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.data.jpa.domain.Specification.where;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
 * @author Greg Turnquist
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SimpleJpaRepositoryUnitTests {

	private SimpleJpaRepository<User, Integer> repo;

	@Mock EntityManager em;
	@Mock EntityManagerFactory entityManagerFactory;
	@Mock PersistenceUnitUtil persistenceUnitUtil;
	@Mock CriteriaBuilder builder;
	@Mock CriteriaQuery<User> criteriaQuery;
	@Mock CriteriaQuery<Long> countCriteriaQuery;
	@Mock TypedQuery<User> query;
	@Mock TypedQuery<Long> countQuery;
	@Mock JpaEntityInformation<User, Integer> information;
	@Mock CrudMethodMetadata metadata;
	@Mock EntityGraph<User> entityGraph;
	@Mock org.springframework.data.jpa.repository.EntityGraph entityGraphAnnotation;

	@BeforeEach
	void setUp() {

		when(em.getDelegate()).thenReturn(em);

		when(information.getJavaType()).thenReturn(User.class);
		when(em.getCriteriaBuilder()).thenReturn(builder);

		when(builder.createQuery(User.class)).thenReturn(criteriaQuery);
		when(builder.createQuery(Long.class)).thenReturn(countCriteriaQuery);

		when(em.createQuery(criteriaQuery)).thenReturn(query);
		when(em.createQuery(countCriteriaQuery)).thenReturn(countQuery);

		MutableQueryHints hints = new MutableQueryHints();
		when(metadata.getQueryHints()).thenReturn(hints);
		when(metadata.getQueryHintsForCount()).thenReturn(hints);

		repo = new SimpleJpaRepository<>(information, em);
		repo.setRepositoryMethodMetadata(metadata);
	}

	@Test // DATAJPA-124, DATAJPA-912
	void retrieveObjectsForPageableOutOfRange() {

		when(countQuery.getSingleResult()).thenReturn(20L);
		repo.findAll(PageRequest.of(2, 10));

		verify(query).getResultList();
	}

	@Test // DATAJPA-912
	void doesNotRetrieveCountWithoutOffsetAndResultsWithinPageSize() {

		when(query.getResultList()).thenReturn(Arrays.asList(new User(), new User()));

		repo.findAll(PageRequest.of(0, 10));

		verify(countQuery, never()).getSingleResult();
	}

	@Test // DATAJPA-912
	void doesNotRetrieveCountWithOffsetAndResultsWithinPageSize() {

		when(query.getResultList()).thenReturn(Arrays.asList(new User(), new User()));

		repo.findAll(PageRequest.of(2, 10));

		verify(countQuery, never()).getSingleResult();
	}

	@Test // DATAJPA-177, gh-2719
	void doesNotThrowExceptionIfEntityToDeleteDoesNotExist() {

		assertThatNoException().isThrownBy(() -> repo.deleteById(4711));
	}

	@Test // DATAJPA-689, DATAJPA-696
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void shouldPropagateConfiguredEntityGraphToFindOne() throws Exception {

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
	void mergeGetsCalledWhenDetached() {

		User detachedUser = new User();

		when(em.contains(detachedUser)).thenReturn(false);

		repo.save(detachedUser);

		verify(em).merge(detachedUser);
	}

	@Test // DATAJPA-931, DATAJPA-1261
	void mergeGetsCalledWhenAttached() {

		User attachedUser = new User();

		when(em.contains(attachedUser)).thenReturn(true);

		repo.save(attachedUser);

		verify(em).merge(attachedUser);
	}

	@Test // DATAJPA-1535
	void doNothingWhenNewInstanceGetsDeleted() {

		User newUser = new User();
		newUser.setId(null);

		when(em.getEntityManagerFactory()).thenReturn(entityManagerFactory);
		when(entityManagerFactory.getPersistenceUnitUtil()).thenReturn(persistenceUnitUtil);

		repo.delete(newUser);

		verify(em, never()).find(any(Class.class), any(Object.class));
		verify(em, never()).remove(newUser);
		verify(em, never()).merge(newUser);
	}

	@Test // DATAJPA-1535
	void doNothingWhenNonExistentInstanceGetsDeleted() {

		User newUser = new User();
		newUser.setId(23);

		when(information.isNew(newUser)).thenReturn(false);
		when(em.getEntityManagerFactory()).thenReturn(entityManagerFactory);
		when(entityManagerFactory.getPersistenceUnitUtil()).thenReturn(persistenceUnitUtil);
		when(persistenceUnitUtil.getIdentifier(any())).thenReturn(23);
		when(em.find(User.class, 23)).thenReturn(null);

		repo.delete(newUser);

		verify(em, never()).remove(newUser);
		verify(em, never()).merge(newUser);
	}

	@Test // GH-2054
	void applyQueryHintsToCountQueriesForSpecificationPageables() {

		when(query.getResultList()).thenReturn(Arrays.asList(new User(), new User()));

		repo.findAll(where(null), PageRequest.of(2, 1));

		verify(metadata).getQueryHintsForCount();
	}
}
