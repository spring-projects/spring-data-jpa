/*
 * Copyright 2024-2025 the original author or authors.
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
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;

import java.util.Collection;
import java.util.Map;

import com.querydsl.core.QueryModifiers;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.FactoryExpression;
import com.querydsl.jpa.JPQLSerializer;
import com.querydsl.jpa.JPQLTemplates;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAUtil;
import org.jspecify.annotations.Nullable;

/**
 * Customized String-Query implementation that specifically routes tuple query creation to
 * {@code EntityManager#createQuery(queryString, Tuple.class)}.
 *
 * @author Mark Paluch
 * @since 3.5
 */
class SpringDataJpaQuery<T> extends JPAQuery<T> {

	public SpringDataJpaQuery(EntityManager em) {
		super(em);
	}

	public SpringDataJpaQuery(EntityManager em, JPQLTemplates templates) {
		super(em, templates);
	}

	protected Query createQuery(@Nullable QueryModifiers modifiers, boolean forCount) {

		JPQLSerializer serializer = serialize(forCount);
		String queryString = serializer.toString();
		logQuery(queryString);

		Query query = getMetadata().getProjection() instanceof JakartaTuple
				? entityManager.createQuery(queryString, Tuple.class)
				: entityManager.createQuery(queryString);

		JPAUtil.setConstants(query, serializer.getConstants(), getMetadata().getParams());
		if (modifiers != null && modifiers.isRestricting()) {
			Integer limit = modifiers.getLimitAsInteger();
			Integer offset = modifiers.getOffsetAsInteger();
			if (limit != null) {
				query.setMaxResults(limit);
			}
			if (offset != null) {
				query.setFirstResult(offset);
			}
		}
		if (lockMode != null) {
			query.setLockMode(lockMode);
		}
		if (flushMode != null) {
			query.setFlushMode(flushMode);
		}

		for (Map.Entry<String, ?> entry : hints.entrySet()) {

			if (entry.getValue() instanceof Collection<?> c) {
				c.forEach((value) -> query.setHint(entry.getKey(), value));
			} else {
				query.setHint(entry.getKey(), entry.getValue());
			}
		}

		// set transformer, if necessary and possible
		Expression<?> projection = getMetadata().getProjection();
		this.projection = null; // necessary when query is reused

		if (!forCount && projection instanceof FactoryExpression) {
			if (!queryHandler.transform(query, (FactoryExpression<?>) projection)) {
				this.projection = (FactoryExpression) projection;
			}
		}

		return query;
	}

}
