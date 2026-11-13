/*
 * Copyright 2026-present the original author or authors.
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
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Integration tests for caching of {@link JpaEntityInformation} and its mapping context.
 *
 * @author Christoph Strobl
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:hibernate-infrastructure.xml")
class JpaEntityInformationCachingIntegrationTests {

	@PersistenceContext EntityManager em;

	@Test // GH-4037
	void sharesMappingContextForSameMetamodel() {

		JpaEntityInformation<User, ?> first = JpaEntityInformationSupport.getEntityInformation(User.class, em);
		JpaEntityInformation<User, ?> second = JpaEntityInformationSupport.getEntityInformation(User.class, em);

		assertThat(ReflectionTestUtils.getField(first, "context")).isSameAs(ReflectionTestUtils.getField(second, "context"));
	}

	@Test // GH-4037
	void wipesMappingContextCacheOnApplicationContextClose() {

		Object context = ReflectionTestUtils.getField(JpaEntityInformationSupport.getEntityInformation(User.class, em),
				"context");

		try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

			applicationContext.registerBean(JpaEntityInformationCacheCleanup.class);
			applicationContext.refresh();
		}

		assertThat(ReflectionTestUtils.getField(JpaEntityInformationSupport.getEntityInformation(User.class, em), "context"))
				.isNotSameAs(context);
	}

	@Test // GH-4037
	void registersCleanupBeanAsNonLazy() {

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		new JpaRepositoryConfigExtension().registerBeansForRoot(beanFactory, mock(RepositoryConfigurationSource.class));

		String[] cleanupBeanNames = beanFactory.getBeanNamesForType(JpaEntityInformationCacheCleanup.class);

		assertThat(cleanupBeanNames).hasSize(1);
		assertThat(beanFactory.getBeanDefinition(cleanupBeanNames[0]).isLazyInit()).isFalse();
	}

	@Test // GH-4037
	void repositoryFactoryCachesEntityInformation() {

		JpaRepositoryFactory factory = new JpaRepositoryFactory(em);

		JpaEntityInformation<User, ?> information = factory.getEntityInformation(User.class);

		assertThat(factory.getEntityInformation(User.class)).isSameAs(information);
		assertThat(factory.getEntityInformation(AbstractRepositoryMetadata.getMetadata(UserRepository.class)))
				.isSameAs(information);
	}
}
