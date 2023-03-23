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
import org.springframework.data.domain.KeysetScrollPosition.Direction;
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

	private static final KeysetScrollDelegate forward = new KeysetScrollDelegate();
	private static final KeysetScrollDelegate reverse = new ReverseKeysetScrollDelegate();

	/**
	 * Factory method to obtain the right {@link KeysetScrollDelegate}.
	 *
	 * @param direction
	 * @return
	 */
	public static KeysetScrollDelegate of(Direction direction) {
		return direction == Direction.Forward ? forward : reverse;
	}

	@Nullable
	public <E, P> P createPredicate(KeysetScrollPosition keyset, Sort sort, QueryAdapter<E, P> queryAdapter) {

		Map<String, Object> keysetValues = keyset.getKeys();

		// first query doesn't come with a keyset
		if (keysetValues.isEmpty()) {
			return null;
		}

		// build matrix query for keyset paging that contains sort^2 queries
		// reflecting a query that follows sort order semantics starting from the last returned keyset

		List<P> or = new ArrayList<>();

		int i = 0;
		// progressive query building
		for (Order order : sort) {

			if (!keysetValues.containsKey(order.getProperty())) {
				throw new IllegalStateException("KeysetScrollPosition does not contain all keyset values");
			}

			List<P> sortConstraint = new ArrayList<>();

			int j = 0;
			for (Order inner : sort) {

				E propertyExpression = queryAdapter.createExpression(inner.getProperty());
				Object o = keysetValues.get(inner.getProperty());

				if (j >= i) { // tail segment

					sortConstraint.add(queryAdapter.compare(inner, propertyExpression, o));
					break;
				}

				sortConstraint.add(queryAdapter.compare(propertyExpression, o));
				j++;
			}

			if (!sortConstraint.isEmpty()) {
				or.add(queryAdapter.and(sortConstraint));
			}

			i++;
		}

		if (or.isEmpty()) {
			return null;
		}

		return queryAdapter.or(or);
	}

	protected Sort getSortOrders(Sort sort) {
		return sort;
	}

	@SuppressWarnings("unchecked")
	protected <T> List<T> postProcessResults(List<T> result) {
		return result;
	}

	protected <T> List<T> getResultWindow(List<T> list, int limit) {
		return CollectionUtils.getFirst(limit, list);
	}

	/**
	 * Reverse scrolling director variant applying {@link Direction#Backward}. In reverse scrolling, we need to flip
	 * directions for the actual query so that we do not get everything from the top position and apply the limit but
	 * rather flip the sort direction, apply the limit and then reverse the result to restore the actual sort order.
	 */
	private static class ReverseKeysetScrollDelegate extends KeysetScrollDelegate {

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
	 * @param <E>
	 * @param <P>
	 */
	public interface QueryAdapter<E, P> {

		/**
		 * Create an expression object from the given {@code property} path.
		 *
		 * @param property
		 * @return
		 */
		E createExpression(String property);

		/**
		 * Create a comparison object according to the {@link Order}.
		 *
		 * @param order
		 * @param propertyExpression
		 * @param o
		 * @return
		 */
		P compare(Order order, E propertyExpression, Object o);

		/**
		 * Create an equals-comparison object.
		 *
		 * @param propertyExpression
		 * @param o
		 * @return
		 */
		P compare(E propertyExpression, Object o);

		/**
		 * AND-combine the {@code intermediate} predicates.
		 *
		 * @param intermediate
		 * @return
		 */
		P and(List<P> intermediate);

		/**
		 * OR-combine the {@code intermediate} predicates.
		 *
		 * @param intermediate
		 * @return
		 */
		P or(List<P> intermediate);
	}

}
