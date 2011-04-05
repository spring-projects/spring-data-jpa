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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.ModifyingExecution;


/**
 * Unit test for {@link QueryExecution}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class JpaQueryExecutionUnitTests {

    @Mock
    EntityManager em;
    @Mock
    AbstractStringBasedJpaQuery jpaQuery;
    @Mock
    ParameterBinder binder;
    @Mock
    Query query;
    @Mock
    JpaQueryMethod method;


    @Test(expected = IllegalArgumentException.class)
    public void rejectsNullQuery() {

        new StubQueryExecution().execute(null, binder);
    }


    @Test(expected = IllegalArgumentException.class)
    public void rejectsNullBinder() throws Exception {

        new StubQueryExecution().execute(jpaQuery, null);
    }


    @Test
    public void transformsNoResultExceptionToNull() {

        assertThat(new JpaQueryExecution() {

            @Override
            protected Object doExecute(AbstractStringBasedJpaQuery query,
                    ParameterBinder binder) {

                throw new NoResultException();
            }


            /*
             * (non-Javadoc)
             * 
             * @see
             * org.springframework.data.jpa.repository.query.JpaQueryExecution
             * #doExecute
             * (org.springframework.data.jpa.repository.query.PartTreeJpaQuery,
             * java.lang.Object[])
             */
            @Override
            protected Object doExecute(PartTreeJpaQuery query,
                    Object[] parameters) {

                return null;
            }
        }.execute(jpaQuery, binder), is(nullValue()));
    }


    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void modifyingExecutionClearsEntityManagerIfSet() {

        Query param = any();
        when(binder.bind(param)).thenReturn(query);
        when(query.executeUpdate()).thenReturn(0);
        when(method.getReturnType()).thenReturn((Class) void.class);

        ModifyingExecution execution = new ModifyingExecution(method, em);
        execution.execute(jpaQuery, binder);

        verify(em, times(1)).clear();
    }


    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void allowsMethodReturnTypesForModifyingQuery() throws Exception {

        when(method.getReturnType()).thenReturn((Class) void.class,
                (Class) int.class, (Class) Integer.class);

        new ModifyingExecution(method, em);
        new ModifyingExecution(method, em);
        new ModifyingExecution(method, em);
    }


    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test(expected = IllegalArgumentException.class)
    public void modifyingExecutionRejectsNonIntegerOrVoidReturnType()
            throws Exception {

        when(method.getReturnType()).thenReturn((Class) Long.class);
        new ModifyingExecution(method, em);
    }

    static class StubQueryExecution extends JpaQueryExecution {

        @Override
        protected Object doExecute(AbstractStringBasedJpaQuery query,
                ParameterBinder binder) {

            return null;
        }


        @Override
        protected Object doExecute(PartTreeJpaQuery query, Object[] parameters) {

            return null;
        }
    }
}
