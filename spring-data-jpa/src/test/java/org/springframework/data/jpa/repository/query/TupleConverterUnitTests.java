/*
 * Copyright 2016-present the original author or authors.
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
import static org.mockito.Mockito.*;

import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.data.jpa.repository.query.AbstractJpaQuery.TupleConverter;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.ReturnedType;

/**
 * Unit tests for {@link TupleConverter}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Christoph Strobl
 * @author Mark Paluch
 * @soundtrack James Bay - Let it go (Chaos and the Calm)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TupleConverterUnitTests {

	@Mock Tuple tuple;
	@Mock TupleElement<String> element;
	@Mock ProjectionFactory factory;

	private ReturnedType type;

	@BeforeEach
	void setUp() throws Exception {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(SampleRepository.class);
		QueryMethod method = new QueryMethod(SampleRepository.class.getMethod("someMethod"), metadata, factory);

		this.type = method.getResultProcessor().getReturnedType();
	}

	@Test // DATAJPA-984
	void returnsSingleTupleElementIfItMatchesExpectedType() {

		doReturn(Collections.singletonList(element)).when(tuple).getElements();
		doReturn("Foo").when(tuple).get(element);

		TupleConverter converter = new TupleConverter(type);

		assertThat(converter.convert(tuple)).isEqualTo("Foo");
	}

	@Test // DATAJPA-1024
	void returnsNullForSingleElementTupleWithNullValue() {

		doReturn(Collections.singletonList(element)).when(tuple).getElements();
		doReturn(null).when(tuple).get(element);

		TupleConverter converter = new TupleConverter(type);

		assertThat(converter.convert(tuple)).isNull();
	}

	@SuppressWarnings("unchecked")
	@Test // DATAJPA-1048
	void findsValuesForAllVariantsSupportedByTheTuple() {

		Tuple tuple = new MockTuple();

		TupleConverter converter = new TupleConverter(type);

		Map<String, Object> map = (Map<String, Object>) converter.convert(tuple);

		SoftAssertions softly = new SoftAssertions();

		softly.assertThat(map.get("ONE")).isEqualTo("one");
		softly.assertThat(map.get("one")).isEqualTo("one");
		softly.assertThat(map.get("OnE")).isEqualTo("one");
		softly.assertThat(map.get("oNe")).isEqualTo("one");

		softly.assertAll();
	}

	@Test // GH-3076
	void dealsWithNullsInArguments() {

		ReturnedType returnedType = ReturnedType.of(WithPC.class, DomainType.class, new SpelAwareProxyProjectionFactory());

		doReturn(List.of(element, element, element)).when(tuple).getElements();
		when(tuple.get(eq(0))).thenReturn("one");
		when(tuple.get(eq(1))).thenReturn(null);
		when(tuple.get(eq(2))).thenReturn(1);

		Object result = new TupleConverter(returnedType).convert(tuple);
		assertThat(result).isInstanceOf(WithPC.class);
	}

	@Test // GH-3076, GH-4088
	void fallsBackToCompatibleConstructor() {

		ReturnedType returnedType = spy(
				ReturnedType.of(MultipleConstructors.class, DomainType.class, new SpelAwareProxyProjectionFactory()));
		when(returnedType.isProjecting()).thenReturn(true);
		when(returnedType.isDtoProjection()).thenReturn(true);
		when(returnedType.getInputProperties()).thenReturn(Arrays.asList("one", "two", "three"));
		when(returnedType.hasInputProperties()).thenReturn(true);

		doReturn(List.of(element, element, element)).when(tuple).getElements();
		when(tuple.get(eq(0))).thenReturn("one");
		when(tuple.get(eq(1))).thenReturn(null);
		when(tuple.get(eq(2))).thenReturn(2);

		MultipleConstructors result = (MultipleConstructors) new TupleConverter(returnedType).convert(tuple);

		assertThat(result.one).isEqualTo("one");
		assertThat(result.two).isNull();
		assertThat(result.three).isEqualTo(2);

		reset(tuple);

		doReturn(List.of(element, element, element)).when(tuple).getElements();
		when(tuple.get(eq(0))).thenReturn("one");
		when(tuple.get(eq(1))).thenReturn(null);
		when(tuple.get(eq(2))).thenReturn('a');

		result = (MultipleConstructors) new TupleConverter(returnedType).convert(tuple);

		assertThat(result.one).isEqualTo("one");
		assertThat(result.two).isNull();
		assertThat(result.three).isEqualTo(97);
	}

	@Test // GH-3076, GH-4088
	void acceptsConstructorWithCastableType() {

		ReturnedType returnedType = spy(
				ReturnedType.of(MultipleConstructors.class, DomainType.class, new SpelAwareProxyProjectionFactory()));
		when(returnedType.isProjecting()).thenReturn(true);
		when(returnedType.isDtoProjection()).thenReturn(true);
		when(returnedType.getInputProperties()).thenReturn(Arrays.asList("one", "two", "three", "four"));
		when(returnedType.hasInputProperties()).thenReturn(true);

		doReturn(List.of(element, element, element, element)).when(tuple).getElements();
		when(tuple.get(eq(0))).thenReturn("one");
		when(tuple.get(eq(1))).thenReturn(null);
		when(tuple.get(eq(2))).thenReturn((byte) 2);
		when(tuple.get(eq(3))).thenReturn(2.1f);

		MultipleConstructors result = (MultipleConstructors) new TupleConverter(returnedType).convert(tuple);

		assertThat(result.one).isEqualTo("one");
		assertThat(result.two).isNull();
		assertThat(result.three).isEqualTo(2);
		assertThat(result.four).isEqualTo(2, offset(0.1d));
	}

	@Test // GH-3076, GH-4088
	void failsForNonResolvableConstructor() {

		ReturnedType returnedType = spy(
				ReturnedType.of(MultipleConstructors.class, DomainType.class, new SpelAwareProxyProjectionFactory()));
		when(returnedType.isProjecting()).thenReturn(true);
		when(returnedType.isDtoProjection()).thenReturn(true);
		when(returnedType.getInputProperties()).thenReturn(Arrays.asList("one", "two"));
		when(returnedType.hasInputProperties()).thenReturn(true);

		doReturn(List.of(element, element)).when(tuple).getElements();
		when(tuple.get(eq(0))).thenReturn(1);
		when(tuple.get(eq(1))).thenReturn(null);

		assertThatIllegalStateException().isThrownBy(() -> new TupleConverter(returnedType).convert(tuple))
				.withMessageContaining("Cannot find compatible constructor for DTO projection");
	}

	interface SampleRepository extends CrudRepository<Object, Long> {
		String someMethod();
	}

	@SuppressWarnings("unchecked")
	private static class MockTuple implements Tuple {

		TupleElement<String> one = new StringTupleElement("oNe");
		TupleElement<String> two = new StringTupleElement("tWo");

		@Override
		public <X> X get(TupleElement<X> tupleElement) {
			return (X) get(tupleElement.getAlias());
		}

		@Override
		public <X> X get(String alias, Class<X> type) {
			return (X) get(alias);
		}

		@Override
		public Object get(String alias) {
			return alias.toLowerCase();
		}

		@Override
		public <X> X get(int i, Class<X> type) {
			return (X) String.valueOf(i);
		}

		@Override
		public Object get(int i) {
			return get(i, Object.class);
		}

		@Override
		public Object[] toArray() {
			return new Object[] { one.getAlias().toLowerCase(), two.getAlias().toLowerCase() };
		}

		@Override
		public List<TupleElement<?>> getElements() {
			return Arrays.asList(one, two);
		}

		private static class StringTupleElement implements TupleElement<String> {

			private final String value;

			private StringTupleElement(String value) {
				this.value = value;
			}

			@Override
			public Class<? extends String> getJavaType() {
				return String.class;
			}

			@Override
			public String getAlias() {
				return value;
			}
		}
	}

	static class DomainType {
		String one, two, three;
	}

	static class WithPC {
		String one;
		String two;
		long three;

		public WithPC(String one, String two, long three) {
			this.one = one;
			this.two = two;
			this.three = three;
		}
	}

	static class MultipleConstructors {
		String one;
		String two;
		long three;
		double four;

		public MultipleConstructors(String one) {
			this.one = one;
		}

		public MultipleConstructors(String one, String two, long three) {
			this.one = one;
			this.two = two;
			this.three = three;
		}

		public MultipleConstructors(String one, String two, short three, double four) {
			this.one = one;
			this.two = two;
			this.three = three;
			this.four = four;
		}

	}
}
