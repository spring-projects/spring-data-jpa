/*
 * Copyright 2022-2023 the original author or authors.
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
package org.springframework.data.jpa.provider;

import javax.persistence.EntityManager;

import org.hibernate.SessionFactory;
import org.hibernate.TypeHelper;
import org.hibernate.jpa.TypedParameterValue;
import org.hibernate.type.Type;
import org.springframework.data.jpa.repository.query.JpaParametersParameterAccessor;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersParameterAccessor;

/**
 * {@link org.springframework.data.repository.query.ParameterAccessor} based on an {@link Parameters} instance. In
 * addition to the {@link JpaParametersParameterAccessor} functions, the bindable value is provided by fetching the
 * method type when there is null.
 *
 * @author Wonchul Heo
 * @author Jens Schauder
 * @author Cedomir Igaly
 * @since 2.7
 */
class HibernateJpaParametersParameterAccessor extends JpaParametersParameterAccessor {

	private final TypeHelper typeHelper;

	/**
	 * Creates a new {@link ParametersParameterAccessor}.
	 *
	 * @param parameters must not be {@literal null}.
	 * @param values must not be {@literal null}.
	 * @param em must not be {@literal null}.
	 */
	HibernateJpaParametersParameterAccessor(Parameters<?, ?> parameters, Object[] values, EntityManager em) {

		super(parameters, values);

		this.typeHelper = em.getEntityManagerFactory().unwrap(SessionFactory.class).getTypeHelper();
	}

	@Override
	public Object getValue(Parameter parameter) {

		Object value = super.getValue(parameter.getIndex());
		if (value != null) {
			return value;
		}

		Type type = typeHelper.basic(parameter.getType());
		if (type == null) {
			return null;
		}
		return new TypedParameterValue(type, null);
	}

	/**
	 * Utility method to potentially unwrap {@link TypedParameterValue}s. For certain operations, Hibernate doesn't
	 * properly support them, so we must unwrap them before passing through.
	 *
	 * @param extractedValue
	 * @return the value behind a {@link TypedParameterValue}
	 */
	@Override
	public Object potentiallyUnwrap(Object extractedValue) {

		if (extractedValue instanceof TypedParameterValue) {
			return ((TypedParameterValue) extractedValue).getValue();
		} else {
			return extractedValue;
		}
	}
}
