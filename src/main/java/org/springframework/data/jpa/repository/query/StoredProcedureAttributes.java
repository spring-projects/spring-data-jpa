/*
 * Copyright 2014 the original author or authors.
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

import javax.persistence.StoredProcedureQuery;

import org.springframework.util.Assert;

/**
 * Stored procedure configuration for JPA 2.1 {@link StoredProcedureQuery}s.
 * 
 * @author Thomas Darimont
 */
public class StoredProcedureAttributes {

	private final boolean namedStoredProcedure;
	private final String procedureName;
	private final String outputParameterName;
	private final Class<?> outputParameterType;

	/**
	 * Creates a new {@link StoredProcedureAttributes}.
	 * 
	 * @param procedureName must not be {@literal null}
	 * @param outputParameterName may be {@literal null}
	 * @param outputParameterIndex must not be {@literal null}
	 * @param outputParameterType
	 */
	public StoredProcedureAttributes(String procedureName, String outputParameterName, Class<?> outputParameterType,
			boolean namedStoredProcedure) {

		Assert.notNull(procedureName, "ProcedureName must not be null!");
		Assert.notNull(outputParameterType, "OutputParameterType must not be null!");

		this.procedureName = procedureName;
		this.outputParameterName = outputParameterName;
		this.outputParameterType = outputParameterType;
		this.namedStoredProcedure = namedStoredProcedure;
	}

	public String getProcedureName() {
		return procedureName;
	}

	public String getOutputParameterName() {
		return outputParameterName;
	}

	public Class<?> getOutputParameterType() {
		return outputParameterType;
	}

	public boolean isNamedStoredProcedure() {
		return namedStoredProcedure;
	}

	public boolean hasReturnValue() {
		return !(void.class.equals(outputParameterType) || Void.class.equals(outputParameterType));
	}
}
