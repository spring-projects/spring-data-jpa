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
package org.springframework.data.jpa.repository;

import org.springframework.core.annotation.AliasFor;
import org.springframework.data.annotation.QueryAnnotation;

import java.lang.annotation.*;

/**
 * Annotation to declare native queries directly on repository methods.
 * <p>
 * Specifically {@code @NativeQuery} is a <em>composed annotation</em> that
 * acts as a shortcut for {@code @Query(nativeQuery = true)}.
 *
 * @author Danny van den Elshout
 * @since 3.3
 * @see Query
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@QueryAnnotation
@Documented
@Query(nativeQuery = true)
public @interface NativeQuery {

    /**
     * Alias for {@link Query#value()}
     */
    @AliasFor(annotation = Query.class)
    String value() default "";

    /**
     * Alias for {@link Query#countQuery()}
     */
    @AliasFor(annotation = Query.class)
    String countQuery() default "";

    /**
     * Alias for {@link Query#countProjection()}
     */
    @AliasFor(annotation = Query.class)
    String countProjection() default "";

    /**
     * Alias for {@link Query#name()}
     */
    @AliasFor(annotation = Query.class)
    String name() default "";

    /**
     * Alias for {@link Query#countName()}
     */
    @AliasFor(annotation = Query.class)
    String countName() default "";

    /**
     * Alias for {@link Query#queryRewriter()}
     */
    @AliasFor(annotation = Query.class)
    Class<? extends QueryRewriter> queryRewriter() default QueryRewriter.IdentityQueryRewriter.class;
}
