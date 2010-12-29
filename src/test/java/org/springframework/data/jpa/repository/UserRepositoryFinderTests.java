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

package org.springframework.data.jpa.repository;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;


/**
 * Integration test for executing finders, thus testing various query lookup
 * strategies.
 * 
 * @see QueryLookupStrategy
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:config/namespace-application-context.xml")
@Transactional
public class UserRepositoryFinderTests {

    @Autowired
    UserRepository userRepository;

    User firstUser, secondUser;


    @Before
    public void setUp() {

        // This one matches both criterias
        firstUser = new User();
        firstUser.setEmailAddress("foo");
        firstUser.setLastname("bar");
        firstUser.setFirstname("foobar");

        userRepository.save(firstUser);

        // This one matches only the second one
        secondUser = new User();
        secondUser.setEmailAddress("bar");
        secondUser.setLastname("foo");

        userRepository.save(secondUser);
    }


    /**
     * Tests creation of a simple query.
     */
    @Test
    public void testSimpleCustomCreatedFinder() {

        User user = userRepository.findByEmailAddressAndLastname("foo", "bar");
        assertEquals(firstUser, user);
    }


    /**
     * Tests that the repository returns {@code null} for not found objects for
     * finder methods that return a single domain object.
     */
    @Test
    public void returnsNullIfNothingFound() {

        User user = userRepository.findByEmailAddress("foobar");
        assertEquals(null, user);
    }


    /**
     * Tests creation of a simple query consisting of {@code AND} and {@code OR}
     * parts.
     */
    @Test
    public void testAndOrFinder() {

        List<User> users =
                userRepository.findByEmailAddressAndLastnameOrFirstname("bar",
                        "foo", "foobar");

        assertNotNull(users);
        assertEquals(2, users.size());
        assertTrue(users.contains(firstUser));
        assertTrue(users.contains(secondUser));
    }


    @Test
    public void executesPagingMethodToPageCorrectly() throws Exception {

        Page<User> page =
                userRepository
                        .findByFirstname(new PageRequest(0, 20), "foobar");
        assertEquals(1, page.getNumberOfElements());
    }


    @Test
    public void executesPagingMethodToListCorrectly() throws Exception {

        List<User> list =
                userRepository
                        .findByFirstname("foobar", new PageRequest(0, 20));
        assertThat(list.size(), is(1));
    }
}
