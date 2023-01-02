/*
 * Copyright 2013-2023 the original author or authors.
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

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.QueryRewriter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;

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
final class NativeJpaQuery extends AbstractStringBasedJpaQuery {

	/**
	 * Creates a new {@link NativeJpaQuery} encapsulating the query annotated on the given {@link JpaQueryMethod}.
	 *
	 * @param method must not be {@literal null}.
	 * @param em must not be {@literal null}.
	 * @param queryString must not be {@literal null} or empty.
	 * @param countQueryString must not be {@literal null} or empty.
	 * @param rewriter the query rewriter to use.
	 */
	public NativeJpaQuery(JpaQueryMethod method, EntityManager em, String queryString, @Nullable String countQueryString,
			QueryRewriter rewriter, QueryMethodEvaluationContextProvider evaluationContextProvider,
			SpelExpressionParser parser) {

		super(method, em, queryString, countQueryString, rewriter, evaluationContextProvider, parser);

		Parameters<?, ?> parameters = method.getParameters();

		if (parameters.hasSortParameter() && !queryString.contains("#sort")) {
			throw new InvalidJpaQueryMethodException("Cannot use native queries with dynamic sorting in method " + method);
		}
	}

	@Override
	protected Query createJpaQuery(String queryString, Sort sort, Pageable pageable, ReturnedType returnedType) {

		EntityManager em = getEntityManager();
		Class<?> type = getTypeToQueryFor(returnedType);

		return type == null ? em.createNativeQuery(potentiallyRewriteQuery(queryString, sort, pageable))
				: em.createNativeQuery(potentiallyRewriteQuery(queryString, sort, pageable), type);
	}

	@Nullable
	private Class<?> getTypeToQueryFor(ReturnedType returnedType) {

		Class<?> result = getQueryMethod().isQueryForEntity() ? returnedType.getDomainType() : null;

		if (this.getQuery().hasConstructorExpression() || this.getQuery().isDefaultProjection()) {
			return result;
		}

		return returnedType.isProjecting() && !getMetamodel().isJpaManaged(returnedType.getReturnedType()) //
				? Tuple.class
				: result;
	}
}
