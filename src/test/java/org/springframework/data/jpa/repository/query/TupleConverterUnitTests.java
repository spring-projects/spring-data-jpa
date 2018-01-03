/*
 * Copyright 2016-2018 the original author or authors.
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
import static org.mockito.Mockito.*;

import java.util.Arrays;

import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.jpa.repository.query.AbstractJpaQuery.TupleConverter;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.ReturnedType;

/**
 * Unit tests for {@link TupleConverter}.
 *
 * @author Oliver Gierke
 * @soundtrack James Bay - Let it go (Chaos and the Calm)
 */
@RunWith(MockitoJUnitRunner.class)
public class TupleConverterUnitTests {

	@Mock Tuple tuple;
	@Mock TupleElement<String> element;
	@Mock ProjectionFactory factory;

	ReturnedType type;

	@Before
	public void setUp() throws Exception {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(SampleRepository.class);
		QueryMethod method = new QueryMethod(SampleRepository.class.getMethod("someMethod"), metadata, factory);

		this.type = method.getResultProcessor().getReturnedType();
	}

	@Test // DATAJPA-984
	@SuppressWarnings("unchecked")
	public void returnsSingleTupleElementIfItMatchesExpectedType() throws Exception {

		doReturn(Arrays.asList(element)).when(tuple).getElements();
		doReturn("Foo").when(tuple).get(element);

		TupleConverter converter = new TupleConverter(type);

		assertThat(converter.convert(tuple), is((Object) "Foo"));
	}

	@Test // DATAJPA-1024
	@SuppressWarnings("unchecked")
	public void returnsNullForSingleElementTupleWithNullValue() throws Exception {

		doReturn(Arrays.asList(element)).when(tuple).getElements();
		doReturn(null).when(tuple).get(element);

		TupleConverter converter = new TupleConverter(type);

		assertThat(converter.convert(tuple), is(nullValue()));
	}

	static interface SampleRepository extends CrudRepository<Object, Long> {
		String someMethod();
	}
}
