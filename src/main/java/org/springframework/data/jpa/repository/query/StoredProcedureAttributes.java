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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Stored procedure configuration for JPA 2.1 {@link StoredProcedureQuery}s.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Jeff Sheets
 * @since 1.6
 */
class StoredProcedureAttributes {

	// A syntheic output parameter name to be used in case of derived stored procedures and named parameters
	static final String SYNTHETIC_OUTPUT_PARAMETER_NAME = "out";

	private final boolean namedStoredProcedure;
	private final String procedureName;
	private final List<String> outputParameterNames;
	private final List<Class<?>> outputParameterTypes;

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
		this(procedureName, Arrays.asList(outputParameterName), Arrays.asList(outputParameterType), namedStoredProcedure);
	}

	/**
	 * Creates a new {@link StoredProcedureAttributes}.
	 *
	 * @param procedureName must not be {@literal null}
	 * @param outputParameterNames may be empty, but not null
	 * @param outputParameterTypes must not be empty, and cannot be a single element of null
	 * @param namedStoredProcedure
	 */
	public StoredProcedureAttributes(String procedureName, List<String> outputParameterNames,
			List<Class<?>> outputParameterTypes, boolean namedStoredProcedure) {

		Assert.notNull(procedureName, "ProcedureName must not be null!");
		Assert.notNull(outputParameterNames, "OutputParameterNames must not be null!");
		Assert.notEmpty(outputParameterTypes, "OutputParameterTypes must not be empty!");
		Assert.isTrue(outputParameterTypes.size() != 1 || outputParameterTypes.get(0) != null, "OutputParameterTypes must not have size 1 with a null value");

		this.procedureName = procedureName;
		this.outputParameterNames = namedStoredProcedure ? outputParameterNames : IntStream.range(0, outputParameterNames.size()).mapToObj(i -> {
			String paramName = outputParameterNames.get(i);
			return !StringUtils.hasText(paramName) ? SYNTHETIC_OUTPUT_PARAMETER_NAME + (i == 0 ? "" : i) : paramName;
		}).collect(Collectors.toList());
		this.outputParameterTypes = outputParameterTypes;
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
	 * Returns the names of the output parameters.
	 *
	 * @return
	 */
	public List<String> getOutputParameterNames() {
		return outputParameterNames;
	}

	/**
	 * Returns the types of the output parameters.
	 *
	 * @return
	 */
	public List<Class<?>> getOutputParameterTypes() {
		return outputParameterTypes;
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
		return !(outputParameterTypes.size() == 1 && (void.class.equals(outputParameterTypes.get(0)) || Void.class.equals(outputParameterTypes.get(0))));
	}
}
