/*
 * Copyright 2025 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;

import java.util.Iterator;

import org.junit.jupiter.api.Test;

import org.springframework.data.jpa.domain.sample.QCustomer;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryFragment;

import com.querydsl.core.types.EntityPath;

/**
 * Unit tests for {@link JpaRepositoryFragmentsContributor}.
 *
 * @author Mark Paluch
 * @author Ariel Morelli Andres (Atlassian US, Inc.)
 */
class JpaRepositoryFragmentsContributorUnitTests {

	@Test // GH-3279
	void composedContributorShouldCreateFragments() {

		JpaRepositoryFragmentsContributor contributor = JpaRepositoryFragmentsContributor.DEFAULT
				.andThen(MyJpaRepositoryFragmentsContributor.INSTANCE);

		EntityPathResolver entityPathResolver = mock(EntityPathResolver.class);
		when(entityPathResolver.createPath(any())).thenReturn((EntityPath) QCustomer.customer);

		EntityManager entityManager = mock(EntityManager.class);
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		PersistenceUnitUtil persistenceUnitUtil = mock(PersistenceUnitUtil.class);
		when(entityManager.getDelegate()).thenReturn(entityManager);
		when(entityManager.getEntityManagerFactory()).thenReturn(emf);
		when(emf.getPersistenceUnitUtil()).thenReturn(persistenceUnitUtil);

		RepositoryComposition.RepositoryFragments fragments = contributor.contribute(
				AbstractRepositoryMetadata.getMetadata(QuerydslUserRepository.class),
				new JpaEntityInformationSupportUnitTests.DummyJpaEntityInformation<>(QuerydslUserRepository.class),
				entityManager, entityPathResolver);

		assertThat(fragments).hasSize(2);

		Iterator<RepositoryFragment<?>> iterator = fragments.iterator();

		RepositoryFragment<?> querydsl = iterator.next();
		assertThat(querydsl.getImplementationClass()).contains(QuerydslJpaPredicateExecutor.class);

		RepositoryFragment<?> additional = iterator.next();
		assertThat(additional.getImplementationClass()).contains(MyFragment.class);
	}

	enum MyJpaRepositoryFragmentsContributor implements JpaRepositoryFragmentsContributor {

		INSTANCE;

		@Override
		public RepositoryComposition.RepositoryFragments contribute(RepositoryMetadata metadata,
				JpaEntityInformation<?, ?> entityInformation, EntityManager entityManager, EntityPathResolver resolver) {
			return RepositoryComposition.RepositoryFragments.just(new MyFragment());
		}

		@Override
		public RepositoryComposition.RepositoryFragments describe(RepositoryMetadata metadata) {
			return RepositoryComposition.RepositoryFragments.just(new MyFragment());
		}
	}

	static class MyFragment {

	}

	interface QuerydslUserRepository extends Repository<User, Long>, QuerydslPredicateExecutor<User> {}

}
