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
package org.springframework.data.jpa.repository;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
@ContextConfiguration("classpath:eclipselink.xml")
public class EclipseLinkEntityGraphRepositoryMethodsIntegrationTests
		extends EntityGraphRepositoryMethodsIntegrationTests {

	@Ignore("Bug 510627 - https://bugs.eclipse.org/bugs/show_bug.cgi?id=510627")
	@Test
	@Override
	public void shouldRespectNamedEntitySubGraph() {}

	@Ignore("Bug 510627 - https://bugs.eclipse.org/bugs/show_bug.cgi?id=510627")
	@Test
	@Override
	public void shouldRespectMultipleSubGraphForSameAttributeWithDynamicFetchGraph() {}

	@Ignore("Bug 510627 - https://bugs.eclipse.org/bugs/show_bug.cgi?id=510627")
	@Test
	@Override
	public void shouldRespectDynamicFetchGraphForGetOneWithAttributeNamesById() {}

	@Ignore("Bug 510627 - https://bugs.eclipse.org/bugs/show_bug.cgi?id=510627")
	@Test
	@Override
	public void shouldRespectConfiguredJpaEntityGraphWithPaginationAndQueryDslPredicates() {}

	@Ignore("Bug 510627 - https://bugs.eclipse.org/bugs/show_bug.cgi?id=510627")
	@Test
	@Override
	public void shouldRespectConfiguredJpaEntityGraphWithPaginationAndSpecification() {}

	@Ignore("Bug 510627 - https://bugs.eclipse.org/bugs/show_bug.cgi?id=510627")
	@Test
	@Override
	public void shouldCreateDynamicGraphWithMultipleLevelsOfSubgraphs() {}

	@Ignore("Bug 510627 - https://bugs.eclipse.org/bugs/show_bug.cgi?id=510627")
	@Test
	@Override
	public void shouldRespectConfiguredJpaEntityGraphInFindOne() {}

	@Ignore("Bug 510627 - https://bugs.eclipse.org/bugs/show_bug.cgi?id=510627")
	@Test
	@Override
	public void shouldRespectInferFetchGraphFromMethodName() {}
}
