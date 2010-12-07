package org.springframework.data.jpa.repository.query;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.SimpleParameterAccessor;
import org.springframework.data.repository.query.parser.PartTree;


/**
 * Special {@link JpaQueryCreator} that creates a count projecting query.
 * 
 * @author Oliver Gierke
 */
public class JpaCountQueryCreator extends JpaQueryCreator {

    private final Class<?> domainClass;


    /**
     * Creates a new {@link JpaCountQueryCreator}.
     * 
     * @param tree
     * @param parameters
     * @param domainClass
     * @param em
     */
    public JpaCountQueryCreator(PartTree tree,
            SimpleParameterAccessor parameters, Class<?> domainClass,
            EntityManager em) {

        super(tree, parameters, domainClass, em);
        this.domainClass = domainClass;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.jpa.repository.query.JpaQueryCreator#finalize
     * (javax.persistence.criteria.Predicate,
     * org.springframework.data.domain.Sort,
     * javax.persistence.criteria.CriteriaQuery,
     * javax.persistence.criteria.CriteriaBuilder)
     */
    @Override
    protected CriteriaQuery<Object> finalize(Predicate predicate, Sort sort,
            CriteriaQuery<Object> query, CriteriaBuilder builder) {

        return query.select(builder.count(query.from(domainClass)));
    }
}
