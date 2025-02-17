/*
 * Copyright 2017-2025 the original author or authors.
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

import java.util.function.BiConsumer;

import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.query.Jpa21Utils;
import org.springframework.data.jpa.repository.query.JpaEntityGraph;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link QueryHints}.
 *
 * @author Christoph Strobl
 * @author Oliver Gierke
 * @author Jens Schauder
 * @since 2.0
 */
class DefaultQueryHints implements QueryHints {

	private final JpaEntityInformation<?, ?> information;
	private final CrudMethodMetadata metadata;
	private final @Nullable EntityManager entityManager;
	private final boolean forCounts;

	/**
	 * Creates a new {@link DefaultQueryHints} instance for the given {@link JpaEntityInformation},
	 * {@link CrudMethodMetadata}, {@link EntityManager} and whether to include fetch graphs.
	 *
	 * @param information must not be {@literal null}.
	 * @param metadata can be {@literal null}.
	 * @param entityManager must not be {@literal null}.
	 * @param forCounts
	 */
	private DefaultQueryHints(JpaEntityInformation<?, ?> information, CrudMethodMetadata metadata,
			@Nullable EntityManager entityManager, boolean forCounts) {

		this.information = information;
		this.metadata = metadata;
		this.entityManager = entityManager;
		this.forCounts = forCounts;
	}

	/**
	 * Creates a new {@link QueryHints} instance for the given {@link JpaEntityInformation}, {@link CrudMethodMetadata}
	 * and {@link EntityManager}.
	 *
	 * @param information must not be {@literal null}.
	 * @param metadata must not be {@literal null}.
	 * @return
	 */
	public static QueryHints of(JpaEntityInformation<?, ?> information, CrudMethodMetadata metadata) {

		Assert.notNull(information, "JpaEntityInformation must not be null");
		Assert.notNull(metadata, "CrudMethodMetadata must not be null");

		return new DefaultQueryHints(information, metadata, null, false);
	}

	@Override
	public QueryHints withFetchGraphs(EntityManager em) {
		return new DefaultQueryHints(this.information, this.metadata, em, this.forCounts);
	}

	@Override
	public QueryHints forCounts() {
		return new DefaultQueryHints(this.information, this.metadata, this.entityManager, true);
	}

	@Override
	public void forEach(BiConsumer<String, Object> action) {
		combineHints().forEach(action);
	}

	private QueryHints combineHints() {
		return QueryHints.from(forCounts ? metadata.getQueryHintsForCount() : metadata.getQueryHints(), getFetchGraphs());
	}

	private QueryHints getFetchGraphs() {

		if(entityManager != null && metadata.getEntityGraph() != null) {
			return Jpa21Utils.getFetchGraphHint(entityManager, getEntityGraph(metadata.getEntityGraph()), information.getJavaType());
		}
		return new MutableQueryHints();
	}

	private JpaEntityGraph getEntityGraph(EntityGraph entityGraph) {

		String fallbackName = information.getEntityName() + "." + metadata.getMethod().getName();
		return new JpaEntityGraph(entityGraph, fallbackName);
	}
}
