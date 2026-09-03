/*
 * Copyright 2026-present the original author or authors.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.query.sqm.mutation.spi.MultiTableHandler;
import org.hibernate.query.sqm.mutation.spi.MultiTableHandlerBuildResult;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.jspecify.annotations.Nullable;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.util.ClassUtils;

/**
 * Builds the {@link Dialect} used to satisfy Hibernate's bootstrap requirements
 * ({@link org.hibernate.cfg.JdbcSettings#DIALECT}) during the AOT phase.
 * <p>
 * Hibernate 8 relocated and reshaped {@link Dialect} compared to Hibernate 7. {@link #INSTANCE} provides an on-the-fly
 * generated {@link Dialect} implementation that is compatible with Hibernate 7 and 8.
 *
 * @author Christoph Strobl
 * @since 4.2
 */
class AotDialectFactory {

	/**
	 * Flag if {@link Dialect#getMultiTableMutationSupport()} exists so we need to prevent Hibernate 8 from creating an
	 * implementation that we cannot tolerate during AOT.
	 */
	private static final boolean NEEDS_IDENTITY_COLUMN_SUPPORT_OVERRIDE = ClassUtils.hasMethod(Dialect.class,
			"getMultiTableMutationSupport");

	static final Dialect INSTANCE = create();

	static Dialect create() {

		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(Dialect.class);
		enhancer.setClassLoader(Dialect.class.getClassLoader());
		enhancer.setCallback(new AotDialectInterceptor());

		return (Dialect) enhancer.create(new Class<?>[] { DatabaseVersion.class },
				new Object[] { DatabaseVersion.make(1, 0) });
	}

	/**
	 * Resolve the class named {@code simpleName} living next to {@code type} - in its own package (Hibernate 7), or the
	 * {@code .spi} sub-package (Hibernate 8).
	 */
	@SuppressWarnings("NullAway")
	private static Object getAvailableInstanceFor(Class<?> type, String simpleName) {

		Class<?> implementation = resolveAvailableType(type.getPackageName(), simpleName, type.getClassLoader());

		Field instanceField = org.springframework.util.ReflectionUtils.findField(implementation, "INSTANCE");

		if (instanceField != null) {
			org.springframework.util.ReflectionUtils.makeAccessible(instanceField);
			return org.springframework.util.ReflectionUtils.getField(instanceField, null);
		}

		try {

			Constructor<?> constructor = implementation.getDeclaredConstructor();
			org.springframework.util.ReflectionUtils.makeAccessible(constructor);
			return constructor.newInstance();
		} catch (ReflectiveOperationException ex) {
			throw new IllegalStateException("Cannot instantiate %s".formatted(implementation.getName()), ex);
		}
	}

	private static Class<?> resolveAvailableType(String packageName, String simpleName, ClassLoader classLoader) {

		String h7className = packageName + "." + simpleName;
		if (ClassUtils.isPresent(h7className, classLoader)) {
			return ClassUtils.resolveClassName(h7className, classLoader);
		}

		if (!packageName.endsWith(".spi")) {
			String h8className = packageName + ".spi." + simpleName;
			if (ClassUtils.isPresent(h8className, classLoader)) {
				return ClassUtils.resolveClassName(h8className, classLoader);
			}
		}

		throw new IllegalStateException("Unable to resolve [%s] next via [%s]".formatted(simpleName, packageName));
	}

	/**
	 * A generic no-op {@link Proxy} for a pure-behavioral extension point interface where AOT bootstrap only needs
	 * construction not to fail - the returned values are never actually used, since AOT never executes SQL.
	 */
	private static Object noOpProxy(Class<?> interfaceType) {

		InvocationHandler handler = (proxy, method, args) -> {

			Class<?> returnType = method.getReturnType();

			if (returnType.equals(void.class)) {
				return null;
			}

			if (returnType.isPrimitive()) {
				return ReflectionUtils.getPrimitiveDefault(returnType);
			}

			// just use any value for the enum to satisfy the contract
			if (returnType.isEnum()) {
				return returnType.getEnumConstants()[0];
			}

			return null;
		};

		return Proxy.newProxyInstance(interfaceType.getClassLoader(), new Class<?>[] { interfaceType }, handler);
	}

	/**
	 * {@link MethodInterceptor} to handle {@link Dialect} changes between Hibernate 7 and 8 by opting for the most
	 * appropriate implementation based on return type rather than method name since Hiberate 8 keeps moving isntances
	 * around.
	 */
	private static class AotDialectInterceptor implements MethodInterceptor {

		@Override
		public @Nullable Object intercept(Object obj, Method method, Object @Nullable [] args, MethodProxy proxy)
				throws Throwable {

			return switch (method.getName()) {
				case "getLimitHandler" -> getAvailableInstanceFor(method.getReturnType(), "OffsetFetchLimitHandler");
				case "getSequenceSupport" -> getAvailableInstanceFor(method.getReturnType(), "ANSISequenceSupport");
				case "getSqlAstTranslatorFactory" ->
					getAvailableInstanceFor(method.getReturnType(), "StandardSqlAstTranslatorFactory");
				case "getFallbackSqmInsertStrategy" -> FallbackMultiTableStrategies.INSERT;
				case "getFallbackSqmMutationStrategy" -> FallbackMultiTableStrategies.MUTATION;
				// Hibernate 8's getMultiTableMutationSupport() defaults to PERSISTENT_TABLE, which - unlike the
				// getFallbackSqmInsertStrategy/getFallbackSqmMutationStrategy hooks above - always eagerly builds a
				// real strategy at boot, hitting the identity-column support this fake Dialect does not implement.
				case "getIdentityColumnSupport" ->
					NEEDS_IDENTITY_COLUMN_SUPPORT_OVERRIDE ? noOpProxy(method.getReturnType()) : proxy.invokeSuper(obj, args);
				case "isCurrentTimestampSelectStringCallable" -> false;
				case "getCurrentTimestampSelectString" -> "call current_timestamp()";
				// unit == null, kept as reflection-friendly positional check to avoid importing TemporalUnit
				case "timestampdiffPattern" -> args[0] == null ? "(?3-?2)" : "datediff(?1,?2,?3)";
				default -> proxy.invokeSuper(obj, args);
			};
		}
	}

	/**
	 * No-op fallback multi-table mutation strategies for AOT.
	 */
	private static class FallbackMultiTableStrategies {

		static final MultiTableHandler HANDLER = (MultiTableHandler) Proxy.newProxyInstance(
				AotDialectFactory.class.getClassLoader(), new Class[] { MultiTableHandler.class }, (proxy, method, args) -> {

					if (method.getName().equals("createJdbcParameterBindings")) {
						return JdbcParameterBindings.NO_BINDINGS;
					}

					if (method.getReturnType().isPrimitive()) {
						return ReflectionUtils.getPrimitiveDefault(method.getReturnType());
					}

					return null;
				});

		static final SqmMultiTableInsertStrategy INSERT = (SqmMultiTableInsertStrategy) Proxy.newProxyInstance(
				AotDialectFactory.class.getClassLoader(), new Class[] { SqmMultiTableInsertStrategy.class },
				(proxy, method, args) -> fallbackHandlerResult(method));

		static final SqmMultiTableMutationStrategy MUTATION = (SqmMultiTableMutationStrategy) Proxy.newProxyInstance(
				AotDialectFactory.class.getClassLoader(), new Class[] { SqmMultiTableMutationStrategy.class },
				(proxy, method, args) -> fallbackHandlerResult(method));

		private static @Nullable Object fallbackHandlerResult(Method method) {

			if (method.getName().equals("buildHandler")) {
				return new MultiTableHandlerBuildResult(HANDLER, JdbcParameterBindings.NO_BINDINGS);
			}

			if (method.getReturnType().equals(void.class)) {
				return null;
			}

			if (method.getReturnType().isPrimitive()) {
				return ReflectionUtils.getPrimitiveDefault(method.getReturnType());
			}

			return null;
		}
	}
}
