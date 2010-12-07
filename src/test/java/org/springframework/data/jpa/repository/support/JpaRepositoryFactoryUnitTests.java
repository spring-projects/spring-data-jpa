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

package org.springframework.data.jpa.repository.support;

import static junit.framework.Assert.*;

import java.io.IOException;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.custom.CustomGenericJpaRepositoryFactory;
import org.springframework.data.jpa.repository.custom.UserCustomExtendedRepository;
import org.springframework.transaction.annotation.Transactional;


/**
 * Unit test for {@code GenericDaoFactoryBean}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class JpaRepositoryFactoryUnitTests {

    JpaRepositoryFactory factory;

    @Mock
    EntityManager entityManager;


    @Before
    public void setUp() {

        // Setup standard factory configuration
        factory = new JpaRepositoryFactory(entityManager);
    }


    /**
     * Assert that the instance created for the standard configuration is a
     * valid {@code UserDao}.
     * 
     * @throws Exception
     */
    @Test
    public void setsUpBasicInstanceCorrectly() throws Exception {

        assertNotNull(factory.getRepository(SimpleSampleDao.class));
    }


    @Test
    public void allowsCallingOfObjectMethods() {

        SimpleSampleDao userDao = factory.getRepository(SimpleSampleDao.class);

        userDao.hashCode();
        userDao.toString();
        userDao.equals(userDao);
    }


    /**
     * Asserts that the factory recognized configured DAO classes that contain
     * custom method but no custom implementation could be found. Furthremore
     * the exception has to contain the name of the DAO interface as for a large
     * DAO configuration it's hard to find out where this error occured.
     * 
     * @throws Exception
     */
    @Test
    public void capturesMissingCustomImplementationAndProvidesInterfacename()
            throws Exception {

        try {
            factory.getRepository(SampleDao.class);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains(SampleDao.class.getName()));
        }
    }


    @Test(expected = IllegalArgumentException.class)
    public void handlesRuntimeExceptionsCorrectly() {

        SampleDao dao =
                factory.getRepository(SampleDao.class,
                        new SampleCustomDaoImpl());
        dao.throwingRuntimeException();
    }


    @Test(expected = IOException.class)
    public void handlesCheckedExceptionsCorrectly() throws Exception {

        SampleDao dao =
                factory.getRepository(SampleDao.class,
                        new SampleCustomDaoImpl());
        dao.throwingCheckedException();
    }


    @Test(expected = UnsupportedOperationException.class)
    public void createsProxyWithCustomBaseClass() throws Exception {

        JpaRepositoryFactory factory =
                new CustomGenericJpaRepositoryFactory(entityManager);
        UserCustomExtendedRepository dao =
                factory.getRepository(UserCustomExtendedRepository.class);

        dao.customMethod(1);
    }

    private interface SimpleSampleDao extends JpaRepository<User, Integer> {

        @Transactional
        User readByPrimaryKey(Integer primaryKey);
    }

    /**
     * Sample interface to contain a custom method.
     * 
     * @author Oliver Gierke
     */
    public interface SampleCustomDao {

        void throwingRuntimeException();


        void throwingCheckedException() throws IOException;
    }

    /**
     * Implementation of the custom DAO interface.
     * 
     * @author Oliver Gierke
     */
    private class SampleCustomDaoImpl implements SampleCustomDao {

        public void throwingRuntimeException() {

            throw new IllegalArgumentException("You lose!");
        }


        public void throwingCheckedException() throws IOException {

            throw new IOException("You lose!");
        }
    }

    private interface SampleDao extends JpaRepository<User, Integer>,
            SampleCustomDao {

    }
}
