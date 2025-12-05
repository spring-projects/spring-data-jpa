/*
 * Copyright 2017-2025 the original author or authors.
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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.query.ParameterBinding.BindingIdentifier;
import org.springframework.data.jpa.repository.query.ParameterBinding.ParameterOrigin;
import org.springframework.data.jpa.repository.support.JpqlQueryTemplates;
import org.springframework.data.repository.query.parser.Part;

/**
 * Unit tests for {@link ParameterBinding}.
 *
 * @author Jens Schauder
 * @author Diego Krupitza
 * @author Mark Paluch
 */
class ParameterBindingUnitTests {

	static ParameterBinding hibernateCollectionBinding = new ParameterBinding.PartTreeParameterBinding(
			BindingIdentifier.of("foo"), ParameterOrigin.ofParameter("foo"), Collection.class,
			new Part("name", TestEntity.class), null, JpqlQueryTemplates.UPPER, EscapeCharacter.DEFAULT,
			PersistenceProvider.HIBERNATE);

	static ParameterBinding eclipselinkCollectionBinding = new ParameterBinding.PartTreeParameterBinding(
			BindingIdentifier.of("foo"), ParameterOrigin.ofParameter("foo"), Collection.class,
			new Part("name", TestEntity.class), null, JpqlQueryTemplates.UPPER, EscapeCharacter.DEFAULT,
			PersistenceProvider.ECLIPSELINK);

	@Test // GH-4112
	void shouldPrepareEmptyListForHibernate() {

		assertThat(hibernateCollectionBinding.prepare(List.of())).isEqualTo(List.of());
		assertThat(hibernateCollectionBinding.prepare(new Integer[0])).isEqualTo(List.of());
	}

	@Test // GH-4112
	void shouldPrepareEmptyListToNullForEclipselink() {

		assertThat(eclipselinkCollectionBinding.prepare(List.of())).isNull();
		assertThat(eclipselinkCollectionBinding.prepare(new Integer[0])).isNull();
	}

	@ParameterizedTest // GH-4112
	@MethodSource("bindings")
	void shouldPrepareListToCollection(ParameterBinding binding) {

		assertThat(binding.prepare(List.of("foo"))).isEqualTo(List.of("foo"));
		assertThat(binding.prepare(new Integer[] { 1, 2, 3 })).isEqualTo(List.of(1, 2, 3));
	}

	@ParameterizedTest // GH-4112
	@MethodSource("bindings")
	void shouldPrepareValueToSingleElementCollection(ParameterBinding binding) {
		assertThat(binding.prepare("foo")).isEqualTo(Set.of("foo"));
	}

	@ParameterizedTest // GH-4112
	@MethodSource("bindings")
	void shouldPrepareNull(ParameterBinding binding) {
		assertThat(binding.prepare(null)).isNull();
	}

	static Stream<Arguments.ArgumentSet> bindings() {
		return Stream.of(Arguments.argumentSet("Hibernate", hibernateCollectionBinding),
				Arguments.argumentSet("EclipseLink", eclipselinkCollectionBinding));
	}

	private record TestEntity(String name) {

	}

}
