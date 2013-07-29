/*
 * Copyright 2013 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Date;

import javax.persistence.TemporalType;

import org.springframework.core.MethodParameter;
import org.springframework.data.jpa.repository.TemporalParam;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;

/**
 * Custom extension of {@link Parameters} discovering additional query parameter annotations.
 * 
 * @author Thomas Darimont
 */
public class JpaParameters extends Parameters {

	/**
	 * Creates a new {@link JpaParameters} instance from the given {@link Method}.
	 * 
	 * @param method must not be {@literal null}.
	 */
	public JpaParameters(Method method) {
		super(method);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.query.Parameters#createParameter(org.springframework.core.MethodParameter)
	 */
	@Override
	protected JpaParameter createParameter(MethodParameter parameter) {
		return new JpaParameter(parameter);
	}

	/**
	 * Custom {@link Parameter} implementation adding parameters of type {@link TemporalParam} to the special ones.
	 * 
	 * @author Oliver Gierke
	 */
	static class JpaParameter extends Parameter {

		/**
		 * Creates a new {@link JpaParameter}.
		 * 
		 * @param parameter must not be {@literal null}.
		 */
		JpaParameter(MethodParameter parameter) {
			super(parameter);

			if (!isDateParameter() && hasTemporalParamAnnotation()) {
				throw new IllegalArgumentException(TemporalParam.class.getSimpleName()
						+ " annotation is only allowed on Date parameter!");
			}
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.repository.query.Parameter#isBindable()
		 */
		@Override
		public boolean isBindable() {
			return super.isBindable() || isTemporalParameter();
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.repository.query.Parameter#isSpecialParameter()
		 */
		@Override
		public boolean isSpecialParameter() {
			return super.isSpecialParameter() || isTemporalParameter();
		}

		/**
		 * @return {@literal true} if this parameter is of type {@link Date} and has an {@link TemporalParam} annotation.
		 */
		public boolean isTemporalParameter() {
			return isDateParameter() && hasTemporalParamAnnotation();
		}

		/**
		 * @return the {@link TemporalType} on the {@link TemporalParam} annotation of the given {@link Parameter}.
		 */
		public TemporalType getTemporalType() {
			return getParameterAnnotation(TemporalParam.class).value();
		}

		private boolean hasTemporalParamAnnotation() {
			return getParameterAnnotation(TemporalParam.class) != null;
		}

		private boolean isDateParameter() {
			return getType().equals(Date.class);
		}
	}
}
