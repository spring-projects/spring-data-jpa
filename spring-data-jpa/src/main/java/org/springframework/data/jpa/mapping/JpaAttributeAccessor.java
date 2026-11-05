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
package org.springframework.data.jpa.mapping;

import jakarta.persistence.Tuple;
import jakarta.persistence.metamodel.Attribute;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

import org.springframework.data.util.DirectFieldAccessFallbackBeanWrapper;
import org.springframework.data.util.Lazy;
import org.springframework.util.ReflectionUtils;

/**
 * Domain service to allow accessing {@link Attribute}s of a JPA entity.
 *
 * @author Mark Paluch
 */
class JpaAttributeAccessor {

	private final Object bean;

	private final Lazy<DirectFieldAccessFallbackBeanWrapper> wrapper;

	public JpaAttributeAccessor(Object bean) {
		this.bean = bean;
		this.wrapper = Lazy.of(() -> new DirectFieldAccessFallbackBeanWrapper(bean));
	}

	/**
	 * Return the value for the given {@link Attribute}.
	 *
	 * @param attribute the attribute to obtain.
	 * @return attribute value, can be {@code null}.
	 */
	public @Nullable Object getAttribute(Attribute<?, ?> attribute) {

		if (bean instanceof Tuple tuple) {
			return tuple.get(attribute.getName());
		}

		Member member = attribute.getJavaMember();
		if (member != null && member.getDeclaringClass().isInstance(bean)) {
			if (member instanceof Field field) {
				ReflectionUtils.makeAccessible(field);
				return ReflectionUtils.getField(field, bean);
			}
			if (member instanceof Method method) {
				ReflectionUtils.makeAccessible(method);
				return ReflectionUtils.invokeMethod(method, bean);
			}
		}
		return wrapper.get().getPropertyValue(attribute.getName());
	}

	/**
	 * Return the value for the given {@link Attribute} by its name.
	 *
	 * @param attribute attribute name.
	 * @return attribute value, can be {@code null}.
	 */
	public @Nullable Object getAttribute(String attribute) {
		return bean instanceof Tuple tuple ? tuple.get(attribute) : wrapper.get().getPropertyValue(attribute);
	}

}
