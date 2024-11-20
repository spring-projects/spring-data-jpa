/*
 * Copyright 2011-2024 the original author or authors.
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
package org.springframework.data.jpa.repository;

import org.junit.jupiter.api.Disabled;
import org.springframework.test.context.ContextConfiguration;

/**
 * Ignores some test cases using IN queries as long as we wait for fix for
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=349477.
 *
 * @author Oliver Gierke
 * @author Greg Turnquist
 */
@ContextConfiguration("classpath:eclipselink-h2.xml")
class EclipseLinkUserRepositoryFinderTests extends UserRepositoryFinderTests {

	@Disabled
	@Override
	void executesNotInQueryCorrectly() {}

	@Disabled
	@Override
	void executesInKeywordForPageCorrectly() {}

	@Disabled
	@Override
	void shouldProjectWithKeysetScrolling() {

	}

}
