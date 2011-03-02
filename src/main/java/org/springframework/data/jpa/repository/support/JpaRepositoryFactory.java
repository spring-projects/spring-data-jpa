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

import javax.persistence.EntityManager;

import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.repository.query.JpaQueryLookupStrategy;
import org.springframework.data.jpa.repository.query.QueryExtractor;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.support.EntityMetadata;
import org.springframework.data.repository.support.PersistableEntityMetadata;
import org.springframework.data.repository.support.RepositoryFactorySupport;
import org.springframework.data.repository.support.RepositoryMetadata;
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
    protected Object getTargetRepository(RepositoryMetadata metadata) {

        return getTargetRepository(metadata, entityManager);
    }


    /**
     * Callback to create a {@link RepositorySupport} instance with the given
     * {@link EntityManager}
     * 
     * @param <T>
     * @param <ID>
     * @param entityManager
     * @see #getTargetRepository(RepositoryMetadata)
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected Object getTargetRepository(RepositoryMetadata metadata,
            EntityManager entityManager) {

        return new SimpleJpaRepository(createEntityInformation(
                metadata.getDomainClass(), entityManager), entityManager);
    }


    /**
     * Creates a new {@link EntityMetadata} instance for the given domain class
     * and {@link EntityManager}. Default implementation will use a
     * {@link PersistableMetadata} for domain classes implementing
     * {@link Persistable} and fall back to the JPA meta model though
     * {@link JpaMetamodelEntityInformation} otherwise.
     * 
     * @param domainClass
     * @param em
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected EntityMetadata<?> createEntityInformation(Class<?> domainClass,
            EntityManager em) {

        if (Persistable.class.isAssignableFrom(domainClass)) {
            return new PersistableEntityMetadata(domainClass);
        } else {
            return new JpaMetamodelEntityMetadata(domainClass,
                    em.getMetamodel());
        }
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.support.RepositoryFactorySupport#
     * getRepositoryBaseClass()
     */
    @Override
    protected Class<?> getRepositoryBaseClass(Class<?> repositoryInterface) {

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
