/*
 * Copyright 2017-2023 the original author or authors.
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
package org.springframework.data.jpa.convert.threeten;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;

import jakarta.persistence.AttributeConverter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link Jsr310JpaConverters}.
 *
 * @author Oliver Gierke
 */
class Jsr310JpaConvertersUnitTests {

	static Iterable<? extends Object> data() {

		return Arrays.asList(new Jsr310JpaConverters.InstantConverter(), //
				new Jsr310JpaConverters.LocalDateConverter(), //
				new Jsr310JpaConverters.LocalDateTimeConverter(), //
				new Jsr310JpaConverters.LocalTimeConverter(), //
				new Jsr310JpaConverters.ZoneIdConverter());
	}


	@ParameterizedTest
	@MethodSource("data")
	void convertersHandleNullValuesCorrectly(AttributeConverter<?, ?> converter) {

		assertThat(converter.convertToDatabaseColumn(null)).isNull();
		assertThat(converter.convertToEntityAttribute(null)).isNull();
	}
}
