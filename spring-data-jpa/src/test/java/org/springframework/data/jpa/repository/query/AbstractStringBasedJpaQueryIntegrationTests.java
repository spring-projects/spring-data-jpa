/*
 * Copyright 2016-2023 the original author or authors.
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;

import java.lang.reflect.Method;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryRewriter;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Integration tests for {@link AbstractStringBasedJpaQuery}.
 *
 * @author Oliver Gierke
 * @author Greg Turnquist
 * @soundtrack Henrik Freischlader Trio - Nobody Else To Blame (Openness)
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:infrastructure.xml")
class AbstractStringBasedJpaQueryIntegrationTests {

	@PersistenceContext EntityManager em;

	@Autowired BeanFactory beanFactory;

	@Test // DATAJPA-885
	void createsNormalQueryForJpaManagedReturnTypes() throws Exception {

		EntityManager mock = mock(EntityManager.class);
		when(mock.getDelegate()).thenReturn(mock);
		when(mock.getEntityManagerFactory()).thenReturn(em.getEntityManagerFactory());
		when(mock.getMetamodel()).thenReturn(em.getMetamodel());

		JpaQueryMethod method = getMethod("findRolesByEmailAddress", String.class);
		AbstractStringBasedJpaQuery jpaQuery = new SimpleJpaQuery(method, mock, null, QueryRewriter.IdentityQueryRewriter.INSTANCE,
				QueryMethodEvaluationContextProvider.DEFAULT, new SpelExpressionParser());

		jpaQuery.createJpaQuery(method.getAnnotatedQuery(), Sort.unsorted(), null,
				method.getResultProcessor().getReturnedType());

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

		@Query("select u.roles from User u where u.emailAddress = ?1")
		Set<Role> findRolesByEmailAddress(String emailAddress);
	}
}
