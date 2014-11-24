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

import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.querydsl.QSort;
import org.springframework.util.Assert;

import com.mysema.query.jpa.EclipseLinkTemplates;
import com.mysema.query.jpa.HQLTemplates;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.OpenJPATemplates;
import com.mysema.query.jpa.impl.AbstractJPAQuery;
import com.mysema.query.jpa.impl.JPAQuery;
import com.mysema.query.support.Expressions;
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
	public AbstractJPAQuery<JPAQuery> createQuery() {

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
	public AbstractJPAQuery<JPAQuery> createQuery(EntityPath<?>... paths) {
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

		List<OrderSpecifier<?>> orderSpecifiers = qsort.getOrderSpecifiers();
		return query.orderBy(orderSpecifiers.toArray(new OrderSpecifier[orderSpecifiers.size()]));
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
			query.orderBy(toOrderSpecifier(order));
		}

		return query;
	}

	/**
	 * Transforms a plain {@link Order} into a QueryDsl specific {@link OrderSpecifier}.
	 * 
	 * @param order must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private OrderSpecifier<?> toOrderSpecifier(Order order) {

		return new OrderSpecifier(order.isAscending() ? com.mysema.query.types.Order.ASC
				: com.mysema.query.types.Order.DESC, buildOrderPropertyPathFrom(order),
				toQueryDslNullHandling(order.getNullHandling()));
	}

	/**
	 * Converts the given {@link org.springframework.data.domain.Sort.NullHandling} to the appropriate Querydsl
	 * {@link NullHandling}.
	 * 
	 * @param nullHandling must not be {@literal null}.
	 * @return
	 * @since 1.6
	 */
	private NullHandling toQueryDslNullHandling(org.springframework.data.domain.Sort.NullHandling nullHandling) {

		Assert.notNull(nullHandling, "NullHandling must not be null!");

		switch (nullHandling) {

			case NULLS_FIRST:
				return NullHandling.NullsFirst;

			case NULLS_LAST:
				return NullHandling.NullsLast;

			case NATIVE:
			default:
				return NullHandling.Default;
		}
	}

	/**
	 * Creates an {@link Expression} for the given {@link Order} property.
	 * 
	 * @param order must not be {@literal null}.
	 * @return
	 */
	private Expression<?> buildOrderPropertyPathFrom(Order order) {

		Assert.notNull(order, "Order must not be null!");

		PropertyPath path = PropertyPath.from(order.getProperty(), builder.getType());
		Expression<?> sortPropertyExpression = builder;

		while (path != null) {

			if (!path.hasNext() && order.isIgnoreCase()) {
				// if order is ignore-case we have to treat the last path segment as a String.
				sortPropertyExpression = Expressions.stringPath((Path<?>) sortPropertyExpression, path.getSegment()).lower();
			} else {
				sortPropertyExpression = Expressions.path(path.getType(), (Path<?>) sortPropertyExpression, path.getSegment());
			}

			path = path.next();
		}

		return sortPropertyExpression;
	}
}
