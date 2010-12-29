/*
 * Copyright 2008-2010 the original author or authors.
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

import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.util.Assert;


/**
 * Abstract base class to implement {@link RepositoryQuery}s.
 * 
 * @author Oliver Gierke
 */
public abstract class AbstractJpaQuery implements RepositoryQuery {

    private final Parameters parameters;
    private final JpaQueryExecution execution;
    private final EntityManager em;


    /**
     * Creates a new {@link AbstractJpaQuery} from the given
     * {@link JpaQueryMethod}.
     * 
     * @param method
     * @param em
     */
    public AbstractJpaQuery(JpaQueryMethod method, EntityManager em) {

        Assert.notNull(method);
        Assert.notNull(em);

        this.parameters = method.getParameters();
        this.execution = method.getExecution();
        this.em = em;
    }


    /**
     * @return the parameters
     */
    public Parameters getParameters() {

        return parameters;
    }


    /**
     * @return the em
     */
    public EntityManager getEntityManager() {

        return em;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.query.RepositoryQuery#execute(java
     * .lang.Object[])
     */
    public Object execute(Object[] parameters) {

        return doExecute(execution, parameters);
    }


    protected abstract Object doExecute(JpaQueryExecution execution,
            Object[] parameters);

}