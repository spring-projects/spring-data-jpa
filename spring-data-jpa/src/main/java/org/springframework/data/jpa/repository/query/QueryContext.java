/*
 * Copyright 2023 the original author or authors.
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

import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.QueryRewriter;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;

/**
 * Captures the context for a fully formed query, whether it's a custom finder, annotation-based query, stored
 * procedure, or other type. This is used to generate JPA queries.
 * 
 * @author Greg Turnquist
 */
public interface QueryContext {

	Optional<JpaQueryMethod> getQueryMethod();

	@Nullable
	JpaQueryMethod queryMethod();

	Query createJpaQuery(JpaParametersParameterAccessor accessor);

	Query createJpaCountQuery(JpaParametersParameterAccessor accessor);

	/**
	 * Create an {@link Example}-based {@link QueryContext}
	 * 
	 * @param entityManager
	 * @param example
	 * @param queryHints
	 * @return
	 * @param <T>
	 */
	static <T> QueryByExampleQueryContext extractQueryByExampleContext(EntityManager entityManager, Example<T> example,
			Consumer<Query> queryHints) {
		return new QueryByExampleQueryContext<>(entityManager, example, Sort.unsorted(), queryHints);
	}

	/**
	 * Create an {@link Example}-based {@link QueryContext} with {@link Sort} applied.
	 * 
	 * @param entityManager
	 * @param example
	 * @param sort
	 * @param queryHints
	 * @return
	 * @param <T>
	 */
	static <T> QueryByExampleQueryContext extractQueryByExampleContext(EntityManager entityManager, Example<T> example,
			Sort sort, Consumer<Query> queryHints) {
		return new QueryByExampleQueryContext<>(entityManager, example, sort, queryHints);
	}

	/**
	 * Create an {@literal @Query}-based {@link QueryContext}.
	 *
	 * @param method
	 * @param entityManager
	 * @param queryString
	 * @param countQueryString
	 * @param evaluationContextProvider
	 * @param parser
	 * @param queryRewriter
	 * @return
	 */
	static AnnotationBasedQueryContext extractAnnotatedQueryContext(JpaQueryMethod method, EntityManager entityManager,
			String queryString, @Nullable String countQueryString,
			QueryMethodEvaluationContextProvider evaluationContextProvider, SpelExpressionParser parser,
			QueryRewriter queryRewriter) {
		return new AnnotationBasedQueryContext(method, entityManager, queryString, countQueryString,
				evaluationContextProvider, parser, queryRewriter);
	}

	/**
	 * Create a customer finder-based {@link QueryContext}.
	 * 
	 * @param method
	 * @param entityManager
	 * @param escape
	 * @return
	 */
	static CustomFinderQueryContext extractCustomFinderContext(JpaQueryMethod method, EntityManager entityManager,
			EscapeCharacter escape) {
		return new CustomFinderQueryContext(method, entityManager, escape);
	}

	/**
	 * Created a stored procedure-based {@link QueryContext}.
	 * 
	 * @param method
	 * @param entityManager
	 * @return
	 */
	static StoredProcedureQueryContext extractStoredProcedureContext(JpaQueryMethod method, EntityManager entityManager) {
		return new StoredProcedureQueryContext(method, entityManager);
	}
}
