/*
 * Copyright 2014-2023 the original author or authors.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.TypedQuery;

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
 * @author Jeff Sheets
 * @author JyotirmoyVS
 * @since 1.6
 */
class StoredProcedureJpaQuery extends AbstractJpaQuery {

	private final StoredProcedureAttributes procedureAttributes;
	private final boolean useNamedParameters;
	private final QueryParameterSetter.QueryMetadataCache metadataCache = new QueryParameterSetter.QueryMetadataCache();

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

	@Override
	protected StoredProcedureQuery createQuery(JpaParametersParameterAccessor accessor) {
		return applyHints(doCreateQuery(accessor), getQueryMethod());
	}

	@Override
	protected StoredProcedureQuery doCreateQuery(JpaParametersParameterAccessor accessor) {

		StoredProcedureQuery storedProcedure = createStoredProcedure();
		QueryParameterSetter.QueryMetadata metadata = metadataCache.getMetadata("singleton", storedProcedure);

		return parameterBinder.get().bind(storedProcedure, metadata, accessor);
	}

	@Override
	protected TypedQuery<Long> doCreateCountQuery(JpaParametersParameterAccessor accessor) {
		throw new UnsupportedOperationException("StoredProcedureQuery does not support count queries");
	}

	/**
	 * Extracts the output value from the given {@link StoredProcedureQuery}.
	 *
	 * @param storedProcedureQuery must not be {@literal null}.
	 *          <p>
	 *          Result is either a single value, or a Map<String, Optional<Object>> of output parameter names to nullable
	 *          values
	 */
	@Nullable
	Object extractOutputValue(StoredProcedureQuery storedProcedureQuery) {

		Assert.notNull(storedProcedureQuery, "StoredProcedureQuery must not be null");

		if (!procedureAttributes.hasReturnValue()) {
			return null;
		}

		List<ProcedureParameter> outputParameters = procedureAttributes.getOutputProcedureParameters();

		if (outputParameters.size() == 1) {
			return extractOutputParameterValue(outputParameters.get(0), 0, storedProcedureQuery);
		}

		Map<String, Object> outputValues = new HashMap<>();

		for (int i = 0; i < outputParameters.size(); i++) {
			ProcedureParameter outputParameter = outputParameters.get(i);
			outputValues.put(outputParameter.getName(),
					extractOutputParameterValue(outputParameter, i, storedProcedureQuery));
		}

		return outputValues;
	}

	/**
	 * @return The value of an output parameter either by name or by index.
	 */
	private Object extractOutputParameterValue(ProcedureParameter outputParameter, Integer index,
			StoredProcedureQuery storedProcedureQuery) {

		JpaParameters methodParameters = getQueryMethod().getParameters();

		return useNamedParameters && StringUtils.hasText(outputParameter.getName())
				? storedProcedureQuery.getOutputParameterValue(outputParameter.getName())
				: storedProcedureQuery.getOutputParameterValue(methodParameters.getNumberOfParameters() + index + 1);
	}

	/**
	 * Creates a new JPA 2.1 {@link StoredProcedureQuery} from this {@link StoredProcedureJpaQuery}.
	 */
	private StoredProcedureQuery createStoredProcedure() {

		return procedureAttributes.isNamedStoredProcedure() //
				? newNamedStoredProcedureQuery()
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
		StoredProcedureQuery procedureQuery = createAdhocStoredProcedureQuery();

		for (JpaParameter param : params) {

			if (!param.isBindable()) {
				continue;
			}

			if (useNamedParameters) {
				procedureQuery.registerStoredProcedureParameter(
						param.getName()
								.orElseThrow(() -> new IllegalArgumentException(ParameterBinder.PARAMETER_NEEDS_TO_BE_NAMED)),
						param.getType(), ParameterMode.IN);
			} else {
				procedureQuery.registerStoredProcedureParameter(param.getIndex() + 1, param.getType(), ParameterMode.IN);
			}
		}

		if (procedureAttributes.hasReturnValue()) {

			ProcedureParameter procedureOutput = procedureAttributes.getOutputProcedureParameters().get(0);

			/*
			 * If there is a {@link java.sql.ResultSet} with a {@link ParameterMode#REF_CURSOR}, find the output parameter.
			 * Otherwise, no need, there is no need to find an output parameter.
			 */
			if (storedProcedureHasResultSetUsingRefCursor(procedureOutput) || !isResultSetProcedure()) {

				if (useNamedParameters) {
					procedureQuery.registerStoredProcedureParameter(procedureOutput.getName(), procedureOutput.getType(),
							procedureOutput.getMode());
				} else {

					// Output parameter should be after the input parameters
					int outputParameterIndex = params.getNumberOfParameters() + 1;

					procedureQuery.registerStoredProcedureParameter(outputParameterIndex, procedureOutput.getType(),
							procedureOutput.getMode());
				}
			}
		}

		return procedureQuery;
	}

	/**
	 * Does this stored procedure have a {@link java.sql.ResultSet} using {@link ParameterMode#REF_CURSOR}?
	 *
	 * @param procedureOutput
	 * @return
	 */
	private boolean storedProcedureHasResultSetUsingRefCursor(ProcedureParameter procedureOutput) {
		return isResultSetProcedure() && procedureOutput.getMode() == ParameterMode.REF_CURSOR;
	}

	private StoredProcedureQuery createAdhocStoredProcedureQuery() {

		if (getQueryMethod().isQueryForEntity()) {

			return getEntityManager().createStoredProcedureQuery(procedureAttributes.getProcedureName(),
					getQueryMethod().getEntityInformation().getJavaType());
		}

		return getEntityManager().createStoredProcedureQuery(procedureAttributes.getProcedureName());
	}

	/**
	 *
	 * @return true if the stored procedure will use a ResultSet to return data and not output parameters
	 */
	private boolean isResultSetProcedure() {
		return getQueryMethod().isCollectionQuery() || getQueryMethod().isQueryForEntity();
	}
}
