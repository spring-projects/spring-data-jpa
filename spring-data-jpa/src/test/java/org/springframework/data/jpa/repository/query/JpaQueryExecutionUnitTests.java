/*
 * Copyright 2008-2023 the original author or authors.
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
package org.springframework.data.jpa.repository.query;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.vavr.control.Try;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.ModifyingExecution;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.PagedExecution;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;

/**
 * Unit test for {@link JpaQueryExecution}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 * @author Nicolas Cirigliano
 * @author Jens Schauder
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JpaQueryExecutionUnitTests {

	@Mock EntityManager em;
	@Mock AbstractStringBasedJpaQuery jpaQuery;
	@Mock Query query;
	@Mock JpaQueryMethod method;
	@Mock JpaParametersParameterAccessor accessor;

	@Mock TypedQuery<Long> countQuery;

	// needs to be public
	public static void sampleMethod(Pageable pageable) {}

	@BeforeEach
	void setUp() {

		when(query.executeUpdate()).thenReturn(0);
		when(jpaQuery.createQuery(Mockito.any(JpaParametersParameterAccessor.class))).thenReturn(query);
		when(jpaQuery.getQueryMethod()).thenReturn(method);
	}

	@Test
	void rejectsNullQuery() {

		assertThatIllegalArgumentException().isThrownBy(() -> new StubQueryExecution().execute(null, accessor));
	}

	@Test
	void rejectsNullBinder() {

		assertThatIllegalArgumentException().isThrownBy(() -> new StubQueryExecution().execute(jpaQuery, null));
	}

	@Test // DATAJPA-1827
	void supportsModifyingResultsUsingWrappers() throws Exception {

		Method method = VavrRepository.class.getMethod("updateUsingVavrMethod");
		DefaultRepositoryMetadata repositoryMetadata = new DefaultRepositoryMetadata(VavrRepository.class);
		JpaQueryMethod queryMethod = new JpaQueryMethod(method, repositoryMetadata, new SpelAwareProxyProjectionFactory(),
				mock(QueryExtractor.class));

		new JpaQueryExecution.ModifyingExecution(queryMethod, mock(EntityManager.class));

		assertThat(queryMethod.isModifyingQuery()).isTrue();
	}

	interface VavrRepository extends Repository<String, String> {

		// Wrapped outcome allowed
		@org.springframework.data.jpa.repository.Query("update Credential d set d.enabled = false where d.enabled = true")
		@Modifying
		Try<Integer> updateUsingVavrMethod();
	}

	@Test
	void transformsNoResultExceptionToNull() {

		assertThat(new JpaQueryExecution() {

			@Override
			protected Object doExecute(AbstractJpaQuery query, JpaParametersParameterAccessor accessor) {

				return null;
			}
		}.execute(jpaQuery, accessor)).isNull();
	}

	@Test // DATAJPA-806
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void modifyingExecutionFlushesEntityManagerIfSet() {

		when(method.getReturnType()).thenReturn((Class) void.class);
		when(method.getFlushAutomatically()).thenReturn(true);

		ModifyingExecution execution = new ModifyingExecution(method, em);
		execution.execute(jpaQuery, accessor);

		verify(em, times(1)).flush();
		verify(em, times(0)).clear();
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void modifyingExecutionClearsEntityManagerIfSet() {

		when(method.getReturnType()).thenReturn((Class) void.class);
		when(method.getClearAutomatically()).thenReturn(true);

		ModifyingExecution execution = new ModifyingExecution(method, em);
		execution.execute(jpaQuery, accessor);

		verify(em, times(0)).flush();
		verify(em, times(1)).clear();
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void allowsMethodReturnTypesForModifyingQuery() {

		when(method.getReturnType()).thenReturn((Class) void.class, (Class) int.class, (Class) Integer.class);

		new ModifyingExecution(method, em);
		new ModifyingExecution(method, em);
		new ModifyingExecution(method, em);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	void modifyingExecutionRejectsNonIntegerOrVoidReturnType() {

		when(method.getReturnType()).thenReturn((Class) Long.class);
		assertThatIllegalArgumentException().isThrownBy(() -> new ModifyingExecution(method, em));
	}

	@Test // DATAJPA-124, DATAJPA-912
	void pagedExecutionRetrievesObjectsForPageableOutOfRange() throws Exception {

		JpaParameters parameters = new JpaParameters(getClass().getMethod("sampleMethod", Pageable.class));
		when(jpaQuery.createCountQuery(Mockito.any())).thenReturn(countQuery);
		when(jpaQuery.createQuery(Mockito.any())).thenReturn(query);
		when(countQuery.getResultList()).thenReturn(Arrays.asList(20L));

		PagedExecution execution = new PagedExecution();
		execution.doExecute(jpaQuery,
				new JpaParametersParameterAccessor(parameters, new Object[] { PageRequest.of(2, 10) }));

		verify(query).getResultList();
		verify(countQuery).getResultList();
	}

	@Test // DATAJPA-477, DATAJPA-912
	void pagedExecutionShouldNotGenerateCountQueryIfQueryReportedNoResults() throws Exception {

		JpaParameters parameters = new JpaParameters(getClass().getMethod("sampleMethod", Pageable.class));
		when(jpaQuery.createQuery(Mockito.any())).thenReturn(query);
		when(query.getResultList()).thenReturn(Arrays.asList(0L));

		PagedExecution execution = new PagedExecution();
		execution.doExecute(jpaQuery,
				new JpaParametersParameterAccessor(parameters, new Object[] { PageRequest.of(0, 10) }));

		verify(countQuery, times(0)).getResultList();
		verify(jpaQuery, times(0)).createCountQuery(any());
	}

	@Test // DATAJPA-912
	void pagedExecutionShouldUseCountFromResultIfOffsetIsZeroAndResultsWithinPageSize() throws Exception {

		JpaParameters parameters = new JpaParameters(getClass().getMethod("sampleMethod", Pageable.class));
		when(jpaQuery.createQuery(Mockito.any())).thenReturn(query);
		when(query.getResultList()).thenReturn(Arrays.asList(new Object(), new Object(), new Object(), new Object()));

		PagedExecution execution = new PagedExecution();
		execution.doExecute(jpaQuery,
				new JpaParametersParameterAccessor(parameters, new Object[] { PageRequest.of(0, 10) }));

		verify(jpaQuery, times(0)).createCountQuery(any());
	}

	@Test // DATAJPA-912
	void pagedExecutionShouldUseCountFromResultWithOffsetAndResultsWithinPageSize() throws Exception {

		JpaParameters parameters = new JpaParameters(getClass().getMethod("sampleMethod", Pageable.class));
		when(jpaQuery.createQuery(Mockito.any())).thenReturn(query);
		when(query.getResultList()).thenReturn(Arrays.asList(new Object(), new Object(), new Object(), new Object()));

		PagedExecution execution = new PagedExecution();
		execution.doExecute(jpaQuery,
				new JpaParametersParameterAccessor(parameters, new Object[] { PageRequest.of(5, 10) }));

		verify(jpaQuery, times(0)).createCountQuery(any());
	}

	@Test // DATAJPA-912
	void pagedExecutionShouldUseRequestCountFromResultWithOffsetAndResultsHitLowerPageSizeBounds()
			throws Exception {

		JpaParameters parameters = new JpaParameters(getClass().getMethod("sampleMethod", Pageable.class));
		when(jpaQuery.createQuery(Mockito.any())).thenReturn(query);
		when(query.getResultList()).thenReturn(Collections.emptyList());
		when(jpaQuery.createCountQuery(Mockito.any())).thenReturn(query);
		when(countQuery.getResultList()).thenReturn(Arrays.asList(20L));

		PagedExecution execution = new PagedExecution();
		execution.doExecute(jpaQuery,
				new JpaParametersParameterAccessor(parameters, new Object[] { PageRequest.of(4, 4) }));

		verify(jpaQuery).createCountQuery(any());
	}

	@Test // DATAJPA-912
	void pagedExecutionShouldUseRequestCountFromResultWithOffsetAndResultsHitUpperPageSizeBounds()
			throws Exception {

		JpaParameters parameters = new JpaParameters(getClass().getMethod("sampleMethod", Pageable.class));
		when(jpaQuery.createQuery(Mockito.any())).thenReturn(query);
		when(query.getResultList()).thenReturn(Arrays.asList(new Object(), new Object(), new Object(), new Object()));
		when(jpaQuery.createCountQuery(Mockito.any())).thenReturn(query);
		when(countQuery.getResultList()).thenReturn(Arrays.asList(20L));

		PagedExecution execution = new PagedExecution();
		execution.doExecute(jpaQuery,
				new JpaParametersParameterAccessor(parameters, new Object[] { PageRequest.of(4, 4) }));

		verify(jpaQuery).createCountQuery(any());
	}

	@Test // DATAJPA-951
	void doesNotPreemtivelyWrapResultIntoOptional() {

		doReturn(method).when(jpaQuery).getQueryMethod();
		doReturn(Optional.class).when(method).getReturnType();
		JpaParametersParameterAccessor accessor = mock(JpaParametersParameterAccessor.class);

		StubQueryExecution execution = new StubQueryExecution() {
			@Override
			protected Object doExecute(AbstractJpaQuery query, JpaParametersParameterAccessor accessor) {
				return "result";
			}
		};

		Object result = execution.execute(jpaQuery, accessor);

		assertThat(result).isInstanceOf(String.class);
	}

	static class StubQueryExecution extends JpaQueryExecution {

		@Override
		protected Object doExecute(AbstractJpaQuery query, JpaParametersParameterAccessor accessor) {
			return null;
		}
	}
}
