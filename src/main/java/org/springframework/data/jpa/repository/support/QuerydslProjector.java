/*
 * Copyright 2021 the original author or authors.
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

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;

import com.querydsl.jpa.impl.AbstractJPAQuery;

/**
 * Applies fetchgraph hints to {@code AbstractJPAQuery}.
 *
 * @author Jens Schauder
 * @since 2.6
 */
class QuerydslProjector extends Projector<AbstractJPAQuery<?, ?>> {

	QuerydslProjector(EntityManager entityManager) {
		super(entityManager);
	}

	@Override
	void applyEntityGraph(AbstractJPAQuery<?, ?> query, EntityGraph<?> entityGraph) {
		query.setHint("javax.persistence.fetchgraph", entityGraph);
	}
}
