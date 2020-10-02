/*
 * Copyright 2016-2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link TupleConverter}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @soundtrack James Bay - Let it go (Chaos and the Calm)
 */
@RunWith(MockitoJUnitRunner.class)
public class TupleConverterUnitTests {

	@Mock Tuple tuple;
	@Mock TupleElement<String> element;

	Class<?> type;

	@Before
	public void setUp() throws Exception {
		this.type = String.class;
	}

	@Test // DATAJPA-984
	public void returnsSingleTupleElementIfItMatchesExpectedType() {

		doReturn(Collections.singletonList(element)).when(tuple).getElements();
		doReturn("Foo").when(tuple).get(element);

		TupleConverter converter = new TupleConverter(type);

		assertThat(converter.convert(tuple)).isEqualTo("Foo");
	}

	@Test // DATAJPA-1024
	public void returnsNullForSingleElementTupleWithNullValue() {

		doReturn(Collections.singletonList(element)).when(tuple).getElements();
		doReturn(null).when(tuple).get(element);

		TupleConverter converter = new TupleConverter(type);

		assertThat(converter.convert(tuple)).isNull();
	}

	@SuppressWarnings("unchecked")
	@Test // DATAJPA-1048
	public void findsValuesForAllVariantsSupportedByTheTuple() {

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
}
