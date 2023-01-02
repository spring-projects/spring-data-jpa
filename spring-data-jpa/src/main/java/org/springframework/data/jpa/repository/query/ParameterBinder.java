/*
 * Copyright 2008-2023 the original author or authors.
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

import jakarta.persistence.Query;

import org.springframework.data.jpa.repository.query.QueryParameterSetter.ErrorHandling;
import org.springframework.data.jpa.support.PageableUtils;
import org.springframework.util.Assert;

/**
 * {@link ParameterBinder} is used to bind method parameters to a {@link Query}. This is usually done whenever an
 * {@link AbstractJpaQuery} is executed.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Jens Schauder
 */
public class ParameterBinder {

	static final String PARAMETER_NEEDS_TO_BE_NAMED = "For queries with named parameters you need to provide names for method parameters; Use @Param for query method parameters, or when on Java 8+ use the javac flag -parameters";

	private final JpaParameters parameters;
	private final Iterable<QueryParameterSetter> parameterSetters;
	private final boolean useJpaForPaging;

	/**
	 * Creates a new {@link ParameterBinder} for the given {@link JpaParameters} and {@link QueryParameterSetter}s.
	 * Defaults to use JPA API to apply pagination offsets.
	 *
	 * @param parameters must not be {@literal null}.
	 * @param parameterSetters must not be {@literal null}.
	 * @since 2.0.6
	 */
	ParameterBinder(JpaParameters parameters, Iterable<QueryParameterSetter> parameterSetters) {
		this(parameters, parameterSetters, true);
	}

	/**
	 * Creates a new {@link ParameterBinder} for the given {@link JpaParameters} and {@link QueryParameterSetter}s.
	 *
	 * @param parameters must not be {@literal null}.
	 * @param parameterSetters must not be {@literal null}.
	 * @param useJpaForPaging determines whether {@link Query#setFirstResult(int)} and {@link Query#setMaxResults(int)}
	 *          shall be used for paging.
	 */
	public ParameterBinder(JpaParameters parameters, Iterable<QueryParameterSetter> parameterSetters,
			boolean useJpaForPaging) {

		Assert.notNull(parameters, "JpaParameters must not be null");
		Assert.notNull(parameterSetters, "Parameter setters must not be null");

		this.parameters = parameters;
		this.parameterSetters = parameterSetters;
		this.useJpaForPaging = useJpaForPaging;
	}

	public <T extends Query> T bind(T jpaQuery, QueryParameterSetter.QueryMetadata metadata,
			JpaParametersParameterAccessor accessor) {
		bind(metadata.withQuery(jpaQuery), accessor, ErrorHandling.STRICT);
		return jpaQuery;
	}

	public void bind(QueryParameterSetter.BindableQuery query, JpaParametersParameterAccessor accessor,
			ErrorHandling errorHandling) {

		for (QueryParameterSetter setter : parameterSetters) {
			setter.setParameter(query, accessor, errorHandling);
		}
	}

	/**
	 * Binds the parameters to the given query and applies special parameter types (e.g. pagination).
	 *
	 * @param query must not be {@literal null}.
	 * @param metadata must not be {@literal null}.
	 * @param accessor must not be {@literal null}.
	 */
	Query bindAndPrepare(Query query, QueryParameterSetter.QueryMetadata metadata,
			JpaParametersParameterAccessor accessor) {

		bind(query, metadata, accessor);

		if (!useJpaForPaging || !parameters.hasPageableParameter() || accessor.getPageable().isUnpaged()) {
			return query;
		}

		query.setFirstResult(PageableUtils.getOffsetAsInteger(accessor.getPageable()));
		query.setMaxResults(accessor.getPageable().getPageSize());

		return query;
	}
}
