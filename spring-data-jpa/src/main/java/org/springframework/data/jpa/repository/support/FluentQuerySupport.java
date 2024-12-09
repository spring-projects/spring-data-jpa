/*
 * Copyright 2021-2024 the original author or authors.
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
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.query.AbstractJpaQuery;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.lang.Nullable;

/**
 * Supporting class containing some state and convenience methods for building and executing fluent queries.
 *
 * @param <R> The resulting type of the query.
 * @author Greg Turnquist
 * @author Jens Schauder
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 2.6
 */
abstract class FluentQuerySupport<S, R> {

	protected final ReturnedType returnedType;
	protected final Class<R> resultType;
	protected final Sort sort;
	protected final int limit;
	protected final Set<String> properties;
	protected final Class<S> entityType;
	protected final ProjectionFactory projectionFactory;

	FluentQuerySupport(Class<R> resultType, Sort sort, int limit, @Nullable Collection<String> properties,
		Class<S> entityType, ProjectionFactory projectionFactory) {

		this.returnedType = ReturnedType.of(resultType, entityType, projectionFactory);
		this.resultType = resultType;
		this.sort = sort;
		this.limit = limit;

		if (properties != null) {
			this.properties = new HashSet<>(properties);
		} else {
			this.properties = Collections.emptySet();
		}

		this.entityType = entityType;
		this.projectionFactory = projectionFactory;
	}

	final Collection<String> mergeProperties(Collection<String> additionalProperties) {

		Set<String> newProperties = new HashSet<>();
		newProperties.addAll(properties);
		newProperties.addAll(additionalProperties);
		return Collections.unmodifiableCollection(newProperties);
	}

	@SuppressWarnings("unchecked")
	final Function<Object, R> getConversionFunction(Class<S> inputType, Class<R> targetType) {

		if (targetType.isAssignableFrom(inputType)) {
			return (Function<Object, R>) Function.identity();
		}

		if (returnedType.isProjecting()) {

			AbstractJpaQuery.TupleConverter tupleConverter = new AbstractJpaQuery.TupleConverter(returnedType);

			if (resultType.isInterface()) {
				return o -> projectionFactory.createProjection(targetType, tupleConverter.convert(o));
			}
		}

		return o -> DefaultConversionService.getSharedInstance().convert(o, targetType);
	}

	interface ScrollQueryFactory<Q> {
		Q createQuery(ReturnedType returnedType, Sort sort, ScrollPosition scrollPosition);
	}

}
