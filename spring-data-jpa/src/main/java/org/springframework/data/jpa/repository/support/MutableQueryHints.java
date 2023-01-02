/*
 * Copyright 2020-2023 the original author or authors.
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
package org.springframework.data.jpa.repository.support;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import jakarta.persistence.EntityManager;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Mutable implementation of {@link QueryHints}, without the Option to switch between
 * {@link #forCounts()}/{@link #withFetchGraphs(EntityManager)} hints.
 *
 * @author Jens Schauder
 * @author Mark Paluch
 * @since 2.4
 * @see QueryHints
 */
public class MutableQueryHints implements QueryHints {

	private final MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();

	@Override
	public QueryHints withFetchGraphs(EntityManager em) {
		return this;
	}

	@Override
	public QueryHints forCounts() {
		return this;
	}

	@Override
	public void forEach(BiConsumer<String, Object> action) {

		for (Map.Entry<String, List<Object>> entry : values.entrySet()) {

			for (Object value : entry.getValue()) {
				action.accept(entry.getKey(), value);
			}
		}
	}

	/**
	 * Add a new key-value pair for a hint.
	 *
	 * @param name
	 * @param value
	 */
	public void add(String name, Object value) {
		values.add(name, value);
	}

	MultiValueMap<String, Object> getValues() {
		return values;
	}
}
