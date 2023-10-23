/*
 * Copyright 2023 the original author or authors.
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.Query;
import jakarta.persistence.StoredProcedureQuery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.repository.query.Parameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * An {@link AbstractJpaQueryContext} used to handle stored procedures.
 *
 * @author Greg Turnquist
 */
class StoredProcedureQueryContext extends AbstractJpaQueryContext {

	private final StoredProcedureAttributes procedureAttributes;
	private final boolean useNamedParameters;

	StoredProcedureQueryContext(JpaQueryMethod method, EntityManager entityManager) {

		super(Optional.of(method), entityManager);

		Assert.notNull(method, "JpaQueryMethod cannot be null");

		this.procedureAttributes = method.getProcedureAttributes();
		this.useNamedParameters = method.getParameters().stream() //
				.anyMatch(Parameter::isNamedParameter);
	}

	@Override
	protected ContextualQuery createQuery(JpaParametersParameterAccessor accessor) {
		return ContextualQuery.of(procedureAttributes.getProcedureName());
	}

	@Override
	protected Query turnIntoJpaQuery(ContextualQuery query, JpaParametersParameterAccessor accessor) {

		return procedureAttributes.isNamedStoredProcedure() //
				? newNamedStoredProcedureQuery(query.getQuery())
				: newAdhocStoredProcedureQuery(query.getQuery());
	}

	@Override
	protected Query bindParameters(Query query, JpaParametersParameterAccessor accessor) {

		Assert.isInstanceOf(StoredProcedureQuery.class, query);
		StoredProcedureQuery storedProcedure = (StoredProcedureQuery) query;

		QueryParameterSetter.QueryMetadata metadata = metadataCache.getMetadata("singleton", storedProcedure);

		return parameterBinder.get().bind(storedProcedure, metadata, accessor);
	}

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

		JpaParameters methodParameters = queryMethod().getParameters();

		return useNamedParameters && StringUtils.hasText(outputParameter.getName())
				? storedProcedureQuery.getOutputParameterValue(outputParameter.getName())
				: storedProcedureQuery.getOutputParameterValue(methodParameters.getNumberOfParameters() + index + 1);
	}

	private Query newNamedStoredProcedureQuery(String query) {
		return getEntityManager().createNamedStoredProcedureQuery(query);
	}

	private Query newAdhocStoredProcedureQuery(String query) {

		StoredProcedureQuery procedureQuery = queryMethod().isQueryForEntity() //
				? getEntityManager().createStoredProcedureQuery(query, queryMethod().getEntityInformation().getJavaType()) //
				: getEntityManager().createStoredProcedureQuery(query);

		JpaParameters params = queryMethod().getParameters();

		for (JpaParameters.JpaParameter param : params) {

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

	/**
	 * @return true if the stored procedure will use a ResultSet to return data and not output parameters
	 */
	private boolean isResultSetProcedure() {
		return queryMethod().isCollectionQuery() || queryMethod().isQueryForEntity();
	}
}
