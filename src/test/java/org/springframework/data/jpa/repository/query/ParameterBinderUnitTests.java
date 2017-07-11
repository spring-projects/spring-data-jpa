/*
 * Copyright 2008-2017 the original author or authors.
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

import static java.util.Collections.*;
import static javax.persistence.TemporalType.*;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.persistence.Embeddable;
import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.TemporalType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Temporal;
import org.springframework.data.repository.query.Param;

/**
 * Unit test for {@link ParameterBinder}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Jens Schauder
 */
@RunWith(MockitoJUnitRunner.class)
public class ParameterBinderUnitTests {

	private Method valid;

	@Mock private Query query;
	private Method useIndexedParameters;
	private Method indexedParametersWithSort;

	@Before
	public void setUp() throws SecurityException, NoSuchMethodException {

		valid = SampleRepository.class.getMethod("valid", String.class);

		useIndexedParameters = SampleRepository.class.getMethod("useIndexedParameters", String.class);
		indexedParametersWithSort = SampleRepository.class.getMethod("indexedParameterWithSort", String.class, Sort.class);
	}

	static class User {

	}

	static interface SampleRepository {

		User useIndexedParameters(String lastname);

		User indexedParameterWithSort(String lastname, Sort sort);

		User valid(@Param("username") String username);

		User validWithPageable(@Param("username") String username, Pageable pageable);

		User validWithSort(@Param("username") String username, Sort sort);

		User validWithDefaultTemporalTypeParameter(@Temporal Date registerDate);

		User validWithCustomTemporalTypeParameter(@Temporal(TIMESTAMP) Date registerDate);

		User invalidWithTemporalTypeParameter(@Temporal String registerDate);

		List<User> validWithVarArgs(Integer... ids);

		User optionalParameter(Optional<String> name);
	}

	@Test
	public void bindWorksWithNullForSort() throws Exception {

		Method validWithSort = SampleRepository.class.getMethod("validWithSort", String.class, Sort.class);

		Object[] values = { "foo", null };
		ParameterBinderFactory.createParameterBinder(new JpaParameters(validWithSort)).bind(query, values);
		verify(query).setParameter(eq(1), eq("foo"));
	}

	@Test
	public void bindWorksWithNullForPageable() throws Exception {

		Method validWithPageable = SampleRepository.class.getMethod("validWithPageable", String.class, Pageable.class);

		Object[] values = { "foo", null };
		ParameterBinderFactory.createParameterBinder(new JpaParameters(validWithPageable)).bind(query, values);
		verify(query).setParameter(eq(1), eq("foo"));
	}

	@Test
	public void usesIndexedParametersIfNoParamAnnotationPresent() throws Exception {

		Object[] values = { "foo" };
		ParameterBinderFactory.createParameterBinder(new JpaParameters(useIndexedParameters)).bind(query, values);
		verify(query).setParameter(eq(1), anyObject());
	}

	@Test

	public void usesParameterNameIfAnnotated() throws Exception {

		when(query.setParameter(eq("username"), anyObject())).thenReturn(query);

		Parameter parameter = mock(Parameter.class);
		when(parameter.getName()).thenReturn("username");
		when(query.getParameters()).thenReturn(singleton(parameter));

		Object[] values = { "foo" };
		ParameterBinderFactory.createParameterBinder(new JpaParameters(valid)).bind(query, values);

		verify(query).setParameter(eq("username"), anyObject());
	}

	@Test
	public void bindsEmbeddableCorrectly() throws Exception {

		Method method = getClass().getMethod("findByEmbeddable", SampleEmbeddable.class);
		JpaParameters parameters = new JpaParameters(method);
		SampleEmbeddable embeddable = new SampleEmbeddable();

		Object[] values = { embeddable };
		ParameterBinderFactory.createParameterBinder(parameters).bind(query, values);

		verify(query).setParameter(1, embeddable);
	}

	@Test // DATAJPA-107
	public void shouldSetTemporalQueryParameterToDate() throws Exception {

		Method method = SampleRepository.class.getMethod("validWithDefaultTemporalTypeParameter", Date.class);
		JpaParameters parameters = new JpaParameters(method);
		Date date = new Date();

		Object[] values = { date };
		ParameterBinderFactory.createParameterBinder(parameters).bind(query, values);

		verify(query).setParameter(eq(1), eq(date), eq(TemporalType.DATE));
	}

	@Test // DATAJPA-107
	public void shouldSetTemporalQueryParameterToTimestamp() throws Exception {

		Method method = SampleRepository.class.getMethod("validWithCustomTemporalTypeParameter", Date.class);
		JpaParameters parameters = new JpaParameters(method);
		Date date = new Date();

		Object[] values = { date };
		ParameterBinderFactory.createParameterBinder(parameters).bind(query, values);

		verify(query).setParameter(eq(1), eq(date), eq(TemporalType.TIMESTAMP));
	}

	@Test(expected = IllegalArgumentException.class) // DATAJPA-107
	public void shouldThrowIllegalArgumentExceptionIfIsAnnotatedWithTemporalParamAndParameterTypeIsNotDate()
			throws Exception {
		Method method = SampleRepository.class.getMethod("invalidWithTemporalTypeParameter", String.class);

		JpaParameters parameters = new JpaParameters(method);
		ParameterBinderFactory.createParameterBinder(parameters);
	}

	@Test // DATAJPA-461
	public void shouldAllowBindingOfVarArgsAsIs() throws Exception {

		Method method = SampleRepository.class.getMethod("validWithVarArgs", Integer[].class);
		JpaParameters parameters = new JpaParameters(method);
		Integer[] ids = new Integer[] { 1, 2, 3 };
		Object[] values = { ids };
		ParameterBinderFactory.createParameterBinder(parameters).bind(query, values);

		verify(query).setParameter(eq(1), eq(ids));
	}

	@Test // DATAJPA-809
	public void unwrapsOptionalParameter() throws Exception {

		Method method = SampleRepository.class.getMethod("optionalParameter", Optional.class);
		JpaParameters parameters = new JpaParameters(method);

		Object[] values = { Optional.of("Foo") };
		ParameterBinderFactory.createParameterBinder(parameters).bind(query, values);

		verify(query).setParameter(eq(1), eq("Foo"));
	}

	public SampleEntity findByEmbeddable(SampleEmbeddable embeddable) {

		return null;
	}

	@SuppressWarnings("unused")
	static class SampleEntity {

		private SampleEmbeddable embeddable;
	}

	@Embeddable
	@SuppressWarnings("unused")
	public static class SampleEmbeddable {

		private String foo;
		private String bar;
	}
}
