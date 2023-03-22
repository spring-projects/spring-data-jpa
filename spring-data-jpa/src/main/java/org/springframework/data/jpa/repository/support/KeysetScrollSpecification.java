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

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.KeysetScrollPosition.Direction;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.lang.Nullable;

/**
 * {@link Specification} to create scroll queries using keyset-scrolling.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 3.1
 */
record KeysetScrollSpecification<T> (KeysetScrollPosition position, Sort sort,
		JpaEntityInformation<?, ?> entity) implements Specification<T> {

	KeysetScrollSpecification(KeysetScrollPosition position, Sort sort, JpaEntityInformation<?, ?> entity) {

		this.position = position;
		this.entity = entity;

		KeysetScrollDirector director = KeysetScrollDirector.of(position.getDirection());

		if (entity.hasCompositeId()) {
			sort = sort.and(Sort.by(entity.getIdAttributeNames().toArray(new String[0])));
		} else {
			sort = sort.and(Sort.by(entity.getIdAttribute().getName()));
		}

		this.sort = director.getSortOrders(sort);
	}

	@Override
	public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {

		// TODO selection
		// accept entities, make sure for the selection to include keyset

		KeysetScrollDirector director = KeysetScrollDirector.of(position.getDirection());

		return director.createQuery(position, sort, root, criteriaBuilder);
	}

	/**
	 * Director for keyset scrolling.
	 */
	static class KeysetScrollDirector {

		private static final KeysetScrollDirector forward = new KeysetScrollDirector();
		private static final KeysetScrollDirector reverse = new ReverseKeysetScrollDirector();

		/**
		 * Factory method to obtain the right {@link KeysetScrollDirector}.
		 *
		 * @param direction
		 * @return
		 */
		public static KeysetScrollDirector of(Direction direction) {
			return direction == Direction.Forward ? forward : reverse;
		}

		public Sort getSortOrders(Sort sort) {
			return sort;
		}

		@Nullable
		public Predicate createQuery(KeysetScrollPosition keyset, Sort sort, From<?, ?> from, CriteriaBuilder cb) {

			Map<String, Object> keysetValues = keyset.getKeys();

			// first query doesn't come with a keyset
			if (keysetValues.isEmpty()) {
				return null;
			}

			List<String> sortKeys = sort.stream().map(Sort.Order::getProperty).toList();

			if (!keysetValues.keySet().containsAll(sortKeys)) {
				throw new IllegalStateException("KeysetScrollPosition does not contain all keyset values");
			}

			// build matrix query for keyset paging that contains sort^2 queries
			// reflecting a query that follows sort order semantics starting from the last returned keyset
			List<Predicate> or = new ArrayList<>();
			for (int i = 0; i < sortKeys.size(); i++) {

				List<Predicate> sortConstraint = new ArrayList<>();

				for (int j = 0; j < sortKeys.size(); j++) {

					String sortSegment = sortKeys.get(j);
					PropertyPath property = PropertyPath.from(sortSegment, from.getJavaType());
					Expression<Comparable> propertyExpression = QueryUtils.toExpressionRecursively(from, property);
					Sort.Order sortOrder = sort.getOrderFor(sortSegment);
					Comparable<?> o = (Comparable<?>) keysetValues.get(sortSegment);

					if (j >= i) { // tail segment

						sortConstraint.add(getComparator(sortOrder, propertyExpression, o, cb));
						break;
					}

					sortConstraint.add(cb.equal(propertyExpression, o));
				}

				if (!sortConstraint.isEmpty()) {
					or.add(cb.and(sortConstraint.toArray(new Predicate[0])));
				}
			}

			if (or.isEmpty()) {
				return null;
			}

			return cb.or(or.toArray(new Predicate[0]));
		}

		protected Predicate getComparator(Sort.Order sortOrder, Expression<Comparable> propertyExpression,
				Comparable object, CriteriaBuilder cb) {

			return sortOrder.isAscending() ? cb.greaterThan(propertyExpression, object)
					: cb.lessThan(propertyExpression, object);
		}

		public <T> void postPostProcessResults(List<T> result) {

		}

	}

	/**
	 * Reverse scrolling director variant applying {@link Direction#Backward}. In reverse scrolling, we need to flip
	 * directions for the actual query so that we do not get everything from the top position and apply the limit but
	 * rather flip the sort direction, apply the limit and then reverse the result to restore the actual sort order.
	 */
	private static class ReverseKeysetScrollDirector extends KeysetScrollDirector {

		public Sort getSortOrders(Sort sort) {

			List<Sort.Order> orders = new ArrayList<>();
			for (Sort.Order order : sort) {
				orders.add(new Sort.Order(order.isAscending() ? Sort.Direction.DESC : Sort.Direction.ASC, order.getProperty()));
			}

			return Sort.by(orders);
		}

		@Override
		protected Predicate getComparator(Sort.Order sortOrder, Expression<Comparable> propertyExpression,
				Comparable object, CriteriaBuilder cb) {
			return sortOrder.isAscending() ? cb.greaterThanOrEqualTo(propertyExpression, object)
					: cb.lessThanOrEqualTo(propertyExpression, object);
		}

		@Override
		public <T> void postPostProcessResults(List<T> result) {
			// flip direction of the result list as we need to accomodate for the flipped sort order for proper offset
			// querying.
			Collections.reverse(result);
		}
	}

}
