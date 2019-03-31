/*
 * Copyright 2016-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository.query;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Tuple;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration tests for {@link AbstractStringBasedJpaQuery}.
 *
 * @author Oliver Gierke
 * @soundtrack Henrik Freischlader Trio - Nobody Else To Blame (Openness)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:infrastructure.xml")
public class AbstractStringBasedJpaQueryIntegrationTests {

	@PersistenceContext EntityManager em;

	@Test // DATAJPA-885
	public void createsNormalQueryForJpaManagedReturnTypes() throws Exception {

		EntityManager mock = mock(EntityManager.class);
		when(mock.getDelegate()).thenReturn(mock);
		when(mock.getEntityManagerFactory()).thenReturn(em.getEntityManagerFactory());
		when(mock.getMetamodel()).thenReturn(em.getMetamodel());

		JpaQueryMethod method = getMethod("findRolesByEmailAddress", String.class);
		AbstractStringBasedJpaQuery jpaQuery = new SimpleJpaQuery(method, mock,
				QueryMethodEvaluationContextProvider.DEFAULT, new SpelExpressionParser());

		jpaQuery.createJpaQuery(method.getAnnotatedQuery(), method.getResultProcessor().getReturnedType());

		verify(mock, times(1)).createQuery(anyString());
		verify(mock, times(0)).createQuery(anyString(), eq(Tuple.class));
	}

	private JpaQueryMethod getMethod(String name, Class<?>... parameterTypes) throws Exception {

		Method method = SampleRepository.class.getMethod(name, parameterTypes);
		PersistenceProvider persistenceProvider = PersistenceProvider.fromEntityManager(em);

		return new JpaQueryMethod(method, new DefaultRepositoryMetadata(SampleRepository.class),
				new SpelAwareProxyProjectionFactory(), persistenceProvider);
	}

	interface SampleRepository extends Repository<User, Integer> {

		@org.springframework.data.jpa.repository.Query("select u.roles from User u where u.emailAddress = ?1")
		Set<Role> findRolesByEmailAddress(String emailAddress);
	}
}
