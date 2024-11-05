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
package org.springframework.data.jpa.repository.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;
import org.mockito.Mockito;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

/**
 * @author Christoph Strobl
 */
public class PartTreeQueryCacheUnitTests {

	PartTreeQueryCache cache;

	static Supplier<Stream<Arguments>> cacheInput = () -> Stream.of(
			Arguments.arguments(Sort.unsorted(), StubJpaParameterParameterAccessor.accessor()), //
			Arguments.arguments(Sort.by(Direction.ASC, "one"), StubJpaParameterParameterAccessor.accessor()), //
			Arguments.arguments(Sort.by(Direction.DESC, "one"), StubJpaParameterParameterAccessor.accessor()), //
			Arguments.arguments(Sort.unsorted(),
					StubJpaParameterParameterAccessor.accessorFor(String.class).withValues("value")), //
			Arguments.arguments(Sort.unsorted(),
					StubJpaParameterParameterAccessor.accessorFor(String.class).withValues(new Object[] { null })), //
			Arguments.arguments(Sort.by(Direction.ASC, "one"),
					StubJpaParameterParameterAccessor.accessorFor(String.class).withValues("value")), //
			Arguments.arguments(Sort.by(Direction.ASC, "one"),
					StubJpaParameterParameterAccessor.accessorFor(String.class).withValues(new Object[] { null })));

	@BeforeEach
	void beforeEach() {
		cache = new PartTreeQueryCache();
	}

	@ParameterizedTest
	@FieldSource("cacheInput")
	void getReturnsNullForEmptyCache(Sort sort, JpaParametersParameterAccessor accessor) {
		assertThat(cache.get(sort, accessor)).isNull();
	}

	@ParameterizedTest
	@FieldSource("cacheInput")
	void getReturnsCachedInstance(Sort sort, JpaParametersParameterAccessor accessor) {

		JpaQueryCreator queryCreator = Mockito.mock(JpaQueryCreator.class);

		assertThat(cache.put(sort, accessor, queryCreator)).isNull();
		assertThat(cache.get(sort, accessor)).isSameAs(queryCreator);
	}

	@ParameterizedTest
	@FieldSource("cacheInput")
	void cacheGetWithSort(Sort sort, JpaParametersParameterAccessor accessor) {

		JpaQueryCreator queryCreator = Mockito.mock(JpaQueryCreator.class);
		assertThat(cache.put(Sort.by("not-in-cache"), accessor, queryCreator)).isNull();

		assertThat(cache.get(sort, accessor)).isNull();
	}

	@ParameterizedTest
	@FieldSource("cacheInput")
	void cacheGetWithccessor(Sort sort, JpaParametersParameterAccessor accessor) {

		JpaQueryCreator queryCreator = Mockito.mock(JpaQueryCreator.class);
		assertThat(cache.put(sort, StubJpaParameterParameterAccessor.accessor("spring", "data"), queryCreator)).isNull();

		assertThat(cache.get(sort, accessor)).isNull();
	}

	@Test
	void cachesOnNullableNotArgumentType() {

		JpaQueryCreator queryCreator = Mockito.mock(JpaQueryCreator.class);
		Sort sort = Sort.unsorted();
		assertThat(cache.put(sort, StubJpaParameterParameterAccessor.accessor("spring", "data"), queryCreator)).isNull();

		assertThat(cache.get(sort,
				StubJpaParameterParameterAccessor.accessor(new Class[] { String.class, String.class }, "spring", null)))
				.isNull();

		assertThat(cache.get(sort,
				StubJpaParameterParameterAccessor.accessor(new Class[] { String.class, String.class }, null, "data"))).isNull();

		assertThat(cache.get(sort,
				StubJpaParameterParameterAccessor.accessor(new Class[] { String.class, String.class }, "data", "spring")))
				.isSameAs(queryCreator);

		assertThat(cache.get(Sort.by("not-in-cache"),
				StubJpaParameterParameterAccessor.accessor(new Class[] { String.class, String.class }, "data", "spring")))
				.isNull();
	}

}
