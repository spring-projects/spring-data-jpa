package org.springframework.data.jpa.repository.query;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaQuery;

import org.springframework.data.repository.query.SimpleParameterAccessor;
import org.springframework.data.repository.query.parser.PartTree;


/**
 * A {@link AbstractJpaQuery} implementation based on a {@link PartTree}.
 * 
 * @author Oliver Gierke
 */
public class PartTreeJpaQuery extends AbstractJpaQuery {

    private final PartTree tree;
    private final Class<?> domainClass;


    /**
     * Creates a new {@link PartTreeJpaQuery}.
     * 
     * @param method
     * @param em
     */
    public PartTreeJpaQuery(JpaQueryMethod method, EntityManager em) {

        super(method, em);

        this.tree = new PartTree(method.getName(), method.getDomainClass());
        this.domainClass = method.getDomainClass();
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.jpa.repository.query.AbstractJpaQuery#createQuery
     * (javax.persistence.EntityManager,
     * org.springframework.data.jpa.repository.query.ParameterBinder)
     */
    public Query createQuery(Object[] parameters) {

        JpaQueryCreator jpaQueryCreator =
                new JpaQueryCreator(tree, new SimpleParameterAccessor(
                        getParameters(), parameters), domainClass,
                        getEntityManager());

        return getEntityManager().createQuery(jpaQueryCreator.createQuery());
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.jpa.repository.query.AbstractJpaQuery#
     * createCountQuery(javax.persistence.EntityManager)
     */
    public Query createCountQuery(Object[] parameters) {

        CriteriaQuery<Object> createQuery =
                new JpaCountQueryCreator(tree, new SimpleParameterAccessor(
                        getParameters(), parameters), domainClass,
                        getEntityManager()).createQuery();
        return getEntityManager().createQuery(createQuery);
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

        return execution.execute(this, parameters);
    }
}
