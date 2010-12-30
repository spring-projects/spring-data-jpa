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
package org.springframework.data.jpa.repository.support;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.jpa.domain.sample.QUser;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.mysema.query.types.expr.BooleanExpression;


/**
 * Integration test for {@link QueryDslJpaRepository}.
 * 
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "classpath:infrastructure.xml" })
@Transactional
public class QueryDslJpaRepositoryTests {

    @PersistenceContext
    EntityManager em;

    QueryDslJpaRepository<User, Integer> repository;
    QUser user = new QUser("user");
    User dave, carter;


    @Before
    public void setUp() {

        repository = new QueryDslJpaRepository<User, Integer>(user, em);
        dave =
                repository.save(new User("Dave", "Matthews",
                        "dave@matthews.com"));
        carter =
                repository.save(new User("Carter", "Beauford",
                        "carter@beauford.com"));
    }


    @Test
    public void executesPredicatesCorrectly() throws Exception {

        BooleanExpression isCalledDave = user.firstname.eq("Dave");
        BooleanExpression isBeauford = user.lastname.eq("Beauford");

        List<User> result = repository.findAll(isCalledDave.or(isBeauford));

        assertThat(result.size(), is(2));
        assertThat(result, hasItems(carter, dave));
    }


    @Test
    public void createsRepositoryFromDomainClassCorrectly() throws Exception {

        new QueryDslJpaRepository<User, Integer>(User.class, em);
    }
}
