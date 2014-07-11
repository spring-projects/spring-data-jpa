/*
 * Copyright 2008-2014 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.util.Assert;

/**
 * A {@link JpaResultPostProcessor} that potentially detaches resulting Domain-Objects from the configured
 * {@link EntityManager}.
 * 
 * @author Thomas Darimont
 */
public class DetachingJpaResultPostProcessor implements JpaResultPostProcessor {

	private final EntityManager em;

	/**
	 * Creates a new {@link DetachingJpaResultPostProcessor}.
	 * 
	 * @param em must not be {@literal null}.
	 */
	public DetachingJpaResultPostProcessor(EntityManager em) {

		Assert.notNull(em, "EntityManager must not be null!");

		this.em = em;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.support.JpaResultPostProcessor#postProcessResult(java.lang.Class, java.lang.Object)
	 */
	@Override
	public <T, R> R postProcessResult(Class<T> domainClass, R entity) {

		if (domainClass.isInstance(entity)) {
			em.detach(entity);
		}

		return entity;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.support.JpaResultPostProcessor#postProcessResults(java.lang.Class, java.util.List)
	 */
	@Override
	public <T> List<T> postProcessResults(Class<T> domainClass, List<T> results) {

		List<T> postProcessedResults = new ArrayList<T>();

		for (T entity : results) {
			if (domainClass.isInstance(entity)) {
				postProcessedResults.add(postProcessResult(domainClass, entity));
			}
		}

		return postProcessedResults;
	}
}
