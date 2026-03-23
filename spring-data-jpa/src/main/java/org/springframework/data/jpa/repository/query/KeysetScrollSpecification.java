/*
 * Copyright 2023-present the original author or authors.
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
import jakarta.persistence.metamodel.Bindable;
import jakarta.persistence.metamodel.Metamodel;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.data.core.PropertyPath;
import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.KeysetScrollDelegate.QueryStrategy;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;

/**
 * {@link Specification} to create scroll queries using keyset-scrolling.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 3.1
 */
public record KeysetScrollSpecification<T>(KeysetScrollPosition position, Sort sort,
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
		return KeysetScrollDelegate.of(position.getDirection()).createSort(sort, entity);
	}

	@Override
	public @Nullable Predicate toPredicate(Root<T> root, @Nullable CriteriaQuery<?> query,
			CriteriaBuilder criteriaBuilder) {
		return createPredicate(root, criteriaBuilder);
	}

	public @Nullable Predicate createPredicate(Root<?> root, CriteriaBuilder criteriaBuilder) {

		KeysetScrollDelegate delegate = KeysetScrollDelegate.of(position.getDirection());
		return delegate.createPredicate(position, sort, new CriteriaBuilderStrategy(root, criteriaBuilder));
	}

	public JpqlQueryBuilder.@Nullable Predicate createJpqlPredicate(Metamodel metamodel, Bindable<?> from,
			JpqlQueryBuilder.Entity entity,
			ParameterFactory factory) {

		KeysetScrollDelegate delegate = KeysetScrollDelegate.of(position.getDirection());
		return delegate.createPredicate(position, sort, new JpqlStrategy(metamodel, from, entity, factory));
	}

	/**
	 * Determine whether the strict tail predicate for this column is {@code IS NOT NULL} if the keyset value is
	 * {@code null} returning {@code true} or unsatisfiable {@code false}.
	 */
	public static boolean requiresNonNullTail(Order order) {

		boolean ascending = order.isAscending();
		boolean nullIsMaximum = (isNullsLast(order) && ascending) || (isNullsFirst(order) && !ascending);
		return ascending != nullIsMaximum;
	}

	public static boolean isNullsFirst(Order order) {
		return order.getNullHandling() == Sort.NullHandling.NULLS_FIRST;
	}

	public static boolean isNullsLast(Order order) {
		return order.getNullHandling() == Sort.NullHandling.NULLS_LAST;
	}

	@SuppressWarnings("rawtypes")
	record CriteriaBuilderStrategy(From<?, ?> from,
			CriteriaBuilder cb) implements QueryStrategy<Expression<Comparable>, Predicate> {

		@Override
		public Expression<Comparable> createExpression(String property) {
			PropertyPath path = PropertyPath.from(property, from.getJavaType());
			return QueryUtils.toExpressionRecursively(from, path);
		}

		@Override
		public Predicate compare(Order order, Expression<Comparable> propertyExpression, @Nullable Object value) {

			if (value != null) {
				Predicate strict = order.isAscending() ? cb.greaterThan(propertyExpression, (Comparable) value)
						: cb.lessThan(propertyExpression, (Comparable) value);
				return isNullsLast(order) ? cb.or(strict, cb.isNull(propertyExpression)) : strict;
			}

			return requiresNonNullTail(order) ? cb.isNotNull(propertyExpression) : cb.disjunction();
		}

		@Override
		public Predicate compare(String property, Expression<Comparable> propertyExpression, @Nullable Object value) {
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

	record JpqlStrategy(Metamodel metamodel, Bindable<?> from, JpqlQueryBuilder.Entity entity,
			ParameterFactory factory) implements QueryStrategy<JpqlQueryBuilder.Expression, JpqlQueryBuilder.Predicate> {

		@Override
		public JpqlQueryBuilder.Expression createExpression(String property) {

			PropertyPath path = PropertyPath.from(property, from.getBindableJavaType());
			return JpqlUtils.toExpressionRecursively(metamodel, entity, from, path);
		}

		@Override
		public JpqlQueryBuilder.Predicate compare(Order order, JpqlQueryBuilder.Expression propertyExpression,
				@Nullable Object value) {

			JpqlQueryBuilder.WhereStep where = JpqlQueryBuilder.where(propertyExpression);

			QueryUtils.checkSortExpression(order);

			if (value != null) {
				JpqlQueryBuilder.Predicate strict = order.isAscending() ? where.gt(factory.capture(order.getProperty(), value))
						: where.lt(factory.capture(order.getProperty(), value));
				return isNullsLast(order) ? strict.or(where.isNull()) : strict;
			}

			if (requiresNonNullTail(order)) {
				return where.isNotNull();
			}

			return JpqlQueryBuilder.where(JpqlQueryBuilder.literal(1)).eq(JpqlQueryBuilder.literal(0));
		}

		@Override
		public JpqlQueryBuilder.Predicate compare(String property, JpqlQueryBuilder.Expression propertyExpression,
				@Nullable Object value) {

			JpqlQueryBuilder.WhereStep where = JpqlQueryBuilder.where(propertyExpression);

			return value == null ? where.isNull() : where.eq(factory.capture(property, value));
		}

		@Override
		public JpqlQueryBuilder.@Nullable Predicate and(List<JpqlQueryBuilder.Predicate> intermediate) {
			return JpqlQueryBuilder.and(intermediate);
		}

		@Override
		public JpqlQueryBuilder.@Nullable Predicate or(List<JpqlQueryBuilder.Predicate> intermediate) {
			return JpqlQueryBuilder.or(intermediate);
		}
	}

	public interface ParameterFactory {
		JpqlQueryBuilder.Expression capture(String name, @Nullable Object value);
	}

}
