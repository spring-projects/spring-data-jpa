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
package org.springframework.data.jpa.repository.support;

import java.io.Serializable;

import javax.persistence.EntityManager;

import org.springframework.data.jpa.repository.query.JpaQueryLookupStrategy;
import org.springframework.data.jpa.repository.query.QueryExtractor;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.support.RepositoryFactorySupport;
import org.springframework.data.repository.support.RepositorySupport;
import org.springframework.util.Assert;


/**
 * JPA specific generic repository factory.
 * 
 * @author Oliver Gierke
 */
public class JpaRepositoryFactory extends RepositoryFactorySupport {

    private final EntityManager entityManager;
    private final QueryExtractor extractor;


    /**
     * Creates a new {@link JpaRepositoryFactory}.
     * 
     * @param entityManager must not be {@literal null}
     */
    public JpaRepositoryFactory(EntityManager entityManager) {

        Assert.notNull(entityManager);
        this.entityManager = entityManager;
        this.extractor = PersistenceProvider.fromEntityManager(entityManager);
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.support.RepositoryFactorySupport#
     * getTargetRepository(java.lang.Class)
     */
    @Override
    protected <T, ID extends Serializable> RepositorySupport<T, ID> getTargetRepository(
            Class<T> domainClass, Class<?> repositoryInterface) {

        return getTargetRepository(domainClass, repositoryInterface,
                entityManager);
    }


    /**
     * Callback to create a {@link RepositorySupport} instance with the given
     * {@link EntityManager}
     * 
     * @param <T>
     * @param <ID>
     * @param domainClass
     * @param entityManager
     * @see #getTargetRepository(Class)
     * @return
     */
    protected <T, ID extends Serializable> RepositorySupport<T, ID> getTargetRepository(
            Class<T> domainClass, Class<?> repositoryInterface,
            EntityManager entityManager) {

        return new SimpleJpaRepository<T, ID>(domainClass, entityManager);
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.support.RepositoryFactorySupport#
     * getRepositoryClass(java.lang.Class)
     */
    @Override
    @SuppressWarnings("rawtypes")
    protected Class<? extends RepositorySupport> getRepositoryClass(
            Class<?> repositoryInterface) {

        return SimpleJpaRepository.class;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.support.RepositoryFactorySupport#
     * getQueryLookupStrategy
     * (org.springframework.data.repository.query.QueryLookupStrategy.Key)
     */
    @Override
    protected QueryLookupStrategy getQueryLookupStrategy(Key key) {

        return JpaQueryLookupStrategy.create(entityManager, key, extractor);
    }
}
