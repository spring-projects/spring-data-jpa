/*
 * Copyright 2025-present the original author or authors.
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
package org.springframework.data.jpa.repository.aot;

import static org.assertj.core.api.Assertions.*;

import jakarta.persistence.LockOption;

import org.eclipse.persistence.sessions.DatabaseSession;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;

import org.springframework.data.util.TypeCollector;

/**
 * Unit tests for {@link JpaTypeFilters}.
 *
 * @author Mark Paluch
 */
class JpaTypeFiltersUnitTests {

	@Test // GH-4014
	void shouldFilterUnreachableField() {
		assertThat(TypeCollector.inspect(EnhancedEntity.class).list()).containsOnly(EnhancedEntity.class, Reachable.class);
	}

	static class Unreachable {

	}

	static class Reachable {

	}

	static class EnhancedEntity {

		private Unreachable $$_hibernate_field;
		private Reachable reachable;
		private Session session;
		private DatabaseSession databaseSession;
		private LockOption lockOption;

		public EnhancedEntity(Session session, LockOption lockOption) {
			this.session = session;
			this.lockOption = lockOption;
		}

		public void setSession(Session session) {

		}
	}

}
