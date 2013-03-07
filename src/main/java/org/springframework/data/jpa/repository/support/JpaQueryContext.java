/*
 * Copyright 2013 the original author or authors.
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

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.springframework.data.jpa.repository.query.QueryExtractor;
import org.springframework.data.repository.augment.QueryContext;

/**
 * @author Oliver Gierke
 */
public class JpaQueryContext extends QueryContext<Query> {

	private final EntityManager entityManager;
	private final QueryExtractor extractor;

	/**
	 * @param query
	 * @param queryMode
	 */
	public JpaQueryContext(QueryMode queryMode, EntityManager entityManager, Query query) {

		super(query, queryMode);
		this.entityManager = entityManager;
		this.extractor = PersistenceProvider.fromEntityManager(entityManager);
	}

	/**
	 * @return the entityManager
	 */
	public EntityManager getEntityManager() {
		return entityManager;
	}

	public String getQueryString() {
		return extractor.extractQueryString(getQuery());
	}

	@SuppressWarnings("unchecked")
	public JpaQueryContext withQuery(String query) {

		Query createQuery = entityManager.createQuery(query);
		return new JpaQueryContext(getMode(), entityManager, createQuery);
	}
}
