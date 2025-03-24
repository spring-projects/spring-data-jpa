/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.data.jpa.repository.aot.generated;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

import org.springframework.data.domain.Sort;
import org.springframework.data.expression.ValueEvaluationContextProvider;
import org.springframework.data.expression.ValueExpression;
import org.springframework.data.jpa.repository.query.DeclaredQuery;
import org.springframework.data.jpa.repository.query.JpaParameters;
import org.springframework.data.jpa.repository.query.QueryEnhancer;
import org.springframework.data.jpa.repository.query.QueryEnhancerSelector;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.query.ParametersSource;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.util.ConcurrentLruCache;

/**
 * @author Mark Paluch
 */
public class AotRepositoryFragmentSupport {

	private final RepositoryMetadata repositoryMetadata;

	private final ValueExpressionDelegate valueExpressions;

	private final ProjectionFactory projectionFactory;

	private final ConcurrentLruCache<DeclaredQuery, QueryEnhancer> enhancers;

	private final ConcurrentLruCache<String, ValueExpression> expressions;

	private final ConcurrentLruCache<Method, ValueEvaluationContextProvider> contextProviders;

	protected AotRepositoryFragmentSupport(QueryEnhancerSelector selector,
			RepositoryFactoryBeanSupport.FragmentCreationContext context) {
		this(selector, context.getRepositoryMetadata(), context.getValueExpressionDelegate(),
				context.getProjectionFactory());
	}

	protected AotRepositoryFragmentSupport(QueryEnhancerSelector selector, RepositoryMetadata repositoryMetadata,
			ValueExpressionDelegate valueExpressions, ProjectionFactory projectionFactory) {

		this.repositoryMetadata = repositoryMetadata;
		this.valueExpressions = valueExpressions;
		this.projectionFactory = projectionFactory;
		this.enhancers = new ConcurrentLruCache<>(32, query -> selector.select(query).create(query));
		this.expressions = new ConcurrentLruCache<>(32, valueExpressions::parse);
		this.contextProviders = new ConcurrentLruCache<>(32, it -> valueExpressions
				.createValueContextProvider(new JpaParameters(ParametersSource.of(repositoryMetadata, it))));
	}

	/**
	 * Rewrite a {@link DeclaredQuery} to apply {@link Sort} and {@link Class} projection.
	 *
	 * @param query
	 * @param sort
	 * @param returnedType
	 * @return
	 */
	protected String rewriteQuery(DeclaredQuery query, Sort sort, Class<?> returnedType) {

		QueryEnhancer queryStringEnhancer = this.enhancers.get(query);
		return queryStringEnhancer.rewrite(new DefaultQueryRewriteInformation(sort,
				ReturnedType.of(returnedType, repositoryMetadata.getDomainType(), projectionFactory)));
	}

	/**
	 * Evaluate a Value Expression.
	 *
	 * @param method
	 * @param expressionString
	 * @param args
	 * @return
	 */
	protected @Nullable Object evaluateExpression(Method method, String expressionString, Object... args) {

		ValueExpression expression = this.expressions.get(expressionString);
		ValueEvaluationContextProvider contextProvider = this.contextProviders.get(method);

		return expression.evaluate(contextProvider.getEvaluationContext(args, expression.getExpressionDependencies()));
	}

	private record DefaultQueryRewriteInformation(Sort sort,
			ReturnedType returnedType) implements QueryEnhancer.QueryRewriteInformation {

		@Override
		public Sort getSort() {
			return sort();
		}

		@Override
		public ReturnedType getReturnedType() {
			return returnedType();
		}

	}

}
