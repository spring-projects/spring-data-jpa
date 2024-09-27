/*
 * Copyright 2013-2024 the original author or authors.
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

import org.springframework.data.jpa.repository.QueryRewriter;
import org.springframework.data.repository.query.QueryCreationException;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.lang.Nullable;

/**
 * Factory to create the appropriate {@link RepositoryQuery} for a {@link JpaQueryMethod}.
 *
 * @author Thomas Darimont
 * @author Mark Paluch
 */
enum JpaQueryFactory {

	INSTANCE;

	/**
	 * Creates a {@link RepositoryQuery} from the given {@link String} query.
	 */
	AbstractJpaQuery fromMethodWithQueryString(JpaQueryMethod method, EntityManager em, String queryString,
			@Nullable String countQueryString, QueryRewriter queryRewriter,
			ValueExpressionDelegate valueExpressionDelegate) {

		if (method.isScrollQuery()) {
			throw QueryCreationException.create(method, "Scroll queries are not supported using String-based queries");
		}

		return method.isNativeQuery()
				? new NativeJpaQuery(method, em, queryString, countQueryString, queryRewriter, valueExpressionDelegate)
				: new SimpleJpaQuery(method, em, queryString, countQueryString, queryRewriter, valueExpressionDelegate);
	}

	/**
	 * Creates a {@link StoredProcedureJpaQuery} from the given {@link JpaQueryMethod} query.
	 *
	 * @param method must not be {@literal null}.
	 * @param em must not be {@literal null}.
	 * @return
	 */
	public StoredProcedureJpaQuery fromProcedureAnnotation(JpaQueryMethod method, EntityManager em) {

		if (method.isScrollQuery()) {
			throw QueryCreationException.create(method, "Scroll queries are not supported using stored procedures");
		}

		return new StoredProcedureJpaQuery(method, em);
	}
}
