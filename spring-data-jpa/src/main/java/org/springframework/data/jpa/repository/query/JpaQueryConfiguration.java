/*
 * Copyright 2024 the original author or authors.
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

import org.springframework.data.jpa.repository.QueryRewriter;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * Configuration object holding configuration information for JPA queries within a repository.
 *
 * @author Mark Paluch
 */
public class JpaQueryConfiguration {

	private final QueryRewriterProvider queryRewriter;
	private final QueryEnhancerSelector selector;
	private final EscapeCharacter escapeCharacter;
	private final QueryMethodEvaluationContextProvider evaluationContextProvider;
	private final SpelExpressionParser parser;

	public JpaQueryConfiguration(QueryRewriterProvider queryRewriter, QueryEnhancerSelector selector,
			QueryMethodEvaluationContextProvider evaluationContextProvider, EscapeCharacter escapeCharacter,
			SpelExpressionParser parser) {

		this.queryRewriter = queryRewriter;
		this.selector = selector;
		this.escapeCharacter = escapeCharacter;
		this.evaluationContextProvider = evaluationContextProvider;
		this.parser = parser;
	}

	public QueryRewriter getQueryRewriter(JpaQueryMethod queryMethod) {
		return queryRewriter.getQueryRewriter(queryMethod);
	}

	public QueryEnhancerSelector getSelector() {
		return selector;
	}

	public EscapeCharacter getEscapeCharacter() {
		return escapeCharacter;
	}

	public QueryMethodEvaluationContextProvider getEvaluationContextProvider() {
		return evaluationContextProvider;
	}

	public SpelExpressionParser getParser() {
		return parser;
	}
}
