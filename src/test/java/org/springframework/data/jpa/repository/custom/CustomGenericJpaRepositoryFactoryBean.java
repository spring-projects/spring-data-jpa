/*
 * Copyright 2008-2019 the original author or authors.
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
package org.springframework.data.jpa.repository.custom;

import java.io.Serializable;

import javax.persistence.EntityManager;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

/**
 * {@link JpaRepositoryFactoryBean} to return a custom repository base class.
 *
 * @author Gil Markham
 * @author Oliver Gierke
 */
public class CustomGenericJpaRepositoryFactoryBean<T extends JpaRepository<Object, Serializable>>
		extends JpaRepositoryFactoryBean<T, Object, Serializable> {

	public CustomGenericJpaRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
		super(repositoryInterface);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.data.jpa.repository.support.
	 * GenericJpaRepositoryFactoryBean#getFactory()
	 */
	@Override
	protected RepositoryFactorySupport createRepositoryFactory(EntityManager em) {

		return new CustomGenericJpaRepositoryFactory(em);
	}
}
