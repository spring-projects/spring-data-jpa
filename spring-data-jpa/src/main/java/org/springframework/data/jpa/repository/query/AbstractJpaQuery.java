/*
 * Copyright 2008-2025 the original author or authors.
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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.core.MethodParameter;
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
import org.springframework.data.jpa.util.TupleBackedMap;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.model.PreferredConstructorDiscoverer;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.util.Lazy;
import org.springframework.lang.Contract;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

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

	@Override
	public @Nullable Object execute(Object[] parameters) {
		return doExecute(getExecution(), parameters);
	}

	/**
	 * @param execution
	 * @param values
	 * @return
	 */
	private @Nullable Object doExecute(JpaQueryExecution execution, Object[] values) {

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
	@SuppressWarnings("NullAway")
	@Contract("_, _ -> param1")
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
	protected @Nullable Class<?> getTypeToRead(ReturnedType returnedType) {

		if (PersistenceProvider.ECLIPSELINK.equals(provider)) {
			return null;
		}

		return returnedType.isProjecting() && returnedType.getReturnedType().isInterface()
				&& !getMetamodel().isJpaManaged(returnedType.getReturnedType()) //
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

	public static class TupleConverter implements Converter<Object, Object> {

		private final ReturnedType type;

		private final UnaryOperator<Tuple> tupleWrapper;

		private final boolean dtoProjection;

		private final @Nullable PreferredConstructor<?, ?> preferredConstructor;

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
			this.tupleWrapper = nativeQuery ? TupleBackedMap::underscoreAware : UnaryOperator.identity();
			this.dtoProjection = type.isProjecting() && !type.getReturnedType().isInterface()
					&& !type.getInputProperties().isEmpty();

			if (this.dtoProjection) {
				this.preferredConstructor = PreferredConstructorDiscoverer.discover(type.getReturnedType());
			} else {
				this.preferredConstructor = null;
			}
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

			if (dtoProjection) {

				Object[] ctorArgs = new Object[elements.size()];
				for (int i = 0; i < ctorArgs.length; i++) {
					ctorArgs[i] = tuple.get(i);
				}

				List<Class<?>> argTypes = getArgumentTypes(ctorArgs);

				if (preferredConstructor != null && isConstructorCompatible(preferredConstructor.getConstructor(), argTypes)) {
					return BeanUtils.instantiateClass(preferredConstructor.getConstructor(), ctorArgs);
				}

				return BeanUtils.instantiateClass(getFirstMatchingConstructor(ctorArgs, argTypes), ctorArgs);
			}

			return new TupleBackedMap(tupleWrapper.apply(tuple));
		}

		private Constructor<?> getFirstMatchingConstructor(Object[] ctorArgs, List<Class<?>> argTypes) {

			for (Constructor<?> ctor : type.getReturnedType().getDeclaredConstructors()) {

				if (ctor.getParameterCount() != ctorArgs.length) {
					continue;
				}

				if (isConstructorCompatible(ctor, argTypes)) {
					return ctor;
				}
			}

			throw new IllegalStateException(String.format(
					"Cannot find compatible constructor for DTO projection '%s' accepting '%s'", type.getReturnedType().getName(),
					argTypes.stream().map(Class::getName).collect(Collectors.joining(", "))));
		}

		private static List<Class<?>> getArgumentTypes(Object[] ctorArgs) {
			List<Class<?>> argTypes = new ArrayList<>(ctorArgs.length);

			for (Object ctorArg : ctorArgs) {
				argTypes.add(ctorArg == null ? Void.class : ctorArg.getClass());
			}
			return argTypes;
		}

		public static boolean isConstructorCompatible(Constructor<?> constructor, List<Class<?>> argumentTypes) {

			if (constructor.getParameterCount() != argumentTypes.size()) {
				return false;
			}

			for (int i = 0; i < argumentTypes.size(); i++) {

				MethodParameter methodParameter = MethodParameter.forExecutable(constructor, i);
				Class<?> argumentType = argumentTypes.get(i);

				if (!areAssignmentCompatible(methodParameter.getParameterType(), argumentType)) {
					return false;
				}
			}
			return true;
		}

		private static boolean areAssignmentCompatible(Class<?> to, Class<?> from) {

			if (from == Void.class && !to.isPrimitive()) {
				// treat Void as the bottom type, the class of null
				return true;
			}

			if (to.isPrimitive()) {

				if (to == Short.TYPE) {
					return from == Character.class || from == Byte.class;
				}

				if (to == Integer.TYPE) {
					return from == Short.class || from == Character.class || from == Byte.class;
				}

				if (to == Long.TYPE) {
					return from == Integer.class || from == Short.class || from == Character.class || from == Byte.class;
				}

				if (to == Double.TYPE) {
					return from == Float.class;
				}

				return ClassUtils.isAssignable(to, from);
			}

			return ClassUtils.isAssignable(to, from);
		}

	}

}
