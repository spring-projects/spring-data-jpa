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
package org.springframework.data.jpa.repository.support;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.data.jpa.domain.sample.QUser;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.support.QueryDslJpaRepository.EntityPathResolver;
import org.springframework.data.jpa.repository.support.QueryDslJpaRepository.SimpleEntityPathResolver;
import org.springframework.data.jpa.repository.util.JpaClassUtilsUnitTests.NamedUser;
import org.springframework.data.jpa.repository.util.QJpaClassUtilsUnitTests_NamedUser;


/**
 * Unit test for {@link SimpleEntityPathResolver}.
 * 
 * @author Oliver Gierke
 */
public class SimpleEntityPathResolverUnitTests {

    EntityPathResolver resolver =
            QueryDslJpaRepository.SimpleEntityPathResolver.INSTANCE;


    @Test
    public void createsRepositoryFromDomainClassCorrectly() throws Exception {

        assertThat(resolver.createPath(User.class), is(QUser.class));
    }


    @Test
    public void resolvesEntityPathForInnerClassCorrectly() throws Exception {

        assertThat(resolver.createPath(NamedUser.class),
                is(QJpaClassUtilsUnitTests_NamedUser.class));
    }


    @Test(expected = IllegalStateException.class)
    public void rejectsFoundClassWithoutStaticFieldOfSameType()
            throws Exception {

        resolver.createPath(Sample.class);
    }


    @Test(expected = IllegalArgumentException.class)
    public void rejectsClassWithoutQueryClassConfrmingToTheNamingScheme()
            throws Exception {

        resolver.createPath(QSimpleEntityPathResolverUnitTests_Sample.class);
    }

    static class Sample {

    }
}
