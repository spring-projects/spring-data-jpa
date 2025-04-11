/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.data.jpa.convert;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.jspecify.annotations.Nullable;

import org.springframework.data.domain.Vector;

/**
 * JPA {@link Converter} for {@link Vector} types.
 *
 * @author Mark Paluch
 * @since 4.0
 */
public class VectorConverters {

	@Converter(autoApply = true)
	public static class VectorAsFloatArrayConverter implements AttributeConverter<@Nullable Vector, @Nullable float[]> {

		@Override
		public @Nullable float[] convertToDatabaseColumn(@Nullable Vector vector) {
			return vector == null ? null : vector.toFloatArray();
		}

		@Override
		public @Nullable Vector convertToEntityAttribute(@Nullable float[] floats) {
			return floats == null ? null : Vector.of(floats);
		}
	}

	@Converter(autoApply = true)
	public static class VectorAsDoubleArrayConverter implements AttributeConverter<@Nullable Vector, @Nullable double[]> {

		@Override
		public @Nullable double[] convertToDatabaseColumn(@Nullable Vector vector) {
			return vector == null ? null : vector.toDoubleArray();
		}

		@Override
		public @Nullable Vector convertToEntityAttribute(@Nullable double[] doubles) {
			return doubles == null ? null : Vector.of(doubles);
		}
	}

}
