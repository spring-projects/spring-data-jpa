package org.springframework.data.jpa.repository.support;

import java.io.Serializable;
import java.lang.reflect.Method;

import javax.persistence.EntityManager;

import org.springframework.data.jpa.repository.query.JpaQueryLookupStrategy;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
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
public class JpaRepositoryFactory extends
        RepositoryFactorySupport<JpaQueryMethod> {

    private final EntityManager entityManager;


    /**
     * Creates a new {@link JpaRepositoryFactory}.
     * 
     * @param entityManager must not be {@literal null}
     */
    public JpaRepositoryFactory(EntityManager entityManager) {

        Assert.notNull(entityManager);
        this.entityManager = entityManager;
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
            Class<T> domainClass) {

        return getTargetRepository(domainClass, entityManager);
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
            Class<T> domainClass, EntityManager entityManager) {

        return new SimpleJpaRepository<T, ID>(domainClass, entityManager);
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.support.RepositoryFactorySupport#
     * getQueryMethod(java.lang.reflect.Method)
     */
    @Override
    protected JpaQueryMethod getQueryMethod(Method method) {

        QueryExtractor extractor =
                PersistenceProvider.fromEntityManager(entityManager);
        return new JpaQueryMethod(method, extractor, entityManager);
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.support.RepositoryFactorySupport#
     * getRepositoryClass()
     */
    @Override
    @SuppressWarnings("rawtypes")
    protected Class<? extends RepositorySupport> getRepositoryClass() {

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
    protected QueryLookupStrategy<JpaQueryMethod> getQueryLookupStrategy(Key key) {

        return JpaQueryLookupStrategy.create(entityManager, key);
    }
}
