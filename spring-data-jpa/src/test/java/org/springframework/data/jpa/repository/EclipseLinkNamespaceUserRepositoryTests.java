/*
 * Copyright 2008-present the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import jakarta.persistence.Query;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.test.context.ContextConfiguration;

/**
 * Testcase to run {@link UserRepository} integration tests on top of EclipseLink.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Jens Schauder
 * @author Moritz Becker
 * @author Andrey Kovalev
 * @author Krzysztof Krason
 * @author Mark Paluch
 */
@ContextConfiguration(value = "classpath:eclipselink.xml")
class EclipseLinkNamespaceUserRepositoryTests extends NamespaceUserRepositoryTests {

	@Disabled("EclipseLink does not support records, additionally, it does not support constructor creation using nested (join) properties")
	@Override
	@Test
	public void findByFluentSpecificationWithDtoProjectionJoins() {}

	/**
	 * Ignored until https://bugs.eclipse.org/bugs/show_bug.cgi?id=422450 is resolved.
	 */
	@Override
	void sortByAssociationPropertyShouldUseLeftOuterJoin() {}

	/**
	 * Ignored until https://bugs.eclipse.org/bugs/show_bug.cgi?id=422450 is resolved.
	 */
	@Override
	void sortByAssociationPropertyInPageableShouldUseLeftOuterJoin() {}

	/**
	 * This test will fail once https://bugs.eclipse.org/bugs/show_bug.cgi?id=521915 is fixed.
	 */
	@Override
	@Test // DATAJPA-1172
	void queryProvidesCorrectNumberOfParametersForNativeQuery() {

		Query query = em.createNativeQuery("select 1 from User where firstname=? and lastname=?");
		assertThat(query.getParameters()).describedAs(
				"Due to a bug eclipse has size 0; If this is no longer the case the special code path triggered in NamedOrIndexedQueryParameterSetter.registerExcessParameters can be removed")
				.isEmpty();
	}

	/**
	 * Ignored until https://bugs.eclipse.org/bugs/show_bug.cgi?id=525319 is fixed.
	 */
	@Disabled
	@Override
	@Test // DATAJPA-980
	void supportsInterfaceProjectionsWithNativeQueries() {}

	/**
	 * Ignored until https://bugs.eclipse.org/bugs/show_bug.cgi?id=525319 is fixed.
	 */
	@Disabled
	@Override
	@Test // DATAJPA-1248
	void supportsProjectionsWithNativeQueriesAndCamelCaseProperty() {}

	/**
	 * Ignored until https://bugs.eclipse.org/bugs/show_bug.cgi?id=525319 is fixed.
	 */
	@Disabled
	@Override
	@Test // DATAJPA-1301
	void returnsNullValueInMap() {}

	/**
	 * TODO: Remove, once https://bugs.eclipse.org/bugs/show_bug.cgi?id=289141 is fixed.
	 */
	@Disabled
	@Override
	@Test
	void bindsNativeQueryResultsToProjectionByName() {}

	/**
	 * Ignores the test for EclipseLink.
	 */
	@Override
	@Test
	@Disabled
	void savingUserThrowsAnException() {}

	/**
	 * Ignored until https://bugs.eclipse.org/bugs/show_bug.cgi?id=349477 is resolved.
	 */
	@Disabled
	@Override
	@Test // DATAJPA-1303
	void findByElementCollectionInAttributeIgnoreCase() {}

	/**
	 * Ignored until https://bugs.eclipse.org/bugs/show_bug.cgi?id=349477 is resolved.
	 */
	@Disabled
	@Override
	@Test // DATAJPA-1303
	void findByElementCollectionNotInAttributeIgnoreCase() {}

	/**
	 * Ignored until https://bugs.eclipse.org/bugs/show_bug.cgi?id=349477 is resolved.
	 */
	@Disabled
	@Override
	@Test // DATAJPA-1303
	void findByElementVarargInAttributeIgnoreCase() {}

	/**
	 * Ignored until https://bugs.eclipse.org/bugs/show_bug.cgi?id=349477 is resolved.
	 */
	@Disabled
	@Override
	@Test // DATAJPA-1303
	void findByElementCollectionInAttributeIgnoreCaseWithNulls() {}

	@Disabled("Named parameters in native SQL queries are not supported in EclipseLink")
	@Override
	@Test
	void insertStatementModifyingQueryWithParamsWorks() {}

	@Disabled("Named parameters in native SQL queries are not supported in EclipseLink")
	@Override
	@Test
	void bindsSpELParameterOnlyUsedInCountQuery() {}

	@Disabled
	@Override
	@Test
	public void correctlyBuildSortClauseWhenSortingByFunctionAliasAndFunctionContainsNamedParameters() {}

	@Disabled
	@Override
	@Test
	public void allowsExecutingPageableMethodWithUnpagedArgument() {}

	@Disabled("No Tuples support in EclipseLink")
	@Override
	@Test
	public void supportsProjectionsWithNativeQueriesAndUnderscoresColumnNameToCamelCaseProperty() {}

	@Disabled
	@Override
	@Test
	public void findByFluentExampleWithSimplePropertyPathsDoesntLoadUnrequestedPaths() {}

	@Disabled("Not spec-compliant")
	@Override
	@Test
	public void findByFluentSpecificationWithInterfaceBasedProjection() {}

	@Disabled("Not spec-compliant")
	@Override
	@Test
	public void findByFluentSpecificationWithDtoProjection() {}

	@Disabled("Named parameters in native SQL queries are not supported in EclipseLink")
	@Override
	@Test
	public void modifyingUpdateNativeQueryWorksWithJSQLParser() {}

	@Disabled("EclipseLink treats id as keyword")
	@Override
	@Test
	public void scrollByPredicateKeysetWithInterfaceProjection() {}

	@Disabled("Not spec-compliant")
	@Override
	@Test
	public void findByFluentSpecificationWithCollectionPropertyPathsDoesntLoadUnrequestedPaths() {}

	@Disabled("Not spec-compliant")
	@Override
	@Test
	public void findByFluentExampleWithComplexPropertyPathsDoesntLoadUnrequestedPaths() {}

	@Disabled("Not spec-compliant")
	@Override
	@Test
	public void findByFluentExampleWithCollectionPropertyPathsDoesntLoadUnrequestedPaths() {}

	@Disabled("Not spec-compliant")
	@Override
	@Test
	public void findByFluentSpecificationWithSimplePropertyPathsDoesntLoadUnrequestedPaths() {}

	@Disabled("Not spec-compliant")
	@Override
	@Test
	public void findByFluentSpecificationWithComplexPropertyPathsDoesntLoadUnrequestedPaths() {}

	@Disabled("Not spec-compliant")
	@Override
	@Test
	public void findByFluentPredicateWithProjectionAndPageRequest() {}

	@Disabled("Not spec-compliant")
	@Override
	@Test
	public void findByFluentPredicateWithProjectionAndAll() {}

	@Disabled("EclipseLink does not support records")
	@Override
	@Test
	public void supportsRecordsWithNativeQueries() {}

	@Disabled("Not spec-compliant")
	@Override
	@Test
	public void correctlyBuildSortClauseWhenSortingByFunctionAliasAndFunctionContainsPositionalParameters() {}

}
