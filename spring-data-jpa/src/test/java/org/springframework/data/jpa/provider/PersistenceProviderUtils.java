/*
 * Copyright 2023 the original author or authors.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.ReflectionUtils;

/**
 * Utility to hide various {@link PersistenceProvider} from test scenarios.
 *
 * @author Greg Turnquist
 */
public final class PersistenceProviderUtils {

	private PersistenceProviderUtils() {
		// Utility class
	}

	public static void doWithHibernateHidden(Runnable callback) {
		doWithPersistenceProvidersHidden(List.of(PersistenceProvider.HIBERNATE), callback);
	}

	public static void doWithEclipseLinkHidden(Runnable callback) {
		doWithPersistenceProvidersHidden(List.of(PersistenceProvider.ECLIPSELINK), callback);
	}

	public static void doWithPersistenceProvidersHidden(List<PersistenceProvider> persistenceProviders,
			Runnable callback) {

		try (PersistenceProviderHider hidden = new PersistenceProviderHider(persistenceProviders)) {

			hidden.hide();
			callback.run();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Utility class that will hide a list of {@link PersistenceProvider}s visible status, thus altering classpath checks,
	 * and then later restores that status once the try-with-resources block is exited.
	 * 
	 * @author Greg Turnquist
	 */
	private static class PersistenceProviderHider implements AutoCloseable {

		private static Field presentField;
		private Map<PersistenceProvider, Boolean> persistenceProviderCache;

		/**
		 * Make the {@link PersistenceProvider#isPresent()}} field accessible so it can be altered temporarily for test
		 * purposes.
		 */
		static {
			presentField = ReflectionUtils.findField(PersistenceProvider.class, "present");
			ReflectionUtils.makeAccessible(presentField);
		}

		/**
		 * Initialize the cache with {@literal null} values for every {@link PersistenceProvider}.
		 */
		public PersistenceProviderHider(List<PersistenceProvider> persistenceProviders) {

			this.persistenceProviderCache = new HashMap<>();

			persistenceProviders.forEach(persistenceProvider -> {
				this.persistenceProviderCache.put(persistenceProvider, null);
			});
		}

		/**
		 * Cache the current state of the list of {@link PersistenceProvider}s and then set their visible status to
		 * {@literal false}, thus hiding them from classpath checks.
		 */
		public void hide() {

			persistenceProviderCache.keySet().forEach(persistenceProvider -> {

				persistenceProviderCache.put(persistenceProvider, persistenceProvider.isPresent());
				ReflectionUtils.setField(presentField, persistenceProvider, false);
			});
		}

		/**
		 * Update the list of {@link PersistenceProvider}s visible status to their cached value and then null out their
		 * cached status, thus making them available to classpath checks.
		 */
		public void restore() {

			persistenceProviderCache.keySet().forEach(persistenceProvider -> {

				ReflectionUtils.setField(presentField, persistenceProvider, persistenceProviderCache.get(persistenceProvider));
				persistenceProviderCache.put(persistenceProvider, null);
			});
		}

		@Override
		public void close() {
			restore();
		}
	}
}
