/*
 * Copyright 2026-present the original author or authors.
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

import org.springframework.data.jpa.repository.aot.JpaRepositoryContributor;
import org.springframework.util.ClassUtils;

/**
 * A common delegate for detecting JPA presence and for identifying JPA versions. All the methods of this class can be
 * safely used without any preliminary classpath checks.
 *
 * @author Mark Paluch
 */
public abstract class JpaDetector {

	private static final boolean JPA32_PRESENT = ClassUtils.isPresent("jakarta.persistence.FindOption",
			JpaRepositoryContributor.class.getClassLoader());

	private static final boolean JPA4_PRESENT = ClassUtils.isPresent("jakarta.persistence.EntityAgent",
			JpaRepositoryContributor.class.getClassLoader());

	/**
	 * Determine whether JPA 3.2 (or newer) is present.
	 */
	public static boolean isJpa32Present() {
		return JPA32_PRESENT;
	}

	/**
	 * Determine whether JPA 4.0 is present in general.
	 */
	public static boolean isJpa4Present() {
		return JPA4_PRESENT;
	}

}
