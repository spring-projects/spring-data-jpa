/*
 * Copyright 2008-2018 the original author or authors.
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
package org.springframework.data.jpa.repository.query;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * {@link RepositoryQuery} implementation that inspects a {@link org.springframework.data.repository.query.QueryMethod}
 * for the existence of an {@link org.springframework.data.jpa.repository.Query} annotation and creates a JPA
 * {@link Query} from it.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 */
final class SimpleJpaQuery extends AbstractStringBasedJpaQuery {

	/**
	 * Creates a new {@link SimpleJpaQuery} encapsulating the query annotated on the given {@link JpaQueryMethod}.
	 *
	 * @param method must not be {@literal null}
	 * @param em must not be {@literal null}
	 * @param evaluationContextProvider must not be {@literal null}
	 * @param parser must not be {@literal null}
	 */
	public SimpleJpaQuery(JpaQueryMethod method, EntityManager em,
			QueryMethodEvaluationContextProvider evaluationContextProvider, SpelExpressionParser parser) {
		this(method, em, method.getRequiredAnnotatedQuery(), evaluationContextProvider, parser);
	}

	/**
	 * Creates a new {@link SimpleJpaQuery} that encapsulates a simple query string.
	 *
	 * @param method must not be {@literal null}
	 * @param em must not be {@literal null}
	 * @param queryString must not be {@literal null} or empty
	 * @param evaluationContextProvider must not be {@literal null}
	 * @param parser must not be {@literal null}
	 */
	public SimpleJpaQuery(JpaQueryMethod method, EntityManager em, String queryString,
			QueryMethodEvaluationContextProvider evaluationContextProvider, SpelExpressionParser parser) {

		super(method, em, queryString, evaluationContextProvider, parser);

		validateQuery(getQuery().getQueryString(), "Validation failed for query for method %s!", method);

		if (method.isPageQuery()) {
			validateQuery(getCountQuery().getQueryString(),
					String.format("Count query validation failed for method %s!", method));
		}
	}

	/**
	 * Validates the given query for syntactical correctness.
	 *
	 * @param query
	 * @param errorMessage
	 */
	private void validateQuery(String query, String errorMessage, Object... arguments) {

		if (getQueryMethod().isProcedureQuery()) {
			return;
		}

		EntityManager validatingEm = null;

		try {
			validatingEm = getEntityManager().getEntityManagerFactory().createEntityManager();
			validatingEm.createQuery(query);

		} catch (RuntimeException e) {

			// Needed as there's ambiguities in how an invalid query string shall be expressed by the persistence provider
			// http://java.net/projects/jpa-spec/lists/jsr338-experts/archive/2012-07/message/17
			throw new IllegalArgumentException(String.format(errorMessage, arguments), e);

		} finally {

			if (validatingEm != null) {
				validatingEm.close();
			}
		}
	}
}
