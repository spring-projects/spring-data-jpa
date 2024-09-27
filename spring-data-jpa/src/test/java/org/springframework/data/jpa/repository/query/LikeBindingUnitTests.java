/*
 * Copyright 2013-2024 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.data.jpa.repository.query.ParameterBinding.BindingIdentifier;
import org.springframework.data.jpa.repository.query.ParameterBinding.LikeParameterBinding;
import org.springframework.data.jpa.repository.query.ParameterBinding.ParameterOrigin;
import org.springframework.data.repository.query.parser.Part.Type;

/**
 * Unit tests for {@link LikeParameterBinding}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Jens Schauder
 * @author Mark Paluch
 */
class LikeBindingUnitTests {

	private static void assertAugmentedValue(Type type, Object value) {

		LikeParameterBinding binding = new LikeParameterBinding(BindingIdentifier.of("foo"),
				ParameterOrigin.ofParameter(1), type);
		assertThat(binding.prepare("value")).isEqualTo(value);
	}

	@Test
	void rejectsNullName() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new LikeParameterBinding(null, ParameterOrigin.ofParameter(0), Type.CONTAINING));
	}

	@Test
	void rejectsEmptyName() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new LikeParameterBinding(BindingIdentifier.of(""), ParameterOrigin.ofParameter(0), Type.CONTAINING));
	}

	@Test
	void rejectsNullType() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new LikeParameterBinding(BindingIdentifier.of("foo"), ParameterOrigin.ofParameter(0), null));
	}

	@Test
	void rejectsInvalidType() {
		assertThatIllegalArgumentException().isThrownBy(() -> new LikeParameterBinding(BindingIdentifier.of("foo"),
				ParameterOrigin.ofParameter(0), Type.SIMPLE_PROPERTY));
	}

	@Test
	void rejectsInvalidPosition() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new LikeParameterBinding(BindingIdentifier.of(0), ParameterOrigin.ofParameter(0), Type.CONTAINING));
	}

	@Test
	void augmentsValueCorrectly() {

		assertAugmentedValue(Type.CONTAINING, "%value%");
		assertAugmentedValue(Type.ENDING_WITH, "%value");
		assertAugmentedValue(Type.STARTING_WITH, "value%");

		assertThat(new LikeParameterBinding(BindingIdentifier.of(1), ParameterOrigin.ofParameter(null, 1), Type.CONTAINING)
				.prepare(null)).isNull();
	}
}
