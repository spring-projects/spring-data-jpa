/*
 * Copyright 2012 the original author or authors.
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

import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.util.Assert;

import com.mysema.query.jpa.EclipseLinkTemplates;
import com.mysema.query.jpa.HQLTemplates;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.OpenJPATemplates;
import com.mysema.query.jpa.impl.JPAQuery;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.Expression;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.PathBuilder;

/**
 * Helper instance to ease access to Querydsl JPA query API.
 * 
 * @author Oliver Gierke
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

		for (Order order : sort) {
			query.orderBy(toOrder(order, query));
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
	private OrderSpecifier<?> toOrder(Order order, JPQLQuery query) {

		Expression<?> property = createExpressionAndPotentionallyAddLeftJoinForReferencedAssociation(order, query);

		return new OrderSpecifier(order.isAscending() ? com.mysema.query.types.Order.ASC
				: com.mysema.query.types.Order.DESC, property);
	}

	/**
	 * Potentially adds a left join to the given {@link JPQLQuery} query if the order contains a property path that uses
	 * an association and returns the property expression build from the path of the association.
	 * 
	 * @param order must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @return property expression.
	 */
	private Expression<?> createExpressionAndPotentionallyAddLeftJoinForReferencedAssociation(Order order, JPQLQuery query) {

		Assert.notNull(order, "Order must not be null!");
		Assert.notNull(query, "JPQLQuery must not be null!");

		if (!order.getProperty().contains(".")) {
			// Apply ignore case in case we have a String and ignore case ordering is requested
			return order.isIgnoreCase() ? builder.getString(order.getProperty()).lower() : builder.get(order.getProperty());
		}

		EntityType<?> entitytype = em.getMetamodel().entity(builder.getType());

		Set<Attribute<?, ?>> combinedAttributes = new LinkedHashSet<Attribute<?, ?>>();
		combinedAttributes.addAll(entitytype.getSingularAttributes());
		combinedAttributes.addAll(entitytype.getPluralAttributes());

		for (Attribute<?, ?> attribute : combinedAttributes) {

			if (order.getProperty().startsWith(attribute.getName() + ".")) {

				switch (attribute.getPersistentAttributeType()) {
					case EMBEDDED:
						return builder.get(order.getProperty());
					default:
						return createLeftJoinForAttributeInOrderBy(attribute, order, query);
				}
			}
		}

		throw new IllegalArgumentException(
				String.format("Could not create property expression for %s", order.getProperty()));
	}

	/**
	 * @param attribute
	 * @param order
	 * @param query
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Expression<?> createLeftJoinForAttributeInOrderBy(Attribute<?, ?> attribute, Order order, JPQLQuery query) {

		EntityPathBase<?> associationPathRoot = new EntityPathBase<Object>(attribute.getJavaType(), attribute.getName());
		query.leftJoin((EntityPath) builder.get(attribute.getName()), associationPathRoot);
		PathBuilder<Object> attributePathBuilder = new PathBuilder<Object>(attribute.getJavaType(),
				associationPathRoot.getMetadata());

		String nestedAttributePath = order.getProperty().substring(attribute.getName().length() + 1); // exclude "."
		return order.isIgnoreCase() ? attributePathBuilder.getString(nestedAttributePath).lower() : attributePathBuilder
				.get(nestedAttributePath);
	}
}
