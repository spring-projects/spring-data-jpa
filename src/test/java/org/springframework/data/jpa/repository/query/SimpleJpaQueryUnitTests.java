/*
 * Copyright 2008-2011 the original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.QueryHint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.Parameters;

/**
 * Unit test for {@link SimpleJpaQuery}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class SimpleJpaQueryUnitTests {

	JpaQueryMethod method;

	@Mock
	EntityManager em;
	@Mock
	QueryExtractor extractor;
	@Mock
	Query query;
	@Mock
	RepositoryMetadata metadata;
	@Mock
	ParameterBinder binder;

	@Before
	@QueryHints(@QueryHint(name = "foo", value = "bar"))
	public void setUp() throws SecurityException, NoSuchMethodException {

		when(em.createQuery(anyString())).thenReturn(query);

		Method setUp = UserRepository.class.getMethod("findByLastname", String.class);
		method = new JpaQueryMethod(setUp, metadata, extractor);
	}

	@Test
	public void appliesHintsCorrectly() throws Exception {

		SimpleJpaQuery jpaQuery = new SimpleJpaQuery(method, em, "foobar");
		jpaQuery.createQuery(new Object[] { "gierke" });

		verify(query).setHint("foo", "bar");
	}

	@Test
	public void prefersDeclaredCountQueryOverCreatingOne() throws Exception {

		method = mock(JpaQueryMethod.class);
		when(method.getCountQuery()).thenReturn("foo");
		when(method.getParameters()).thenReturn(
				new Parameters(SimpleJpaQueryUnitTests.class.getMethod("prefersDeclaredCountQueryOverCreatingOne")));
		when(em.createQuery("foo")).thenReturn(query);

		SimpleJpaQuery jpaQuery = new SimpleJpaQuery(method, em, "select u from User u");

		assertThat(jpaQuery.createCountQuery(new Object[] {}), is(query));
	}

	/**
	 * @see DATAJPA-77
	 */
	@Test
	public void doesNotApplyPaginationToCountQuery() throws Exception {

		when(em.createQuery(Mockito.anyString())).thenReturn(query);

		Method method = UserRepository.class.getMethod("findAllPaged", Pageable.class);
		JpaQueryMethod queryMethod = new JpaQueryMethod(method, metadata, extractor);

		AbstractJpaQuery jpaQuery = new SimpleJpaQuery(queryMethod, em, "select u from User u");
		jpaQuery.createCountQuery(new Object[] { new PageRequest(1, 10) });

		verify(query, times(0)).setFirstResult(anyInt());
		verify(query, times(0)).setMaxResults(anyInt());
	}
}
