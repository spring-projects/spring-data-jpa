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

import java.util.List;

import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.KeysetScrollDelegate.QueryAdapter;
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

		KeysetScrollDelegate director = KeysetScrollDelegate.of(position.getDirection());

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

		KeysetScrollDelegate director = KeysetScrollDelegate.of(position.getDirection());
		return director.createPredicate(position, sort, new JpaQueryAdapter(root, criteriaBuilder));
	}

	@SuppressWarnings("rawtypes")
	private static class JpaQueryAdapter implements QueryAdapter<Expression<Comparable>, Predicate> {

		private final From<?, ?> from;
		private final CriteriaBuilder cb;

		public JpaQueryAdapter(From<?, ?> from, CriteriaBuilder cb) {
			this.from = from;
			this.cb = cb;
		}

		@Override
		public Expression<Comparable> createExpression(String property) {
			PropertyPath path = PropertyPath.from(property, from.getJavaType());
			return QueryUtils.toExpressionRecursively(from, path);
		}

		@Override
		public Predicate compare(Order order, Expression<Comparable> propertyExpression, Object o) {
			return order.isAscending() ? cb.greaterThan(propertyExpression, (Comparable) o)
					: cb.lessThan(propertyExpression, (Comparable) o);
		}

		@Override
		public Predicate compare(Expression<Comparable> propertyExpression, Object o) {
			return cb.equal(propertyExpression, o);
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
