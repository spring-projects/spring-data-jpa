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
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.data.repository.augment.QueryContext;

/**
 * A {@link QueryContext}
 * 
 * @author Oliver Gierke
 */
public class JpaCriteriaQueryContext<S, T> extends QueryContext<CriteriaQuery<S>> {

	private final EntityManager em;
	private final Root<T> root;

	/**
	 * @param mode
	 * @param em
	 * @param query
	 * @param root
	 */
	public JpaCriteriaQueryContext(QueryMode mode, EntityManager em, CriteriaQuery<S> query, Root<T> root) {

		super(query, mode);

		this.em = em;
		this.root = root;
	}

	/**
	 * @return the root
	 */
	public Root<T> getRoot() {
		return root;
	}

	public CriteriaBuilder getCriteriaBuilder() {
		return em.getCriteriaBuilder();
	}

	public JpaCriteriaQueryContext<S, T> with(CriteriaQuery<S> query) {
		return new JpaCriteriaQueryContext<S, T>(getMode(), em, query, root);
	}
}
