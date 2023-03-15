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

import jakarta.persistence.EntityManager;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.TypedParameterValue;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.springframework.data.jpa.repository.query.JpaParametersParameterAccessor;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.lang.Nullable;

/**
 * {@link org.springframework.data.repository.query.ParameterAccessor} based on an {@link Parameters} instance. In
 * addition to the {@link JpaParametersParameterAccessor} functions, the bindable parameterValue is provided by fetching
 * the method type when there is null.
 *
 * @author Wonchul Heo
 * @author Jens Schauder
 * @author Cedomir Igaly
 * @author Robert Wilson
 * @author Oliver Drotbohm
 * @author Greg Turnquist
 * @since 2.7
 */
class HibernateJpaParametersParameterAccessor extends JpaParametersParameterAccessor {

	private final BasicTypeRegistry typeHelper;

	/**
	 * Creates a new {@link ParametersParameterAccessor}.
	 *
	 * @param parameters must not be {@literal null}.
	 * @param values must not be {@literal null}.
	 * @param em must not be {@literal null}.
	 */
	HibernateJpaParametersParameterAccessor(Parameters<?, ?> parameters, Object[] values, EntityManager em) {

		super(parameters, values);

		this.typeHelper = em.getEntityManagerFactory() //
				.unwrap(SessionFactoryImplementor.class) //
				.getTypeConfiguration() //
				.getBasicTypeRegistry();
	}

	@Override
	@Nullable
	@SuppressWarnings("unchecked")
	public Object getValue(Parameter parameter) {

		Object value = super.getValue(parameter.getIndex());

		if (value != null) {
			return value;
		}

		BasicType<?> type = typeHelper.getRegisteredType(parameter.getType());

		if (type == null) {
			return null;
		}

		return new TypedParameterValue<>(type, null);
	}

	/**
	 * For Hibernate, check if the incoming parameterValue can be wrapped inside a {@link TypedParameterValue} before
	 * extracting.
	 *
	 * @param parameterValue a parameterValue that is either a plain value or a {@link TypedParameterValue} containing a
	 *          {@literal Date}.
	 * @since 3.0.4
	 */
	@Override
	protected Object potentiallyUnwrap(Object parameterValue) {

		return (parameterValue instanceof TypedParameterValue<?> typedParameterValue) //
				? typedParameterValue.getValue() //
				: parameterValue;
	}
}
