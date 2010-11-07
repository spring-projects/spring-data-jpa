/*
 * Copyright 2008-2010 the original author or authors.
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
package org.springframework.data.jpa.repository.query;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.Date;

import javax.persistence.Embeddable;
import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.query.JpaQueryMethodUnitTests.InvalidDao;
import org.springframework.data.repository.query.QueryCreationException;


/**
 * Unit test for {@link QueryCreator}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class QueryCreatorUnitTests {

    private Method method;

    @Mock
    QueryExtractor extractor;
    @Mock
    EntityManager em;


    @Before
    public void setup() throws SecurityException, NoSuchMethodException {

        method =
                QueryCreatorUnitTests.class.getMethod(
                        "findByFirstnameAndMethod", String.class);
    }


    @Test(expected = QueryCreationException.class)
    public void rejectsInvalidProperty() throws Exception {

        JpaQueryMethod finderMethod = new JpaQueryMethod(method, extractor, em);
        new QueryCreator(finderMethod).constructQuery();
    }


    @Test
    public void splitsKeywordsCorrectly() throws SecurityException,
            NoSuchMethodException {

        method =
                QueryCreatorUnitTests.class.getMethod(
                        "findByNameOrOrganization", String.class, String.class);

        assertCreatesQueryForMethod(
                "where x.name = :name or x.organization = :organization",
                method);
    }


    /**
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @see #265
     * @throws Exception
     */
    @Test
    public void createsQueryWithEmbeddableCorrectly() throws SecurityException,
            NoSuchMethodException {

        method =
                getClass()
                        .getMethod("findByEmbeddable", SampleEmbeddable.class);

        assertCreatesQueryForMethod("where x.embeddable = :embeddable", method);
    }


    @Test
    public void createsQueryWithBetweenKeywordCorrectly() throws Exception {

        method =
                getClass().getMethod("findByStartDateBetweenAndName",
                        Date.class, Date.class, String.class);

        assertCreatesQueryForMethod(
                "where x.startDate between :first and :second and x.name = :name",
                method);
    }


    @Test
    public void createsQueryWithLessThanKeywordCorrectly() throws Exception {

        method = getClass().getMethod("findByAgeLessThan", int.class);

        assertCreatesQueryForMethod("where x.age < :age", method);
    }


    @Test
    public void createsQueryWithGreaterThanKeywordCorrectly() throws Exception {

        method = getClass().getMethod("findByAgeGreaterThan", int.class);

        assertCreatesQueryForMethod("where x.age > :age", method);
    }


    @Test(expected = IllegalArgumentException.class)
    public void rejectsModifyingMethodWithoutBacking()
            throws SecurityException, NoSuchMethodException {

        Method invalidModifyingMethod =
                InvalidDao.class.getMethod("updateMethod", String.class);

        JpaQueryMethod method =
                new JpaQueryMethod(invalidModifyingMethod, extractor, em);

        new QueryCreator(method);
    }


    @Test
    public void parsesLikeOperatorCorrectly() throws Exception {

        method = getClass().getMethod("findByNameLike", String.class);
        assertCreatesQueryForMethod("where x.name like :name", method);
    }


    @Test
    public void parsesNotOperatorCorrectly() throws Exception {

        method = getClass().getMethod("findByNameNot", String.class);
        assertCreatesQueryForMethod("where x.name <> :name", method);
    }


    @Test
    public void parsesNotNullOperatorCorrectly() throws Exception {

        method = getClass().getMethod("findByNameNotNull");
        assertCreatesQueryForMethod("where x.name is not null", method);
    }


    @Test
    public void parsesLikeCorrectly() throws Exception {

        method = getClass().getMethod("findByNameLike", String.class);
        assertCreatesQueryForMethod("where x.name like :name", method);
    }


    @Test
    public void parsesNotLikeCorrectly() throws Exception {

        method = getClass().getMethod("findByNameNotLike", String.class);
        assertCreatesQueryForMethod("where x.name not like :name", method);
    }


    @Test
    public void parsesOrderByClauseCorrectly() throws Exception {

        method =
                getClass().getMethod("findByNameOrderByOrganizationDesc",
                        String.class);
        assertCreatesQueryForMethod(
                "where x.name = :name order by x.organization desc", method);
    }


    /**
     * Asserts that the query created for the given {@link Method} results in a
     * query ending with the given {@link String}.
     * 
     * @param queryEnd
     * @param method
     */
    private void assertCreatesQueryForMethod(String queryEnd, Method method) {

        JpaQueryMethod queryMethod = new JpaQueryMethod(method, extractor, em);
        String result = new QueryCreator(queryMethod).constructQuery();
        assertThat(result, endsWith(queryEnd));
    }


    /**
     * Sample method to test failing query creation.
     * 
     * @param firstname
     * @return
     */
    public User findByFirstnameAndMethod(String firstname) {

        return null;
    }


    /**
     * A method to check that query keyowrds are considered correctly. The
     * {@link QueryCreator} must not detect the {@code Or} in
     * {@code Organization} as keyword.
     * 
     * @param name
     * @param organization
     * @return
     */
    public SampleEntity findByNameOrOrganization(String name,
            String organization) {

        return null;
    }


    /**
     * Sample method to create a finder query for that references an
     * {@link Embeddable}.
     * 
     * @see #265
     * @param embeddable
     * @return
     */
    public SampleEntity findByEmbeddable(SampleEmbeddable embeddable) {

        return null;
    }


    public SampleEntity findByStartDateBetweenAndName(Date first, Date second,
            String name) {

        return null;
    }


    public SampleEntity findByAgeLessThan(int age) {

        return null;
    }


    public SampleEntity findByAgeGreaterThan(int age) {

        return null;
    }


    public SampleEntity findByNameLike(String name) {

        return null;
    }


    public SampleEntity findByNameNotLike(String name) {

        return null;
    }


    public SampleEntity findByNameNot(String name) {

        return null;
    }


    public SampleEntity findByNameNotNull() {

        return null;
    }


    public SampleEntity findByNameOrderByOrganizationDesc(String name) {

        return null;
    }

    /**
     * Sample class for keyword split check.
     * 
     * @author Oliver Gierke
     */
    @SuppressWarnings("unused")
    static class SampleEntity {

        private String organization;
        private String name;

        private Date startDate;
        private int age;

        private SampleEmbeddable embeddable;
    }

    @Embeddable
    @SuppressWarnings("unused")
    static class SampleEmbeddable {

        private String foo;
        private String bar;
    }
}
