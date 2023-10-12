/*
 * Copyright 2008-2023 the original author or authors.
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
package org.springframework.data.jpa.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Holds the values for query parameters Used to set values for Specifications that have named parameters via
 * JpaSpecificationExecutor
 *
 * @author Vladimir Iftodi
 */
public class ParametersValues {

	private final Map<String, Object> valuesByParameterName = new HashMap<>();

	public static ParametersValues empty() {
		return new ParametersValues();
	}

	public static ParametersValues of(String name, Object value) {
		var paramValues = new ParametersValues();

		paramValues.setValue(name, value);

		return paramValues;
	}

	public static ParametersValues of(String name1, Object value1, String name2, Object value2) {
		var paramValues = new ParametersValues();

		paramValues.setValue(name1, value1);
		paramValues.setValue(name2, value2);

		return paramValues;
	}

	public static ParametersValues from(Map<String, Object> paramValuesMap) {
		var paramValues = new ParametersValues();

		paramValuesMap.forEach(paramValues::setValue);

		return paramValues;
	}

	public void setValue(String name, Object value) {
		valuesByParameterName.put(name, value);
	}

	public void forEach(BiConsumer<String, Object> action) {
		valuesByParameterName.forEach(action);
	}

}
