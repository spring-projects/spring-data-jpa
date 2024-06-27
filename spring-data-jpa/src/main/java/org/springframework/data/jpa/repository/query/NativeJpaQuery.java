/*
 * Copyright 2013-2025 the original author or authors.
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
package org.springframework.data.jpa.repository.query;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;

import org.springframework.core.annotation.MergedAnnotation;

import org.jspecify.annotations.Nullable;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.util.ObjectUtils;

/**
 * {@link RepositoryQuery} implementation that inspects a {@link org.springframework.data.repository.query.QueryMethod}
 * for the existence of an {@link org.springframework.data.jpa.repository.Query} annotation and creates a JPA native
 * {@link Query} from it.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Mark Paluch
 * @author Greg Turnquist
 */
class NativeJpaQuery extends AbstractStringBasedJpaQuery {

	private final @Nullable String sqlResultSetMapping;

	private final boolean queryForEntity;

	/**
	 * Creates a new {@link NativeJpaQuery} encapsulating the query annotated on the given {@link JpaQueryMethod}.
	 *
	 * @param method must not be {@literal null}.
	 * @param em must not be {@literal null}.
	 * @param queryString must not be {@literal null} or empty.
	 * @param countQueryString must not be {@literal null} or empty.
	 * @param queryConfiguration must not be {@literal null}.
	 */
	public NativeJpaQuery(JpaQueryMethod method, EntityManager em, String queryString, @Nullable String countQueryString,
			JpaQueryConfiguration queryConfiguration) {

		super(method, em, queryString, countQueryString, queryConfiguration);

		MergedAnnotations annotations = MergedAnnotations.from(method.getMethod());
		MergedAnnotation<NativeQuery> annotation = annotations.get(NativeQuery.class);
		this.sqlResultSetMapping = annotation.isPresent() ? annotation.getString("sqlResultSetMapping") : null;

		this.queryForEntity = getQueryMethod().isQueryForEntity();
	}

	@Override
	protected Query createJpaQuery(String queryString, Sort sort, @Nullable Pageable pageable, ReturnedType returnedType) {

		EntityManager em = getEntityManager();
		String query = potentiallyRewriteQuery(queryString, sort, pageable);

		if (!ObjectUtils.isEmpty(sqlResultSetMapping)) {
			return em.createNativeQuery(query, sqlResultSetMapping);
		}

		Class<?> type = getTypeToQueryFor(returnedType);
		return type == null ? em.createNativeQuery(query) : em.createNativeQuery(query, type);
	}

	private @Nullable Class<?> getTypeToQueryFor(ReturnedType returnedType) {

		Class<?> result = queryForEntity ? returnedType.getDomainType() : null;

		if (getQuery().hasConstructorExpression() || getQuery().isDefaultProjection()) {
			return result;
		}

		if (returnedType.isProjecting()) {

			if (returnedType.getReturnedType().isInterface()) {
				return Tuple.class;
			}

			return returnedType.getReturnedType();
		}

		return result;
	}
}
