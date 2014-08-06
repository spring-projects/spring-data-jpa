/*
 * Copyright 2014 the original author or authors.
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

import org.springframework.data.repository.augment.UpdateContext;

import com.mysema.query.dml.DeleteClause;
import com.mysema.query.dml.UpdateClause;
import com.mysema.query.jpa.impl.JPADeleteClause;
import com.mysema.query.jpa.impl.JPAUpdateClause;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.Path;

/**
 * @author Dev Naruka
 */
public class QueryDslJpaUpdateContext<T> extends UpdateContext<T> {

	private final EntityPath<T> root;
	private final EntityManager em;

	public QueryDslJpaUpdateContext(T entity, EntityManager entityManager, EntityPath<T> root, UpdateMode mode) {
		super(entity, mode);

		this.root = root;
		this.em = entityManager;
	}

	public Path<T> getRoot() {
		return root;
	}

	public UpdateClause<JPAUpdateClause> update() {
		return new JPAUpdateClause(em, root);
	}

	public DeleteClause<JPADeleteClause> delete() {
		return new JPADeleteClause(em, root);
	}
}
