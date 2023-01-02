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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.persistence.StoredProcedureQuery;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Stored procedure configuration for JPA 2.1 {@link StoredProcedureQuery}s.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Jeff Sheets
 * @author Jens Schauder
 * @author Gabriel Basilio
 * @since 1.6
 */
class StoredProcedureAttributes {

	// A synthetic output parameter name to be used in case of derived stored procedures and named parameters
	static final String SYNTHETIC_OUTPUT_PARAMETER_NAME = "out";

	private final boolean namedStoredProcedure;
	private final String procedureName;
	private final List<ProcedureParameter> outputProcedureParameters;

	/**
	 * Creates a new {@link StoredProcedureAttributes}.
	 *
	 * @param procedureName must not be {@literal null}.
	 */
	StoredProcedureAttributes(String procedureName, ProcedureParameter parameter) {
		this(procedureName, Collections.singletonList(parameter), false);
	}

	/**
	 * Creates a new {@link StoredProcedureAttributes}.
	 *
	 * @param procedureName must not be {@literal null}.
	 * @param namedStoredProcedure flag signaling if the stored procedure has a name.
	 */
	StoredProcedureAttributes(String procedureName, List<ProcedureParameter> outputProcedureParameters,
			boolean namedStoredProcedure) {

		Assert.notNull(procedureName, "ProcedureName must not be null");
		Assert.notNull(outputProcedureParameters, "OutputProcedureParameters must not be null");
		Assert.isTrue(outputProcedureParameters.size() != 1 || outputProcedureParameters.get(0) != null,
				"ProcedureParameters must not have size 1 with a null value");

		this.procedureName = procedureName;
		this.namedStoredProcedure = namedStoredProcedure;

		if (namedStoredProcedure) {
			this.outputProcedureParameters = outputProcedureParameters;
		} else {
			this.outputProcedureParameters = getParametersWithCompletedNames(outputProcedureParameters);
		}
	}

	private List<ProcedureParameter> getParametersWithCompletedNames(List<ProcedureParameter> procedureParameters) {

		return IntStream.range(0, procedureParameters.size()) //
				.mapToObj(i -> getParameterWithCompletedName(procedureParameters.get(i), i)) //
				.collect(Collectors.toList());
	}

	private ProcedureParameter getParameterWithCompletedName(ProcedureParameter parameter, int i) {

		return new ProcedureParameter(completeOutputParameterName(i, parameter.getName()), parameter.getMode(),
				parameter.getType());
	}

	private String completeOutputParameterName(int i, String paramName) {

		return StringUtils.hasText(paramName) //
				? paramName //
				: createSyntheticParameterName(i);
	}

	private String createSyntheticParameterName(int i) {
		return SYNTHETIC_OUTPUT_PARAMETER_NAME + (i == 0 ? "" : i);
	}

	/**
	 * Returns the name of the stored procedure.
	 *
	 * @return
	 */
	public String getProcedureName() {
		return procedureName;
	}

	/**
	 * Returns the names of the output parameters.
	 *
	 * @return
	 */
	public boolean isNamedStoredProcedure() {
		return namedStoredProcedure;
	}

	/**
	 * @return Returns the stored procedure output parameter list
	 */
	public List<ProcedureParameter> getOutputProcedureParameters() {
		return outputProcedureParameters;
	}

	/**
	 * Returns whether the stored procedure will produce a result.
	 *
	 * @return
	 */
	public boolean hasReturnValue() {

		if (getOutputProcedureParameters().isEmpty())
			return false;

		Class<?> outputType = getOutputProcedureParameters().get(0).getType();
		return !(void.class.equals(outputType) || Void.class.equals(outputType));
	}
}
