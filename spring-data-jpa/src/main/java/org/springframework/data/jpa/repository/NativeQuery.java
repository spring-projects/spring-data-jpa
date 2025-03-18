/*
 * Copyright 2024-2025 the original author or authors.
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
package org.springframework.data.jpa.repository;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * Annotation to declare native queries directly on repository query methods.
 * <p>
 * Specifically {@code @NativeQuery} is a <em>composed annotation</em> that acts as a shortcut for
 * {@code @Query(nativeQuery = true)} for most attributes.
 * <p>
 * This annotation defines {@code sqlResultSetMapping} to apply JPA SQL ResultSet mapping for native queries. Make sure
 * to use the corresponding return type as defined in {@code @SqlResultSetMapping}. When using named native queries,
 * define SQL result set mapping through {@code @NamedNativeQuery(resultSetMapping=…)} as named queries do not accept
 * {@code sqlResultSetMapping}.
 *
 * @author Danny van den Elshout
 * @author Mark Paluch
 * @since 3.4
 * @see Query
 * @see Modifying
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Documented
@Query(nativeQuery = true)
public @interface NativeQuery {

	/**
	 * Defines the native query to be executed when the annotated method is called. Alias for {@link Query#value()}.
	 */
	@AliasFor(annotation = Query.class)
	String value() default "";

	/**
	 * Defines a special count query that shall be used for pagination queries to look up the total number of elements for
	 * a page. If none is configured we will derive the count query from the original query or {@link #countProjection()}
	 * query if any. Alias for {@link Query#countQuery()}.
	 */
	@AliasFor(annotation = Query.class)
	String countQuery() default "";

	/**
	 * Defines the projection part of the count query that is generated for pagination. If neither {@link #countQuery()}
	 * nor {@code countProjection()} is configured we will derive the count query from the original query. Alias for
	 * {@link Query#countProjection()}.
	 */
	@AliasFor(annotation = Query.class)
	String countProjection() default "";

	/**
	 * The named query to be used. If not defined, a {@link jakarta.persistence.NamedQuery} with name of
	 * {@code ${domainClass}.${queryMethodName}} will be used. Alias for {@link Query#name()}.
	 */
	@AliasFor(annotation = Query.class)
	String name() default "";

	/**
	 * Returns the name of the {@link jakarta.persistence.NamedQuery} to be used to execute count queries when pagination
	 * is used. Will default to the named query name configured suffixed by {@code .count}. Alias for
	 * {@link Query#countName()}.
	 */
	@AliasFor(annotation = Query.class)
	String countName() default "";

	/**
	 * Define a {@link QueryRewriter} that should be applied to the query string after the query is fully assembled. Alias
	 * for {@link Query#queryRewriter()}.
	 */
	@AliasFor(annotation = Query.class)
	Class<? extends QueryRewriter> queryRewriter() default QueryRewriter.IdentityQueryRewriter.class;

	/**
	 * Name of the {@link jakarta.persistence.SqlResultSetMapping @SqlResultSetMapping(name)} to apply for this query.
	 */
	String sqlResultSetMapping() default "";

}
