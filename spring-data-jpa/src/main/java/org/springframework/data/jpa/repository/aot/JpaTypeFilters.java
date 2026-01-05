/*
 * Copyright 2025-present the original author or authors.
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

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.function.Predicate;

import org.springframework.data.util.Predicates;
import org.springframework.data.util.TypeCollector;
import org.springframework.data.util.TypeUtils;

/**
 * {@link TypeCollector} predicates to exclude JPA provider types.
 *
 * @author Mark Paluch
 * @since 4.0
 */
class JpaTypeFilters implements TypeCollector.TypeCollectorFilters {

	/**
	 * Match for bytecode-enhanced members.
	 */
	private static final Predicate<Member> IS_HIBERNATE_MEMBER = member -> member.getName().startsWith("$$_hibernate");

	private static final Predicate<Class<?>> CLASS_FILTER = it -> TypeUtils.type(it).isPartOf("org.hibernate",
			"org.eclipse.persistence", "jakarta.persistence");

	@Override
	public Predicate<Class<?>> classPredicate() {
		return CLASS_FILTER.negate();
	}

	@Override
	public Predicate<Field> fieldPredicate() {
		return Predicates.<Field> declaringClass(CLASS_FILTER).or(IS_HIBERNATE_MEMBER).negate();
	}

	@Override
	public Predicate<Method> methodPredicate() {
		return Predicates.<Method> declaringClass(CLASS_FILTER).or(IS_HIBERNATE_MEMBER).negate();
	}

}
