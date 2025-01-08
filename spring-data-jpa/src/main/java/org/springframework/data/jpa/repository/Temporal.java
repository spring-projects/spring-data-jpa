/*
 * Copyright 2013-2025 the original author or authors.
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
import java.util.Date;

import jakarta.persistence.TemporalType;

/**
 * Annotation to declare an appropriate {@code TemporalType} on query method parameters. Note that this annotation can
 * only be used on parameters of type {@link Date}.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @deprecated since 4.0. Please use {@literal java.time} types instead.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@Documented
@Deprecated(since = "4.0", forRemoval = true)
public @interface Temporal {

	/**
	 * Defines the {@link TemporalType} to use for the annotated parameter.
	 */
	TemporalType value() default TemporalType.DATE;
}
