/*
 * Copyright 2008-2014 the original author or authors.
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
package org.springframework.data.jpa.repository;

import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.test.context.ContextConfiguration;

/**
 * Testcase to run {@link UserRepository} integration tests on top of EclipseLink.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
@ContextConfiguration(value = "classpath:eclipselink.xml")
public class EclipseLinkNamespaceUserRepositoryTests extends NamespaceUserRepositoryTests {

	/**
	 * Ignored until https://bugs.eclipse.org/bugs/show_bug.cgi?id=349477 is resolved.
	 */
	@Override
	public void findsAllByGivenIds() {

	}

	/**
	 * Ignored until https://bugs.eclipse.org/bugs/show_bug.cgi?id=349477 is resolved.
	 */
	@Override
	public void handlesIterableOfIdsCorrectly() {

	}

	/**
	 * Ignored until https://bugs.eclipse.org/bugs/show_bug.cgi?id=349477 is resolved.
	 */
	@Override
	public void allowsExecutingPageableMethodWithNullPageable() {

	}

	/**
	 * Ignored until https://bugs.eclipse.org/bugs/show_bug.cgi?id=349477 is resolved.
	 */
	@Override
	public void invokesQueryWithVarargsParametersCorrectly() {}

	/**
	 * Ignored until https://bugs.eclipse.org/bugs/show_bug.cgi?id=422450 is resolved.
	 */
	@Override
	public void sortByAssociationPropertyShouldUseLeftOuterJoin() {}

	/**
	 * Ignored until https://bugs.eclipse.org/bugs/show_bug.cgi?id=422450 is resolved.
	 */
	@Override
	public void sortByAssociationPropertyInPageableShouldUseLeftOuterJoin() {}

	/**
	 * Ignored until https://bugs.eclipse.org/bugs/show_bug.cgi?id=349477 is resolved.
	 */
	@Override
	public void findByElementCollectionAttribute() {}
}
