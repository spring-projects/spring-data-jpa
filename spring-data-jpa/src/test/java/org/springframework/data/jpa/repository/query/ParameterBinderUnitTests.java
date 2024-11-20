/*
 * Copyright 2008-2024 the original author or authors.
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

import static jakarta.persistence.TemporalType.*;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Parameter;
import jakarta.persistence.Query;
import jakarta.persistence.TemporalType;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Temporal;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.ParametersSource;

/**
 * Unit test for {@link ParameterBinder}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Jens Schauder
 * @author Mark Paluch
 * @author Yanming Zhou
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ParameterBinderUnitTests {

	private static final int MAX_PARAMETERS = 1;
	private Method valid;

	@Mock(answer = Answers.RETURNS_DEEP_STUBS) private Query query;
	private Method useIndexedParameters;

	@BeforeEach
	void setUp() throws SecurityException, NoSuchMethodException {

		valid = SampleRepository.class.getMethod("valid", String.class);

		useIndexedParameters = SampleRepository.class.getMethod("useIndexedParameters", String.class);

		when(query.getParameters().size()).thenReturn(MAX_PARAMETERS);
	}

	static class User {

	}

	interface SampleRepository extends Repository<User, Long> {

		User useIndexedParameters(String lastname);

		User valid(@Param("username") String username);

		User validWithPageable(@Param("username") String username, Pageable pageable);

		User validWithLimit(@Param("username") String username, Limit limit);

		User validWithSort(@Param("username") String username, Sort sort);

		User validWithDefaultTemporalTypeParameter(@Temporal Date registerDate);

		User validWithCustomTemporalTypeParameter(@Temporal(TIMESTAMP) Date registerDate);

		User invalidWithTemporalTypeParameter(@Temporal String registerDate);

		List<User> validWithVarArgs(Integer... ids);

		User optionalParameter(Optional<String> name);

		@org.springframework.data.jpa.repository.Query("select x from User where name = :name")
		User withQuery(String name, String other);
	}

	@Test
	void bindWorksWithNullForSort() throws Exception {

		Method validWithSort = SampleRepository.class.getMethod("validWithSort", String.class, Sort.class);

		Object[] values = { "foo", null };
		bind(validWithSort, values);
		verify(query).setParameter(eq(1), eq("foo"));
	}

	@Test
	void bindWorksWithNullForPageable() throws Exception {

		Method validWithPageable = SampleRepository.class.getMethod("validWithPageable", String.class, Pageable.class);

		Object[] values = { "foo", null };
		bind(validWithPageable, values);
		verify(query).setParameter(eq(1), eq("foo"));
	}

	@Test // GH-3242
	void bindAndPrepareWorksWithPageable() throws Exception {

		Method validWithPageable = SampleRepository.class.getMethod("validWithPageable", String.class, Pageable.class);
		Object[] values = { "foo", Pageable.ofSize(10).withPage(3) };

		bindAndPrepare(validWithPageable, values);

		verify(query).setParameter(1, "foo");
		verify(query).setFirstResult(30);
		verify(query).setMaxResults(10);
	}

	@Test // GH-3242
	void bindWorksWithNullForLimit() throws Exception {

		Method validWithLimit = SampleRepository.class.getMethod("validWithLimit", String.class, Limit.class);
		Object[] values = { "foo", null };

		bind(validWithLimit, values);

		verify(query).setParameter(1, "foo");
		verify(query, never()).setFirstResult(anyInt());
	}

	@Test // GH-3242
	void bindAndPrepareWorksWithLimit() throws Exception {

		Method validWithLimit = SampleRepository.class.getMethod("validWithLimit", String.class, Limit.class);
		Object[] values = { "foo", Limit.of(10) };

		bindAndPrepare(validWithLimit, values);

		verify(query).setParameter(1, "foo");
		verify(query).setMaxResults(10);
		verify(query, never()).setFirstResult(anyInt());
	}

	@Test
	void usesIndexedParametersIfNoParamAnnotationPresent() {

		Object[] values = { "foo" };
		bind(useIndexedParameters, values);
		verify(query).setParameter(eq(1), any());
	}

	@Test
	void usesParameterNameIfAnnotated() {

		when(query.setParameter(eq("username"), any())).thenReturn(query);

		Parameter parameter = mock(Parameter.class);
		when(parameter.getName()).thenReturn("username");
		when(query.getParameters()).thenReturn(singleton(parameter));

		Object[] values = { "foo" };
		bind(valid, values);

		verify(query).setParameter(eq("username"), any());
	}

	@Test
	void bindsEmbeddableCorrectly() throws Exception {

		Method method = getClass().getMethod("findByEmbeddable", SampleEmbeddable.class);
		JpaParameters parameters = createParameters(method);
		SampleEmbeddable embeddable = new SampleEmbeddable();

		Object[] values = { embeddable };
		bind(method, parameters, values);

		verify(query).setParameter(1, embeddable);
	}

	@Test // DATAJPA-107
	void shouldSetTemporalQueryParameterToDate() throws Exception {

		Method method = SampleRepository.class.getMethod("validWithDefaultTemporalTypeParameter", Date.class);
		JpaParameters parameters = createParameters(method);
		Date date = new Date();

		Object[] values = { date };
		bind(method, parameters, values);

		verify(query).setParameter(eq(1), eq(date), eq(TemporalType.DATE));
	}

	@Test // DATAJPA-107
	void shouldSetTemporalQueryParameterToTimestamp() throws Exception {

		Method method = SampleRepository.class.getMethod("validWithCustomTemporalTypeParameter", Date.class);
		JpaParameters parameters = createParameters(method);
		Date date = new Date();

		Object[] values = { date };
		bind(method, parameters, values);

		verify(query).setParameter(eq(1), eq(date), eq(TemporalType.TIMESTAMP));
	}

	@Test // DATAJPA-107
	void shouldThrowIllegalArgumentExceptionIfIsAnnotatedWithTemporalParamAndParameterTypeIsNotDate() throws Exception {
		Method method = SampleRepository.class.getMethod("invalidWithTemporalTypeParameter", String.class);

		assertThatIllegalArgumentException().isThrownBy(() -> createParameters(method));
	}

	@Test // DATAJPA-461
	void shouldAllowBindingOfVarArgsAsIs() throws Exception {

		Method method = SampleRepository.class.getMethod("validWithVarArgs", Integer[].class);
		JpaParameters parameters = createParameters(method);
		Integer[] ids = new Integer[] { 1, 2, 3 };
		Object[] values = { ids };
		bind(method, parameters, values);

		verify(query).setParameter(eq(1), eq(ids));
	}

	@Test // DATAJPA-809
	void unwrapsOptionalParameter() throws Exception {

		Method method = SampleRepository.class.getMethod("optionalParameter", Optional.class);
		JpaParameters parameters = createParameters(method);

		Object[] values = { Optional.of("Foo") };
		bind(method, parameters, values);

		verify(query).setParameter(eq(1), eq("Foo"));
	}

	@Test // DATAJPA-1172
	void doesNotBindExcessParameters() throws Exception {

		Method method = SampleRepository.class.getMethod("withQuery", String.class, String.class);

		Object[] values = { "foo", "superfluous" };
		bind(method, createParameters(method), values);

		verify(query).setParameter(eq(1), any());
		verify(query, never()).setParameter(eq(2), any());
	}

	private void bind(Method method, Object[] values) {
		bind(method, createParameters(method), values);
	}

	private void bind(Method method, JpaParameters parameters, Object[] values) {
		ParameterBinderFactory.createBinder(parameters, false).bind(QueryParameterSetter.BindableQuery.from(query),
				getAccessor(method, values), QueryParameterSetter.ErrorHandling.STRICT);
	}

	private void bindAndPrepare(Method method, Object[] values) {
		ParameterBinderFactory.createBinder(createParameters(method), false).bindAndPrepare(query,
				getAccessor(method, values));
	}

	private JpaParametersParameterAccessor getAccessor(Method method, Object... values) {
		return new JpaParametersParameterAccessor(createParameters(method), values);
	}

	private static JpaParameters createParameters(Method method) {
		return new JpaParameters(ParametersSource.of(method));
	}

	// needs to be public
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
