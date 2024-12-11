/*
 * Copyright 2008-2024 the original author or authors.
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
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import jakarta.persistence.QueryHint;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import jakarta.persistence.TypedQuery;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.CollectionExecution;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.ModifyingExecution;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.PagedExecution;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.ProcedureExecution;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.SingleEntityExecution;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.SlicedExecution;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.StreamExecution;
import org.springframework.data.jpa.repository.support.QueryHints;
import org.springframework.data.jpa.util.JpaMetamodel;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.util.Lazy;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Abstract base class to implement {@link RepositoryQuery}s.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Nicolas Cirigliano
 * @author Jens Schauder
 * @author Сергей Цыпанов
 * @author Wonchul Heo
 * @author Julia Lee
 * @author Yanming Zhou
 */
public abstract class AbstractJpaQuery implements RepositoryQuery {

	private final JpaQueryMethod method;
	private final EntityManager em;
	private final JpaMetamodel metamodel;
	private final PersistenceProvider provider;
	private final Lazy<JpaQueryExecution> execution;

	final Lazy<ParameterBinder> parameterBinder = Lazy.of(this::createBinder);

	/**
	 * Creates a new {@link AbstractJpaQuery} from the given {@link JpaQueryMethod}.
	 *
	 * @param method
	 * @param em
	 */
	public AbstractJpaQuery(JpaQueryMethod method, EntityManager em) {

		Assert.notNull(method, "JpaQueryMethod must not be null");
		Assert.notNull(em, "EntityManager must not be null");

		this.method = method;
		this.em = em;
		this.metamodel = JpaMetamodel.of(em.getMetamodel());
		this.provider = PersistenceProvider.fromEntityManager(em);
		this.execution = Lazy.of(() -> {

			if (method.isStreamQuery()) {
				return new StreamExecution();
			} else if (method.isProcedureQuery()) {
				return new ProcedureExecution(method.isCollectionQuery());
			} else if (method.isCollectionQuery()) {
				return new CollectionExecution();
			} else if (method.isSliceQuery()) {
				return new SlicedExecution();
			} else if (method.isPageQuery()) {
				return new PagedExecution();
			} else if (method.isModifyingQuery()) {
				return null;
			} else {
				return new SingleEntityExecution();
			}
		});
	}

	@Override
	public JpaQueryMethod getQueryMethod() {
		return method;
	}

	/**
	 * Returns the {@link EntityManager}.
	 *
	 * @return will never be {@literal null}.
	 */
	protected EntityManager getEntityManager() {
		return em;
	}

	/**
	 * Returns the {@link JpaMetamodel}.
	 *
	 * @return
	 */
	protected JpaMetamodel getMetamodel() {
		return metamodel;
	}

	@Nullable
	@Override
	public Object execute(Object[] parameters) {
		return doExecute(getExecution(), parameters);
	}

	/**
	 * @param execution
	 * @param values
	 * @return
	 */
	@Nullable
	private Object doExecute(JpaQueryExecution execution, Object[] values) {

		JpaParametersParameterAccessor accessor = obtainParameterAccessor(values);
		Object result = execution.execute(this, accessor);

		ResultProcessor withDynamicProjection = method.getResultProcessor().withDynamicProjection(accessor);
		return withDynamicProjection.processResult(result,
				new TupleConverter(withDynamicProjection.getReturnedType(), method.isNativeQuery()));
	}

	private JpaParametersParameterAccessor obtainParameterAccessor(Object[] values) {

		if (method.isNativeQuery() && PersistenceProvider.HIBERNATE.equals(provider)) {
			return new HibernateJpaParametersParameterAccessor(method.getParameters(), values, em);
		}

		return new JpaParametersParameterAccessor(method.getParameters(), values);
	}

	protected JpaQueryExecution getExecution() {

		JpaQueryExecution execution = this.execution.getNullable();

		if (execution != null) {
			return execution;
		}

		if (method.isModifyingQuery()) {
			return new ModifyingExecution(method, em);
		}

		return new SingleEntityExecution();
	}

	/**
	 * Applies the declared query hints to the given query.
	 *
	 * @param query
	 * @return
	 */
	protected <T extends Query> T applyHints(T query, JpaQueryMethod method) {

		List<QueryHint> hints = method.getHints();

		if (!hints.isEmpty()) {
			for (QueryHint hint : hints) {
				applyQueryHint(query, hint);
			}
		}

		// Apply any meta-attributes that exist
		if (method.hasQueryMetaAttributes()) {

			if (provider.getCommentHintKey() != null) {
				query.setHint( //
						provider.getCommentHintKey(), provider.getCommentHintValue(method.getQueryMetaAttributes().getComment()));
			}
		}

		return query;
	}

	/**
	 * Protected to be able to customize in sub-classes.
	 *
	 * @param query must not be {@literal null}.
	 * @param hint must not be {@literal null}.
	 */
	protected <T extends Query> void applyQueryHint(T query, QueryHint hint) {

		Assert.notNull(query, "Query must not be null");
		Assert.notNull(hint, "QueryHint must not be null");

		query.setHint(hint.name(), hint.value());
	}

	/**
	 * Applies the {@link LockModeType} provided by the {@link JpaQueryMethod} to the given {@link Query}.
	 *
	 * @param query must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return
	 */
	private Query applyLockMode(Query query, JpaQueryMethod method) {

		LockModeType lockModeType = method.getLockModeType();
		return lockModeType == null ? query : query.setLockMode(lockModeType);
	}

	ParameterBinder createBinder() {
		return ParameterBinderFactory.createBinder(getQueryMethod().getParameters(), false);
	}

	protected Query createQuery(JpaParametersParameterAccessor parameters) {
		return applyLockMode(applyEntityGraphConfiguration(applyHints(doCreateQuery(parameters), method), method), method);
	}

	/**
	 * Configures the {@link jakarta.persistence.EntityGraph} to use for the given {@link JpaQueryMethod} if the
	 * {@link EntityGraph} annotation is present.
	 *
	 * @param query must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return
	 */
	private Query applyEntityGraphConfiguration(Query query, JpaQueryMethod method) {

		JpaEntityGraph entityGraph = method.getEntityGraph();

		if (entityGraph != null) {
			QueryHints hints = Jpa21Utils.getFetchGraphHint(em, entityGraph,
					getQueryMethod().getEntityInformation().getJavaType());

			hints.forEach(query::setHint);
		}

		return query;
	}

	protected Query createCountQuery(JpaParametersParameterAccessor values) {
		Query countQuery = doCreateCountQuery(values);
		return method.applyHintsToCountQuery() ? applyHints(countQuery, method) : countQuery;
	}

	/**
	 * Returns the type to be used when creating the JPA query.
	 *
	 * @return
	 * @since 2.0.5
	 */
	@Nullable
	protected Class<?> getTypeToRead(ReturnedType returnedType) {

		if (PersistenceProvider.ECLIPSELINK.equals(provider)) {
			return null;
		}

		return returnedType.isProjecting() && !getMetamodel().isJpaManaged(returnedType.getReturnedType()) //
				? Tuple.class //
				: null;
	}

	/**
	 * Creates a {@link Query} instance for the given values.
	 *
	 * @param accessor must not be {@literal null}.
	 * @return
	 */
	protected abstract Query doCreateQuery(JpaParametersParameterAccessor accessor);

	/**
	 * Creates a {@link TypedQuery} for counting using the given values.
	 *
	 * @param accessor must not be {@literal null}.
	 * @return
	 */
	protected abstract Query doCreateCountQuery(JpaParametersParameterAccessor accessor);

	static class TupleConverter implements Converter<Object, Object> {

		private final ReturnedType type;

		private final UnaryOperator<Tuple> tupleWrapper;

		/**
		 * Creates a new {@link TupleConverter} for the given {@link ReturnedType}.
		 *
		 * @param type must not be {@literal null}.
		 */
		public TupleConverter(ReturnedType type) {
			this(type, false);
		}

		/**
		 * Creates a new {@link TupleConverter} for the given {@link ReturnedType}.
		 *
		 * @param type must not be {@literal null}.
		 * @param nativeQuery whether the actual query is a native one to attempt camelCase property names to snake_case
		 *          column names translation in case the exact column name is not found using the requested property name.
		 */
		public TupleConverter(ReturnedType type, boolean nativeQuery) {

			Assert.notNull(type, "Returned type must not be null");

			this.type = type;
			this.tupleWrapper = nativeQuery ? FallbackTupleWrapper::new : UnaryOperator.identity();
		}

		@Override
		public Object convert(Object source) {

			if (!(source instanceof Tuple tuple)) {
				return source;
			}

			List<TupleElement<?>> elements = tuple.getElements();

			if (elements.size() == 1) {

				Object value = tuple.get(elements.get(0));

				if (type.getDomainType().isInstance(value) || type.isInstance(value) || value == null) {
					return value;
				}
			}

			return new TupleBackedMap(tupleWrapper.apply(tuple));
		}

		/**
		 * A {@link Map} implementation which delegates all calls to a {@link Tuple}. Depending on the provided
		 * {@link Tuple} implementation it might return the same value for various keys of which only one will appear in the
		 * key/entry set.
		 *
		 * @author Jens Schauder
		 */
		private static class TupleBackedMap implements Map<String, Object> {

			private static final String UNMODIFIABLE_MESSAGE = "A TupleBackedMap cannot be modified";

			private final Tuple tuple;

			TupleBackedMap(Tuple tuple) {
				this.tuple = tuple;
			}

			@Override
			public int size() {
				return tuple.getElements().size();
			}

			@Override
			public boolean isEmpty() {
				return tuple.getElements().isEmpty();
			}

			/**
			 * If the key is not a {@code String} or not a key of the backing {@link Tuple} this returns {@code false}.
			 * Otherwise this returns {@code true} even when the value from the backing {@code Tuple} is {@code null}.
			 *
			 * @param key the key for which to get the value from the map.
			 * @return whether the key is an element of the backing tuple.
			 */
			@Override
			public boolean containsKey(Object key) {

				try {
					tuple.get((String) key);
					return true;
				} catch (IllegalArgumentException e) {
					return false;
				}
			}

			@Override
			public boolean containsValue(Object value) {
				return Arrays.asList(tuple.toArray()).contains(value);
			}

			/**
			 * If the key is not a {@code String} or not a key of the backing {@link Tuple} this returns {@code null}.
			 * Otherwise the value from the backing {@code Tuple} is returned, which also might be {@code null}.
			 *
			 * @param key the key for which to get the value from the map.
			 * @return the value of the backing {@link Tuple} for that key or {@code null}.
			 */
			@Override
			@Nullable
			public Object get(Object key) {

				if (!(key instanceof String)) {
					return null;
				}

				try {
					return tuple.get((String) key);
				} catch (IllegalArgumentException e) {
					return null;
				}
			}

			@Override
			public Object put(String key, Object value) {
				throw new UnsupportedOperationException(UNMODIFIABLE_MESSAGE);
			}

			@Override
			public Object remove(Object key) {
				throw new UnsupportedOperationException(UNMODIFIABLE_MESSAGE);
			}

			@Override
			public void putAll(Map<? extends String, ?> m) {
				throw new UnsupportedOperationException(UNMODIFIABLE_MESSAGE);
			}

			@Override
			public void clear() {
				throw new UnsupportedOperationException(UNMODIFIABLE_MESSAGE);
			}

			@Override
			public Set<String> keySet() {

				return tuple.getElements().stream() //
						.map(TupleElement::getAlias) //
						.collect(Collectors.toSet());
			}

			@Override
			public Collection<Object> values() {
				return Arrays.asList(tuple.toArray());
			}

			@Override
			public Set<Entry<String, Object>> entrySet() {

				return tuple.getElements().stream() //
						.map(e -> new HashMap.SimpleEntry<String, Object>(e.getAlias(), tuple.get(e))) //
						.collect(Collectors.toSet());
			}
		}
	}

	private static class FallbackTupleWrapper implements Tuple {

		private final Tuple delegate;
		private final UnaryOperator<String> fallbackNameTransformer = JdbcUtils::convertPropertyNameToUnderscoreName;

		FallbackTupleWrapper(Tuple delegate) {
			this.delegate = delegate;
		}

		@Override
		public <X> X get(TupleElement<X> tupleElement) {
			return get(tupleElement.getAlias(), tupleElement.getJavaType());
		}

		@Override
		public <X> X get(String s, Class<X> type) {
			try {
				return delegate.get(s, type);
			} catch (IllegalArgumentException original) {
				try {
					return delegate.get(fallbackNameTransformer.apply(s), type);
				} catch (IllegalArgumentException next) {
					original.addSuppressed(next);
					throw original;
				}
			}
		}

		@Override
		public Object get(String s) {
			try {
				return delegate.get(s);
			} catch (IllegalArgumentException original) {
				try {
					return delegate.get(fallbackNameTransformer.apply(s));
				} catch (IllegalArgumentException next) {
					original.addSuppressed(next);
					throw original;
				}
			}
		}

		@Override
		public <X> X get(int i, Class<X> aClass) {
			return delegate.get(i, aClass);
		}

		@Override
		public Object get(int i) {
			return delegate.get(i);
		}

		@Override
		public Object[] toArray() {
			return delegate.toArray();
		}

		@Override
		public List<TupleElement<?>> getElements() {
			return delegate.getElements();
		}
	}
}
