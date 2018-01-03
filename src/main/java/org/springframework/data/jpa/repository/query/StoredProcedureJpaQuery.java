/*
 * Copyright 2014-2018 the original author or authors.
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

import javax.persistence.EntityManager;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TypedQuery;

import org.springframework.data.jpa.repository.query.JpaParameters.JpaParameter;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link AbstractJpaQuery} implementation that inspects a {@link JpaQueryMethod} for the existence of an
 * {@link Procedure} annotation and creates a JPA 2.1 {@link StoredProcedureQuery} from it.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Jens Schauder
 * @author Mark Paluch
 * @since 1.6
 */
class StoredProcedureJpaQuery extends AbstractJpaQuery {

	private final StoredProcedureAttributes procedureAttributes;
	private final boolean useNamedParameters;

	/**
	 * Creates a new {@link StoredProcedureJpaQuery}.
	 *
	 * @param method must not be {@literal null}
	 * @param em must not be {@literal null}
	 */
	StoredProcedureJpaQuery(JpaQueryMethod method, EntityManager em) {

		super(method, em);
		this.procedureAttributes = method.getProcedureAttributes();
		this.useNamedParameters = useNamedParameters(method);

	}

	/**
	 * Determine whether to used named parameters for the given query method.
	 *
	 * @param method must not be {@literal null}.
	 */
	private static boolean useNamedParameters(QueryMethod method) {

		for (Parameter parameter : method.getParameters()) {
			if (parameter.isNamedParameter()) {
				return true;
			}
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.AbstractJpaQuery#createQuery(java.lang.Object[])
	 */
	@Override
	protected StoredProcedureQuery createQuery(Object[] values) {
		return applyHints(doCreateQuery(values), getQueryMethod());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.AbstractJpaQuery#doCreateQuery(java.lang.Object[])
	 */
	@Override
	protected StoredProcedureQuery doCreateQuery(Object[] values) {
		return parameterBinder.get().bind(createStoredProcedure(), values);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.AbstractJpaQuery#doCreateCountQuery(java.lang.Object[])
	 */
	@Override
	protected TypedQuery<Long> doCreateCountQuery(Object[] values) {
		throw new UnsupportedOperationException("StoredProcedureQuery does not support count queries!");
	}

	/**
	 * Extracts the output value from the given {@link StoredProcedureQuery}.
	 *
	 * @param storedProcedureQuery must not be {@literal null}.
	 */
	@Nullable
	Object extractOutputValue(StoredProcedureQuery storedProcedureQuery) {

		Assert.notNull(storedProcedureQuery, "StoredProcedureQuery must not be null!");

		if (!procedureAttributes.hasReturnValue()) {
			return null;
		}

		String outputParameterName = procedureAttributes.getOutputParameterName();
		JpaParameters parameters = getQueryMethod().getParameters();

		return useNamedParameters && StringUtils.hasText(outputParameterName) ? //
				storedProcedureQuery.getOutputParameterValue(outputParameterName)
				: storedProcedureQuery.getOutputParameterValue(parameters.getNumberOfParameters() + 1);
	}

	/**
	 * Creates a new JPA 2.1 {@link StoredProcedureQuery} from this {@link StoredProcedureJpaQuery}.
	 */
	private StoredProcedureQuery createStoredProcedure() {
		return procedureAttributes.isNamedStoredProcedure() ? newNamedStoredProcedureQuery()
				: newAdhocStoredProcedureQuery();
	}

	/**
	 * Creates a new named {@link StoredProcedureQuery} defined via an {@link NamedStoredProcedureQuery} on an entity.
	 */
	private StoredProcedureQuery newNamedStoredProcedureQuery() {
		return getEntityManager().createNamedStoredProcedureQuery(procedureAttributes.getProcedureName());
	}

	/**
	 * Creates a new ad-hoc {@link StoredProcedureQuery} from the given {@link StoredProcedureAttributes}.
	 */
	private StoredProcedureQuery newAdhocStoredProcedureQuery() {

		JpaParameters params = getQueryMethod().getParameters();
		String procedureName = procedureAttributes.getProcedureName();

		StoredProcedureQuery procedureQuery = getEntityManager().createStoredProcedureQuery(procedureName);

		for (JpaParameter param : params) {

			if (!param.isBindable()) {
				continue;
			}

			if (useNamedParameters) {
				procedureQuery.registerStoredProcedureParameter(
						param.getName().orElseThrow(() -> new IllegalArgumentException(ParameterBinder.PARAMETER_NEEDS_TO_BE_NAMED)),
						param.getType(), ParameterMode.IN);
			} else {
				procedureQuery.registerStoredProcedureParameter(param.getIndex() + 1, param.getType(), ParameterMode.IN);
			}
		}

		if (procedureAttributes.hasReturnValue()) {

			Class<?> outputParameterType = procedureAttributes.getOutputParameterType();
			ParameterMode mode = ParameterMode.OUT;

			if (useNamedParameters) {

				String outputParameterName = procedureAttributes.getOutputParameterName();
				procedureQuery.registerStoredProcedureParameter(outputParameterName, outputParameterType, mode);

			} else {
				procedureQuery.registerStoredProcedureParameter(params.getNumberOfParameters() + 1, outputParameterType, mode);
			}
		}

		return procedureQuery;
	}
}
