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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import jakarta.persistence.spi.PersistenceUnitInfo;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.aot.PersistenceUnitContexts.PersistenceUnitContextFactory;

/**
 * @author Christoph Strobl
 */
public class PersistenceUnitContextsUnitTests {

	@Test // GH-4068
	void cachesPersistenceUnitContextFactory() {

		PersistenceUnitInfo persistenceUnitInfo = mock(PersistenceUnitInfo.class);

		PersistenceUnitContextFactory ctxFactory = PersistenceUnitContexts.factory().from(persistenceUnitInfo);

		assertThat(PersistenceUnitContexts.factory().from(persistenceUnitInfo)).isSameAs(ctxFactory);
		assertThat(PersistenceUnitContexts.factory().from(mock(PersistenceUnitInfo.class))).isNotSameAs(ctxFactory);
	}
}
