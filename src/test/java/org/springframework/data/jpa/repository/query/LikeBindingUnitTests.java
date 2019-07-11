/*
 * Copyright 2013-2019 the original author or authors.
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
import org.springframework.data.jpa.repository.query.StringQuery.LikeParameterBinding;
import org.springframework.data.repository.query.parser.Part.Type;

/**
 * Unit tests for {@link LikeParameterBinding}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Jens Schauder
 */
public class LikeBindingUnitTests {

	private static void assertAugmentedValue(Type type, Object value) {

		LikeParameterBinding binding = new LikeParameterBinding("foo", type);
		assertThat(binding.prepare("value")).isEqualTo(value);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullName() {
		new LikeParameterBinding(null, Type.CONTAINING);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsEmptyName() {
		new LikeParameterBinding("", Type.CONTAINING);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullType() {
		new LikeParameterBinding("foo", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsInvalidType() {
		new LikeParameterBinding("foo", Type.SIMPLE_PROPERTY);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsInvalidPosition() {
		new LikeParameterBinding(0, Type.CONTAINING);
	}

	@Test
	public void setsUpInstanceForName() {

		LikeParameterBinding binding = new LikeParameterBinding("foo", Type.CONTAINING);

		assertThat(binding.hasName("foo")).isTrue();
		assertThat(binding.hasName("bar")).isFalse();
		assertThat(binding.hasName(null)).isFalse();
		assertThat(binding.hasPosition(0)).isFalse();
		assertThat(binding.getType()).isEqualTo(Type.CONTAINING);
	}

	@Test
	public void setsUpInstanceForIndex() {

		LikeParameterBinding binding = new LikeParameterBinding(1, Type.CONTAINING);

		assertThat(binding.hasName("foo")).isFalse();
		assertThat(binding.hasName(null)).isFalse();
		assertThat(binding.hasPosition(0)).isFalse();
		assertThat(binding.hasPosition(1)).isTrue();
		assertThat(binding.getType()).isEqualTo(Type.CONTAINING);
	}

	@Test
	public void augmentsValueCorrectly() {

		assertAugmentedValue(Type.CONTAINING, "%value%");
		assertAugmentedValue(Type.ENDING_WITH, "%value");
		assertAugmentedValue(Type.STARTING_WITH, "value%");

		assertThat(new LikeParameterBinding(1, Type.CONTAINING).prepare(null)).isNull();
	}
}
