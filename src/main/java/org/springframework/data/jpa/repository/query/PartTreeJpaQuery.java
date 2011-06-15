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

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;

import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.parser.PartTree;


/**
 * A {@link AbstractJpaQuery} implementation based on a {@link PartTree}.
 * 
 * @author Oliver Gierke
 */
public class PartTreeJpaQuery extends AbstractJpaQuery {

    private final Class<?> domainClass;
    private final PartTree tree;
    private final Parameters parameters;


    /**
     * Creates a new {@link PartTreeJpaQuery}.
     * 
     * @param method
     * @param em
     */
    public PartTreeJpaQuery(JpaQueryMethod method, EntityManager em) {

        super(method, em);

        this.domainClass = method.getEntityInformation().getJavaType();
        this.tree = new PartTree(method.getName(), domainClass);
        this.parameters = method.getParameters();
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.jpa.repository.query.AbstractJpaQuery#createQuery
     * (javax.persistence.EntityManager,
     * org.springframework.data.jpa.repository.query.ParameterBinder)
     */
    @Override
    public Query createQuery(Object[] values) {

        ParameterAccessor accessor =
                new ParametersParameterAccessor(parameters, values);
        JpaQueryCreator creator =
                new JpaQueryCreator(tree, domainClass, accessor, parameters,
                        getEntityManager());
        CriteriaQuery<?> source = creator.createQuery();

        TypedQuery<?> jpaQuery = getEntityManager().createQuery(source);
        getBinder(values, creator.getParameterExpressions()).bindAndPrepare(
                jpaQuery);

        return jpaQuery;
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.jpa.repository.query.AbstractJpaQuery#
     * createCountQuery(javax.persistence.EntityManager)
     */
    @Override
    public Query createCountQuery(Object[] values) {

        ParameterAccessor accessor =
                new ParametersParameterAccessor(parameters, values);

        JpaCountQueryCreator creator =
                new JpaCountQueryCreator(tree, domainClass, accessor,
                        parameters, getEntityManager());
        CriteriaQuery<?> source = creator.createQuery();

        TypedQuery<?> jpaQuery = getEntityManager().createQuery(source);
        getBinder(values, creator.getParameterExpressions()).bind(jpaQuery);

        return jpaQuery;
    }


    private ParameterBinder getBinder(Object[] values,
            List<ParameterExpression<?>> expressions) {

        return new CriteriaQueryParameterBinder(parameters, values, expressions);
    }
}
