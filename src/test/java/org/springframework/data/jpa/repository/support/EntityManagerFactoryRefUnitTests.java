/*
 * Copyright 2008-2011 the original author or authors.
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
package org.springframework.data.jpa.repository.support;

import static org.mockito.Mockito.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.ejb.HibernateEntityManager;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.jpa.repository.config.AbstractRepositoryConfigTests;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.test.context.ContextConfiguration;


/**
 * Assures the injected repository instances are wired to the customly
 * configured {@link EntityManagerFactory}.
 * 
 * @author Oliver Gierke
 */
@ContextConfiguration(locations = "classpath:multiple-entity-manager-context.xml")
public class EntityManagerFactoryRefUnitTests extends
        AbstractRepositoryConfigTests {

    @Autowired
    @Qualifier("entityManagerFactory")
    EntityManagerFactory first;

    @Autowired
    @Qualifier("secondEntityManagerFactory")
    EntityManagerFactory second;


    @Test
    public void repositoriesGetTheSecondEntityManagerFactoryInjected()
            throws Exception {

        verify(first, never()).createEntityManager();
        verify(second, atLeastOnce()).createEntityManager();
    }

    /**
     * A simple No-Op {@link PersistenceExceptionTranslator} to be configured in
     * the test case's config file as it is required.
     * 
     * @author Oliver Gierke
     */
    static class NoOpPersistenceExceptionTranslator implements
            PersistenceExceptionTranslator {

        public DataAccessException translateExceptionIfPossible(
                RuntimeException ex) {

            return null;
        }
    }

    /**
     * {@link BeanPostProcessor} to configure the mock
     * {@link EntityManagerFactory} instances. {@code entityManagerFactory} is
     * configured to be never invoked, {@code secondEntityManagerFactory} is
     * configured to be invoked at least once.
     * 
     * @author Oliver Gierke
     */
    static class MockPreparingBeanPostProcessor implements BeanPostProcessor {

        public Object postProcessAfterInitialization(Object bean,
                String beanName) throws BeansException {

            if ("secondEntityManagerFactory".equals(beanName)) {

                EntityManagerFactory entityManagerFactory =
                        (EntityManagerFactory) bean;
                EntityManager em = mock(HibernateEntityManager.class);
                when(entityManagerFactory.createEntityManager()).thenReturn(em);

                EntityManagerFactoryInfo info = (EntityManagerFactoryInfo) bean;
                when(info.getEntityManagerInterface()).thenAnswer(
                        new Answer<Class<?>>() {

                            public Class<?> answer(InvocationOnMock invocation)
                                    throws Throwable {

                                return HibernateEntityManager.class;
                            }
                        });
            }

            return bean;
        }


        public Object postProcessBeforeInitialization(Object bean,
                String beanName) throws BeansException {

            return bean;
        }
    }
}
