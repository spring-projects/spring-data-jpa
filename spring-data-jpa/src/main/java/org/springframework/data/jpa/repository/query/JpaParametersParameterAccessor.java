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

import org.springframework.data.jpa.repository.query.JpaParameters.JpaParameter;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.lang.Nullable;

/**
 * {@link org.springframework.data.repository.query.ParameterAccessor} based on an {@link Parameters} instance. It also
 * offers access to all the values, not just the bindable ones based on a {@link JpaParameter} instance.
 *
 * @author Jens Schauder
 * @author Mark Paluch
 * @author Greg Turnquist
 */
public class JpaParametersParameterAccessor extends ParametersParameterAccessor {

	private final JpaParameters parameters;

	/**
	 * Creates a new {@link ParametersParameterAccessor}.
	 *
	 * @param parameters must not be {@literal null}.
	 * @param values must not be {@literal null}.
	 */
	public JpaParametersParameterAccessor(JpaParameters parameters, Object[] values) {
		super(parameters, values);
		this.parameters = parameters;
	}

	public JpaParameters getParameters() {
		return parameters;
	}

	@Nullable
	public <T> T getValue(Parameter parameter) {
		return super.getValue(parameter.getIndex());
	}

	@Override
	public Object[] getValues() {
		return super.getValues();
	}

	/**
	 * Apply potential unwrapping to {@code parameterValue}.
	 *
	 * @param parameterValue
	 * @since 3.0.4
	 */
	protected Object potentiallyUnwrap(Object parameterValue) {
		return parameterValue;
	}

}
