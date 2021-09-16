/*
 * Copyright 2021 the original author or authors.
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
package org.springframework.data.jpa.repository.support;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.lang.Nullable;

/**
 * Supporting class containing some state and convenience methods for building and executing fluent queries.
 *
 * @param <R> The resulting type of the query.
 * @author Greg Turnquist
 * @since 2.6
 */
abstract class FluentQuerySupport<R> {

	protected final Class<R> resultType;
	protected final Sort sort;
	protected final @Nullable Set<String> properties;
	protected final MappingContext<? extends PersistentEntity<?, ?>, ? extends PersistentProperty<?>> context;

	private final SpelAwareProxyProjectionFactory projectionFactory = new SpelAwareProxyProjectionFactory();

	FluentQuerySupport(Class<R> resultType, Sort sort, @Nullable Collection<String> properties,
			MappingContext<? extends PersistentEntity<?, ?>, ? extends PersistentProperty<?>> context) {

		this.resultType = resultType;
		this.sort = sort;

		if (properties != null) {
			this.properties = new HashSet<>(properties);
		} else {
			this.properties = null;
		}

		this.context = context;
	}

	final Collection<String> mergeProperties(Collection<String> additionalProperties) {

		Set<String> newProperties = new HashSet<>();
		if (this.properties != null) {
			newProperties.addAll(this.properties);
		}
		newProperties.addAll(additionalProperties);
		return Collections.unmodifiableCollection(newProperties);
	}

	@SuppressWarnings("unchecked")
	final <S> Function<Object, R> getConversionFunction(Class<S> inputType, Class<R> targetType) {

		if (targetType.isAssignableFrom(inputType)) {
			return (Function<Object, R>) Function.identity();
		}

		if (targetType.isInterface()) {
			return o -> projectionFactory.createProjection(targetType, o);
		}

		return o -> DefaultConversionService.getSharedInstance().convert(o, targetType);
	}
}
