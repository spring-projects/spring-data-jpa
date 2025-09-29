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
package org.springframework.data.jpa.repository.query;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.springframework.data.jpa.repository.QueryRewriter;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.lang.Nullable;

/**
 * {@link RepositoryQuery} implementation that inspects a {@link org.springframework.data.repository.query.QueryMethod}
 * for the existence of an {@link org.springframework.data.jpa.repository.Query} annotation and creates a JPA
 * {@link Query} from it.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 * @author Greg Turnquist
 */
final class SimpleJpaQuery extends AbstractStringBasedJpaQuery {

	/**
	 * Creates a new {@link SimpleJpaQuery} encapsulating the query annotated on the given {@link JpaQueryMethod}.
	 *
	 * @param method must not be {@literal null}
	 * @param em must not be {@literal null}
	 * @param sourceQuery the original source query, must not be {@literal null} or empty.
	 * @param queryRewriter must not be {@literal null}
	 * @param valueExpressionDelegate must not be {@literal null}
	 */
	public SimpleJpaQuery(JpaQueryMethod method, EntityManager em, @Nullable String sourceQuery,
			QueryRewriter queryRewriter, ValueExpressionDelegate valueExpressionDelegate) {
		this(method, em, method.getRequiredAnnotatedQuery(), sourceQuery, queryRewriter, valueExpressionDelegate);
	}

	/**
	 * Creates a new {@link SimpleJpaQuery} that encapsulates a simple query string.
	 *
	 * @param method must not be {@literal null}
	 * @param em must not be {@literal null}
	 * @param sourceQuery the original source query, must not be {@literal null} or empty
	 * @param countQueryString
	 * @param queryRewriter
	 * @param valueExpressionDelegate must not be {@literal null}
	 */
	public SimpleJpaQuery(JpaQueryMethod method, EntityManager em, String sourceQuery, @Nullable String countQueryString,
			QueryRewriter queryRewriter, ValueExpressionDelegate valueExpressionDelegate) {

		super(method, em, sourceQuery, countQueryString, queryRewriter, valueExpressionDelegate);

		validateQuery(getQuery(), "Query '%s' validation failed for method %s", method);

		if (method.isPageQuery()) {
			validateQuery(getCountQuery(), "Count query '%s' validation failed for method %s", method);
		}
	}

	/**
	 * Validates the given query for syntactical correctness.
	 *
	 * @param query
	 * @param errorMessage
	 */
	private void validateQuery(DeclaredQuery query, String errorMessage, JpaQueryMethod method) {

		if (getQueryMethod().isProcedureQuery()) {
			return;
		}

		String queryString = query.getQueryString();
		try (EntityManager validatingEm = getEntityManager().getEntityManagerFactory().createEntityManager()) {
			validatingEm.createQuery(queryString);
		} catch (RuntimeException e) {

			// Needed as there's ambiguities in how an invalid query string shall be expressed by the persistence provider
			// https://download.oracle.com/javaee-archive/jpa-spec.java.net/users/2012/07/0404.html
			throw new IllegalArgumentException(errorMessage.formatted(queryString, method), e);
		}
	}
}
