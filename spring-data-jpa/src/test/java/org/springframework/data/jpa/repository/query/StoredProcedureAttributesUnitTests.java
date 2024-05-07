/*
 * Copyright 2015-2024 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.jpa.repository.query.StoredProcedureAttributes.*;

import jakarta.persistence.ParameterMode;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link StoredProcedureAttributes}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Thorben Janssen
 */
class StoredProcedureAttributesUnitTests {

	@Test // DATAJPA-681
	void usesSyntheticOutputParameterNameForAdhocProcedureWithoutOutputName() {

		StoredProcedureAttributes attributes = new StoredProcedureAttributes("procedure",
				new ProcedureParameter(null, 1, ParameterMode.OUT, Long.class));
		assertThat(attributes.getOutputProcedureParameters().get(0).getName()).isEqualTo(SYNTHETIC_OUTPUT_PARAMETER_NAME);
	}
}
