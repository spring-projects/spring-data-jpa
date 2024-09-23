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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.function.Function;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.data.jpa.repository.query.EscapeCharacter;
import org.springframework.data.jpa.repository.query.JpaQueryMethodFactory;
import org.springframework.data.jpa.repository.query.QueryEnhancerSelector;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport;
import org.springframework.util.Assert;

/**
 * Special adapter for Springs {@link org.springframework.beans.factory.FactoryBean} interface to allow easy setup of
 * repository factories via Spring configuration.
 *
 * @author Oliver Gierke
 * @author Eberhard Wolff
 * @author Mark Paluch
 * @author Jens Schauder
 * @author Réda Housni Alaoui
 * @param <T> the type of the repository
 */
public class JpaRepositoryFactoryBean<T extends Repository<S, ID>, S, ID>
		extends TransactionalRepositoryFactoryBeanSupport<T, S, ID> {

	private @Nullable BeanFactory beanFactory;
	private @Nullable EntityManager entityManager;
	private EntityPathResolver entityPathResolver;
	private EscapeCharacter escapeCharacter = EscapeCharacter.DEFAULT;
	private @Nullable JpaQueryMethodFactory queryMethodFactory;
	private @Nullable Function<BeanFactory, QueryEnhancerSelector> queryEnhancerSelectorSource;

	/**
	 * Creates a new {@link JpaRepositoryFactoryBean} for the given repository interface.
	 *
	 * @param repositoryInterface must not be {@literal null}.
	 */
	public JpaRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
		super(repositoryInterface);
	}

	/**
	 * The {@link EntityManager} to be used.
	 *
	 * @param entityManager the entityManager to set
	 */
	@PersistenceContext
	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	@Override
	public void setMappingContext(MappingContext<?, ?> mappingContext) {
		super.setMappingContext(mappingContext);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		super.setBeanFactory(beanFactory);
	}

	/**
	 * Configures the {@link EntityPathResolver} to be used. Will expect a canonical bean to be present but fallback to
	 * {@link SimpleEntityPathResolver#INSTANCE} in case none is available.
	 *
	 * @param resolver must not be {@literal null}.
	 */
	@Autowired
	public void setEntityPathResolver(ObjectProvider<EntityPathResolver> resolver) {
		this.entityPathResolver = resolver.getIfAvailable(() -> SimpleEntityPathResolver.INSTANCE);
	}

	/**
	 * Configures the {@link JpaQueryMethodFactory} to be used. Will expect a canonical bean to be present but will
	 * fallback to {@link org.springframework.data.jpa.repository.query.DefaultJpaQueryMethodFactory} in case none is
	 * available.
	 *
	 * @param resolver may be {@literal null}.
	 */
	@Autowired
	public void setQueryMethodFactory(ObjectProvider<JpaQueryMethodFactory> resolver) { // TODO: nullable insteand of ObjectProvider

		JpaQueryMethodFactory factory = resolver.getIfAvailable();
		if (factory != null) {
			this.queryMethodFactory = factory;
		}
	}

	/**
	 * Configures the {@link QueryEnhancerSelector} to be used. Defaults to
	 * {@link QueryEnhancerSelector#DEFAULT_SELECTOR}.
	 *
	 * @param queryEnhancerSelectorSource must not be {@literal null}.
	 */
	public void setQueryEnhancerSelectorSource(QueryEnhancerSelector queryEnhancerSelectorSource) {
		this.queryEnhancerSelectorSource = bf -> queryEnhancerSelectorSource;
	}

	/**
	 * Configures the {@link QueryEnhancerSelector} to be used.
	 *
	 * @param queryEnhancerSelectorType must not be {@literal null}.
	 */
	public void setQueryEnhancerSelector(Class<? extends QueryEnhancerSelector> queryEnhancerSelectorType) {

		this.queryEnhancerSelectorSource = bf -> {

			if (bf != null) {

				ObjectProvider<? extends QueryEnhancerSelector> beanProvider = bf.getBeanProvider(queryEnhancerSelectorType);
				QueryEnhancerSelector selector = beanProvider.getIfAvailable();

				if (selector != null) {
					return selector;
				}

				if (bf instanceof AutowireCapableBeanFactory acbf) {
					return acbf.createBean(queryEnhancerSelectorType);
				}
			}

			return BeanUtils.instantiateClass(queryEnhancerSelectorType);
		};
	}

	@Override
	protected RepositoryFactorySupport doCreateRepositoryFactory() {

		Assert.state(entityManager != null, "EntityManager must not be null");

		return createRepositoryFactory(entityManager);
	}

	/**
	 * Returns a {@link RepositoryFactorySupport}.
	 */
	protected RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {

		JpaRepositoryFactory factory = new JpaRepositoryFactory(entityManager);
		factory.setEntityPathResolver(entityPathResolver);
		factory.setEscapeCharacter(escapeCharacter);

		if (queryMethodFactory != null) {
			factory.setQueryMethodFactory(queryMethodFactory);
		}

		if (queryEnhancerSelectorSource != null) {
			factory.setQueryEnhancerSelector(queryEnhancerSelectorSource.apply(beanFactory));
		}

		return factory;
	}

	@Override
	public void afterPropertiesSet() {

		Assert.state(entityManager != null, "EntityManager must not be null");

		super.afterPropertiesSet();
	}

	public void setEscapeCharacter(char escapeCharacter) {

		this.escapeCharacter = EscapeCharacter.of(escapeCharacter);
	}
}
