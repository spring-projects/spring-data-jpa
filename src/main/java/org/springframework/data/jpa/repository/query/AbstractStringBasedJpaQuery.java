package org.springframework.data.jpa.repository.query;

import javax.persistence.EntityManager;
import javax.persistence.Query;


/**
 * Base class for {@link String} based JPA queries.
 * 
 * @author Oliver Gierke
 */
public abstract class AbstractStringBasedJpaQuery extends AbstractJpaQuery {

    /**
     * Creates a new {@link AbstractStringBasedJpaQuery}.
     * 
     * @param method
     * @param em
     */
    public AbstractStringBasedJpaQuery(JpaQueryMethod method, EntityManager em) {

        super(method, em);
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.jpa.repository.query.AbstractJpaQuery#doExecute
     * (org.springframework.data.jpa.repository.query.JpaQueryExecution,
     * java.lang.Object[])
     */
    @Override
    protected Object doExecute(JpaQueryExecution execution, Object[] parameters) {

        ParameterBinder binder =
                new ParameterBinder(getParameters(), parameters);
        return execution.execute(this, binder);
    }


    /**
     * Create a {@link Query} with the given {@link ParameterBinder}.
     * 
     * @param binder
     * @return
     */
    protected abstract Query createQuery(ParameterBinder binder);


    /**
     * Create a count {@link Query} with the given {@link ParameterBinder}.
     * 
     * @param binder
     * @return
     */
    protected abstract Query createCountQuery(ParameterBinder binder);
}
