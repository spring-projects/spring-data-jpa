/*
 * Copyright 2013-2024 the original author or authors.
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

import jakarta.persistence.TemporalType;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import org.springframework.core.MethodParameter;
import org.springframework.data.jpa.repository.Temporal;
import org.springframework.data.jpa.repository.query.JpaParameters.JpaParameter;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersSource;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;

/**
 * Custom extension of {@link Parameters} discovering additional query parameter annotations.
 *
 * @author Thomas Darimont
 * @author Mark Paluch
 * @author Réda Housni Alaoui
 */
public class JpaParameters extends Parameters<JpaParameters, JpaParameter> {

	/**
	 * Creates a new {@link JpaParameters} instance from the given {@link ParametersSource}.
	 *
	 * @param parametersSource must not be {@literal null}.
	 * @since 3.2.1
	 */
	public JpaParameters(ParametersSource parametersSource) {
		super(parametersSource,
				methodParameter -> new JpaParameter(methodParameter, parametersSource.getDomainTypeInformation()));
	}

	/**
	 * Creates a new {@link JpaParameters} instance from the given {@link Method}.
	 *
	 * @param parametersSource must not be {@literal null}.
	 * @param parameterFactory must not be {@literal null}.
	 * @since 3.2.1
	 */
	protected JpaParameters(ParametersSource parametersSource,
			Function<MethodParameter, JpaParameter> parameterFactory) {
		super(parametersSource, parameterFactory);
	}

	JpaParameters(List<JpaParameter> parameters) {
		super(parameters);
	}

	@Override
	protected JpaParameters createFrom(List<JpaParameter> parameters) {
		return new JpaParameters(parameters);
	}

	/**
	 * @return {@code true} if the method signature declares Limit or Pageable parameters.
	 */
	public boolean hasLimitingParameters() {
		return hasLimitParameter() || hasPageableParameter();
	}

	/**
	 * Custom {@link Parameter} implementation adding parameters of type {@link Temporal} to the special ones.
	 *
	 * @author Thomas Darimont
	 * @author Oliver Gierke
	 */
	public static class JpaParameter extends Parameter {

		private final @Nullable Temporal annotation;
		private @Nullable TemporalType temporalType;

		/**
		 * Creates a new {@link JpaParameter}.
		 *
		 * @param parameter must not be {@literal null}.
		 */
		protected JpaParameter(MethodParameter parameter, TypeInformation<?> domainType) {

			super(parameter, domainType);
			this.annotation = parameter.getParameterAnnotation(Temporal.class);
			this.temporalType = null;
			if (!isDateParameter() && hasTemporalParamAnnotation()) {
				throw new IllegalArgumentException(
						Temporal.class.getSimpleName() + " annotation is only allowed on Date parameter");
			}
		}

		@Override
		public boolean isBindable() {
			return super.isBindable() || isTemporalParameter();
		}

		/**
		 * @return {@literal true} if this parameter is of type {@link Date} and has an {@link Temporal} annotation.
		 */
		boolean isTemporalParameter() {
			return isDateParameter() && hasTemporalParamAnnotation();
		}

		/**
		 * @return the {@link TemporalType} on the {@link Temporal} annotation of the given {@link Parameter}.
		 */
		@Nullable
		TemporalType getTemporalType() {

			if (temporalType == null) {
				this.temporalType = annotation == null ? null : annotation.value();
			}

			return this.temporalType;
		}

		/**
		 * @return the required {@link TemporalType} on the {@link Temporal} annotation of the given {@link Parameter}.
		 * @throws IllegalStateException if the parameter does not define a {@link TemporalType}.
		 * @since 2.0
		 */
		TemporalType getRequiredTemporalType() throws IllegalStateException {

			TemporalType temporalType = getTemporalType();

			if (temporalType != null) {
				return temporalType;
			}

			throw new IllegalStateException(String.format("Required temporal type not found for %s", getType()));
		}

		private boolean hasTemporalParamAnnotation() {
			return annotation != null;
		}

		private boolean isDateParameter() {
			return getType().equals(Date.class);
		}
	}
}
