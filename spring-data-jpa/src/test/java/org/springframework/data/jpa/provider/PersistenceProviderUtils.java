/*
 * Copyright 2014-2023 the original author or authors.
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
package org.springframework.data.jpa.provider;

import java.lang.reflect.Field;

import org.springframework.util.ReflectionUtils;

/**
 * Utilities to ease testing in various JPA provider modes.
 *
 * @author Greg Turnquist
 */
public class PersistenceProviderUtils {

	/**
	 * Temporarily configure {@link PersistenceProvider} to appear to be Hibernate.
	 *
	 * @param runnable that contains any test code to run in a "Hibernate" mode
	 */
	public static void doWithHibernate(Runnable runnable) {

		Field typedParameterValueClassField = findTypedParameterValueClass();
		Object currentValue = ReflectionUtils.getField(typedParameterValueClassField, PersistenceProvider.class);

		try {
			ReflectionUtils.setField(typedParameterValueClassField, PersistenceProvider.class, Object.class);

			runnable.run();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			ReflectionUtils.setField(typedParameterValueClassField, PersistenceProvider.class, currentValue);
		}

	}

	/**
	 * Temporarily configure {@link PersistenceProvider} to appear to be EclipseLink.
	 *
	 * @param runnable that contains any test code to run in a "EclipseLink" mode
	 */
	public static void doWithEclipseLink(Runnable runnable) {

		Field typedParameterValueClassField = findTypedParameterValueClass();
		Object currentValue = ReflectionUtils.getField(typedParameterValueClassField, PersistenceProvider.class);

		try {
			ReflectionUtils.setField(typedParameterValueClassField, PersistenceProvider.class, null);

			runnable.run();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			ReflectionUtils.setField(typedParameterValueClassField, PersistenceProvider.class, currentValue);
		}
	}

	private static Field findTypedParameterValueClass() {

		Field typedParameterValueClassField = ReflectionUtils.findField(PersistenceProvider.class,
				"typedParameterValueClass");
		ReflectionUtils.makeAccessible(typedParameterValueClassField);
		return typedParameterValueClassField;
	}

}
