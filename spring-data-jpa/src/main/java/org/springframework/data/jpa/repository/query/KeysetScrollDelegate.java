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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.ScrollPosition.Direction;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.lang.Nullable;

/**
 * Delegate for keyset scrolling.
 *
 * @author Mark Paluch
 * @since 3.1
 */
public class KeysetScrollDelegate {

	private static final KeysetScrollDelegate FORWARD = new KeysetScrollDelegate();
	private static final KeysetScrollDelegate REVERSE = new ReverseKeysetScrollDelegate();

	/**
	 * Factory method to obtain the right {@link KeysetScrollDelegate}.
	 *
	 * @param direction the direction of scrolling.
	 * @return a {@link KeysetScrollDelegate} matching the requested direction.
	 */
	public static KeysetScrollDelegate of(Direction direction) {
		return direction == Direction.FORWARD ? FORWARD : REVERSE;
	}

	@Nullable
	public <E, P> P createPredicate(KeysetScrollPosition keyset, Sort sort, QueryStrategy<E, P> strategy) {

		Map<String, Object> keysetValues = keyset.getKeys();

		// first query doesn't come with a keyset
		if (keysetValues.isEmpty()) {
			return null;
		}

		List<P> or = new ArrayList<>();
		int i = 0;

		// progressive query building to reconstruct a query matching sorting rules
		for (Order order : sort) {

			if (!keysetValues.containsKey(order.getProperty())) {
				throw new IllegalStateException(String
						.format("KeysetScrollPosition does not contain all keyset values. Missing key: %s", order.getProperty()));
			}

			List<P> sortConstraint = new ArrayList<>();

			int j = 0;
			for (Order inner : sort) {

				E propertyExpression = strategy.createExpression(inner.getProperty());
				Object o = keysetValues.get(inner.getProperty());

				if (j >= i) { // tail segment

					sortConstraint.add(strategy.compare(inner, propertyExpression, o));
					break;
				}

				sortConstraint.add(strategy.compare(propertyExpression, o));
				j++;
			}

			if (!sortConstraint.isEmpty()) {
				or.add(strategy.and(sortConstraint));
			}

			i++;
		}

		if (or.isEmpty()) {
			return null;
		}

		return strategy.or(or);
	}

	protected Sort getSortOrders(Sort sort) {
		return sort;
	}

	protected <T> List<T> postProcessResults(List<T> result) {
		return result;
	}

	protected <T> List<T> getResultWindow(List<T> list, int limit) {
		return CollectionUtils.getFirst(limit, list);
	}

	/**
	 * Reverse scrolling variant applying {@link Direction#Backward}. In reverse scrolling, we need to flip directions for
	 * the actual query so that we do not get everything from the top position and apply the limit but rather flip the
	 * sort direction, apply the limit and then reverse the result to restore the actual sort order.
	 */
	private static class ReverseKeysetScrollDelegate extends KeysetScrollDelegate {

		@Override
		protected Sort getSortOrders(Sort sort) {

			List<Order> orders = new ArrayList<>();
			for (Order order : sort) {
				orders.add(new Order(order.isAscending() ? Sort.Direction.DESC : Sort.Direction.ASC, order.getProperty()));
			}

			return Sort.by(orders);
		}

		@Override
		protected <T> List<T> postProcessResults(List<T> result) {
			Collections.reverse(result);
			return result;
		}

		@Override
		protected <T> List<T> getResultWindow(List<T> list, int limit) {
			return CollectionUtils.getLast(limit, list);
		}
	}

	/**
	 * Adapter to construct scroll queries.
	 *
	 * @param <E> property path expression type.
	 * @param <P> predicate type.
	 */
	public interface QueryStrategy<E, P> {

		/**
		 * Create an expression object from the given {@code property} path.
		 *
		 * @param property must not be {@literal null}.
		 */
		E createExpression(String property);

		/**
		 * Create a comparison object according to the {@link Order}.
		 *
		 * @param order must not be {@literal null}.
		 * @param propertyExpression must not be {@literal null}.
		 * @param value the value to compare with. Must not be {@literal null}.
		 * @return an object representing the comparison predicate.
		 */
		P compare(Order order, E propertyExpression, Object value);

		/**
		 * Create an equals-comparison object.
		 *
		 * @param propertyExpression must not be {@literal null}.
		 * @param value the value to compare with. Must not be {@literal null}.
		 * @return an object representing the comparison predicate.
		 */
		P compare(E propertyExpression, @Nullable Object value);

		/**
		 * AND-combine the {@code intermediate} predicates.
		 *
		 * @param intermediate the predicates to combine. Must not be {@literal null}.
		 * @return a single predicate.
		 */
		P and(List<P> intermediate);

		/**
		 * OR-combine the {@code intermediate} predicates.
		 *
		 * @param intermediate the predicates to combine. Must not be {@literal null}.
		 * @return a single predicate.
		 */
		P or(List<P> intermediate);
	}

}
