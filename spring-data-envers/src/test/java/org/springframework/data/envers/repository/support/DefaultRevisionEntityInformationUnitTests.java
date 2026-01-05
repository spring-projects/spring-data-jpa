/*
 * Copyright 2025-present the original author or authors.
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
package org.springframework.data.envers.repository.support;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DefaultRevisionEntityInformation}.
 *
 * @author Mark Paluch
 * @author Chaedong Im
 */
class DefaultRevisionEntityInformationUnitTests {

	@Test // GH-2850
	void defaultRevisionEntityInformationReturnsStandardTimestampFieldName() {

		DefaultRevisionEntityInformation revisionInfo = DefaultRevisionEntityInformation.INSTANCE;

		assertThat(revisionInfo.getRevisionTimestampPropertyName()).isEqualTo("timestamp");
	}

}
