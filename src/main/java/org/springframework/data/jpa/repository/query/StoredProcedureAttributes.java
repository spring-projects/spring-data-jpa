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

import javax.persistence.StoredProcedureQuery;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Stored procedure configuration for JPA 2.1 {@link StoredProcedureQuery}s.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Mark Paluch
 * @since 1.6
 */
class StoredProcedureAttributes {

	// A syntheic output parameter name to be used in case of derived stored procedures and named parameters
	static final String SYNTHETIC_OUTPUT_PARAMETER_NAME = "out";

	private final boolean namedStoredProcedure;
	private final String procedureName;
	private final String outputParameterName;
	private final Class<?> outputParameterType;

	/**
	 * Creates a new {@link StoredProcedureAttributes}.
	 *
	 * @param procedureName must not be {@literal null}
	 * @param outputParameterName may be {@literal null}
	 * @param outputParameterType must not be {@literal null}
	 * @param namedStoredProcedure
	 */
	public StoredProcedureAttributes(String procedureName, @Nullable String outputParameterName,
			Class<?> outputParameterType, boolean namedStoredProcedure) {

		Assert.notNull(procedureName, "ProcedureName must not be null!");
		Assert.notNull(outputParameterType, "OutputParameterType must not be null!");

		this.procedureName = procedureName;
		this.outputParameterName = !namedStoredProcedure && !StringUtils.hasText(outputParameterName) ? SYNTHETIC_OUTPUT_PARAMETER_NAME
				: outputParameterName;
		this.outputParameterType = outputParameterType;
		this.namedStoredProcedure = namedStoredProcedure;
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
	 * Returns the name of the output parameter.
	 *
	 * @return
	 */
	public String getOutputParameterName() {
		return outputParameterName;
	}

	/**
	 * Returns the type of the output parameter.
	 *
	 * @return
	 */
	public Class<?> getOutputParameterType() {
		return outputParameterType;
	}

	/**
	 * Returns whether the stored procedure is a named one.
	 *
	 * @return
	 */
	public boolean isNamedStoredProcedure() {
		return namedStoredProcedure;
	}

	/**
	 * Returns whether the stored procedure will produce a result.
	 *
	 * @return
	 */
	public boolean hasReturnValue() {
		return !(void.class.equals(outputParameterType) || Void.class.equals(outputParameterType));
	}
}
