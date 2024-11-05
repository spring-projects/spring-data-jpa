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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * @author Christoph Strobl
 */
class PartTreeQueryCache {

	private final Map<CacheKey, JpqlQueryCreator> cache = new LinkedHashMap<CacheKey, JpqlQueryCreator>() {
		@Override
		protected boolean removeEldestEntry(Map.Entry<CacheKey, JpqlQueryCreator> eldest) {
			return size() > 256;
		}
	};

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
		private final Map<Integer, Nulled> params;

		public CacheKey(Sort sort, Map<Integer, Nulled> params) {
			this.sort = sort;
			this.params = params;
		}

		static CacheKey of(Sort sort, JpaParametersParameterAccessor accessor) {

			Object[] values = accessor.getValues();
			if (ObjectUtils.isEmpty(values)) {
				return new CacheKey(sort, Map.of());
			}

			return new CacheKey(sort, toNullableMap(values));
		}

		static Map<Integer, Nulled> toNullableMap(Object[] args) {

			Map<Integer, Nulled> paramMap = new HashMap<>(args.length);
			for (int i = 0; i < args.length; i++) {
				paramMap.put(i, args[i] != null ? Nulled.NO : Nulled.YES);
			}
			return paramMap;
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

	enum Nulled {
		YES, NO
	}

}
