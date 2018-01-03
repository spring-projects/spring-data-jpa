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
package org.springframework.data.jpa.repository.custom;

import static org.mockito.Mockito.*;

import java.io.Serializable;

import javax.persistence.EntityManager;

import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;

/**
 * Sample implementation of a custom {@link JpaRepositoryFactory} to use a custom repository base class.
 *
 * @author Oliver Gierke
 */
public class CustomGenericJpaRepositoryFactory extends JpaRepositoryFactory {

	/**
	 * @param entityManager
	 */
	public CustomGenericJpaRepositoryFactory(EntityManager entityManager) {

		super(entityManager);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.support.JpaRepositoryFactory#getTargetRepository(org.springframework.data.repository.core.RepositoryMetadata, javax.persistence.EntityManager)
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected SimpleJpaRepository<?, ?> getTargetRepository(RepositoryInformation information, EntityManager em) {

		JpaEntityInformation<Object, Serializable> entityMetadata = mock(JpaEntityInformation.class);
		when(entityMetadata.getJavaType()).thenReturn((Class<Object>) information.getDomainType());
		return new CustomGenericJpaRepository<Object, Serializable>(entityMetadata, em);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.support.JpaRepositoryFactory#getRepositoryBaseClass(org.springframework.data.repository.core.RepositoryMetadata)
	 */
	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return CustomGenericJpaRepository.class;
	}
}
