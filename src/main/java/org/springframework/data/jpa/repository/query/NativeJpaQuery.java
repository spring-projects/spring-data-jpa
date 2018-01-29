/*
 * Copyright 2013-2018 the original author or authors.
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

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.Tuple;

import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * {@link RepositoryQuery} implementation that inspects a {@link org.springframework.data.repository.query.QueryMethod}
 * for the existence of an {@link org.springframework.data.jpa.repository.Query} annotation and creates a JPA native
 * {@link Query} from it.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Jens Schauder
 */
final class NativeJpaQuery extends AbstractStringBasedJpaQuery {

	private final Class<?> resultType;

	/**
	 * Creates a new {@link NativeJpaQuery} encapsulating the query annotated on the given {@link JpaQueryMethod}.
	 *
	 * @param method must not be {@literal null}.
	 * @param em must not be {@literal null}.
	 * @param queryString must not be {@literal null} or empty.
	 * @param evaluationContextProvider
	 */
	public NativeJpaQuery(JpaQueryMethod method, EntityManager em, String queryString,
			EvaluationContextProvider evaluationContextProvider, SpelExpressionParser parser) {

		super(method, em, queryString, evaluationContextProvider, parser);

		Parameters<?, ?> parameters = method.getParameters();

		if (parameters.hasSortParameter() && !queryString.contains("#sort")) {
			throw new InvalidJpaQueryMethodException(
					"Cannot use native queries with dynamic sorting in method " + method);
		}

		this.resultType = getTypeToQueryFor();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.AbstractStringBasedJpaQuery#createJpaQuery(java.lang.String)
	 */
	@Override
	protected Query createJpaQuery(String queryString) {

		EntityManager em = getEntityManager();

		return this.resultType == null ? em.createNativeQuery(queryString)
				: em.createNativeQuery(queryString, this.resultType);
	}

	@Nullable
	private Class<?> getTypeToQueryFor() {

		ResultProcessor resultFactory = getQueryMethod().getResultProcessor();
		ReturnedType returnedType = resultFactory.getReturnedType();

		Class<?> result = getQueryMethod().isQueryForEntity() ? returnedType.getDomainType() : null;

		if (this.getQuery().hasConstructorExpression() || this.getQuery().isDefaultProjection()) {
			return result;
		}

		return returnedType.isProjecting() && !getMetamodel().isJpaManaged(returnedType.getReturnedType()) //
				? Tuple.class
				: result;
	}
}
