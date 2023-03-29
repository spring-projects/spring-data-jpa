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
package org.springframework.data.jpa.repository.query;

import java.util.List;

/**
 * Utility methods to obtain sublists.
 *
 * @author Mark Paluch
 */
class CollectionUtils {

	/**
	 * Return the first {@code count} items from the list.
	 *
	 * @param count the number of first elements to be included in the returned list.
	 * @param list must not be {@literal null}
	 * @return the returned sublist if the {@code list} is greater {@code count}.
	 * @param <T> the element type of the lists.
	 */
	public static <T> List<T> getFirst(int count, List<T> list) {

		if (count > 0 && list.size() > count) {
			return list.subList(0, count);
		}

		return list;
	}

	/**
	 * Return the last {@code count} items from the list.
	 *
	 * @param count the number of last elements to be included in the returned list.
	 * @param list must not be {@literal null}
	 * @return the returned sublist if the {@code list} is greater {@code count}.
	 * @param <T> the element type of the lists.
	 */
	public static <T> List<T> getLast(int count, List<T> list) {

		if (count > 0 && list.size() > count) {
			return list.subList(list.size() - (count), list.size());
		}

		return list;
	}

}
