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

import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;
import org.junit.jupiter.api.Test;

import org.springframework.data.envers.sample.CustomRevisionEntity;

/**
 * Unit tests for {@link ReflectionRevisionEntityInformation}.
 *
 * @author Mark Paluch
 * @author Chaedong Im
 */
class ReflectionRevisionEntityInformationUnitTests {

	@Test // GH-2850
	void reflectionRevisionEntityInformationDetectsStandardTimestampField() {

		ReflectionRevisionEntityInformation revisionInfo = new ReflectionRevisionEntityInformation(
				CustomRevisionEntity.class);

		assertThat(revisionInfo.getRevisionTimestampPropertyName()).isEqualTo("timestamp");
	}

	@Test // GH-2850
	void reflectionRevisionEntityInformationDetectsCustomTimestampField() {

		ReflectionRevisionEntityInformation revisionInfo = new ReflectionRevisionEntityInformation(
				WithCustomTimestampPropertyName.class);

		assertThat(revisionInfo.getRevisionTimestampPropertyName()).isEqualTo("myCustomTimestamp");
	}

	/**
	 * Custom revision entity with a non-standard timestamp field name to test dynamic timestamp property detection.
	 *
	 * @author Chaedong Im
	 */
	private static class WithCustomTimestampPropertyName {

		@RevisionNumber private int revisionId;

		@RevisionTimestamp private long myCustomTimestamp; // Non-standard field name
	}

}
