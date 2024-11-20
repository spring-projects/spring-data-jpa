/*
 * Copyright 2017-2024 the original author or authors.
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
import static java.util.Arrays.*;
import static org.mockito.Mockito.*;
import static org.springframework.data.jpa.repository.query.QueryParameterSetter.*;
import static org.springframework.data.jpa.repository.query.QueryParameterSetter.ErrorHandling.*;

import jakarta.persistence.Parameter;
import jakarta.persistence.Query;
import jakarta.persistence.TemporalType;
import jakarta.persistence.criteria.ParameterExpression;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.data.jpa.repository.query.QueryParameterSetter.*;

/**
 * Unit tests fir {@link NamedOrIndexedQueryParameterSetter}.
 *
 * @author Jens Schauder
 * @author Oliver Gierke
 * @author Mark Paluch
 */
class NamedOrIndexedQueryParameterSetterUnitTests {

	private static final String EXCEPTION_MESSAGE = "mock exception";
	private Function<JpaParametersParameterAccessor, Object> firstValueExtractor = args -> args.getValues()[0];
	private JpaParametersParameterAccessor methodArguments;

	private List<TemporalType> temporalTypes = asList(null, TIME);
	private List<Parameter<?>> parameters = Arrays.<Parameter<?>> asList( //
			mock(ParameterExpression.class), //
			new ParameterImpl("name", null), //
			new ParameterImpl(null, 1) //
	);

	private SoftAssertions softly = new SoftAssertions();

	@BeforeEach
	void before() {

		JpaParametersParameterAccessor accessor = mock(JpaParametersParameterAccessor.class);

		Date testDate = new Date();

		when(accessor.getValues()).thenReturn(new Object[] { testDate });
		when(accessor.potentiallyUnwrap(testDate)).thenReturn(testDate);

		this.methodArguments = accessor;
	}

	@Test // DATAJPA-1233
	void strictErrorHandlingThrowsExceptionForAllVariationsOfParameters() {

		Query query = mockExceptionThrowingQueryWithNamedParameters();

		for (Parameter parameter : parameters) {
			for (TemporalType temporalType : temporalTypes) {

				QueryParameterSetter setter = QueryParameterSetter.create( //
						firstValueExtractor, //
						parameter, //
						temporalType //
				);

				softly
						.assertThatThrownBy(
								() -> setter.setParameter(BindableQuery.from(query), methodArguments, STRICT)) //
						.describedAs("p-type: %s, p-name: %s, p-position: %s, temporal: %s", //
								parameter.getClass(), //
								parameter.getName(), //
								parameter.getPosition(), //
								temporalType) //
						.hasMessage(EXCEPTION_MESSAGE);
			}
		}

		softly.assertAll();
	}

	@Test // DATAJPA-1233
	void lenientErrorHandlingThrowsNoExceptionForAllVariationsOfParameters() {

		Query query = mockExceptionThrowingQueryWithNamedParameters();

		for (Parameter<?> parameter : parameters) {
			for (TemporalType temporalType : temporalTypes) {

				QueryParameterSetter setter = QueryParameterSetter.create( //
						firstValueExtractor, //
						parameter, //
						temporalType //
				);

				softly
						.assertThatCode(
								() -> setter.setParameter(BindableQuery.from(query), methodArguments, LENIENT)) //
						.describedAs("p-type: %s, p-name: %s, p-position: %s, temporal: %s", //
								parameter.getClass(), //
								parameter.getName(), //
								parameter.getPosition(), //
								temporalType) //
						.doesNotThrowAnyException();
			}
		}

		softly.assertAll();
	}

	/**
	 * setParameter should be called in the lenient case even if the number of parameters seems to suggest that it fails,
	 * since the index might not be continuous due to missing parts of count queries compared to the main query. This
	 * happens when a parameter gets used in the ORDER BY clause which gets stripped of for the count query.
	 */
	@Test // DATAJPA-1233
	void lenientSetsParameterWhenSuccessIsUnsure() {

		Query query = mock(Query.class);

		for (TemporalType temporalType : temporalTypes) {

			QueryParameterSetter setter = QueryParameterSetter.create( //
					firstValueExtractor, //
					new ParameterImpl(null, 11), // parameter position is beyond number of parametes in query (0)
					temporalType //
			);

			setter.setParameter(BindableQuery.from(query), methodArguments, LENIENT);

			if (temporalType == null) {
				verify(query).setParameter(eq(11), any(Date.class));
			} else {
				verify(query).setParameter(eq(11), any(Date.class), eq(temporalType));
			}
		}

		softly.assertAll();

	}

	/**
	 * This scenario happens when the only (name) parameter is part of an ORDER BY clause and gets stripped of for the
	 * count query. Then the count query has no named parameter but the parameter provided has a {@literal null} position.
	 */
	@Test // DATAJPA-1233
	void parameterNotSetWhenSuccessImpossible() {

		Query query = mock(Query.class);

		for (TemporalType temporalType : temporalTypes) {

			QueryParameterSetter setter = QueryParameterSetter.create( //
					firstValueExtractor, //
					new ParameterImpl(null, null), // no position (and no name) makes a success of a setParameter impossible
					temporalType //
			);

			setter.setParameter(BindableQuery.from(query), methodArguments, LENIENT);

			if (temporalType == null) {
				verify(query, never()).setParameter(anyInt(), any(Date.class));
			} else {
				verify(query, never()).setParameter(anyInt(), any(Date.class), eq(temporalType));
			}
		}

		softly.assertAll();

	}

	@SuppressWarnings("unchecked")
	private static Query mockExceptionThrowingQueryWithNamedParameters() {

		Query query = mock(Query.class);

		// make it a query with named parameters
		doReturn(Collections.singleton(new ParameterImpl("aName", 3))) //
				.when(query).getParameters();
		doThrow(new RuntimeException(EXCEPTION_MESSAGE)) //
				.when(query).setParameter(any(Parameter.class), any(Date.class), any(TemporalType.class));
		doThrow(new RuntimeException(EXCEPTION_MESSAGE)) //
				.when(query).setParameter(any(Parameter.class), any(Date.class));
		doThrow(new RuntimeException(EXCEPTION_MESSAGE)) //
				.when(query).setParameter(anyString(), any(Date.class), any(TemporalType.class));
		doThrow(new RuntimeException(EXCEPTION_MESSAGE)) //
				.when(query).setParameter(anyString(), any(Date.class));
		doThrow(new RuntimeException(EXCEPTION_MESSAGE)) //
				.when(query).setParameter(anyInt(), any(Date.class), any(TemporalType.class));
		doThrow(new RuntimeException(EXCEPTION_MESSAGE)) //
				.when(query).setParameter(anyInt(), any(Date.class));

		return query;
	}

	private static final class ParameterImpl implements Parameter<Object> {

		private final String name;
		private final Integer position;

		public ParameterImpl(String name, Integer position) {

			this.name = name;
			this.position = position;
		}

		@Override
		public Class<Object> getParameterType() {
			return Object.class;
		}

		public String getName() {
			return this.name;
		}

		public Integer getPosition() {
			return this.position;
		}

		public String toString() {
			return "NamedOrIndexedQueryParameterSetterUnitTests.ParameterImpl(name=" + this.getName() + ", position="
					+ this.getPosition() + ")";
		}
	}
}
