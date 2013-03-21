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

package org.springframework.data.jpa.auditing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.aspectj.SpringConfiguredConfiguration;

/**
 * Annotation to enable auditing. Imports {@link SpringConfiguredConfiguration}, that allows dependency-injection on objects not managed by
 * spring.
 *
 * @author Ranie Jade Ramiso
 * @since 1.4
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Inherited
@Import({SpringConfiguredConfiguration.class, JpaAuditingRegistrar.class})
public @interface EnableJpaAuditing {

	/**
	 * Reference to a bean of type {@link org.springframework.data.domain.AuditorAware}.
	 */
	String auditorAwareRef() default "";

	/**
	 * Configures whether the creation and modification dates are set.
	 */
	boolean setDates() default true;


	/**
	 * Configuration whether to treat creation also as modification.
	 */
	boolean modifyOnCreate() default true;

	/**
	 * Reference to a bean of type {@link org.springframework.data.auditing.DateTimeProvider}. If not set
	 * a default implementation will be provided.
	 */
	String dateTimeProviderRef() default "";
}
