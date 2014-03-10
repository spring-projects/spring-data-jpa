/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository.support;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.querydsl.QSort;
import org.springframework.util.Assert;

import com.mysema.query.jpa.EclipseLinkTemplates;
import com.mysema.query.jpa.HQLTemplates;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.OpenJPATemplates;
import com.mysema.query.jpa.impl.JPAQuery;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.Expression;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.OrderSpecifier.NullHandling;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathBuilder;

/**
 * Helper instance to ease access to Querydsl JPA query API.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class Querydsl {

	private final EntityManager em;
	private final PersistenceProvider provider;
	private final PathBuilder<?> builder;

	/**
	 * Creates a new {@link Querydsl} for the given {@link EntityManager} and {@link PathBuilder}.
	 * 
	 * @param em must not be {@literal null}.
	 * @param builder must not be {@literal null}.
	 */
	public Querydsl(EntityManager em, PathBuilder<?> builder) {

		Assert.notNull(em);
		Assert.notNull(builder);

		this.em = em;
		this.provider = PersistenceProvider.fromEntityManager(em);
		this.builder = builder;
	}

	/**
	 * Creates the {@link JPQLQuery} instance based on the configured {@link EntityManager}.
	 * 
	 * @return
	 */
	public JPQLQuery createQuery() {

		switch (provider) {
			case ECLIPSELINK:
				return new JPAQuery(em, EclipseLinkTemplates.DEFAULT);
			case HIBERNATE:
				return new JPAQuery(em, HQLTemplates.DEFAULT);
			case OPEN_JPA:
				return new JPAQuery(em, OpenJPATemplates.DEFAULT);
			case GENERIC_JPA:
			default:
				return new JPAQuery(em);
		}
	}

	/**
	 * Creates the {@link JPQLQuery} instance based on the configured {@link EntityManager}.
	 * 
	 * @return
	 */
	public JPQLQuery createQuery(EntityPath<?>... paths) {
		return createQuery().from(paths);
	}

	/**
	 * Applies the given {@link Pageable} to the given {@link JPQLQuery}.
	 * 
	 * @param pageable
	 * @param query must not be {@literal null}.
	 * @return the Querydsl {@link JPQLQuery}.
	 */
	public JPQLQuery applyPagination(Pageable pageable, JPQLQuery query) {

		if (pageable == null) {
			return query;
		}

		query.offset(pageable.getOffset());
		query.limit(pageable.getPageSize());

		return applySorting(pageable.getSort(), query);
	}

	/**
	 * Applies sorting to the given {@link JPQLQuery}.
	 * 
	 * @param sort
	 * @param query must not be {@literal null}.
	 * @return the Querydsl {@link JPQLQuery}
	 */
	public JPQLQuery applySorting(Sort sort, JPQLQuery query) {

		if (sort == null) {
			return query;
		}

		if (sort instanceof QSort) {
			return addOrderByFrom((QSort) sort, query);
		}

		return addOrderByFrom(sort, query);
	}

	/**
	 * Applies the given {@link OrderSpecifier}s to the given {@link JPQLQuery}. Potentially transforms the given
	 * {@code OrderSpecifier}s to be able to injection potentially necessary left-joins.
	 * 
	 * @param qsort must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 */

	private JPQLQuery addOrderByFrom(QSort qsort, JPQLQuery query) {
		return query.orderBy(adjustOrderSpecifierIfNecessary(qsort.getOrderSpecifiers(), query));
	}

	/**
	 * Rewrites the given {@link OrderSpecifier} if necessary, e.g. generates proper aliases and left-joins to be created
	 * if we detect ordering by an nested attribute.
	 * 
	 * @param originalOrderSpecifiers must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private OrderSpecifier<?>[] adjustOrderSpecifierIfNecessary(List<OrderSpecifier<?>> originalOrderSpecifiers,
			JPQLQuery query) {

		Assert.notNull(originalOrderSpecifiers, "Original order specifiers must not be null!");
		Assert.notNull(query, "Query must not be null!");

		boolean orderModificationNecessary = false;
		List<OrderSpecifier<?>> modifiedOrderSpecifiers = new ArrayList<OrderSpecifier<?>>();

		for (OrderSpecifier<?> order : originalOrderSpecifiers) {

			Path targetPath = ((Path) order.getTarget()).getMetadata().getParent();

			boolean targetPathRootIsEntityRoot = targetPath.getRoot().equals(builder.getRoot());
			boolean targetPathEqualsRootEnityPath = targetPath.toString().equals(builder.toString());

			if (!targetPathRootIsEntityRoot) {

				query.leftJoin((EntityPath) builder.get((String) targetPath.getMetadata().getElement()), targetPath);
			} else if (targetPathRootIsEntityRoot && !targetPathEqualsRootEnityPath) {

				PathBuilder joinPathBuilder = new PathBuilder(targetPath.getType(), targetPath.getMetadata().getElement()
						.toString());
				query.leftJoin((EntityPath) targetPath, joinPathBuilder);
				OrderSpecifier<?> modifiedOrder = new OrderSpecifier(order.getOrder(), joinPathBuilder.get(((Path) order
						.getTarget()).getMetadata().getElement().toString()), order.getNullHandling());
				modifiedOrderSpecifiers.add(modifiedOrder);
				orderModificationNecessary = true;
				continue;
			}

			modifiedOrderSpecifiers.add(order);
		}

		return orderModificationNecessary ? modifiedOrderSpecifiers.toArray(new OrderSpecifier<?>[modifiedOrderSpecifiers
				.size()]) : originalOrderSpecifiers.toArray(new OrderSpecifier<?>[originalOrderSpecifiers.size()]);
	}

	/**
	 * Converts the {@link Order} items of the given {@link Sort} into {@link OrderSpecifier} and attaches those to the
	 * given {@link JPQLQuery}.
	 * 
	 * @param sort must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @return
	 */
	private JPQLQuery addOrderByFrom(Sort sort, JPQLQuery query) {

		Assert.notNull(sort, "Sort must not be null!");
		Assert.notNull(query, "Query must not be null!");

		for (Order order : sort) {
			query.orderBy(toOrderSpecifier(order, query));
		}

		return query;
	}

	/**
	 * Transforms a plain {@link Order} into a QueryDsl specific {@link OrderSpecifier}.
	 * 
	 * @param order
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private OrderSpecifier<?> toOrderSpecifier(Order order, JPQLQuery query) {

		return new OrderSpecifier(order.isAscending() ? com.mysema.query.types.Order.ASC
				: com.mysema.query.types.Order.DESC, createSortPropertyExpression(order), NullHandling.NullsLast);
	}

	/**
	 * Creates an {@link Expression} for the given {@link Order} property.
	 * 
	 * @param order must not be {@literal null}.
	 * @return
	 */
	private Expression<?> createSortPropertyExpression(Order order) {

		Assert.notNull(order, "Order must not be null!");

		PropertyPath path = PropertyPath.from(order.getProperty(), builder.getType());
		Expression<?> sortPropertyExpression = builder;

		while (path != null) {

			if (!path.hasNext() && order.isIgnoreCase()) {
				sortPropertyExpression = ((PathBuilder<?>) sortPropertyExpression).getString(path.getSegment()).lower();
			} else {
				sortPropertyExpression = ((PathBuilder<?>) sortPropertyExpression).get(path.getSegment());
			}

			path = path.next();
		}
		return sortPropertyExpression;
	}
}
