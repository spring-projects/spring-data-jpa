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

import java.lang.reflect.Method;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.SimpleParameterAccessor;
import org.springframework.util.Assert;


/**
 * Set of classes to contain query execution strategies. Depending (mostly) on
 * the return type of a {@link QueryMethod} a
 * {@link AbstractStringBasedJpaQuery} can be executed in various flavours.
 * 
 * @author Oliver Gierke
 */
public abstract class JpaQueryExecution {

    /**
     * Executes the given {@link AbstractStringBasedJpaQuery} with the given
     * {@link ParameterBinder}.
     * 
     * @param query
     * @param binder
     * @return
     */

    public Object execute(AbstractStringBasedJpaQuery query,
            ParameterBinder binder) {

        Assert.notNull(query);
        Assert.notNull(binder);

        try {
            return doExecute(query, binder);
        } catch (NoResultException e) {
            return null;
        }
    }


    public Object execute(PartTreeJpaQuery query, Object[] parameters) {

        Assert.notNull(query);
        Assert.notNull(parameters);

        try {
            return doExecute(query, parameters);
        } catch (NoResultException e) {
            return null;
        }
    }


    /**
     * Method to implement {@link AbstractStringBasedJpaQuery} executions by
     * single enum values.
     * 
     * @param query
     * @param binder
     * @return
     */
    protected abstract Object doExecute(AbstractStringBasedJpaQuery query,
            ParameterBinder parameters);


    protected abstract Object doExecute(PartTreeJpaQuery query,
            Object[] parameters);

    /**
     * Executes the {@link AbstractStringBasedJpaQuery} to return a simple
     * collection of entities.
     */
    static class CollectionExecution extends JpaQueryExecution {

        @Override
        protected Object doExecute(AbstractStringBasedJpaQuery query,
                ParameterBinder binder) {

            return binder.bindAndPrepare(query.createQuery(binder))
                    .getResultList();
        }


        /*
         * (non-Javadoc)
         * 
         * @see
         * org.springframework.data.jpa.repository.query.JpaQueryExecution#doExecute
         * (org.springframework.data.jpa.repository.query.DerivedJpaQuery)
         */
        @Override
        protected Object doExecute(PartTreeJpaQuery query, Object[] parameters) {

            return query.createQuery(parameters).getResultList();
        }
    }

    /**
     * Executes the {@link AbstractStringBasedJpaQuery} to return a {@link Page}
     * of entities.
     */
    static class PagedExecution extends JpaQueryExecution {

        private final Parameters parameters;


        public PagedExecution(Parameters parameters) {

            this.parameters = parameters;
        }


        @Override
        @SuppressWarnings("unchecked")
        protected Object doExecute(AbstractStringBasedJpaQuery repositoryQuery,
                ParameterBinder binder) {

            // Execute query to compute total
            Query projection =
                    binder.bind(repositoryQuery.createCountQuery(binder));
            Long total = (Long) projection.getSingleResult();

            Query query =
                    binder.bindAndPrepare(repositoryQuery.createQuery(binder));

            return new PageImpl<Object>(query.getResultList(),
                    binder.getPageable(), total);
        }


        /*
         * (non-Javadoc)
         * 
         * @see
         * org.springframework.data.jpa.repository.query.JpaQueryExecution#doExecute
         * (org.springframework.data.jpa.repository.query.DerivedJpaQuery,
         * java.lang.Object[])
         */
        @Override
        @SuppressWarnings("unchecked")
        protected Object doExecute(PartTreeJpaQuery query, Object[] parameters) {

            SimpleParameterAccessor accessor =
                    new SimpleParameterAccessor(this.parameters, parameters);

            Query countQuery = query.createCountQuery(parameters);
            Long total = (Long) countQuery.getSingleResult();

            Query jpaQuery = query.createQuery(parameters);
            return new PageImpl<Object>(jpaQuery.getResultList(),
                    accessor.getPageable(), total);
        }
    }

    /**
     * Executes a {@link AbstractStringBasedJpaQuery} to return a single entity.
     */
    static class SingleEntityExecution extends JpaQueryExecution {

        @Override
        protected Object doExecute(AbstractStringBasedJpaQuery query,
                ParameterBinder binder) {

            return binder.bind(query.createQuery(binder)).getSingleResult();
        }


        /*
         * (non-Javadoc)
         * 
         * @see
         * org.springframework.data.jpa.repository.query.JpaQueryExecution#doExecute
         * (org.springframework.data.jpa.repository.query.DerivedJpaQuery,
         * java.lang.Object[])
         */
        @Override
        protected Object doExecute(PartTreeJpaQuery query, Object[] parameters) {

            return query.createQuery(parameters).getSingleResult();
        }
    }

    /**
     * Executes a modifying query such as an update, insert or delete.
     */
    static class ModifyingExecution extends JpaQueryExecution {

        private final EntityManager em;


        /**
         * Creates an execution that automatically clears the given
         * {@link EntityManager} after execution if the given
         * {@link EntityManager} is not {@literal null}.
         * 
         * @param em
         */
        public ModifyingExecution(Method method, EntityManager em) {

            Class<?> type = method.getReturnType();

            boolean isVoid = void.class.equals(type) || Void.class.equals(type);
            boolean isInt =
                    int.class.equals(type) || Integer.class.equals(type);

            Assert.isTrue(isInt || isVoid,
                    "Modifying queries can only use void or int/Integer as return type!");

            this.em = em;
        }


        /*
         * (non-Javadoc)
         * 
         * @see
         * org.springframework.data.repository.query.QueryExecution#doExecute
         * (org.springframework.data.repository.query.AbstractRepositoryQuery,
         * org.springframework.data.repository.query.ParameterBinder)
         */
        @Override
        protected Object doExecute(AbstractStringBasedJpaQuery query,
                ParameterBinder binder) {

            int result = binder.bind(query.createQuery(binder)).executeUpdate();

            if (em != null) {
                em.clear();
            }

            return result;
        }


        @Override
        protected Object doExecute(PartTreeJpaQuery query, Object[] parameters) {

            throw new UnsupportedOperationException();
        }
    }
}