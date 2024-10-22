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
package org.springframework.data.jpa.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.extension.ExecutionCondition;

/**
 * {@link ExecutionCondition} for {@link DisabledOnHibernate @DisabledOnHibernate}.
 *
 * @see DisabledOnHibernate
 */
class DisabledOnHibernateCondition extends BooleanExecutionCondition<DisabledOnHibernate> {

	static final String ENABLED_ON_CURRENT_HIBERNATE = //
			"Enabled on Hibernate version: " + org.hibernate.Version.getVersionString();

	static final String DISABLED_ON_CURRENT_HIBERNATE = //
			"Disabled on Hibernate version: " + org.hibernate.Version.getVersionString();

	DisabledOnHibernateCondition() {
		super(DisabledOnHibernate.class, ENABLED_ON_CURRENT_HIBERNATE, DISABLED_ON_CURRENT_HIBERNATE,
				DisabledOnHibernate::disabledReason);
	}

	@Override
	boolean isEnabled(DisabledOnHibernate annotation) {

		VersionMatcher disabled = VersionMatcher.parse(annotation.value());
		VersionMatcher hibernate = VersionMatcher.parse(org.hibernate.Version.getVersionString());

		return !disabled.matches(hibernate);
	}

	static class VersionMatcher {

		private static final Pattern PATTERN = Pattern.compile("(\\d+)+");
		private final int[] components;

		private VersionMatcher(int[] components) {
			this.components = components;
		}

		/**
		 * Parse the given version string into a {@link VersionMatcher}.
		 *
		 * @param version
		 * @return
		 */
		public static VersionMatcher parse(String version) {

			Matcher matcher = PATTERN.matcher(version);
			List<Integer> ints = new ArrayList<>();
			while (matcher.find()) {
				ints.add(Integer.parseInt(matcher.group()));
			}

			return new VersionMatcher(ints.stream().mapToInt(value -> value).toArray());
		}

		/**
		 * Match the given version against another VersionMatcher. This matcher's version spec controls the expected length.
		 * If the other version is shorter, then the match returns {@code false}.
		 *
		 * @param version
		 * @return
		 */
		public boolean matches(VersionMatcher version) {

			for (int i = 0; i < components.length; i++) {
				if (version.components.length <= i) {
					return false;
				}
				if (components[i] != version.components[i]) {
					return false;
				}
			}

			return true;
		}
	}

}
