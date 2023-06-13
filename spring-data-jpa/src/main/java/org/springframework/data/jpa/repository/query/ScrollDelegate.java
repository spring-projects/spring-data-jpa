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

import jakarta.persistence.Query;

import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.OffsetScrollPosition;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.ScrollPosition.Direction;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.util.Assert;

/**
 * Delegate to run {@link ScrollPosition scroll queries} and create result {@link Window}.
 *
 * @author Mark Paluch
 * @author Yanming Zhou
 * @since 3.1
 */
public class ScrollDelegate<T> {

	private final JpaEntityInformation<T, ?> entity;

	protected ScrollDelegate(JpaEntityInformation<T, ?> entity) {
		this.entity = entity;
	}

	/**
	 * Run the {@link Query} and return a scroll {@link Window}.
	 *
	 * @param query must not be {@literal null}.
	 * @param sort must not be {@literal null}.
	 * @param scrollPosition must not be {@literal null}.
	 * @return the scroll {@link Window}.
	 */
	@SuppressWarnings("unchecked")
	public Window<T> scroll(Query query, Sort sort, ScrollPosition scrollPosition) {

		Assert.notNull(scrollPosition, "ScrollPosition must not be null");

		int limit = query.getMaxResults();
		if (limit > 0 && limit != Integer.MAX_VALUE) {
			query = query.setMaxResults(limit + 1);
		}

		List<T> result = query.getResultList();

		if (scrollPosition instanceof KeysetScrollPosition keyset) {
			return createWindow(sort, limit, keyset.getDirection(), entity, result);
		}

		if (scrollPosition instanceof OffsetScrollPosition offset) {
			return createWindow(result, limit, OffsetScrollPosition.positionFunction(offset.getOffset()));
		}

		throw new UnsupportedOperationException("ScrollPosition " + scrollPosition + " not supported");
	}

	private static <T> Window<T> createWindow(Sort sort, int limit, Direction direction,
			JpaEntityInformation<T, ?> entity, List<T> result) {

		KeysetScrollDelegate delegate = KeysetScrollDelegate.of(direction);
		List<T> resultsToUse = delegate.getResultWindow(delegate.postProcessResults(result), limit);

		IntFunction<ScrollPosition> positionFunction = value -> {

			T object = resultsToUse.get(value);
			Map<String, Object> keys = entity.getKeyset(sort.stream().map(Order::getProperty).toList(), object);

			return ScrollPosition.of(keys, direction);
		};

		return Window.from(resultsToUse, positionFunction, hasMoreElements(result, limit));
	}

	private static <T> Window<T> createWindow(List<T> result, int limit,
			IntFunction<? extends ScrollPosition> positionFunction) {
		return Window.from(CollectionUtils.getFirst(limit, result), positionFunction, hasMoreElements(result, limit));
	}

	private static boolean hasMoreElements(List<?> result, int limit) {
		return !result.isEmpty() && result.size() > limit;
	}

}
