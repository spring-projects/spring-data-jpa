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

import org.springframework.data.domain.Sort;
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

    private final QueryPreparer query;
    private final QueryPreparer countQuery;


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

        this.query = new QueryPreparer();
        this.countQuery = new CountQueryPreparer();
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

        return query.createQuery(values);
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.jpa.repository.query.AbstractJpaQuery#
     * createCountQuery(javax.persistence.EntityManager)
     */
    @Override
    public Query createCountQuery(Object[] values) {

        return countQuery.createQuery(values);
    }

    /**
     * Query preparer to create {@link CriteriaQuery} instances and potentially
     * cache them.
     * 
     * @author Oliver Gierke
     */
    private class QueryPreparer {

        private CriteriaQuery<?> query;
        private JpaQueryCreator creator;


        /**
         * Creates a new {@link Query} for the given parameter values.
         * 
         * @param values
         * @return
         */
        public Query createQuery(Object[] values) {

            if (parameters.potentiallySortsDynamically() || query == null
                    || creator == null) {
                creator = createCreator();
                query = creator.createQuery(getDynamicSort(values));
            }

            TypedQuery<?> jpaQuery = getEntityManager().createQuery(query);
            return invokeBinding(
                    getBinder(values, creator.getParameterExpressions()),
                    jpaQuery);
        }


        protected JpaQueryCreator createCreator() {

            return new JpaQueryCreator(tree, domainClass, parameters,
                    getEntityManager());
        }


        /**
         * Invokes parameter binding on the given {@link TypedQuery}.
         * 
         * @param binder
         * @param query
         * @return
         */
        protected Query invokeBinding(ParameterBinder binder,
                TypedQuery<?> query) {

            return binder.bindAndPrepare(query);
        }


        private ParameterBinder getBinder(Object[] values,
                List<ParameterExpression<?>> expressions) {

            return new CriteriaQueryParameterBinder(parameters, values,
                    expressions);
        }


        private Sort getDynamicSort(Object[] values) {

            return parameters.hasSortParameter() ? new ParametersParameterAccessor(
                    parameters, values).getSort() : null;
        }
    }

    /**
     * Special {@link QueryPreparer} to create count queries.
     * 
     * @author Oliver Gierke
     */
    private class CountQueryPreparer extends QueryPreparer {

        /*
         * (non-Javadoc)
         * 
         * @see org.springframework.data.jpa.repository.query.PartTreeJpaQuery.
         * QueryPreparer#createCreator()
         */
        @Override
        protected JpaQueryCreator createCreator() {

            return new JpaCountQueryCreator(tree, domainClass, parameters,
                    getEntityManager());
        }


        /**
         * Customizes binding by skipping the pagination.
         * 
         * @see org.springframework.data.jpa.repository.query.PartTreeJpaQuery.QueryPreparer#invokeBinding(org.springframework.data.jpa.repository.query.ParameterBinder,
         *      javax.persistence.TypedQuery)
         */
        protected Query invokeBinding(ParameterBinder binder,
                javax.persistence.TypedQuery<?> query) {

            return binder.bind(query);
        }
    }
}
