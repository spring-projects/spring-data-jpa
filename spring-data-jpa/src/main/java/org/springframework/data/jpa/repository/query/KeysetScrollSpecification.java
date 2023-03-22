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
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.lang.Nullable;

/**
 * {@link Specification} to create scroll queries using keyset-scrolling.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 3.1
 */
public record KeysetScrollSpecification<T> (KeysetScrollPosition position, Sort sort,
		JpaEntityInformation<?, ?> entity) implements Specification<T> {

	public KeysetScrollSpecification(KeysetScrollPosition position, Sort sort, JpaEntityInformation<?, ?> entity) {

		this.position = position;
		this.entity = entity;
		this.sort = createSort(position, sort, entity);
	}

	/**
	 * Create a {@link Sort} object to be used with the actual query.
	 *
	 * @param position
	 * @param sort
	 * @param entity
	 * @return
	 */
	public static Sort createSort(KeysetScrollPosition position, Sort sort, JpaEntityInformation<?, ?> entity) {

		KeysetScrollDirector director = KeysetScrollDirector.of(position.getDirection());

		Sort sortToUse;
		if (entity.hasCompositeId()) {
			sortToUse = sort.and(Sort.by(entity.getIdAttributeNames().toArray(new String[0])));
		} else {
			sortToUse = sort.and(Sort.by(entity.getRequiredIdAttribute().getName()));
		}

		return director.getSortOrders(sortToUse);
	}

	@Override
	public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
		return createPredicate(root, criteriaBuilder);
	}

	@Nullable
	public Predicate createPredicate(Root<?> root, CriteriaBuilder criteriaBuilder) {

		KeysetScrollDirector director = KeysetScrollDirector.of(position.getDirection());
		return director.createQuery(position, sort, root, criteriaBuilder);
	}

	/**
	 * Director for keyset scrolling.
	 */
	@SuppressWarnings("rawtypes")
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

			// build matrix query for keyset paging that contains sort^2 queries
			// reflecting a query that follows sort order semantics starting from the last returned keyset
			List<Predicate> or = createPredicates(sort, from, cb, keysetValues);

			if (or.isEmpty()) {
				return null;
			}

			return cb.or(or.toArray(new Predicate[0]));
		}

		List<Predicate> createPredicates(Sort sort, From<?, ?> from, CriteriaBuilder cb, Map<String, Object> keysetValues) {

			List<Predicate> or = new ArrayList<>();

			int i = 0;
			// progressive query building
			for (Order order : sort) {

				if (!keysetValues.containsKey(order.getProperty())) {
					throw new IllegalStateException("KeysetScrollPosition does not contain all keyset values");
				}

				List<Predicate> sortConstraint = new ArrayList<>();

				int j = 0;
				for (Sort.Order inner : sort) {

					Expression<Comparable> propertyExpression = getExpression(from, inner);
					Comparable<?> o = (Comparable<?>) keysetValues.get(inner.getProperty());

					if (j >= i) { // tail segment

						sortConstraint.add(getComparator(inner, propertyExpression, o, cb));
						break;
					}

					sortConstraint.add(cb.equal(propertyExpression, o));
					j++;
				}

				if (!sortConstraint.isEmpty()) {
					or.add(cb.and(sortConstraint.toArray(new Predicate[0])));
				}

				i++;
			}

			return or;
		}

		@SuppressWarnings("unchecked")
		protected Predicate getComparator(Sort.Order sortOrder, Expression<Comparable> propertyExpression,
				Comparable object, CriteriaBuilder cb) {

			return sortOrder.isAscending() ? cb.greaterThan(propertyExpression, object)
					: cb.lessThan(propertyExpression, object);
		}

		protected static <T> Expression<T> getExpression(From<?, ?> from, Order order) {
			PropertyPath property = PropertyPath.from(order.getProperty(), from.getJavaType());
			return QueryUtils.toExpressionRecursively(from, property);
		}

		public <T> List<T> postProcessResults(List<T> result) {
			return result;
		}

		public <T> List<T> getResultWindow(List<T> list, int limit) {
			return CollectionUtils.getFirst(limit, list);
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

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		protected Predicate getComparator(Sort.Order sortOrder, Expression<Comparable> propertyExpression,
				Comparable object, CriteriaBuilder cb) {
			return sortOrder.isAscending() ? cb.greaterThan(propertyExpression, object)
					: cb.lessThan(propertyExpression, object);
		}

		@Override
		public <T> List<T> postProcessResults(List<T> result) {
			Collections.reverse(result);
			return result;
		}

		@Override
		public <T> List<T> getResultWindow(List<T> list, int limit) {
			return CollectionUtils.getLast(limit, list);
		}
	}

}
