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
package org.springframework.data.jpa.repository.query;

import java.util.List;

import javax.persistence.Query;

import org.springframework.data.jpa.repository.query.StringQuery.ParameterBinding;
import org.springframework.data.jpa.repository.support.ExpressionEvaluationContextProvider;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.ConstructorResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.OperatorOverloader;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypeComparator;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;

/**
 * A {@link StringQueryParameterBinder} that is able to bind synthetic query parameters.
 * 
 * @author Thomas Darimont
 */
class SpelExpressionStringQueryParameterBinder extends StringQueryParameterBinder {

	private final StringQuery query;
	private final ExpressionEvaluationContextProvider evaluationContextProvider;

	/**
	 * Creates a new {@link SpelExpressionStringQueryParameterBinder}.
	 * 
	 * @param parameters must not be {@literal null}
	 * @param values must not be {@literal null}
	 * @param query must not be {@literal null}
	 * @param evaluationContextProvider must not be {@literal null}
	 */
	public SpelExpressionStringQueryParameterBinder(JpaParameters parameters, Object[] values, StringQuery query,
			ExpressionEvaluationContextProvider evaluationContextProvider) {

		super(parameters, values, query);

		Assert.notNull(evaluationContextProvider, "ExpressionEvaluationContextProvider must not be null!");

		this.query = query;
		this.evaluationContextProvider = evaluationContextProvider;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.ParameterBinder#bind(javax.persistence.Query)
	 */
	@Override
	public <T extends Query> T bind(T jpaQuery) {
		return potentiallyBindExpressionParameters(super.bind(jpaQuery));
	}

	/**
	 * @param jpaQuery must not be {@literal null}
	 * @return
	 */
	private <T extends Query> T potentiallyBindExpressionParameters(T jpaQuery) {

		for (ParameterBinding binding : query.getParameterBindings()) {

			if (binding.isExpression()) {

				Expression expr = new SpelExpressionParser().parseExpression(binding.getExpression());

				EvaluationContext delegatee = evaluationContextProvider.getEvaluationContext();
				StandardEvaluationContext evalContext = new DelegatingStandardEvaluationContext(getValues(), delegatee);
				Object actualValue = expr.getValue(evalContext, String.class);

				if (binding.getName() != null) {
					jpaQuery.setParameter(binding.getName(), binding.prepare(actualValue));
				} else {
					jpaQuery.setParameter(binding.getPosition(), binding.prepare(actualValue));
				}
			}
		}

		return jpaQuery;
	}

	/**
	 * A {@link StandardEvaluationContext} that delegates to the given {@link EvaluationContext}. Variables are first
	 * looked-up locally and if not the lookup is performed against the delegatee.
	 * 
	 * @author Thomas Darimont
	 */
	static class DelegatingStandardEvaluationContext extends StandardEvaluationContext {

		private final EvaluationContext delegatee;

		/**
		 * Creates a new {@link DelegatingStandardEvaluationContext}.
		 * 
		 * @param values must not be {@literal null}
		 * @param delegatee must not be {@literal null}
		 */
		public DelegatingStandardEvaluationContext(Object[] values, EvaluationContext delegatee) {

			super(values);

			Assert.notNull(delegatee, "EvaluationContext delegatee must not be null!");

			this.delegatee = delegatee;
		}

		/* (non-Javadoc)
		 * @see org.springframework.expression.spel.support.StandardEvaluationContext#getConstructorResolvers()
		 */
		@Override
		public List<ConstructorResolver> getConstructorResolvers() {
			return delegatee.getConstructorResolvers();
		}

		/* (non-Javadoc)
		 * @see org.springframework.expression.spel.support.StandardEvaluationContext#getMethodResolvers()
		 */
		@Override
		public List<MethodResolver> getMethodResolvers() {
			return delegatee.getMethodResolvers();
		}

		/* (non-Javadoc)
		 * @see org.springframework.expression.spel.support.StandardEvaluationContext#getPropertyAccessors()
		 */
		@Override
		public List<PropertyAccessor> getPropertyAccessors() {
			return delegatee.getPropertyAccessors();
		}

		/* (non-Javadoc)
		 * @see org.springframework.expression.spel.support.StandardEvaluationContext#getTypeLocator()
		 */
		@Override
		public TypeLocator getTypeLocator() {
			return delegatee.getTypeLocator();
		}

		/* (non-Javadoc)
		 * @see org.springframework.expression.spel.support.StandardEvaluationContext#getTypeConverter()
		 */
		@Override
		public TypeConverter getTypeConverter() {
			return delegatee.getTypeConverter();
		}

		/* (non-Javadoc)
		 * @see org.springframework.expression.spel.support.StandardEvaluationContext#getTypeComparator()
		 */
		@Override
		public TypeComparator getTypeComparator() {
			return delegatee.getTypeComparator();
		}

		/* (non-Javadoc)
		 * @see org.springframework.expression.spel.support.StandardEvaluationContext#getOperatorOverloader()
		 */
		@Override
		public OperatorOverloader getOperatorOverloader() {
			return delegatee.getOperatorOverloader();
		}

		/* (non-Javadoc)
		 * @see org.springframework.expression.spel.support.StandardEvaluationContext#getBeanResolver()
		 */
		@Override
		public BeanResolver getBeanResolver() {
			return delegatee.getBeanResolver();
		}

		/* (non-Javadoc)
		 * @see org.springframework.expression.spel.support.StandardEvaluationContext#lookupVariable(java.lang.String)
		 */
		@Override
		public Object lookupVariable(String name) {

			Object result = super.lookupVariable(name);
			if (result != null) {
				return result;
			}

			return delegatee.lookupVariable(name);
		}
	}
}
