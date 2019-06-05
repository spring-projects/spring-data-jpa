/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.data.jpa.repository.query;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

/**
 * Unit tests for {@link EscapeCharacter}.
 *
 * @author Jens Schauder
 */
public class EscapeCharacterUnitTests {

	@Test // DATAJPA-1522
	public void nothingToEscape() {
		assertThat(EscapeCharacter.of('x').escape("alpha")).isEqualTo("alpha");
	}

	@Test // DATAJPA-1522
	public void wildcardGetsEscaped() {
		assertThat(EscapeCharacter.of('x').escape("alp_ha")).isEqualTo("alpx_ha");
	}

	@Test // DATAJPA-1522
	public void escapeCharacterGetsEscaped() {
		assertThat(EscapeCharacter.of('x').escape("axlpx_ha")).isEqualTo("axxlpxxx_ha");
	}
}
