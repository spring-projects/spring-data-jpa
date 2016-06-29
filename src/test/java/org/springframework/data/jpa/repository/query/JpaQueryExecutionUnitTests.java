/*
 * Copyright 2008-2016 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.ModifyingExecution;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.PagedExecution;
import org.springframework.data.repository.query.DefaultParameters;
import org.springframework.data.repository.query.Parameters;

/**
 * Unit test for {@link JpaQueryExecution}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class JpaQueryExecutionUnitTests {

	@Mock EntityManager em;
	@Mock AbstractStringBasedJpaQuery jpaQuery;
	@Mock Query query;
	@Mock JpaQueryMethod method;

	@Mock TypedQuery<Long> countQuery;

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullQuery() {

		new StubQueryExecution().execute(null, new Object[] {});
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullBinder() throws Exception {

		new StubQueryExecution().execute(jpaQuery, null);
	}

	@Test
	public void transformsNoResultExceptionToNull() {

		assertThat(new JpaQueryExecution() {

			@Override
			protected Object doExecute(AbstractJpaQuery query, Object[] values) {

				return null;
			}
		}.execute(jpaQuery, new Object[] {}), is(nullValue()));
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void modifyingExecutionClearsEntityManagerIfSet() {

		when(query.executeUpdate()).thenReturn(0);
		when(method.getReturnType()).thenReturn((Class) void.class);
		when(jpaQuery.createQuery(Mockito.any(Object[].class))).thenReturn(query);
		when(jpaQuery.getQueryMethod()).thenReturn(method);

		ModifyingExecution execution = new ModifyingExecution(method, em);
		execution.execute(jpaQuery, new Object[] {});

		verify(em, times(1)).clear();
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void allowsMethodReturnTypesForModifyingQuery() throws Exception {

		when(method.getReturnType()).thenReturn((Class) void.class, (Class) int.class, (Class) Integer.class);

		new ModifyingExecution(method, em);
		new ModifyingExecution(method, em);
		new ModifyingExecution(method, em);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test(expected = IllegalArgumentException.class)
	public void modifyingExecutionRejectsNonIntegerOrVoidReturnType() throws Exception {

		when(method.getReturnType()).thenReturn((Class) Long.class);
		new ModifyingExecution(method, em);
	}

	/**
	 * @see DATAJPA-124
	 * @see DATAJPA-912
	 */
	@Test
	public void pagedExecutionRetrievesObjectsForPageableOutOfRange() throws Exception {

		Parameters<?, ?> parameters = new DefaultParameters(getClass().getMethod("sampleMethod", Pageable.class));
		when(jpaQuery.createCountQuery(Mockito.any(Object[].class))).thenReturn(countQuery);
		when(jpaQuery.createQuery(Mockito.any(Object[].class))).thenReturn(query);
		when(countQuery.getResultList()).thenReturn(Arrays.asList(20L));

		PagedExecution execution = new PagedExecution(parameters);
		execution.doExecute(jpaQuery, new Object[] { new PageRequest(2, 10) });

		verify(query).getResultList();
		verify(countQuery).getResultList();
	}

	/**
	 * @see DATAJPA-477
	 * @see DATAJPA-912
	 */
	@Test
	public void pagedExecutionShouldNotGenerateCountQueryIfQueryReportedNoResults() throws Exception {

		Parameters<?, ?> parameters = new DefaultParameters(getClass().getMethod("sampleMethod", Pageable.class));
		when(jpaQuery.createQuery(Mockito.any(Object[].class))).thenReturn(query);
		when(query.getResultList()).thenReturn(Arrays.asList(0L));

		PagedExecution execution = new PagedExecution(parameters);
		execution.doExecute(jpaQuery, new Object[] { new PageRequest(0, 10) });

		verify(countQuery, times(0)).getResultList();
		verify(jpaQuery, times(0)).createCountQuery((Object[]) any());
	}

	/**
	 * @see DATAJPA-912
	 */
	@Test
	public void pagedExecutionShouldUseCountFromResultIfOffsetIsZeroAndResultsWithinPageSize() throws Exception {

		Parameters<?, ?> parameters = new DefaultParameters(getClass().getMethod("sampleMethod", Pageable.class));
		when(jpaQuery.createQuery(Mockito.any(Object[].class))).thenReturn(query);
		when(query.getResultList()).thenReturn(Arrays.asList(new Object(), new Object(), new Object(), new Object()));

		PagedExecution execution = new PagedExecution(parameters);
		execution.doExecute(jpaQuery, new Object[] { new PageRequest(0, 10) });

		verify(jpaQuery, times(0)).createCountQuery((Object[]) any());
	}

	/**
	 * @see DATAJPA-912
	 */
	@Test
	public void pagedExecutionShouldUseCountFromResultWithOffsetAndResultsWithinPageSize() throws Exception {

		Parameters<?, ?> parameters = new DefaultParameters(getClass().getMethod("sampleMethod", Pageable.class));
		when(jpaQuery.createQuery(Mockito.any(Object[].class))).thenReturn(query);
		when(query.getResultList()).thenReturn(Arrays.asList(new Object(), new Object(), new Object(), new Object()));

		PagedExecution execution = new PagedExecution(parameters);
		execution.doExecute(jpaQuery, new Object[] { new PageRequest(5, 10) });

		verify(jpaQuery, times(0)).createCountQuery((Object[]) any());
	}

	/**
	 * @see DATAJPA-912
	 */
	@Test
	public void pagedExecutionShouldUseRequestCountFromResultWithOffsetAndResultsHitLowerPageSizeBounds() throws Exception {

		Parameters<?, ?> parameters = new DefaultParameters(getClass().getMethod("sampleMethod", Pageable.class));
		when(jpaQuery.createQuery(Mockito.any(Object[].class))).thenReturn(query);
		when(query.getResultList()).thenReturn(Collections.emptyList());
		when(jpaQuery.createCountQuery(Mockito.any(Object[].class))).thenReturn(query);
		when(countQuery.getResultList()).thenReturn(Arrays.asList(20L));

		PagedExecution execution = new PagedExecution(parameters);
		execution.doExecute(jpaQuery, new Object[] { new PageRequest(4, 4) });

		verify(jpaQuery).createCountQuery((Object[]) any());
	}

	/**
	 * @see DATAJPA-912
	 */
	@Test
	public void pagedExecutionShouldUseRequestCountFromResultWithOffsetAndResultsHitUpperPageSizeBounds() throws Exception {

		Parameters<?, ?> parameters = new DefaultParameters(getClass().getMethod("sampleMethod", Pageable.class));
		when(jpaQuery.createQuery(Mockito.any(Object[].class))).thenReturn(query);
		when(query.getResultList()).thenReturn(Arrays.asList(new Object(), new Object(), new Object(), new Object()));
		when(jpaQuery.createCountQuery(Mockito.any(Object[].class))).thenReturn(query);
		when(countQuery.getResultList()).thenReturn(Arrays.asList(20L));

		PagedExecution execution = new PagedExecution(parameters);
		execution.doExecute(jpaQuery, new Object[] { new PageRequest(4, 4) });

		verify(jpaQuery).createCountQuery((Object[]) any());
	}

	public static void sampleMethod(Pageable pageable) {

	}

	static class StubQueryExecution extends JpaQueryExecution {

		@Override
		protected Object doExecute(AbstractJpaQuery query, Object[] values) {

			return null;
		}
	}
}
