/*
 * Copyright 2008-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.data.jpa.repository.query;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.QueryHint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.jpa.repository.sample.UserRepository;


/**
 * Unit test for {@link SimpleHadesQuery}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class SimpleJpaQueryUnitTests {

    private JpaQueryMethod method;

    @Mock
    private EntityManager em;
    @Mock
    private QueryExtractor extractor;
    @Mock
    private Query query;


    @Before
    @QueryHints(@QueryHint(name = "foo", value = "bar"))
    public void setUp() throws SecurityException, NoSuchMethodException {

        when(em.createQuery(anyString())).thenReturn(query);

        Method setUp =
                UserRepository.class.getMethod("findByLastname", String.class);
        method = new JpaQueryMethod(setUp, extractor, em);
    }


    @Test
    public void appliesHintsCorrectly() throws Exception {

        SimpleJpaQuery hadesQuery = new SimpleJpaQuery(method, em, "foobar");
        hadesQuery.createQuery(em, new ParameterBinder(method.getParameters(),
                new Object[] { "gierke" }));

        verify(query).setHint("foo", "bar");
    }


    @Test
    public void prefersDeclaredCountQueryOverCreatingOne() throws Exception {

        method = mock(JpaQueryMethod.class);
        when(method.getCountQuery()).thenReturn("foo");
        when(em.createQuery("foo")).thenReturn(query);

        SimpleJpaQuery hadesQuery =
                new SimpleJpaQuery(method, em, "select u from User u");

        assertThat(hadesQuery.createCountQuery(em), is(query));
    }
}
