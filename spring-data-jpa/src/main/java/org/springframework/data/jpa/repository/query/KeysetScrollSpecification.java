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
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.KeysetScrollDelegate.QueryStrategy;
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
	 * @param position must not be {@literal null}.
	 * @param sort must not be {@literal null}.
	 * @param entity must not be {@literal null}.
	 */
	public static Sort createSort(KeysetScrollPosition position, Sort sort, JpaEntityInformation<?, ?> entity) {

		KeysetScrollDelegate delegate = KeysetScrollDelegate.of(position.getDirection());

		Collection<String> sortById;
		Sort sortToUse;
		if (entity.hasCompositeId()) {
			sortById = new ArrayList<>(entity.getIdAttributeNames());
		} else {
			sortById = new ArrayList<>(1);
			sortById.add(entity.getRequiredIdAttribute().getName());
		}

		sort.forEach(it -> sortById.remove(it.getProperty()));

		if (sortById.isEmpty()) {
			sortToUse = sort;
		} else {
			sortToUse = sort.and(Sort.by(sortById.toArray(new String[0])));
		}

		return delegate.getSortOrders(sortToUse);
	}

	@Override
	public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
		return createPredicate(root, criteriaBuilder);
	}

	@Nullable
	public Predicate createPredicate(Root<?> root, CriteriaBuilder criteriaBuilder) {

		KeysetScrollDelegate delegate = KeysetScrollDelegate.of(position.getDirection());
		return delegate.createPredicate(position, sort, new JpaQueryStrategy(root, criteriaBuilder));
	}

	@SuppressWarnings("rawtypes")
	private static class JpaQueryStrategy implements QueryStrategy<Expression<Comparable>, Predicate> {

		private final From<?, ?> from;
		private final CriteriaBuilder cb;

		public JpaQueryStrategy(From<?, ?> from, CriteriaBuilder cb) {

			this.from = from;
			this.cb = cb;
		}

		@Override
		public Expression<Comparable> createExpression(String property) {

			PropertyPath path = PropertyPath.from(property, from.getJavaType());
			return QueryUtils.toExpressionRecursively(from, path);
		}

		@Override
		public Predicate compare(Order order, Expression<Comparable> propertyExpression, Object value) {

			return order.isAscending() ? cb.greaterThan(propertyExpression, (Comparable) value)
					: cb.lessThan(propertyExpression, (Comparable) value);
		}

		@Override
		public Predicate compare(Expression<Comparable> propertyExpression, @Nullable Object value) {
			return value == null ? cb.isNull(propertyExpression) : cb.equal(propertyExpression, value);
		}

		@Override
		public Predicate and(List<Predicate> intermediate) {
			return cb.and(intermediate.toArray(new Predicate[0]));
		}

		@Override
		public Predicate or(List<Predicate> intermediate) {
			return cb.or(intermediate.toArray(new Predicate[0]));
		}
	}
}
