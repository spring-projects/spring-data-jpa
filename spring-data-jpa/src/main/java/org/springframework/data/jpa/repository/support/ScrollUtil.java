/*
 * Copyright 2023 the original author or authors.
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
import java.util.function.IntFunction;

import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.KeysetScrollPosition.Direction;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.repository.support.KeysetScrollSpecification.KeysetScrollDirector;

/**
 * @author Mark Paluch
 */
class ScrollUtil {

	static <T> Window<T> createWindow(Sort sort, int limit, Direction direction, JpaEntityInformation<T, ?> entity,
			List<T> result) {

		KeysetScrollDirector director = KeysetScrollDirector.of(direction);

		director.postPostProcessResults(result);

		IntFunction<KeysetScrollPosition> positionFunction = value -> {

			T object = result.get(value);
			Map<String, Object> keys = entity.getKeyset(sort.stream().map(Order::getProperty).toList(), object);

			return KeysetScrollPosition.of(keys);
		};

		return createWindow(result, limit, positionFunction);
	}

	static <T> Window<T> createWindow(List<T> result, int limit, IntFunction<? extends ScrollPosition> positionFunction) {
		return Window.from(getSubList(result, limit), positionFunction, hasMoreElements(result, limit));
	}

	private static boolean hasMoreElements(List<?> result, int limit) {
		return !result.isEmpty() && result.size() > limit;
	}

	private static <T> List<T> getSubList(List<T> result, int limit) {

		if (limit > 0 && result.size() > limit) {
			return result.subList(0, limit);
		}

		return result;
	}

}
