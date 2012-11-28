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

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.Metamodel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

/**
 * Unit test for {@code JpaRepositoryFactoryBean}.
 * <p>
 * TODO: Check if test methods double the ones in {@link JpaRepositoryFactoryUnitTests}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class JpaRepositoryFactoryBeanUnitTests {

	JpaRepositoryFactoryBean<SimpleSampleRepository, User, Integer> factoryBean;

	@Mock
	EntityManager entityManager;
	@Mock
	RepositoryFactorySupport factory;
	@Mock
	ListableBeanFactory beanFactory;
	@Mock
	PersistenceExceptionTranslator translator;
	@Mock
	Repository<?, ?> repository;
	@Mock
	Metamodel metamodel;

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() {

		Map<String, PersistenceExceptionTranslator> beans = new HashMap<String, PersistenceExceptionTranslator>();
		beans.put("foo", translator);
		when(beanFactory.getBeansOfType(eq(PersistenceExceptionTranslator.class), anyBoolean(), anyBoolean())).thenReturn(
				beans);
		when(factory.getRepository(any(Class.class), any(Object.class))).thenReturn(repository);
		when(entityManager.getMetamodel()).thenReturn(metamodel);

		// Setup standard factory configuration
		factoryBean = new DummyJpaRepositoryFactoryBean<SimpleSampleRepository, User, Integer>();
		factoryBean.setRepositoryInterface(SimpleSampleRepository.class);
		factoryBean.setEntityManager(entityManager);
	}

	/**
	 * Assert that the instance created for the standard configuration is a valid {@code UserRepository}.
	 * 
	 * @throws Exception
	 */
	@Test
	public void setsUpBasicInstanceCorrectly() throws Exception {

		factoryBean.setBeanFactory(beanFactory);
		factoryBean.afterPropertiesSet();

		assertNotNull(factoryBean.getObject());
	}

	@Test(expected = IllegalArgumentException.class)
	public void requiresListableBeanFactory() throws Exception {

		factoryBean.setBeanFactory(mock(BeanFactory.class));
	}

	/**
	 * Assert that the factory rejects calls to {@code JpaRepositoryFactoryBean#setRepositoryInterface(Class)} with
	 * {@literal null} or any other parameter instance not implementing {@code Repository}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void preventsNullRepositoryInterface() {

		factoryBean.setRepositoryInterface(null);
	}

	/**
	 * Assert that the factory detects unset repository class and interface in
	 * {@code JpaRepositoryFactoryBean#afterPropertiesSet()}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void preventsUnsetRepositoryInterface() throws Exception {

		factoryBean = new JpaRepositoryFactoryBean<SimpleSampleRepository, User, Integer>();
		factoryBean.afterPropertiesSet();
	}

	private class DummyJpaRepositoryFactoryBean<T extends JpaRepository<S, ID>, S, ID extends Serializable> extends
			JpaRepositoryFactoryBean<T, S, ID> {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean
		 * #createRepositoryFactory()
		 */
		@Override
		protected RepositoryFactorySupport doCreateRepositoryFactory() {

			return factory;
		}
	}

	private interface SimpleSampleRepository extends JpaRepository<User, Integer> {

	}

	/**
	 * Helper class to make the factory use {@link PersistableMetadata} .
	 * 
	 * @author Oliver Gierke
	 */
	@SuppressWarnings("serial")
	private static abstract class User implements Persistable<Long> {

	}
}
