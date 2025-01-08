/*
 * Copyright 2024-2025 the original author or authors.
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

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mockito.Mockito;
import org.springframework.core.MethodParameter;
import org.springframework.data.jpa.repository.query.JpaParameters.JpaParameter;
import org.springframework.data.util.TypeInformation;

/**
 * @author Christoph Strobl
 */
public class StubJpaParameterParameterAccessor extends JpaParametersParameterAccessor {

	private StubJpaParameterParameterAccessor(JpaParameters parameters, Object[] values) {
		super(parameters, values);
	}

	static JpaParametersParameterAccessor accessor(Object... values) {

		Class<?>[] parameterTypes = Arrays.stream(values).map(it -> it != null ? it.getClass() : Object.class)
				.toArray(Class<?>[]::new);
		return accessor(parameterTypes, values);
	}

	static JpaParametersParameterAccessor accessor(Class<?>... parameterTypes) {
		return accessor(parameterTypes, new Object[parameterTypes.length]);
	}

	static AccessorBuilder accessorFor(Class<?>... parameterTypes) {
		return arguments -> accessor(parameterTypes, arguments);

	}

	interface AccessorBuilder {
		JpaParametersParameterAccessor withValues(Object... arguments);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	static JpaParametersParameterAccessor accessor(Class<?>[] parameterTypes, Object... parameters) {

		List<JpaParameter> parametersList = new ArrayList<>(parameterTypes.length);
		List<Object> valueList = new ArrayList<>(parameterTypes.length);

		for (int i = 0; i < parameterTypes.length; i++) {

			if (i < parameters.length) {
				valueList.add(parameters[i]);
			}

			Class<?> parameterType = parameterTypes[i];
			MethodParameter mock = Mockito.mock(MethodParameter.class);
			when(mock.getParameterType()).thenReturn((Class) parameterType);
			JpaParameter parameter = new JpaParameter(mock, TypeInformation.of(parameterType));
			parametersList.add(parameter);
		}

		return new StubJpaParameterParameterAccessor(new JpaParameters(parametersList), valueList.toArray());
	}

	@Override
	public String toString() {
		List<String> parameters = new ArrayList<>(getParameters().getNumberOfParameters());

		for (int i = 0; i < getParameters().getNumberOfParameters(); i++) {
			Object value = getValue(i);
			if (value == null) {
				value = "null";
			}
			parameters.add("%s: %s (%s)".formatted(i, value, getParameters().getParameter(i).getType().getSimpleName()));
		}
		return "%s".formatted(parameters);
	}
}
