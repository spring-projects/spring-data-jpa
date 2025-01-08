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
package org.springframework.data.jpa.repository.custom;

import java.io.Serializable;

import jakarta.persistence.EntityManager;

import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

/**
 * Sample custom repository base class implementing common custom functionality for all derived repository instances.
 *
 * @author Oliver Gierke
 */
public class CustomGenericJpaRepository<T, ID extends Serializable> extends SimpleJpaRepository<T, ID> implements
		CustomGenericRepository<T, ID> {

	public CustomGenericJpaRepository(JpaEntityInformation<T, ID> metadata, EntityManager entityManager) {
		super(metadata, entityManager);
	}

	@Override
	public T customMethod(ID id) {
		throw new UnsupportedOperationException("Forced exception for testing purposes.");
	}
}
