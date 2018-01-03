/*
 * Copyright 2008-2018 the original author or authors.
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
package org.springframework.data.jpa.repository.query;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.data.jpa.support.EntityManagerTestUtils.*;

import java.lang.reflect.Method;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.QueryHint;
import javax.persistence.TypedQuery;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for {@link AbstractJpaQuery}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:infrastructure.xml")
public class AbstractJpaQueryTests {

	@PersistenceContext EntityManager em;

	Query query;
	TypedQuery<Long> countQuery;

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() {
		query = mock(Query.class);
		countQuery = mock(TypedQuery.class);
	}

	@Test // DATADOC-97
	public void addsHintsToQueryObject() throws Exception {

		JpaQueryMethod queryMethod = getMethod("findByLastname", String.class);

		AbstractJpaQuery jpaQuery = new DummyJpaQuery(queryMethod, em);

		Query result = jpaQuery.createQuery(new Object[] { "Matthews" });
		verify(result).setHint("foo", "bar");

		result = jpaQuery.createCountQuery(new Object[] { "Matthews" });
		verify(result).setHint("foo", "bar");
	}

	@Test // DATAJPA-54
	public void skipsHintsForCountQueryIfConfigured() throws Exception {

		JpaQueryMethod queryMethod = getMethod("findByFirstname", String.class);
		AbstractJpaQuery jpaQuery = new DummyJpaQuery(queryMethod, em);

		Query result = jpaQuery.createQuery(new Object[] { "Dave" });
		verify(result).setHint("bar", "foo");

		result = jpaQuery.createCountQuery(new Object[] { "Dave" });
		verify(result, never()).setHint("bar", "foo");
	}

	@Test // DATAJPA-73
	public void addsLockingModeToQueryObject() throws Exception {

		when(query.setLockMode(any(LockModeType.class))).thenReturn(query);

		JpaQueryMethod queryMethod = getMethod("findOneLocked", Integer.class);

		AbstractJpaQuery jpaQuery = new DummyJpaQuery(queryMethod, em);
		Query result = jpaQuery.createQuery(new Object[] { Integer.valueOf(1) });
		verify(result).setLockMode(LockModeType.PESSIMISTIC_WRITE);
	}

	@Test // DATAJPA-466
	@Transactional
	public void shouldAddEntityGraphHintForFetch() throws Exception {

		Assume.assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

		JpaQueryMethod queryMethod = getMethod("findAll");

		javax.persistence.EntityGraph<?> entityGraph = em.getEntityGraph("User.overview");

		AbstractJpaQuery jpaQuery = new DummyJpaQuery(queryMethod, em);
		Query result = jpaQuery.createQuery(new Object[0]);

		verify(result).setHint("javax.persistence.fetchgraph", entityGraph);
	}

	@Test // DATAJPA-466
	@Transactional
	public void shouldAddEntityGraphHintForLoad() throws Exception {

		Assume.assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

		JpaQueryMethod queryMethod = getMethod("getById", Integer.class);

		javax.persistence.EntityGraph<?> entityGraph = em.getEntityGraph("User.detail");

		AbstractJpaQuery jpaQuery = new DummyJpaQuery(queryMethod, em);
		Query result = jpaQuery.createQuery(new Object[] { 1 });

		verify(result).setHint("javax.persistence.loadgraph", entityGraph);
	}

	private JpaQueryMethod getMethod(String name, Class<?>... parameterTypes) throws Exception {

		Method method = SampleRepository.class.getMethod(name, parameterTypes);
		PersistenceProvider persistenceProvider = PersistenceProvider.fromEntityManager(em);

		return new JpaQueryMethod(method, new DefaultRepositoryMetadata(SampleRepository.class),
				new SpelAwareProxyProjectionFactory(), persistenceProvider);
	}

	interface SampleRepository extends Repository<User, Integer> {

		@QueryHints({ @QueryHint(name = "foo", value = "bar") })
		List<User> findByLastname(String lastname);

		@QueryHints(value = { @QueryHint(name = "bar", value = "foo") }, forCounting = false)
		List<User> findByFirstname(String firstname);

		@Lock(LockModeType.PESSIMISTIC_WRITE)
		@org.springframework.data.jpa.repository.Query("select u from User u where u.id = ?1")
		List<User> findOneLocked(Integer primaryKey);

		// DATAJPA-466
		@EntityGraph(value = "User.detail", type = EntityGraphType.LOAD)
		User getById(Integer id);

		// DATAJPA-466
		@EntityGraph("User.overview")
		List<User> findAll();
	}

	class DummyJpaQuery extends AbstractJpaQuery {

		public DummyJpaQuery(JpaQueryMethod method, EntityManager em) {
			super(method, em);
		}

		@Override
		protected Query doCreateQuery(Object[] values) {
			return query;
		}

		@Override
		protected TypedQuery<Long> doCreateCountQuery(Object[] values) {
			return (TypedQuery<Long>) countQuery;
		}
	}
}
