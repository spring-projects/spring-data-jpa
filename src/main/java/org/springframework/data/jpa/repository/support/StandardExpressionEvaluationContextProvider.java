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
package org.springframework.data.jpa.repository.support;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * Default implementation of {@link ExpressionEvaluationContextProvider} that always creates a new
 * {@link EvaluationContext}.
 * 
 * @author Thomas Darimont
 */
public enum StandardExpressionEvaluationContextProvider implements ExpressionEvaluationContextProvider {

	INSTANCE;

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.support.ExpressionEvaluationContextProvider#getEvaluationContext()
	 */
	@Override
	public StandardEvaluationContext getEvaluationContext() {
		return new StandardEvaluationContext();
	}
}
