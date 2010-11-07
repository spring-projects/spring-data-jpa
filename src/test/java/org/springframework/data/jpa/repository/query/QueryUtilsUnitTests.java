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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.jpa.repository.query.QueryUtils.*;

import org.hamcrest.Matcher;
import org.junit.Test;


/**
 * Unit test for {@link QueryUtils}.
 * 
 * @author Oliver Gierke
 */
public class QueryUtilsUnitTests {

    static final String QUERY = "select u from User u";
    static final String FQ_QUERY =
            "select u from org.synyx.hades.domain.User$Foo_Bar u";
    static final String SIMPLE_QUERY = "from User u";
    static final String COUNT_QUERY = "select count(u) from User u";

    static final String QUERY_WITH_AS =
            "select u from User as u where u.username = ?";

    static final Matcher<String> IS_U = is("u");


    @Test
    public void createsCountQueryCorrectly() throws Exception {

        assertCountQuery(QUERY, COUNT_QUERY);

    }


    /**
     * @see #303
     */
    @Test
    public void createsCountQueriesCorrectlyForCapitalLetterJPQL() {

        assertCountQuery("FROM User u WHERE u.foo.bar = ?",
                "select count(u) FROM User u WHERE u.foo.bar = ?");

        assertCountQuery("SELECT u FROM User u where u.foo.bar = ?",
                "select count(u) FROM User u where u.foo.bar = ?");
    }


    /**
     * @see #351
     */
    @Test
    public void createsCountQueryForDistinctQueries() throws Exception {

        assertCountQuery("select distinct u from User u where u.foo = ?",
                "select count(distinct u) from User u where u.foo = ?");
    }


    /**
     * @see #351
     */
    @Test
    public void createsCountQueryForConstructorQueries() throws Exception {

        assertCountQuery(
                "select distinct new User(u.name) from User u where u.foo = ?",
                "select count(distinct u) from User u where u.foo = ?");
    }


    /**
     * @see #352
     */
    @Test
    public void createsCountQueryForJoins() throws Exception {

        assertCountQuery(
                "select distinct new User(u.name) from User u left outer join u.roles r WHERE r = ?",
                "select count(distinct u) from User u left outer join u.roles r WHERE r = ?");
    }


    /**
     * @see #352
     */
    @Test
    public void createsCountQueryForQueriesWithSubSelects() throws Exception {

        assertCountQuery(
                "select u from User u left outer join u.roles r where r in (select r from Role)",
                "select count(u) from User u left outer join u.roles r where r in (select r from Role)");
    }


    /**
     * @see #355
     */
    @Test
    public void createsCountQueryForAliasesCorrectly() throws Exception {

        assertCountQuery("select u from User as u",
                "select count(u) from User as u");
    }


    @Test
    public void allowsShortJpaSyntax() throws Exception {

        assertCountQuery(SIMPLE_QUERY, COUNT_QUERY);
    }


    @Test
    public void detectsAliasCorrectly() throws Exception {

        assertThat(detectAlias(QUERY), IS_U);
        assertThat(detectAlias(SIMPLE_QUERY), IS_U);
        assertThat(detectAlias(COUNT_QUERY), IS_U);
        assertThat(detectAlias(QUERY_WITH_AS), IS_U);
        assertThat(detectAlias("SELECT FROM USER U"), is("U"));
        assertThat(detectAlias("select u from  User u"), IS_U);
        assertThat(detectAlias("select u from  com.acme.User u"), IS_U);
    }


    @Test
    public void allowsFullyQualifiedEntityNamesInQuery() {

        assertThat(detectAlias(FQ_QUERY), IS_U);
        assertCountQuery(FQ_QUERY,
                "select count(u) from org.synyx.hades.domain.User$Foo_Bar u");
    }


    private void assertCountQuery(String originalQuery, String countQuery) {

        assertThat(createCountQueryFor(originalQuery), is(countQuery));
    }
}
