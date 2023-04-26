/*
 * Copyright 2015-2023 the original author or authors.
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

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.util.ClassUtils;

/**
 * JUnit 5 utilities to support conditional test cases based upon Hibernate classpath settings.
 *
 * @author Greg Turnquist
 * @since 3.1
 */
abstract class HibernateSupport {

	/**
	 * {@literal org.hibernate.dialect.PostgreSQL91Dialect} is deprecated in Hibernate 6.1 and fully removed in Hibernate
	 * 6.2, making it a perfect detector between the two.
	 */
	private static final boolean HIBERNATE_61_ON_CLASSPATH = ClassUtils
			.isPresent("org.hibernate.dialect.PostgreSQL91Dialect", HibernateSupport.class.getClassLoader());

	private static final boolean HIBERNATE_62_ON_CLASSPATH = !HIBERNATE_61_ON_CLASSPATH;

	static class DisabledWhenHibernate61OnClasspath implements ExecutionCondition {

		@Override
		public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {

			return HibernateSupport.HIBERNATE_61_ON_CLASSPATH
					? ConditionEvaluationResult.disabled("Disabled because Hibernate 6.1 is on the classpath")
					: ConditionEvaluationResult.enabled("NOT disabled because Hibernate 6.2 is on the classpath");
		}
	}

	static class DisabledWhenHibernate62OnClasspath implements ExecutionCondition {

		@Override
		public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {

			return HibernateSupport.HIBERNATE_62_ON_CLASSPATH
					? ConditionEvaluationResult.disabled("Disabled because Hibernate 6.2 is on the classpath")
					: ConditionEvaluationResult.enabled("NOT disabled because Hibernate 6.1 is on the classpath");
		}

	}
}
