/*
 * Copyright 2008-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.data.jpa.repository.query;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.QueryHint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.CollectionExecution;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.data.repository.query.QueryMethod;


/**
 * Unit test for {@link QueryMethod}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class JpaQueryMethodUnitTests {

    static final Class<?> DOMAIN_CLASS = User.class;
    static final String METHOD_NAME = "findByFirstname";

    @Mock
    QueryExtractor extractor;
    @Mock
    EntityManager em;

    Method daoMethod, invalidReturnType, pageableAndSort, pageableTwice,
            sortableTwice, modifyingMethod;


    /**
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {

        daoMethod =
                UserRepository.class.getMethod("findByLastname", String.class);

        invalidReturnType =
                InvalidDao.class.getMethod(METHOD_NAME, String.class,
                        Pageable.class);
        pageableAndSort =
                InvalidDao.class.getMethod(METHOD_NAME, String.class,
                        Pageable.class, Sort.class);
        pageableTwice =
                InvalidDao.class.getMethod(METHOD_NAME, String.class,
                        Pageable.class, Pageable.class);

        sortableTwice =
                InvalidDao.class.getMethod(METHOD_NAME, String.class,
                        Sort.class, Sort.class);
        modifyingMethod =
                UserRepository.class
                        .getMethod("renameAllUsersTo", String.class);
    }


    @Test
    public void testname() {

        JpaQueryMethod method = new JpaQueryMethod(daoMethod, extractor, em);

        assertEquals("User.findByLastname", method.getNamedQueryName());
        assertThat(method.getExecution(), is(CollectionExecution.class));
        assertEquals("select x from User x where x.lastname = ?1",
                new QueryCreator(method).constructQuery());
    }


    @Test(expected = IllegalArgumentException.class)
    public void preventsNullDaoMethod() {

        new JpaQueryMethod(null, extractor, em);
    }


    @Test(expected = IllegalArgumentException.class)
    public void preventsNullEntityManager() {

        new JpaQueryMethod(daoMethod, extractor, null);
    }


    @Test(expected = IllegalArgumentException.class)
    public void preventsNullQueryExtractor() {

        new JpaQueryMethod(daoMethod, null, em);
    }


    @Test
    public void returnsCorrectName() {

        JpaQueryMethod method = new JpaQueryMethod(daoMethod, extractor, em);
        assertEquals(daoMethod.getName(), method.getName());
    }


    @Test
    public void returnsQueryIfAvailable() throws Exception {

        JpaQueryMethod method = new JpaQueryMethod(daoMethod, extractor, em);

        assertNull(method.getAnnotatedQuery());

        Method daoMethod =
                UserRepository.class
                        .getMethod("findByHadesQuery", String.class);

        assertNotNull(new JpaQueryMethod(daoMethod, extractor, em)
                .getAnnotatedQuery());
    }


    @Test
    public void returnsCorrectDomainClassName() {

        JpaQueryMethod method = new JpaQueryMethod(daoMethod, extractor, em);
        assertEquals(DOMAIN_CLASS, method.getDomainClass());
    }


    @Test
    public void returnsCorrectNumberOfParameters() {

        JpaQueryMethod method = new JpaQueryMethod(daoMethod, extractor, em);
        assertTrue(method.isCorrectNumberOfParameters(daoMethod
                .getParameterTypes().length));
    }


    @Test(expected = IllegalStateException.class)
    public void rejectsInvalidReturntypeOnPagebleFinder() {

        new JpaQueryMethod(invalidReturnType, extractor, em);
    }


    @Test(expected = IllegalStateException.class)
    public void rejectsPageableAndSortInFinderMethod() {

        new JpaQueryMethod(pageableAndSort, extractor, em);
    }


    @Test(expected = IllegalStateException.class)
    public void rejectsTwoPageableParameters() {

        new JpaQueryMethod(pageableTwice, extractor, em);
    }


    @Test(expected = IllegalStateException.class)
    public void rejectsTwoSortableParameters() {

        new JpaQueryMethod(sortableTwice, extractor, em);
    }


    @Test(expected = IllegalArgumentException.class)
    public void rejectsPageablesOnPersistenceProvidersNotExtractingQueries()
            throws Exception {

        Method method =
                UserRepository.class.getMethod("findByFirstname",
                        Pageable.class, String.class);

        when(extractor.canExtractQuery()).thenReturn(false);

        new JpaQueryMethod(method, extractor, em);
    }


    @Test
    public void recognizesModifyingMethod() {

        JpaQueryMethod method =
                new JpaQueryMethod(modifyingMethod, extractor, em);
        assertTrue(method.isModifyingQuery());
    }


    @Test(expected = IllegalArgumentException.class)
    public void rejectsModifyingMethodWithPageable() throws Exception {

        Method method =
                InvalidDao.class.getMethod("updateMethod", String.class,
                        Pageable.class);

        new JpaQueryMethod(method, extractor, em);
    }


    @Test(expected = IllegalArgumentException.class)
    public void rejectsModifyingMethodWithSort() throws Exception {

        Method method =
                InvalidDao.class.getMethod("updateMethod", String.class,
                        Sort.class);

        new JpaQueryMethod(method, extractor, em);
    }


    @Test
    public void discoversHintsCorrectly() {

        JpaQueryMethod method = new JpaQueryMethod(daoMethod, extractor, em);
        List<QueryHint> hints = method.getHints();

        assertNotNull(hints);
        assertThat(hints.get(0).name(), is("foo"));
        assertThat(hints.get(0).value(), is("bar"));
    }

    /**
     * Interface to define invalid DAO methods for testing.
     * 
     * @author Oliver Gierke
     */
    static interface InvalidDao {

        // Invalid return type
        User findByFirstname(String firstname, Pageable pageable);


        // Should not use Pageable *and* Sort
        Page<User> findByFirstname(String firstname, Pageable pageable,
                Sort sort);


        // Must not use two Pageables
        Page<User> findByFirstname(String firstname, Pageable first,
                Pageable second);


        // Must not use two Pageables
        Page<User> findByFirstname(String firstname, Sort first, Sort second);


        // Not backed by a named query or @Query annotation
        @Modifying
        void updateMethod(String firstname);


        // Modifying and Pageable is not allowed
        @Modifying
        Page<String> updateMethod(String firstname, Pageable pageable);


        // Modifying and Sort is not allowed
        @Modifying
        void updateMethod(String firstname, Sort sort);
    }
}
