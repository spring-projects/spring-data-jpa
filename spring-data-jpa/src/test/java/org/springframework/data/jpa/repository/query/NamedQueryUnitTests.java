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

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.metamodel.Metamodel;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryCreationException;
import org.springframework.data.util.TypeInformation;

/**
 * Unit tests for {@link NamedQuery}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 * @author Erik Pellizzon
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NamedQueryUnitTests {

	@Mock RepositoryMetadata metadata;
	@Mock QueryExtractor extractor;
	@Mock EntityManager em;
	@Mock EntityManagerFactory emf;
	@Mock Metamodel metamodel;

	private ProjectionFactory projectionFactory = new SpelAwareProxyProjectionFactory();

	private Method method;

	@BeforeEach
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void setUp() throws SecurityException, NoSuchMethodException {

		method = SampleRepository.class.getMethod("foo", Pageable.class);
		when(metadata.getDomainType()).thenReturn((Class) String.class);
		when(metadata.getReturnedDomainClass(method)).thenReturn((Class) String.class);
		when(metadata.getReturnType(any(Method.class)))
				.thenAnswer(invocation -> TypeInformation.fromReturnTypeOf(invocation.getArgument(0)));

		when(em.getMetamodel()).thenReturn(metamodel);
		when(em.getEntityManagerFactory()).thenReturn(emf);
		when(em.getDelegate()).thenReturn(em);
		when(emf.createEntityManager()).thenReturn(em);
	}

	@Test
	void rejectsPersistenceProviderIfIncapableOfExtractingQueriesAndPagebleBeingUsed() {

		when(extractor.canExtractQuery()).thenReturn(false);
		JpaQueryMethod queryMethod = new JpaQueryMethod(method, metadata, projectionFactory, extractor);

		when(em.createNamedQuery(queryMethod.getNamedCountQueryName())).thenThrow(new IllegalArgumentException());
		assertThatExceptionOfType(QueryCreationException.class).isThrownBy(() -> NamedQuery.lookupFrom(queryMethod, em));
	}

	@Test // DATAJPA-142
	@SuppressWarnings("unchecked")
	void doesNotRejectPersistenceProviderIfNamedCountQueryIsAvailable() {

		when(extractor.canExtractQuery()).thenReturn(false);
		JpaQueryMethod queryMethod = new JpaQueryMethod(method, metadata, projectionFactory, extractor);

		TypedQuery<Long> countQuery = mock(TypedQuery.class);
		when(em.createNamedQuery(eq(queryMethod.getNamedCountQueryName()), eq(Long.class))).thenReturn(countQuery);
		NamedQuery query = (NamedQuery) NamedQuery.lookupFrom(queryMethod, em);

		query.doCreateCountQuery(new JpaParametersParameterAccessor(queryMethod.getParameters(), new Object[1]));
		verify(em, times(1)).createNamedQuery(queryMethod.getNamedCountQueryName(), Long.class);
		verify(em, never()).createQuery(any(String.class), eq(Long.class));
	}

	interface SampleRepository {

		Page<String> foo(Pageable pageable);
	}
}
