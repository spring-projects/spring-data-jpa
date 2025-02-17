/*
 * Copyright 2024-2025 the original author or authors.
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

import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Sort;

import org.jspecify.annotations.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * Cache for PartTree queries.
 *
 * @author Christoph Strobl
 */
class PartTreeQueryCache {

	private final Map<CacheKey, JpqlQueryCreator> cache = Collections.synchronizedMap(new LinkedHashMap<>() {
		@Override
		protected boolean removeEldestEntry(Map.Entry<CacheKey, JpqlQueryCreator> eldest) {
			return size() > 256;
		}
	});

	@Nullable
	JpqlQueryCreator get(Sort sort, JpaParametersParameterAccessor accessor) {
		return cache.get(CacheKey.of(sort, accessor));
	}

	@Nullable
	JpqlQueryCreator put(Sort sort, JpaParametersParameterAccessor accessor, JpqlQueryCreator creator) {
		return cache.put(CacheKey.of(sort, accessor), creator);
	}

	static class CacheKey {

		private final Sort sort;

		/**
		 * Bitset of null/non-null parameter values. A 0 bit means the parameter value is {@code null}, a 1 bit means the
		 * parameter is not {@code null}.
		 */
		private final BitSet params;

		public CacheKey(Sort sort, BitSet params) {
			this.sort = sort;
			this.params = params;
		}

		static CacheKey of(Sort sort, JpaParametersParameterAccessor accessor) {

			Object[] values = accessor.getValues();

			if (ObjectUtils.isEmpty(values)) {
				return new CacheKey(sort, new BitSet());
			}

			return new CacheKey(sort, toNullableMap(values));
		}

		static BitSet toNullableMap(Object[] args) {

			BitSet bitSet = new BitSet(args.length);
			for (int i = 0; i < args.length; i++) {
				bitSet.set(i, args[i] != null);
			}

			return bitSet;
		}

		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			CacheKey cacheKey = (CacheKey) o;
			return sort.equals(cacheKey.sort) && params.equals(cacheKey.params);
		}

		@Override
		public int hashCode() {
			return Objects.hash(sort, params);
		}
	}

}
