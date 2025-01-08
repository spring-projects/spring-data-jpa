/*
 * Copyright 2008-2025 the original author or authors.
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
package org.springframework.data.jpa.repository.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.Metamodel;

import java.io.IOException;
import java.io.Serializable;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.aop.framework.Advised;
import org.springframework.core.OverridingClassLoader;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.custom.CustomGenericJpaRepositoryFactory;
import org.springframework.data.jpa.repository.custom.UserCustomExtendedRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
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
 * @author Krzysztof Krason
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JpaRepositoryFactoryUnitTests {

	private JpaRepositoryFactory factory;

	@Mock EntityManager entityManager;
	@Mock Metamodel metamodel;
	@Mock
	@SuppressWarnings("rawtypes") JpaEntityInformation entityInformation;
	@Mock EntityManagerFactory emf;

	@BeforeEach
	void setUp() {

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
	 */
	@Test
	void setsUpBasicInstanceCorrectly() {
		assertThat(factory.getRepository(SimpleSampleRepository.class)).isNotNull();
	}

	@Test
	void allowsCallingOfObjectMethods() {

		SimpleSampleRepository repository = factory.getRepository(SimpleSampleRepository.class);

		repository.hashCode();
		repository.toString();
		repository.equals(repository);
	}

	/**
	 * Asserts that the factory recognized configured predicateExecutor classes that contain custom method but no custom
	 * implementation could be found. Furthremore the exception has to contain the name of the predicateExecutor interface
	 * as for a large predicateExecutor configuration it's hard to find out where this error occured.
	 */
	@Test
	void capturesMissingCustomImplementationAndProvidesInterfacename() {

		try {
			factory.getRepository(SampleRepository.class);
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).contains(SampleRepository.class.getName());
		}
	}

	@Test
	void handlesRuntimeExceptionsCorrectly() {

		SampleRepository repository = factory.getRepository(SampleRepository.class, new SampleCustomRepositoryImpl());

		assertThatIllegalArgumentException().isThrownBy(repository::throwingRuntimeException);
	}

	@Test
	void handlesCheckedExceptionsCorrectly() {

		SampleRepository repository = factory.getRepository(SampleRepository.class, new SampleCustomRepositoryImpl());

		assertThatExceptionOfType(IOException.class).isThrownBy(repository::throwingCheckedException);
	}

	@Test
	void createsProxyWithCustomBaseClass() {

		JpaRepositoryFactory factory = new CustomGenericJpaRepositoryFactory(entityManager);
		factory.setQueryLookupStrategyKey(Key.CREATE_IF_NOT_FOUND);
		UserCustomExtendedRepository repository = factory.getRepository(UserCustomExtendedRepository.class);

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> repository.customMethod(1));
	}

	@Test // DATAJPA-710, DATACMNS-542
	void usesConfiguredRepositoryBaseClass() {

		factory.setRepositoryBaseClass(CustomJpaRepository.class);
		SampleRepository repository = factory.getRepository(SampleRepository.class);

		assertThat(((Advised) repository).getTargetClass()).isEqualTo(CustomJpaRepository.class);
	}

	@Test // DATAJPA-819
	void crudMethodMetadataPostProcessorUsesBeanClassLoader() {

		ClassLoader classLoader = new OverridingClassLoader(ClassUtils.getDefaultClassLoader());
		factory.setBeanClassLoader(classLoader);
		Object processor = ReflectionTestUtils.getField(factory, "crudMethodMetadataPostProcessor");

		assertThat(ReflectionTestUtils.getField(processor, "classLoader")).isEqualTo((Object) classLoader);
	}

	private interface SimpleSampleRepository extends JpaRepository<User, Integer> {

		@Transactional
		@Override
		Optional<User> findById(Integer id);
	}

	/**
	 * Sample interface to contain a custom method.
	 *
	 * @author Oliver Gierke
	 */
	interface SampleCustomRepository {

		void throwingRuntimeException();

		void throwingCheckedException() throws IOException;
	}

	private interface SampleRepository extends JpaRepository<User, Integer>, SampleCustomRepository {

	}

	private interface QueryDslSampleRepository extends SimpleSampleRepository, QuerydslPredicateExecutor<User> {

	}

	static class CustomJpaRepository<T, ID extends Serializable> extends SimpleJpaRepository<T, ID> {

		CustomJpaRepository(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
			super(entityInformation, entityManager);
		}
	}

	/**
	 * Implementation of the custom predicateExecutor interface.
	 *
	 * @author Oliver Gierke
	 */
	private class SampleCustomRepositoryImpl implements SampleCustomRepository {

		@Override
		public void throwingRuntimeException() {
			throw new IllegalArgumentException("You lose");
		}

		@Override
		public void throwingCheckedException() throws IOException {
			throw new IOException("You lose");
		}
	};
}
