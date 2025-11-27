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
package org.springframework.data.jpa.repository.aot;

import jakarta.persistence.Query;
import jakarta.persistence.Tuple;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.expression.ValueEvaluationContextProvider;
import org.springframework.data.expression.ValueExpression;
import org.springframework.data.jpa.repository.query.DeclaredQuery;
import org.springframework.data.jpa.repository.query.JpaParameters;
import org.springframework.data.jpa.repository.query.JpaResultConverters;
import org.springframework.data.jpa.repository.query.QueryEnhancer;
import org.springframework.data.jpa.repository.query.QueryEnhancerSelector;
import org.springframework.data.jpa.util.TupleBackedMap;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.query.ParametersSource;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.data.util.Lazy;
import org.springframework.util.ConcurrentLruCache;

/**
 * Support class for JPA AOT repository fragments.
 *
 * @author Mark Paluch
 * @since 4.0
 */
public class AotRepositoryFragmentSupport {

	private static final ConversionService CONVERSION_SERVICE;

	static {

		ConfigurableConversionService conversionService = new DefaultConversionService();

		conversionService.addConverter(JpaResultConverters.BlobToByteArrayConverter.INSTANCE);
		conversionService.removeConvertible(Collection.class, Object.class);
		conversionService.removeConvertible(Object.class, Optional.class);

		CONVERSION_SERVICE = conversionService;
	}

	private final RepositoryMetadata repositoryMetadata;

	private final ValueExpressionDelegate valueExpressions;

	private final ProjectionFactory projectionFactory;

	private final Lazy<ConcurrentLruCache<DeclaredQuery, QueryEnhancer>> enhancers;

	private final Lazy<ConcurrentLruCache<String, ValueExpression>> expressions;

	private final Lazy<ConcurrentLruCache<Method, ValueEvaluationContextProvider>> contextProviders;

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
		this.enhancers = Lazy.of(() -> new ConcurrentLruCache<>(32, query -> selector.select(query).create(query)));
		this.expressions = Lazy.of(() -> new ConcurrentLruCache<>(32, valueExpressions::parse));
		this.contextProviders = Lazy.of(() -> new ConcurrentLruCache<>(32, it -> valueExpressions
				.createValueContextProvider(new JpaParameters(ParametersSource.of(repositoryMetadata, it)))));
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

		QueryEnhancer queryStringEnhancer = this.enhancers.get().get(query);
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

		ValueExpression expression = this.expressions.get().get(expressionString);
		ValueEvaluationContextProvider contextProvider = this.contextProviders.get().get(method);

		return expression.evaluate(contextProvider.getEvaluationContext(args, expression.getExpressionDependencies()));
	}

	protected @Nullable Object mapIgnoreCase(@Nullable Object source, UnaryOperator<String> mapper) {

		if (source == null) {
			return null;
		}

		if (source.getClass().isArray()) {
			int length = Array.getLength(source);
			Collection<Object> result = new ArrayList<>(length);

			for (int i = 0; i < length; i++) {
				result.add(Array.get(source, i));
			}
			source = result;
		}

		if (source instanceof Collection<?> c) {

			Collection<@Nullable Object> result = new ArrayList<>(c.size());

			for (Object o : c) {

				if (o instanceof String s) {
					result.add(mapper.apply(s));
				} else {
					result.add(o != null ? mapper.apply(o.toString()) : null);
				}
			}

			return result;
		}

		return source;
	}

	@SuppressWarnings("NullAway")
	protected long getCount(Query query) {

		List<?> totals = query.getResultList();

		if (totals.size() == 1) {
			Object result = totals.get(0);

			if (result instanceof Number n) {
				return n.longValue();
			}

			return CONVERSION_SERVICE.convert(result, Long.class);
		}

		// group by count
		return totals.size();
	}

	protected <T> @Nullable T convertOne(@Nullable Object result, boolean nativeQuery, Class<T> projection) {

		if (result == null) {
			return null;
		}

		if (projection.isInstance(result)) {
			return projection.cast(result);
		}

		if (CONVERSION_SERVICE.canConvert(result.getClass(), projection)) {
			return CONVERSION_SERVICE.convert(result, projection);
		}

		return projectionFactory.createProjection(projection,
				result instanceof Tuple t ? new TupleBackedMap(nativeQuery ? TupleBackedMap.underscoreAware(t) : t) : result);
	}

	protected @Nullable Object convertMany(@Nullable Object result, boolean nativeQuery, Class<?> projection) {

		if (result == null) {
			return null;
		}

		if (projection.isInstance(result)) {
			return result;
		}

		if (result instanceof Stream<?> stream) {
			return stream.map(it -> convertOne(it, nativeQuery, projection));
		}

		if (result instanceof Slice<?> slice) {
			return slice.map(it -> convertOne(it, nativeQuery, projection));
		}

		if (result instanceof Collection<?> collection) {

			Collection<@Nullable Object> target = CollectionFactory.createCollection(collection.getClass(),
					collection.size());
			for (Object o : collection) {
				target.add(convertOne(o, nativeQuery, projection));
			}

			return target;
		}

		throw new UnsupportedOperationException("Cannot create projection for %s".formatted(result));
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
