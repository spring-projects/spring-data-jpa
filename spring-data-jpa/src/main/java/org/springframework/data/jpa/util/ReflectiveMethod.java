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
package org.springframework.data.jpa.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;

import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * A method resolved by name for reflective invocation.
 * <p>
 * Resolution prefers a public interface declaring the method over the implementation type. Implementation types may
 * live in internal packages without being public, so invoking the interface method avoids making internal members
 * accessible.
 * <p>
 * Intended for internal use only.
 *
 * @author Oscar Fanchin
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 4.2
 */
public class ReflectiveMethod {

	private final Class<?> lookupType;

	private final Method method;

	private ReflectiveMethod(Class<?> lookupType, Method method) {
		this.lookupType = lookupType;

		this.method = method;
		ReflectionUtils.makeAccessible(method);
	}

	/**
	 * Resolve the method named {@code methodName} on {@code type} regardless of its parameters.
	 *
	 * @throws IllegalStateException if no such method exists.
	 */
	public static ReflectiveMethod get(Class<?> type, String methodName) {

		ReflectiveMethod method = find(type, methodName, it -> true);

		if (method == null) {
			throw new IllegalStateException("Cannot resolve %s.%s()".formatted(type.getName(), methodName));
		}

		return method;
	}

	/**
	 * Resolve the method named {@code methodName} on {@code type} accepting exactly {@code parameterTypes}.
	 *
	 * @throws IllegalStateException if no such method exists.
	 */
	public static ReflectiveMethod get(Class<?> type, String methodName, Class<?>... parameterTypes) {

		ReflectiveMethod method = find(type, methodName, it -> Arrays.equals(it.getParameterTypes(), parameterTypes));

		if (method == null) {
			throw new IllegalStateException(
					"Cannot resolve %s.%s(%s)".formatted(type.getName(), methodName, Arrays.asList(parameterTypes)));
		}

		return method;
	}

	/**
	 * Resolve the method named {@code methodName} on {@code type} that matches {@code filter}.
	 *
	 * @throws IllegalStateException if no such method exists.
	 * @see #find(Class, String, ReflectionUtils.MethodFilter)
	 */
	public static ReflectiveMethod get(Class<?> type, String methodName, ReflectionUtils.MethodFilter filter) {

		ReflectiveMethod method = find(type, methodName, filter);

		if (method == null) {
			throw new IllegalStateException("Cannot resolve %s.%s(…)".formatted(type.getName(), methodName));
		}

		return method;
	}

	/**
	 * Find the method named {@code methodName} on {@code type} that matches {@code filter}.
	 *
	 * @return the resolved method, or {@literal null} if none matches.
	 */
	public static @Nullable ReflectiveMethod find(Class<?> type, String methodName, ReflectionUtils.MethodFilter filter) {

		Method method = findMethod(type, methodName, filter);
		return method != null ? new ReflectiveMethod(type, method) : null;
	}

	private static @Nullable Method findMethod(Class<?> type, String methodName, ReflectionUtils.MethodFilter filter) {

		for (Class<?> candidate : ClassUtils.getAllInterfacesForClass(type)) {

			Method method = Modifier.isPublic(candidate.getModifiers()) ? doFindMethod(candidate, methodName, filter) : null;

			if (method != null) {
				return method;
			}
		}

		return doFindMethod(type, methodName, filter);
	}

	private static @Nullable Method doFindMethod(Class<?> type, String methodName, ReflectionUtils.MethodFilter filter) {

		AtomicReference<Method> ref = new AtomicReference<>();

		ReflectionUtils.doWithMethods(type, it -> ref.compareAndSet(null, it),
				it -> it.getName().equals(methodName) && filter.matches(it));

		return ref.get();
	}

	/**
	 * Type on which the method was looked up.
	 *
	 * @return the type on which the method was looked up.
	 */
	public Class<?> getLookupType() {
		return lookupType;
	}

	public String getMethodName() {
		return method.getName();
	}

	public Class<?> getReturnType() {
		return method.getReturnType();
	}

	/**
	 * Invoke the method on {@code target}, rethrowing target exceptions as
	 * {@link ReflectionUtils#invokeMethod(Method, Object, Object...)} does.
	 *
	 * @param target the receiver, or {@literal null} for a static method.
	 */
	public @Nullable Object invoke(@Nullable Object target, Object... args) {
		return ReflectionUtils.invokeMethod(method, target, args);
	}

	@Override
	public String toString() {
		return method.toString();
	}

}
