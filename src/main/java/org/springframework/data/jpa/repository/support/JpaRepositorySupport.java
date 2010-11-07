package org.springframework.data.jpa.repository.support;

import java.io.Serializable;

import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.support.IsNewAware;
import org.springframework.data.repository.support.PersistableEntityInformation;
import org.springframework.data.repository.support.RepositorySupport;


/**
 * Base class to implement JPA CRUD repository implementations. Will use a
 * {@link JpaAnnotationEntityInformation} by default but prefer a
 * {@link PersistableEntityInformation} in case the type to be handled
 * implements {@link Persistable}.
 * 
 * @author Oliver Gierke
 */
public abstract class JpaRepositorySupport<T, ID extends Serializable> extends
        RepositorySupport<T, ID> implements JpaRepository<T, ID> {

    /**
     * @param domainClass
     */
    public JpaRepositorySupport(Class<T> domainClass) {

        super(domainClass);
    }


    @Override
    protected IsNewAware createIsNewStrategy(Class<?> domainClass) {

        if (Persistable.class.isAssignableFrom(domainClass)) {
            return new PersistableEntityInformation();
        } else {
            return new JpaAnnotationEntityInformation(domainClass);
        }
    }
}