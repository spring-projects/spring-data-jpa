/*
 * Copyright 2018-2025 the original author or authors.
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
package org.springframework.data.jpa.repository.config;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link InspectionClassLoader}.
 *
 * @author Mark Paluch
 */
class InspectionClassLoaderUnitTests {

	@Test // DATAJPA-1250
	void shouldLoadExternalClass() throws ClassNotFoundException {

		InspectionClassLoader classLoader = new InspectionClassLoader(getClass().getClassLoader());

		Class<?> isolated = classLoader.loadClass("org.hsqldb.Database");
		Class<?> included = getClass().getClassLoader().loadClass("org.hsqldb.Database");

		assertThat(isolated.getClassLoader()) //
				.isSameAs(classLoader) //
				.isNotSameAs(getClass().getClassLoader());

		assertThat(isolated).isNotEqualTo(included);
	}
}
