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

import org.jspecify.annotations.Nullable;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.QueryCreationException;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * {@link RepositoryQuery} implementation that inspects a {@link org.springframework.data.repository.query.QueryMethod}
 * for the existence of an {@link org.springframework.data.jpa.repository.Query} annotation and creates a JPA
 * {@link Query} from it.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 * @author Greg Turnquist
 * @author Su Ko
 */
class SimpleJpaQuery extends AbstractStringBasedJpaQuery {

	/**
	 * Creates a new {@link SimpleJpaQuery} that encapsulates a simple query string.
	 *
	 * @param method must not be {@literal null}.
	 * @param em must not be {@literal null}.
	 * @param query must not be {@literal null} or empty.
	 * @param countQuery can be {@literal null} if not defined.
	 * @param queryConfiguration must not be {@literal null}.
	 */
	public SimpleJpaQuery(JpaQueryMethod method, EntityManager em, DeclaredQuery query,
			@Nullable DeclaredQuery countQuery, JpaQueryConfiguration queryConfiguration) {

		super(method, em, query, countQuery, queryConfiguration);

		validateQuery(getQuery(), "Query validation failed for '%s'", method);

		if (method.isPageQuery()) {
			validateQuery(getCountQuery(), "Count query validation failed for '%s'", method);
		}
	}

	/**
	 * Validates the given query for syntactical correctness.
	 *
	 * @param query
	 * @param errorMessage
	 */
	private void validateQuery(QueryProvider query, String errorMessage, JpaQueryMethod method) {

		if (getQueryMethod().isProcedureQuery()) {
			return;
		}

        String queryString = query.getQueryString();
        if(query instanceof EntityQuery entityQuery) {
             queryString = entityQuery
                     .rewrite(new DefaultQueryRewriteInformation(Sort.unsorted(),getReturnedType(method.getResultProcessor())))
                     .getQueryString();
        }

		try (EntityManager validatingEm = getEntityManager().getEntityManagerFactory().createEntityManager()) {
			validatingEm.createQuery(potentiallyRewriteQuery(queryString, Sort.unsorted(), null));
		} catch (RuntimeException e) {

			// Needed as there's ambiguities in how an invalid query string shall be expressed by the persistence provider
			// https://download.oracle.com/javaee-archive/jpa-spec.java.net/users/2012/07/0404.html
			throw QueryCreationException.create(method, errorMessage.formatted(queryString), e);
		}
	}
}
