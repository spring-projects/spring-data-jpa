/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.jpa.repository;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.annotation.QueryAnnotation;
import org.springframework.data.jpa.domain.JpaEntityGraph.FetchGraphType;

/**
 * Annotation to configure the JPA 2.1 {@link javax.persistence.EntityGraph}s that should be used on repository methods.
 * 
 * @author Thomas Darimont
 * @since 1.6
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@QueryAnnotation
@Documented
public @interface EntityGraph {

	/**
	 * The name of the EntityGraph to use.
	 * 
	 * @return
	 */
	String value();

	/**
	 * The {@link Type} of the EntityGraph to use, defaults to {@link Type#FETCH}.
	 * 
	 * @return
	 */
	FetchGraphType type() default FetchGraphType.FETCH;
}
