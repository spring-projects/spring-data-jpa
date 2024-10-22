/*
 * Copyright 2015-2024 the original author or authors.
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
package org.springframework.data.jpa.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * {@code @DisabledOnHibernate} is used to signal that the annotated test class or test method is only <em>disabled</em>
 * if the given Hibernate {@linkplain #value version} is being used.
 *
 * @author Greg Turnquist
 * @author Mark Paluch
 * @since 3.2
 */
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DisabledOnHibernateCondition.class)
public @interface DisabledOnHibernate {

	/**
	 * The version of Hibernate to disable the test or container case on. The version specifier can hold individual
	 * version components matching effectively the version in a prefix-manner. The more specific you want to match, the
	 * more version components you can specify, such as {@code  6.2.1} to match a specific service release or {@code 6} to
	 * match a full major version.
	 */
	String value();

	/**
	 * Custom reason to provide if the test or container is disabled.
	 * <p>
	 * If a custom reason is supplied, it will be combined with the default reason for this annotation. If a custom reason
	 * is not supplied, the default reason will be used.
	 */
	String disabledReason() default "";
}
