/*
 * Copyright 2014-2019 the original author or authors.
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
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TypedQuery;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * {@link AbstractJpaQuery} implementation that inspects a {@link JpaQueryMethod} for the existence of an
 * {@link Procedure} annotation and creates a JPA 2.1 {@link StoredProcedureQuery} from it.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Jens Schauder
 * @author Mark Paluch
 * @author Jeff Sheets
 * @since 1.6
 */
class StoredProcedureJpaQuery extends AbstractJpaQuery {

	private final StoredProcedureAttributes procedureAttributes;
	private final boolean useNamedParameters;

	/**
	 * Creates a new {@link StoredProcedureJpaQuery}.
	 *
	 * @param method must not be {@literal null}
	 * @param em     must not be {@literal null}
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
	 *                             <p>
	 *                             Result is either a single value, or a Map<String, Object> of output parameter names to values
	 */
	@Nullable
	Object extractOutputValue(StoredProcedureQuery storedProcedureQuery) {

		Assert.notNull(storedProcedureQuery, "StoredProcedureQuery must not be null!");

		if (!procedureAttributes.hasReturnValue()) {
			return null;
		}

		Map<String, Object> outputValues = IntStream.range(0, procedureAttributes.getOutputParameterNames().size()) //
				.boxed() //
				.collect(Collectors.toMap( //
						procedureAttributes.getOutputParameterNames()::get, //
						i -> extractOutputParameter(storedProcedureQuery, i)));

		return outputValues.size() == 1 ? outputValues.values().iterator().next() : outputValues;
	}

	private Object extractOutputParameter(StoredProcedureQuery storedProcedureQuery, Integer index) {

		String outputParameterName = procedureAttributes.getOutputParameterNames().get(index);
		JpaParameters parameters = getQueryMethod().getParameters();

		return extractOutputParameterValue(storedProcedureQuery, outputParameterName, index, parameters.getNumberOfParameters());
	}

	/**
	 * extract the value of an output parameter either by name or by index.
	 *
	 * @param storedProcedureQuery the query object of the stored procedure.
	 * @param name                 the name of the output parameter
	 * @param index                index of the output parameter
	 * @param offset               for index based access the index after which to find the output parameter values
	 * @return the value
	 */
	private Object extractOutputParameterValue(StoredProcedureQuery storedProcedureQuery, String name, Integer index, int offset) {

		return useNamedParameters && StringUtils.hasText(name) ? //
				storedProcedureQuery.getOutputParameterValue(name)
				: storedProcedureQuery.getOutputParameterValue(offset + index + 1);
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

			ParameterMode mode = ParameterMode.OUT;

			IntStream.range(0, procedureAttributes.getOutputParameterTypes().size()).forEach(i -> {
				Class<?> outputParameterType = procedureAttributes.getOutputParameterTypes().get(i);

				if (useNamedParameters) {

					String outputParameterName = procedureAttributes.getOutputParameterNames().get(i);
					procedureQuery.registerStoredProcedureParameter(outputParameterName, outputParameterType, mode);

				} else {
					procedureQuery.registerStoredProcedureParameter(params.getNumberOfParameters() + i + 1, outputParameterType, mode);
				}
			});
		}

		return procedureQuery;
	}
}
