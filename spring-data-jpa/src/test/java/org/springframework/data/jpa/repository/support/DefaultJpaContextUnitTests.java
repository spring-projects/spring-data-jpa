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
package org.springframework.data.jpa.repository.support;

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DefaultJpaContext}.
 *
 * @author Oliver Gierke
 * @soundtrack Marcus Miller - B's River (Afrodeezia)
 * @since 1.9
 */
class DefaultJpaContextUnitTests {

	@Test // DATAJPA-669
	void rejectsNullEntityManagers() {
		assertThatIllegalArgumentException().isThrownBy(() -> new DefaultJpaContext(null));
	}

	@Test // DATAJPA-669
	void rejectsEmptyEntityManagers() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new DefaultJpaContext(Collections.<EntityManager> emptySet()));
	}
}
