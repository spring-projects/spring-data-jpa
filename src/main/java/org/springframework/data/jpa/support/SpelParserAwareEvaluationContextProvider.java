/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.jpa.support;

import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * @author Thomas Darimont
 */
public class SpelParserAwareEvaluationContextProvider implements EvaluationContextProvider {

	private final SpelExpressionParser parser = new SpelExpressionParser();

	private final EvaluationContextProvider evaluationContextProvider;

	/**
	 * Creates a new {@link SpelParserAwareEvaluationContextProvider}.
	 * 
	 * @param evaluationContextProvider
	 */
	public SpelParserAwareEvaluationContextProvider(EvaluationContextProvider evaluationContextProvider) {
		this.evaluationContextProvider = evaluationContextProvider;
	}

	@Override
	public <T extends Parameters<T, ? extends Parameter>> EvaluationContext getEvaluationContext(T parameters,
			Object[] parameterValues) {
		return evaluationContextProvider.getEvaluationContext(parameters, parameterValues);
	}

	/**
	 * @return the associated SpEL Parser
	 */
	public SpelExpressionParser getParser() {
		return parser;
	}
}
