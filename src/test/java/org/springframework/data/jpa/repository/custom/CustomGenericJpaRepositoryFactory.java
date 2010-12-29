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
package org.springframework.data.jpa.repository.custom;

import java.io.Serializable;

import javax.persistence.EntityManager;

import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.support.RepositorySupport;


/**
 * Sample implementation of a custom {@link JpaRepositoryFactory} to use a
 * custom repository base class.
 * 
 * @author Oliver Gierke
 */
public class CustomGenericJpaRepositoryFactory extends JpaRepositoryFactory {

    /**
     * @param entityManager
     */
    public CustomGenericJpaRepositoryFactory(EntityManager entityManager) {

        super(entityManager);
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.jpa.repository.support.GenericJpaRepositoryFactory
     * #getTargetRepository(java.lang.Class, javax.persistence.EntityManager)
     */
    @Override
    protected <T, ID extends Serializable> RepositorySupport<T, ID> getTargetRepository(
            Class<T> domainClass, Class<?> repositoryInterface, EntityManager em) {

        return new CustomGenericJpaRepository<T, ID>(domainClass, em);
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.jpa.repository.support.GenericJpaRepositoryFactory
     * #getRepositoryClass()
     */
    @Override
    @SuppressWarnings("rawtypes")
    protected Class<? extends RepositorySupport> getRepositoryClass(
            Class<?> repositoryInterface) {

        return CustomGenericJpaRepository.class;
    }
}
