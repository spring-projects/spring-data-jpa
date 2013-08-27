/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository.query;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * @author Thomas Darimont
 */
enum JpaQueryFactory {
	INSTANCE;

	private static final Logger LOG = LoggerFactory.getLogger(JpaQueryFactory.class);

	/**
	 * Creates a {@link RepositoryQuery} from the given {@link org.springframework.data.repository.query.QueryMethod} that
	 * is potentially annotated with {@link org.springframework.data.jpa.repository.Query}.
	 * 
	 * @param queryMethod
	 * @param em
	 * @return the {@link RepositoryQuery} derived from the annotation or {@code null} if no annotation found.
	 */
	RepositoryQuery fromQueryAnnotation(JpaQueryMethod queryMethod, EntityManager em) {

		LOG.debug("Looking up query for method {}", queryMethod.getName());

		return fromMethodWithQueryString(queryMethod, em, queryMethod.getAnnotatedQuery());
	}

	/**
	 * @param method
	 * @param em
	 * @param queryString
	 * @return
	 */
	RepositoryQuery fromMethodWithQueryString(JpaQueryMethod method, EntityManager em, String queryString) {

		if (queryString == null) {
			return null;
		}

		if (method.isNativeQuery()) {
			return new NativeJpaQuery(method, em, queryString);
		}

		return new SimpleJpaQuery(method, em, queryString);
	}
}
