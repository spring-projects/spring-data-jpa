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

import org.springframework.data.jpa.repository.query.JpaParameters.JpaParameter;
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
 * A {@link ParameterBinder} that is able to detect and dynamically evaluate SpEL expression based parameters.
 * 
 * @author Thomas Darimont
 */
class ExpressionAwareParameterBinder extends ParameterBinder {

	private final ExpressionEvaluationContextProvider evaluationContextProvider;

	/**
	 * Creates a new {@literal ExpressionAwareParameterBinder}.
	 * 
	 * @param parameters
	 * @param evaluationContextProvider
	 */
	public ExpressionAwareParameterBinder(JpaParameters parameters,
			ExpressionEvaluationContextProvider evaluationContextProvider) {
		this(parameters, new Object[0], evaluationContextProvider);
	}

	/**
	 * Creates a new {@literal ExpressionAwareParameterBinder}.
	 * 
	 * @param parameters
	 * @param values
	 * @param evaluationContextProvider
	 */
	public ExpressionAwareParameterBinder(JpaParameters parameters, Object[] values,
			ExpressionEvaluationContextProvider evaluationContextProvider) {

		super(parameters, values);

		Assert.notNull(evaluationContextProvider, "ExpressionEvaluationContextProvider must not be null!");

		this.evaluationContextProvider = evaluationContextProvider;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.ParameterBinder#computeParameterValue(org.springframework.data.jpa.repository.query.JpaParameters.JpaParameter, java.lang.Object, java.lang.Object[])
	 */
	@Override
	protected Object computeParameterValue(JpaParameter parameter, Object value, Object[] values) {

		if (!parameter.isExpressionParameter()) {
			return super.computeParameterValue(parameter, value, values);
		}

		if (value instanceof String) {
			return evaluateExpression((String) value);
		}

		if (value instanceof Expression) {
			return evaluateExpression((Expression) value);
		}

		throw new IllegalArgumentException("Cannot convert Value: " + value + " to a SpEL expression");
	}

	/**
	 * Parses the given {@code expressionString} into a SpEL {@link Expression}.
	 * 
	 * @param expressionString
	 * @return
	 */
	protected Expression parseExpressionString(String expressionString) {
		return new SpelExpressionParser().parseExpression(expressionString);
	}

	/**
	 * Evaluates the given {@code expressionString} as a SpEL {@link Expression}.
	 * 
	 * @param expressionString
	 * @return
	 */
	protected Object evaluateExpression(String expressionString) {
		return evaluateExpression(parseExpressionString(expressionString));
	}

	/**
	 * Evaluates the given SpEL {@link Expression}.
	 * 
	 * @param expr
	 * @return
	 */
	protected Object evaluateExpression(Expression expr) {
		return expr.getValue(getEvaluationContext(), Object.class);
	}

	/**
	 * Returns the {@link StandardEvaluationContext} to use for evaluation.
	 * 
	 * @return
	 */
	protected StandardEvaluationContext getEvaluationContext() {

		EvaluationContext delegatee = evaluationContextProvider.getEvaluationContext();
		StandardEvaluationContext evalContext = new DelegatingStandardEvaluationContext(delegatee);

		populateParameterVariables(evalContext);

		return evalContext;
	}

	private void populateParameterVariables(StandardEvaluationContext evalContext) {

		for (JpaParameter param : getParameters()) {
			if (param.isNamedParameter()) {
				evalContext.setVariable(param.getName(), getValues()[param.getIndex()]);
			}
		}

		evalContext.setVariable("args", getValues());
	}

	/**
	 * A {@link StandardEvaluationContext} that delegates to the given {@link EvaluationContext}. Variables are first
	 * looked-up locally and if not the lookup is performed against the delegatee.
	 * 
	 * @author Thomas Darimont
	 */
	protected static class DelegatingStandardEvaluationContext extends StandardEvaluationContext {

		private final EvaluationContext delegatee;

		/**
		 * Creates a new {@link DelegatingStandardEvaluationContext}.
		 * 
		 * @param delegatee must not be {@literal null}
		 */
		public DelegatingStandardEvaluationContext(EvaluationContext delegatee) {

			super(delegatee.getRootObject());

			Assert.notNull(delegatee, "EvaluationContext delegatee must not be null!");

			this.delegatee = delegatee;
			setRootObject(delegatee.getRootObject().getValue(), delegatee.getRootObject().getTypeDescriptor());
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
