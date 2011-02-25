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
package org.springframework.data.jpa.repository.query;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.repository.support.EntityMetadata;


/**
 * A {@link AbstractJpaQuery} implementation based on a {@link PartTree}.
 * 
 * @author Oliver Gierke
 */
public class PartTreeJpaQuery extends AbstractJpaQuery {

    private final PartTree tree;
    private final QueryMethod method;


    /**
     * Creates a new {@link PartTreeJpaQuery}.
     * 
     * @param method
     * @param em
     */
    public PartTreeJpaQuery(JpaQueryMethod method, EntityManager em) {

        super(method, em);
        this.tree =
                new PartTree(method.getName(), method.getEntityMetadata()
                        .getJavaType());
        this.method = method;
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

        ParameterAccessor accessor =
                new ParametersParameterAccessor(getParameters(), parameters);

        EntityMetadata<?> metadata = method.getEntityMetadata();
        JpaQueryCreator jpaQueryCreator =
                new JpaQueryCreator(tree, accessor, metadata.getJavaType(),
                        getEntityManager());

        TypedQuery<Object> query =
                getEntityManager().createQuery(jpaQueryCreator.createQuery());

        if (getParameters().hasPageableParameter()) {
            Pageable pageable = accessor.getPageable();
            query.setFirstResult(pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
        }

        return query;
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.jpa.repository.query.AbstractJpaQuery#
     * createCountQuery(javax.persistence.EntityManager)
     */
    public Query createCountQuery(Object[] parameters) {

        CriteriaQuery<Object> query =
                new JpaCountQueryCreator(tree, new ParametersParameterAccessor(
                        getParameters(), parameters), method
                        .getEntityMetadata().getJavaType(), getEntityManager())
                        .createQuery();
        return getEntityManager().createQuery(query);
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
