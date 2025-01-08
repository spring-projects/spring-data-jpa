/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.jpa.repository.cdi;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.persistence.EntityManager;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.data.jpa.repository.query.QueryRewriterProvider;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.cdi.CdiRepositoryBean;
import org.springframework.data.repository.config.CustomRepositoryImplementationDetector;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.util.Assert;

/**
 * A bean which represents a JPA repository.
 *
 * @author Dirk Mahler
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Christoph Strobl
 * @param <T> The type of the repository.
 */
class JpaRepositoryBean<T> extends CdiRepositoryBean<T> {

	private final Bean<EntityManager> entityManagerBean;
	private final QueryRewriterProvider queryRewriterProvider;

	/**
	 * Constructs a {@link JpaRepositoryBean}.
	 *
	 * @param beanManager must not be {@literal null}.
	 * @param entityManagerBean must not be {@literal null}.
	 * @param qualifiers must not be {@literal null}.
	 * @param repositoryType must not be {@literal null}.
	 * @param detector can be {@link Optional#empty()}.
	 */
	JpaRepositoryBean(BeanManager beanManager, Bean<EntityManager> entityManagerBean, Set<Annotation> qualifiers,
			Class<T> repositoryType, Optional<CustomRepositoryImplementationDetector> detector) {

		super(qualifiers, repositoryType, beanManager, detector);

		Assert.notNull(entityManagerBean, "EntityManager bean must not be null");
		this.entityManagerBean = entityManagerBean;
		this.queryRewriterProvider = new BeanManagerQueryRewriterProvider(beanManager);
	}

	@Override
	protected T create(CreationalContext<T> creationalContext, Class<T> repositoryType) {

		EntityManager entityManager = getDependencyInstance(entityManagerBean, EntityManager.class);

		Supplier<RepositoryFactorySupport> repositoryFactorySupportSupplier = () -> {
			JpaRepositoryFactory jpaRepositoryFactory = new JpaRepositoryFactory(entityManager);
			jpaRepositoryFactory.setQueryRewriterProvider(queryRewriterProvider);
			return jpaRepositoryFactory;
		};

		return create(repositoryFactorySupportSupplier, repositoryType);
	}
}
