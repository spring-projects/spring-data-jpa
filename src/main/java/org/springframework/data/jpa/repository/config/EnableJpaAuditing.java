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
package org.springframework.data.jpa.repository.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;
import org.springframework.data.auditing.DateTimeProvider;

/**
 * Annotation to enable auditing in JPA via annotation configuration.
 * 
 * @author Thomas Darimont
 */
@Inherited
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(JpaAuditingRegistrar.class)
public @interface EnableJpaAuditing {

	/**
	 * @return References a bean of type AuditorAware to represent the current principal.
	 */
	String auditorAwareRef() default "";

	/**
	 * @return Configures whether the creation and modification dates are set and defaults to {@literal true}.
	 */
	boolean setDates() default true;

	/**
	 * @return Configures whether the entity shall be marked as modified on creation and defaults to {@literal true}.
	 */
	boolean modifyOnCreate() default true;

	/**
	 * @return Configures a {@link DateTimeProvider} that allows customizing which DateTime shall be used for setting
	 *         creation and modification dates.
	 */
	String dateTimeProviderRef() default "";
}
