/*
 * Copyright 2008-2012 the original author or authors.
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
package org.springframework.data.jpa.repository.query;

import static org.springframework.data.jpa.repository.query.QueryUtils.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.query.ParameterMetadataProvider.ParameterMetadata;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.util.Assert;

/**
 * Query creator to create a {@link CriteriaQuery} from a {@link PartTree}.
 * 
 * @author Oliver Gierke
 */
public class JpaQueryCreator extends AbstractQueryCreator<CriteriaQuery<Object>, Predicate> {

	private final CriteriaBuilder builder;
	private final Root<?> root;
	private final CriteriaQuery<Object> query;
	private final ParameterMetadataProvider provider;

	/**
	 * Create a new {@link JpaQueryCreator}.
	 * 
	 * @param tree
	 * @param domainClass
	 * @param accessor
	 * @param em
	 */
	public JpaQueryCreator(PartTree tree, Class<?> domainClass, CriteriaBuilder builder,
			ParameterMetadataProvider provider) {

		super(tree);

		this.builder = builder;
		this.query = builder.createQuery().distinct(tree.isDistinct());
		this.root = query.from(domainClass);
		this.provider = provider;
	}

	/**
	 * Returns all {@link javax.persistence.criteria.ParameterExpression} created when creating the query.
	 * 
	 * @return the parameterExpressions
	 */
	public List<ParameterMetadata<?>> getParameterExpressions() {
		return provider.getExpressions();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.parser.AbstractQueryCreator#create(org.springframework.data.repository.query.parser.Part, java.util.Iterator)
	 */
	@Override
	protected Predicate create(Part part, Iterator<Object> iterator) {

		return toPredicate(part, root);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.parser.AbstractQueryCreator#and(org.springframework.data.repository.query.parser.Part, java.lang.Object, java.util.Iterator)
	 */
	@Override
	protected Predicate and(Part part, Predicate base, Iterator<Object> iterator) {

		return builder.and(base, toPredicate(part, root));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.parser.AbstractQueryCreator#or(java.lang.Object, java.lang.Object)
	 */
	@Override
	protected Predicate or(Predicate base, Predicate predicate) {

		return builder.or(base, predicate);
	}

	/**
	 * Finalizes the given {@link Predicate} and applies the given sort. Delegates to
	 * {@link #complete(Predicate, Sort, CriteriaQuery, CriteriaBuilder)} and hands it the current {@link CriteriaQuery}
	 * and {@link CriteriaBuilder}.
	 */
	@Override
	protected final CriteriaQuery<Object> complete(Predicate predicate, Sort sort) {

		return complete(predicate, sort, query, builder, root);
	}

	/**
	 * Template method to finalize the given {@link Predicate} using the given {@link CriteriaQuery} and
	 * {@link CriteriaBuilder}.
	 * 
	 * @param predicate
	 * @param sort
	 * @param query
	 * @param builder
	 * @return
	 */
	protected CriteriaQuery<Object> complete(Predicate predicate, Sort sort, CriteriaQuery<Object> query,
			CriteriaBuilder builder, Root<?> root) {

		CriteriaQuery<Object> select = this.query.select(root).orderBy(QueryUtils.toOrders(sort, root, builder));
		return predicate == null ? select : select.where(predicate);
	}

	/**
	 * Creates a {@link Predicate} from the given {@link Part}.
	 * 
	 * @param part
	 * @param root
	 * @param iterator
	 * @return
	 */
	private Predicate toPredicate(Part part, Root<?> root) {
		return new PredicateBuilder(part, root).build();
	}

	/**
	 * Returns a path to a {@link Comparable}.
	 * 
	 * @param root
	 * @param part
	 * @return
	 */
	@SuppressWarnings({ "rawtypes" })
	private Expression<? extends Comparable> getComparablePath(Root<?> root, Part part) {

		return getTypedPath(root, part);
	}

	private <T> Expression<T> getTypedPath(Root<?> root, Part part) {
		return toExpressionRecursively(root, part.getProperty());
	}

	/**
	 * Simple builder to contain logic to create JPA {@link Predicate}s from {@link Part}s.
	 * 
	 * @author Phil Webb
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private class PredicateBuilder {

		private final Part part;
		private final Root<?> root;

		/**
		 * Creates a new {@link PredicateBuilder} for the given {@link Part} and {@link Root}.
		 * 
		 * @param part must not be {@literal null}.
		 * @param root must not be {@literal null}.
		 */
		public PredicateBuilder(Part part, Root<?> root) {

			Assert.notNull(part);
			Assert.notNull(root);
			this.part = part;
			this.root = root;
		}

		/**
		 * Builds a JPA {@link Predicate} from the underlying {@link Part}.
		 * 
		 * @return
		 */
		public Predicate build() {

			PropertyPath property = part.getProperty();
			Expression<Object> path = toExpressionRecursively(root, property);

			switch (part.getType()) {
				case BETWEEN:
					ParameterMetadata<Comparable> first = provider.next(part);
					ParameterMetadata<Comparable> second = provider.next(part);
					return builder.between(getComparablePath(root, part), first.getExpression(), second.getExpression());
				case AFTER:
				case GREATER_THAN:
					return builder.greaterThan(getComparablePath(root, part), provider.next(part, Comparable.class)
							.getExpression());
				case GREATER_THAN_EQUAL:
					return builder.greaterThanOrEqualTo(getComparablePath(root, part), provider.next(part, Comparable.class)
							.getExpression());
				case BEFORE:
				case LESS_THAN:
					return builder.lessThan(getComparablePath(root, part), provider.next(part, Comparable.class).getExpression());
				case LESS_THAN_EQUAL:
					return builder.lessThanOrEqualTo(getComparablePath(root, part), provider.next(part, Comparable.class)
							.getExpression());
				case IS_NULL:
					return path.isNull();
				case IS_NOT_NULL:
					return path.isNotNull();
				case NOT_IN:
					return path.in(provider.next(part, Collection.class).getExpression()).not();
				case IN:
					return path.in(provider.next(part, Collection.class).getExpression());
				case STARTING_WITH:
				case ENDING_WITH:
				case CONTAINING:
				case LIKE:
				case NOT_LIKE:
					Expression<String> stringPath = getTypedPath(root, part);
					Expression<String> propertyExpression = upperIfIgnoreCase(stringPath);
					Expression<String> parameterExpression = upperIfIgnoreCase(provider.next(part, String.class).getExpression());
					Predicate like = builder.like(propertyExpression, parameterExpression);
					return part.getType() == Type.NOT_LIKE ? like.not() : like;
				case TRUE:
					Expression<Boolean> truePath = getTypedPath(root, part);
					return builder.isTrue(truePath);
				case FALSE:
					Expression<Boolean> falsePath = getTypedPath(root, part);
					return builder.isFalse(falsePath);
				case SIMPLE_PROPERTY:
					ParameterMetadata<Object> expression = provider.next(part);
					return expression.isIsNullParameter() ? path.isNull() : builder.equal(upperIfIgnoreCase(path),
							upperIfIgnoreCase(expression.getExpression()));
				case NEGATING_SIMPLE_PROPERTY:
					return builder.notEqual(upperIfIgnoreCase(path), upperIfIgnoreCase(provider.next(part).getExpression()));
				default:
					throw new IllegalArgumentException("Unsupported keyword " + part.getType());
			}
		}

		/**
		 * Applies an {@code UPPERCASE} conversion to the given {@link Expression} in case the underlying {@link Part}
		 * requires ignoring case.
		 * 
		 * @param expression must not be {@literal null}.
		 * @return
		 */
		private <T> Expression<T> upperIfIgnoreCase(Expression<? extends T> expression) {

			switch (part.shouldIgnoreCase()) {

				case ALWAYS:

					Assert.state(canUpperCase(expression), "Unable to ignore case of " + expression.getJavaType().getName()
							+ " types, the property '" + part.getProperty().getSegment() + "' must reference a String");
					return (Expression<T>) builder.upper((Expression<String>) expression);

				case WHEN_POSSIBLE:

					if (canUpperCase(expression)) {
						return (Expression<T>) builder.upper((Expression<String>) expression);
					}

				case NEVER:
				default:

					return (Expression<T>) expression;
			}
		}

		private boolean canUpperCase(Expression<?> expression) {
			return String.class.equals(expression.getJavaType());
		}
	}
}
