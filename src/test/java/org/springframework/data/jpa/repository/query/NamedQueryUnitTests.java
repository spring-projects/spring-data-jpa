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

import static org.mockito.Mockito.*;

import java.lang.reflect.Method;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryCreationException;

/**
 * Unit tests for {@link NamedQuery}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class NamedQueryUnitTests {

	@Mock
	RepositoryMetadata metadata;
	@Mock
	QueryExtractor extractor;
	@Mock
	EntityManager em;

	Method method;

	@Before
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void setUp() throws SecurityException, NoSuchMethodException {

		method = SampleRepository.class.getMethod("foo", Pageable.class);
		when(metadata.getDomainClass()).thenReturn((Class) String.class);
		when(metadata.getReturnedDomainClass(method)).thenReturn((Class) String.class);
	}

	@Test(expected = QueryCreationException.class)
	public void rejectsPersistenceProviderIfIncapableOfExtractingQueriesAndPagebleBeingUsed() {

		when(extractor.canExtractQuery()).thenReturn(false);

		JpaQueryMethod queryMethod = new JpaQueryMethod(method, metadata, extractor);
		NamedQuery.lookupFrom(queryMethod, em);
	}

	interface SampleRepository {

		Page<String> foo(Pageable pageable);
	}
}
