/*
 * Copyright 2024 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.util.ClassUtils;

/**
 * Pre-defined QueryEnhancerFactories to be used for query enhancement.
 *
 * @author Mark Paluch
 */
public class QueryEnhancerFactories {

	private static final Log LOG = LogFactory.getLog(QueryEnhancerFactory.class);

	static final boolean jSqlParserPresent = ClassUtils.isPresent("net.sf.jsqlparser.parser.JSqlParser",
			QueryEnhancerFactory.class.getClassLoader());

	static {

		if (jSqlParserPresent) {
			LOG.info("JSqlParser is in classpath; If applicable, JSqlParser will be used");
		}

		if (PersistenceProvider.ECLIPSELINK.isPresent()) {
			LOG.info("EclipseLink is in classpath; If applicable, EQL parser will be used.");
		}

		if (PersistenceProvider.HIBERNATE.isPresent()) {
			LOG.info("Hibernate is in classpath; If applicable, HQL parser will be used.");
		}
	}

	enum BuiltinQueryEnhancerFactories implements QueryEnhancerFactory {

		FALLBACK {
			@Override
			public boolean supports(DeclaredQuery query) {
				return true;
			}

			@Override
			public QueryEnhancer create(DeclaredQuery query) {
				return new DefaultQueryEnhancer(query);
			}
		},

		JSQLPARSER {
			@Override
			public boolean supports(DeclaredQuery query) {
				return query.isNativeQuery();
			}

			@Override
			public QueryEnhancer create(DeclaredQuery query) {
				if (jSqlParserPresent) {
					return new JSqlParserQueryEnhancer(query);
				}

				throw new IllegalStateException("JSQLParser is not available on the class path");
			}
		},

		HQL {
			@Override
			public boolean supports(DeclaredQuery query) {
				return !query.isNativeQuery();
			}

			@Override
			public QueryEnhancer create(DeclaredQuery query) {
				return JpaQueryEnhancer.forHql(query.getQueryString());
			}
		},
		EQL {
			@Override
			public boolean supports(DeclaredQuery query) {
				return !query.isNativeQuery();
			}

			@Override
			public QueryEnhancer create(DeclaredQuery query) {
				return JpaQueryEnhancer.forEql(query.getQueryString());
			}
		},
		JPQL {
			@Override
			public boolean supports(DeclaredQuery query) {
				return !query.isNativeQuery();
			}

			@Override
			public QueryEnhancer create(DeclaredQuery query) {
				return JpaQueryEnhancer.forJpql(query.getQueryString());
			}
		}
	}

	/**
	 * Returns the default fallback {@link QueryEnhancerFactory} using regex-based detection. This factory supports only
	 * simple SQL queries.
	 *
	 * @return fallback {@link QueryEnhancerFactory} using regex-based detection.
	 */
	public static QueryEnhancerFactory fallback() {
		return BuiltinQueryEnhancerFactories.FALLBACK;
	}

	/**
	 * Returns a {@link QueryEnhancerFactory} that uses <a href="https://github.com/JSQLParser/JSqlParser">JSqlParser</a>
	 * if it is available from the class path.
	 *
	 * @return a {@link QueryEnhancerFactory} that uses <a href="https://github.com/JSQLParser/JSqlParser">JSqlParser</a>.
	 * @throws IllegalStateException if JSQLParser is not on the class path.
	 */
	public static QueryEnhancerFactory jsqlparser() {

		if (!jSqlParserPresent) {
			throw new IllegalStateException("JSQLParser is not available on the class path");
		}

		return BuiltinQueryEnhancerFactories.JSQLPARSER;
	}

	/**
	 * Returns a {@link QueryEnhancerFactory} using HQL (Hibernate Query Language) parser.
	 *
	 * @return a {@link QueryEnhancerFactory} using HQL (Hibernate Query Language) parser.
	 */
	public static QueryEnhancerFactory hql() {
		return BuiltinQueryEnhancerFactories.HQL;
	}

	/**
	 * Returns a {@link QueryEnhancerFactory} using EQL (EclipseLink Query Language) parser.
	 *
	 * @return a {@link QueryEnhancerFactory} using EQL (EclipseLink Query Language) parser.
	 */
	public static QueryEnhancerFactory eql() {
		return BuiltinQueryEnhancerFactories.EQL;
	}

	/**
	 * Returns a {@link QueryEnhancerFactory} using JPQL (Jakarta Persistence Query Language) parser as per the JPA spec.
	 *
	 * @return a {@link QueryEnhancerFactory} using JPQL (Jakarta Persistence Query Language) parser as per the JPA spec.
	 */
	public static QueryEnhancerFactory jpql() {
		return BuiltinQueryEnhancerFactories.JPQL;
	}
}
