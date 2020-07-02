/*
 * Copyright 2020 the original author or authors.
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

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Just the values of QueryHints, without the Option to switch between count/ fetchGraph hints.
 *
 * @author Jens Schauder
 * @since 2.4
 * @see QueryHints
 */
public class SimpleQueryHints {

	MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();


	public void add(String name, Object value) {
		values.add(name, value);
	}

	public void forEach(BiConsumer<String, Object> consumer) {

		for (Map.Entry<String, List<Object>> entry : values.entrySet()) {

			for (Object value : entry.getValue()) {
				consumer.accept(entry.getKey(), value);
			}
		}
	}

	public void addAll(SimpleQueryHints hints) {
		hints.forEach(this::add);
	}
}
