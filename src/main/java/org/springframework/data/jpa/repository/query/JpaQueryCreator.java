/*
 * Copyright 2008-2019 the original author or authors.
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

import static org.springframework.data.jpa.repository.query.QueryUtils.*;
import static org.springframework.data.repository.query.parser.Part.Type.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.metamodel.SingularAttribute;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.query.ParameterMetadataProvider.ParameterMetadata;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Query creator to create a {@link CriteriaQuery} from a {@link PartTree}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Michael Cramer
 * @author Mark Paluch
 * @author Reda.Housni-Alaoui
 * @author Moritz Becker
 */
public class JpaQueryCreator extends AbstractQueryCreator<CriteriaQuery<? extends Object>, Predicate> {

	private final CriteriaBuilder builder;
	private final Root<?> root;
	private final CriteriaQuery<? extends Object> query;
	private final ParameterMetadataProvider provider;
	private final ReturnedType returnedType;
	private final PartTree tree;
	private final EscapeCharacter escape;

	/**
	 * Create a new {@link JpaQueryCreator}.
	 *
	 * @param tree must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 * @param builder must not be {@literal null}.
	 * @param provider must not be {@literal null}.
	 */
	public JpaQueryCreator(PartTree tree, ReturnedType type, CriteriaBuilder builder,
			ParameterMetadataProvider provider) {

		super(tree);
		this.tree = tree;

		CriteriaQuery<?> criteriaQuery = createCriteriaQuery(builder, type);

		this.builder = builder;
		this.query = criteriaQuery.distinct(tree.isDistinct());
		this.root = query.from(type.getDomainType());
		this.provider = provider;
		this.returnedType = type;
		this.escape = provider.getEscape();
	}

	/**
	 * Creates the {@link CriteriaQuery} to apply predicates on.
	 *
	 * @param builder will never be {@literal null}.
	 * @param type will never be {@literal null}.
	 * @return must not be {@literal null}.
	 */
	protected CriteriaQuery<? extends Object> createCriteriaQuery(CriteriaBuilder builder, ReturnedType type) {

		Class<?> typeToRead = type.getTypeToRead();

		return typeToRead == null || tree.isExistsProjection() ? builder.createTupleQuery()
				: builder.createQuery(typeToRead);
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
	 * {@link #complete(Predicate, Sort, CriteriaQuery, CriteriaBuilder, Root)} and hands it the current {@link CriteriaQuery}
	 * and {@link CriteriaBuilder}.
	 */
	@Override
	protected final CriteriaQuery<? extends Object> complete(Predicate predicate, Sort sort) {
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
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected CriteriaQuery<? extends Object> complete(@Nullable Predicate predicate, Sort sort,
			CriteriaQuery<? extends Object> query, CriteriaBuilder builder, Root<?> root) {

		if (returnedType.needsCustomConstruction()) {

			List<Selection<?>> selections = new ArrayList<>();

			for (String property : returnedType.getInputProperties()) {

				PropertyPath path = PropertyPath.from(property, returnedType.getDomainType());
				selections.add(toExpressionRecursively(root, path, true).alias(property));
			}

			query = query.multiselect(selections);

		} else if (tree.isExistsProjection()) {

			if (root.getModel().hasSingleIdAttribute()) {

				SingularAttribute<?, ?> id = root.getModel().getId(root.getModel().getIdType().getJavaType());
				query = query.multiselect(root.get((SingularAttribute) id).alias(id.getName()));

			} else {

				query = query.multiselect(root.getModel().getIdClassAttributes().stream()//
						.map(it -> (Selection<?>) root.get((SingularAttribute) it).alias(it.getName()))
						.collect(Collectors.toList()));
			}

		} else {
			query = query.select((Root) root);
		}

		CriteriaQuery<? extends Object> select = query.orderBy(QueryUtils.toOrders(sort, root, builder));
		return predicate == null ? select : select.where(predicate);
	}

	/**
	 * Creates a {@link Predicate} from the given {@link Part}.
	 *
	 * @param part
	 * @param root
	 * @return
	 */
	private Predicate toPredicate(Part part, Root<?> root) {
		return new PredicateBuilder(part, root).build();
	}

	/**
	 * Simple builder to contain logic to create JPA {@link Predicate}s from {@link Part}s.
	 *
	 * @author Phil Webb
	 * @author Oliver Gierke
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

			Assert.notNull(part, "Part must not be null!");
			Assert.notNull(root, "Root must not be null!");
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
			Type type = part.getType();

			switch (type) {
				case BETWEEN:
					ParameterMetadata<Comparable> first = provider.next(part);
					ParameterMetadata<Comparable> second = provider.next(part);
					return builder.between(getComparablePath(root, part), first.getExpression(), second.getExpression());
				case AFTER:
				case GREATER_THAN:
					return builder.greaterThan(getComparablePath(root, part),
							provider.next(part, Comparable.class).getExpression());
				case GREATER_THAN_EQUAL:
					return builder.greaterThanOrEqualTo(getComparablePath(root, part),
							provider.next(part, Comparable.class).getExpression());
				case BEFORE:
				case LESS_THAN:
					return builder.lessThan(getComparablePath(root, part), provider.next(part, Comparable.class).getExpression());
				case LESS_THAN_EQUAL:
					return builder.lessThanOrEqualTo(getComparablePath(root, part),
							provider.next(part, Comparable.class).getExpression());
				case IS_NULL:
					return getTypedPath(root, part).isNull();
				case IS_NOT_NULL:
					return getTypedPath(root, part).isNotNull();
				case NOT_IN:
					// cast required for eclipselink workaround, see DATAJPA-433
					return getTypedPath(root, part).in((Expression<Collection<?>>) provider.next(part, Collection.class).getExpression()).not();
				case IN:
					// cast required for eclipselink workaround, see DATAJPA-433
					return getTypedPath(root, part).in((Expression<Collection<?>>) provider.next(part, Collection.class).getExpression());
				case STARTING_WITH:
				case ENDING_WITH:
				case CONTAINING:
				case NOT_CONTAINING:

					if (property.getLeafProperty().isCollection()) {

						Expression<Collection<Object>> propertyExpression = traversePath(root, property);
						ParameterExpression<Object> parameterExpression = provider.next(part).getExpression();

						// Can't just call .not() in case of negation as EclipseLink chokes on that.
						return type.equals(NOT_CONTAINING) ? isNotMember(builder, parameterExpression, propertyExpression)
								: isMember(builder, parameterExpression, propertyExpression);
					}

				case LIKE:
				case NOT_LIKE:
					Expression<String> stringPath = getTypedPath(root, part);
					Expression<String> propertyExpression = upperIfIgnoreCase(stringPath);
					Expression<String> parameterExpression = upperIfIgnoreCase(provider.next(part, String.class).getExpression());
					Predicate like = builder.like(propertyExpression, parameterExpression, escape.getEscapeCharacter());
					return type.equals(NOT_LIKE) || type.equals(NOT_CONTAINING) ? like.not() : like;
				case TRUE:
					Expression<Boolean> truePath = getTypedPath(root, part);
					return builder.isTrue(truePath);
				case FALSE:
					Expression<Boolean> falsePath = getTypedPath(root, part);
					return builder.isFalse(falsePath);
				case SIMPLE_PROPERTY:
					ParameterMetadata<Object> expression = provider.next(part);
					Expression<Object> path = getTypedPath(root, part);
					return expression.isIsNullParameter() ? path.isNull()
							: builder.equal(upperIfIgnoreCase(path), upperIfIgnoreCase(expression.getExpression()));
				case NEGATING_SIMPLE_PROPERTY:
					return builder.notEqual(upperIfIgnoreCase(getTypedPath(root, part)),
							upperIfIgnoreCase(provider.next(part).getExpression()));
				case IS_EMPTY:
				case IS_NOT_EMPTY:

					if (!property.getLeafProperty().isCollection()) {
						throw new IllegalArgumentException("IsEmpty / IsNotEmpty can only be used on collection properties!");
					}

					Expression<Collection<Object>> collectionPath = traversePath(root, property);
					return type.equals(IS_NOT_EMPTY) ? builder.isNotEmpty(collectionPath) : builder.isEmpty(collectionPath);

				default:
					throw new IllegalArgumentException("Unsupported keyword " + type);
			}
		}

		private <T> Predicate isMember(CriteriaBuilder builder, Expression<T> parameter,
				Expression<Collection<T>> property) {
			return builder.isMember(parameter, property);
		}

		private <T> Predicate isNotMember(CriteriaBuilder builder, Expression<T> parameter,
				Expression<Collection<T>> property) {
			return builder.isNotMember(parameter, property);
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

		/**
		 * Returns a path to a {@link Comparable}.
		 *
		 * @param root
		 * @param part
		 * @return
		 */
		private Expression<? extends Comparable> getComparablePath(Root<?> root, Part part) {
			return getTypedPath(root, part);
		}

		private <T> Expression<T> getTypedPath(Root<?> root, Part part) {
			return toExpressionRecursively(root, part.getProperty());
		}

		private <T> Expression<T> traversePath(Path<?> root, PropertyPath path) {

			Path<Object> result = root.get(path.getSegment());
			return (Expression<T>) (path.hasNext() ? traversePath(result, path.next()) : result);
		}
	}
}
