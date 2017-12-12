/*
 * Copyright 2008-2018 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.Metamodel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.aop.framework.Advised;
import org.springframework.core.OverridingClassLoader;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.custom.CustomGenericJpaRepositoryFactory;
import org.springframework.data.jpa.repository.custom.UserCustomExtendedRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ClassUtils;

/**
 * Unit test for {@code JpaRepositoryFactory}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Jens Schauder
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class JpaRepositoryFactoryUnitTests {

	JpaRepositoryFactory factory;

	@Mock EntityManager entityManager;
	@Mock Metamodel metamodel;
	@Mock @SuppressWarnings("rawtypes") JpaEntityInformation entityInformation;
	@Mock EntityManagerFactory emf;

	@Before
	public void setUp() {

		when(entityManager.getMetamodel()).thenReturn(metamodel);
		when(entityManager.getEntityManagerFactory()).thenReturn(emf);
		when(entityManager.getDelegate()).thenReturn(entityManager);
		when(emf.createEntityManager()).thenReturn(entityManager);

		// Setup standard factory configuration
		factory = new JpaRepositoryFactory(entityManager) {

			@Override
			@SuppressWarnings("unchecked")
			public <T, ID> JpaEntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
				return entityInformation;
			};
		};

		factory.setQueryLookupStrategyKey(Key.CREATE_IF_NOT_FOUND);
	}

	/**
	 * Assert that the instance created for the standard configuration is a valid {@code UserRepository}.
	 *
	 * @throws Exception
	 */
	@Test
	public void setsUpBasicInstanceCorrectly() throws Exception {

		assertNotNull(factory.getRepository(SimpleSampleRepository.class));
	}

	@Test
	public void allowsCallingOfObjectMethods() {

		SimpleSampleRepository repository = factory.getRepository(SimpleSampleRepository.class);

		repository.hashCode();
		repository.toString();
		repository.equals(repository);
	}

	/**
	 * Asserts that the factory recognized configured predicateExecutor classes that contain custom method but no custom
	 * implementation could be found. Furthremore the exception has to contain the name of the predicateExecutor interface as for
	 * a large predicateExecutor configuration it's hard to find out where this error occured.
	 *
	 * @throws Exception
	 */
	@Test
	public void capturesMissingCustomImplementationAndProvidesInterfacename() throws Exception {

		try {
			factory.getRepository(SampleRepository.class);
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains(SampleRepository.class.getName()));
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void handlesRuntimeExceptionsCorrectly() {

		SampleRepository repository = factory.getRepository(SampleRepository.class, new SampleCustomRepositoryImpl());
		repository.throwingRuntimeException();
	}

	@Test(expected = IOException.class)
	public void handlesCheckedExceptionsCorrectly() throws Exception {

		SampleRepository repository = factory.getRepository(SampleRepository.class, new SampleCustomRepositoryImpl());
		repository.throwingCheckedException();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void createsProxyWithCustomBaseClass() {

		JpaRepositoryFactory factory = new CustomGenericJpaRepositoryFactory(entityManager);
		factory.setQueryLookupStrategyKey(Key.CREATE_IF_NOT_FOUND);
		UserCustomExtendedRepository repository = factory.getRepository(UserCustomExtendedRepository.class);

		repository.customMethod(1);
	}

	@Test // DATAJPA-710, DATACMNS-542
	public void usesConfiguredRepositoryBaseClass() {

		factory.setRepositoryBaseClass(CustomJpaRepository.class);

		SampleRepository repository = factory.getRepository(SampleRepository.class);
		assertEquals(CustomJpaRepository.class, ((Advised) repository).getTargetClass());
	}

	@Test // DATAJPA-819
	public void crudMethodMetadataPostProcessorUsesBeanClassLoader() {

		ClassLoader classLoader = new OverridingClassLoader(ClassUtils.getDefaultClassLoader());

		factory.setBeanClassLoader(classLoader);

		Object processor = ReflectionTestUtils.getField(factory, "crudMethodMetadataPostProcessor");
		assertThat(ReflectionTestUtils.getField(processor, "classLoader"), is((Object) classLoader));
	}

	private interface SimpleSampleRepository extends JpaRepository<User, Integer> {

		@Transactional
		Optional<User> findById(Integer id);
	}

	/**
	 * Sample interface to contain a custom method.
	 *
	 * @author Oliver Gierke
	 */
	public interface SampleCustomRepository {

		void throwingRuntimeException();

		void throwingCheckedException() throws IOException;
	}

	/**
	 * Implementation of the custom predicateExecutor interface.
	 *
	 * @author Oliver Gierke
	 */
	private class SampleCustomRepositoryImpl implements SampleCustomRepository {

		public void throwingRuntimeException() {

			throw new IllegalArgumentException("You lose!");
		}

		public void throwingCheckedException() throws IOException {

			throw new IOException("You lose!");
		}
	}

	private interface SampleRepository extends JpaRepository<User, Integer>, SampleCustomRepository {

	}

	private interface QueryDslSampleRepository extends SimpleSampleRepository, QuerydslPredicateExecutor<User> {

	}

	static class CustomJpaRepository<T, ID extends Serializable> extends SimpleJpaRepository<T, ID> {

		public CustomJpaRepository(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
			super(entityInformation, entityManager);
		}
	};
}
