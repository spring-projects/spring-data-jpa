/*
 * Copyright 2017-2023 the original author or authors.
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
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.data.jpa.repository.query.JpaParameters.JpaParameter;
import org.springframework.data.jpa.repository.query.StringQuery.ParameterBinding;

/**
 * Unit tests for {@link QueryParameterSetterFactory}.
 *
 * @author Jens Schauder
 * @author Mark Paluch
 * @author Diego Krupitza
 */
class QueryParameterSetterFactoryUnitTests {

	private JpaParameters parameters = mock(JpaParameters.class, Mockito.RETURNS_DEEP_STUBS);
	private ParameterBinding binding = mock(ParameterBinding.class);

	private QueryParameterSetterFactory setterFactory;

	@BeforeEach
	void before() {

		// we have one bindable parameter
		when(parameters.getBindableParameters().iterator()).thenReturn(Stream.of(mock(JpaParameter.class)).iterator());

		setterFactory = QueryParameterSetterFactory.basic(parameters);
	}

	@Test // DATAJPA-1058
	void noExceptionWhenQueryDoesNotContainNamedParameters() {
		setterFactory.create(binding, DeclaredQuery.of("QueryStringWithOutNamedParameter", false));
	}

	@Test // DATAJPA-1058
	void exceptionWhenQueryContainNamedParametersAndMethodParametersAreNotNamed() {

		assertThatExceptionOfType(IllegalStateException.class) //
				.isThrownBy(() -> setterFactory.create(binding, DeclaredQuery.of("QueryStringWith :NamedParameter", false))) //
				.withMessageContaining("Java 8") //
				.withMessageContaining("@Param") //
				.withMessageContaining("-parameters");
	}

	@Test // DATAJPA-1281
	void exceptionWhenCriteriaQueryContainsInsufficientAmountOfParameters() {

		// no parameter present in the criteria query
		List<ParameterMetadataProvider.ParameterMetadata<?>> metadata = Collections.emptyList();
		QueryParameterSetterFactory setterFactory = QueryParameterSetterFactory.forCriteriaQuery(parameters, metadata);

		// one argument present in the method signature
		when(binding.getRequiredPosition()).thenReturn(1);

		assertThatExceptionOfType(IllegalArgumentException.class) //
				.isThrownBy(() -> setterFactory.create(binding, DeclaredQuery.of("QueryStringWith :NamedParameter", false))) //
				.withMessage("At least 1 parameter(s) provided but only 0 parameter(s) present in query");
	}

	@Test // DATAJPA-1281
	void exceptionWhenBasicQueryContainsInsufficientAmountOfParameters() {

		// no parameter present in the criteria query
		QueryParameterSetterFactory setterFactory = QueryParameterSetterFactory.basic(parameters);

		// one argument present in the method signature
		when(binding.getRequiredPosition()).thenReturn(1);

		assertThatExceptionOfType(IllegalArgumentException.class) //
				.isThrownBy(() -> setterFactory.create(binding, DeclaredQuery.of("QueryStringWith ?1", false))) //
				.withMessage("At least 1 parameter(s) provided but only 0 parameter(s) present in query");
	}
}
