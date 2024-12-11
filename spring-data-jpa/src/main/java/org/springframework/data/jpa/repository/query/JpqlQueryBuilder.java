/*
 * Copyright 2024 the original author or authors.
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

import static org.springframework.data.jpa.repository.query.QueryTokens.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.util.Predicates;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * A Domain-Specific Language to build JPQL queries using Java code.
 *
 * @author Mark Paluch
 */
@SuppressWarnings("JavadocDeclaration")
public final class JpqlQueryBuilder {

	private JpqlQueryBuilder() {}

	/**
	 * Create an {@link Entity} from the given {@link Class entity class}.
	 *
	 * @param from the entity type to select from.
	 * @return
	 */
	public static Entity entity(Class<?> from) {
		return new Entity(from.getName(), from.getSimpleName(),
				getAlias(from.getSimpleName(), Predicates.isTrue(), () -> "r"));
	}

	/**
	 * Create a {@link Join INNER JOIN}.
	 *
	 * @param origin the selection origin (a join or the entity itself) to select from.
	 * @param path
	 * @return
	 */
	public static Join innerJoin(Origin origin, String path) {
		return new Join(origin, "INNER JOIN", path);
	}

	/**
	 * Create a {@link Join LEFT JOIN}.
	 *
	 * @param origin the selection origin (a join or the entity itself) to select from.
	 * @param path
	 * @return
	 */
	public static Join leftJoin(Origin origin, String path) {
		return new Join(origin, "LEFT JOIN", path);
	}

	/**
	 * Start building a {@link Select} statement by selecting {@link Class from}. This is a short form for
	 * {@code selectFrom(entity(from))}.
	 *
	 * @param from the entity type to select from.
	 * @return
	 */
	public static SelectStep selectFrom(Class<?> from) {
		return selectFrom(entity(from));
	}

	/**
	 * Start building a {@link Select} statement by selecting {@link Entity from}.
	 *
	 * @param from the entity source to select from.
	 * @return a new select builder.
	 */
	public static SelectStep selectFrom(Entity from) {

		return new SelectStep() {

			boolean distinct = false;

			@Override
			public SelectStep distinct() {

				distinct = true;
				return this;
			}

			@Override
			public Select entity() {
				return new Select(postProcess(new EntitySelection(from)), from);
			}

			@Override
			public Select count() {
				return new Select(new CountSelection(from, distinct), from);
			}

			@Override
			public Select instantiate(String resultType, Collection<JpqlQueryBuilder.PathExpression> paths) {
				return new Select(postProcess(new ConstructorExpression(resultType, new Multiselect(from, paths))), from);
			}

			@Override
			public Select select(Collection<JpqlQueryBuilder.PathExpression> paths) {
				return new Select(postProcess(new Multiselect(from, paths)), from);
			}

			Selection postProcess(Selection selection) {
				return distinct ? new DistinctSelection(selection) : selection;
			}
		};
	}

	private static String getAlias(String from, java.util.function.Predicate<String> predicate,
			Supplier<String> fallback) {

		char c = from.toLowerCase(Locale.ROOT).charAt(0);
		String string = Character.toString(c);
		if (Character.isJavaIdentifierPart(c) && predicate.test(string)) {
			return string;
		}

		return fallback.get();
	}

	/**
	 * Invoke a {@literal function} with the given {@code arguments}.
	 *
	 * @param function function name.
	 * @param arguments function arguments.
	 * @return an expression representing a function call.
	 */
	public static Expression function(String function, Expression... arguments) {
		return new FunctionExpression(function, Arrays.asList(arguments));
	}

	/**
	 * Nest the given {@link Predicate}.
	 *
	 * @param predicate
	 * @return
	 */
	public static Predicate nested(Predicate predicate) {
		return new NestedPredicate(predicate);
	}

	/**
	 * Create a qualified expression for a {@link PropertyPath}.
	 *
	 * @param source
	 * @param path
	 * @return
	 */
	public static Expression expression(Origin source, PropertyPath path) {
		return new PathAndOrigin(path, source, false);
	}

	/**
	 * Create a simple expression from a string as-is.
	 *
	 * @param expression
	 * @return
	 */
	public static Expression expression(String expression) {

		Assert.hasText(expression, "Expression must not be empty or null");

		return new LiteralExpression(expression);
	}

	/**
	 * Create a simple numeric literal.
	 *
	 * @param literal
	 * @return
	 */
	public static Expression literal(Number literal) {
		return new LiteralExpression(literal.toString());
	}

	/**
	 * Create a simple literal from a string by quoting it.
	 *
	 * @param literal
	 * @return
	 */
	public static Expression literal(String literal) {
		return new StringLiteralExpression(literal);
	}

	/**
	 * A parameter placeholder.
	 *
	 * @param parameter
	 * @return
	 */
	public static Expression parameter(String parameter) {

		Assert.hasText(parameter, "Parameter must not be empty or null");

		return new ParameterExpression(new ParameterPlaceholder(parameter));
	}

	/**
	 * A parameter placeholder.
	 *
	 * @param placeholder the placeholder to use.
	 * @return
	 */
	public static Expression parameter(ParameterPlaceholder placeholder) {
		return new ParameterExpression(placeholder);
	}

	/**
	 * Create a new ordering expression.
	 *
	 * @param sortExpression
	 * @param order
	 * @return
	 */
	public static Expression orderBy(Expression sortExpression, Sort.Order order) {
		return new OrderExpression(sortExpression, order);
	}

	/**
	 * Start building a {@link Predicate WHERE predicate} by providing the right-hand side.
	 *
	 * @param source
	 * @param path
	 * @return
	 */
	public static WhereStep where(Origin source, PropertyPath path) {
		return where(expression(source, path));
	}

	/**
	 * Start building a {@link Predicate WHERE predicate} by providing the right-hand side.
	 *
	 * @param rhs
	 * @return
	 */
	public static WhereStep where(Expression rhs) {

		return new WhereStep() {
			@Override
			public Predicate between(Expression lower, Expression upper) {
				return new BetweenPredicate(rhs, lower, upper);
			}

			@Override
			public Predicate gt(Expression value) {
				return new OperatorPredicate(rhs, ">", value);
			}

			@Override
			public Predicate gte(Expression value) {
				return new OperatorPredicate(rhs, ">=", value);
			}

			@Override
			public Predicate lt(Expression value) {
				return new OperatorPredicate(rhs, "<", value);
			}

			@Override
			public Predicate lte(Expression value) {
				return new OperatorPredicate(rhs, "<=", value);
			}

			@Override
			public Predicate isNull() {
				return new LhsPredicate(rhs, "IS NULL");
			}

			@Override
			public Predicate isNotNull() {
				return new LhsPredicate(rhs, "IS NOT NULL");
			}

			@Override
			public Predicate isTrue() {
				return new LhsPredicate(rhs, "= TRUE");
			}

			@Override
			public Predicate isFalse() {
				return new LhsPredicate(rhs, "= FALSE");
			}

			@Override
			public Predicate isEmpty() {
				return new LhsPredicate(rhs, "IS EMPTY");
			}

			@Override
			public Predicate isNotEmpty() {
				return new LhsPredicate(rhs, "IS NOT EMPTY");
			}

			@Override
			public Predicate in(Expression value) {
				return new InPredicate(rhs, "IN", value);
			}

			@Override
			public Predicate notIn(Expression value) {
				return new InPredicate(rhs, "NOT IN", value);
			}

			@Override
			public Predicate memberOf(Expression value) {
				return new MemberOfPredicate(rhs, "MEMBER OF", value);
			}

			@Override
			public Predicate notMemberOf(Expression value) {
				return new MemberOfPredicate(rhs, "NOT MEMBER OF", value);
			}

			@Override
			public Predicate like(Expression value, String escape) {
				return new LikePredicate(rhs, "LIKE", value, escape);
			}

			@Override
			public Predicate notLike(Expression value, String escape) {
				return new LikePredicate(rhs, "NOT LIKE", value, escape);
			}

			@Override
			public Predicate eq(Expression value) {
				return new OperatorPredicate(rhs, "=", value);
			}

			@Override
			public Predicate neq(Expression value) {
				return new OperatorPredicate(rhs, "!=", value);
			}
		};
	}

	@Nullable
	public static Predicate and(List<Predicate> intermediate) {

		Predicate predicate = null;

		for (Predicate other : intermediate) {

			if (predicate == null) {
				predicate = other;
			} else {
				predicate = predicate.and(other);
			}
		}

		return predicate;
	}

	@Nullable
	public static Predicate or(List<Predicate> intermediate) {

		Predicate predicate = null;

		for (Predicate other : intermediate) {

			if (predicate == null) {
				predicate = other;
			} else {
				predicate = predicate.or(other);
			}
		}

		return predicate;
	}

	/**
	 * Fluent interface to build a {@link Select}.
	 */
	public interface SelectStep {

		/**
		 * Apply {@code DISTINCT}.
		 */
		SelectStep distinct();

		/**
		 * Select the entity.
		 */
		Select entity();

		/**
		 * Select the count.
		 */
		Select count();

		/**
		 * Provide a constructor expression to instantiate {@code resultType}. Operates on the underlying {@link Entity
		 * FROM}.
		 *
		 * @param resultType
		 * @param paths
		 * @return
		 */
		default Select instantiate(Class<?> resultType, Collection<JpqlQueryBuilder.PathExpression> paths) {
			return instantiate(resultType.getName(), paths);
		}

		/**
		 * Provide a constructor expression to instantiate {@code resultType}.
		 *
		 * @param resultType
		 * @param paths
		 * @return
		 */
		Select instantiate(String resultType, Collection<JpqlQueryBuilder.PathExpression> paths);

		/**
		 * Specify a multi-select.
		 *
		 * @param paths
		 * @return
		 */
		Select select(Collection<JpqlQueryBuilder.PathExpression> paths);

		/**
		 * Select a single attribute.
		 *
		 * @param path
		 * @return
		 */
		default Select select(JpqlQueryBuilder.PathExpression path) {
			return select(List.of(path));
		}

	}

	interface Selection {
		String render(RenderContext context);
	}

	/**
	 * {@code DISTINCT} wrapper.
	 *
	 * @param selection
	 */
	record DistinctSelection(Selection selection) implements Selection {

		@Override
		public String render(RenderContext context) {
			return "DISTINCT %s".formatted(selection.render(context));
		}

		@Override
		public String toString() {
			return render(RenderContext.EMPTY);
		}
	}

	static PathAndOrigin path(Origin origin, String path) {

		if (origin instanceof Entity entity) {

			try {
				PropertyPath from = PropertyPath.from(path, ClassUtils.forName(entity.entity, Entity.class.getClassLoader()));
				return new PathAndOrigin(from, entity, false);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		if (origin instanceof Join join) {

			Origin parent = join.source;
			List<String> segments = new ArrayList<>();
			segments.add(join.path);
			while (!(parent instanceof Entity)) {
				if (parent instanceof Join pj) {
					parent = pj.source;
					segments.add(pj.path);
				} else {
					parent = null;
				}
			}

			if (parent instanceof Entity) {
				Collections.reverse(segments);
				segments.add(path);
				PathAndOrigin path1 = path(parent, StringUtils.collectionToDelimitedString(segments, "."));
				return new PathAndOrigin(path1.path().getLeafProperty(), origin, false);
			}
		}
		throw new IllegalStateException(" oh no ");

	}

	/**
	 * Entity selection.
	 *
	 * @param source
	 */
	record EntitySelection(Entity source) implements Selection {

		@Override
		public String render(RenderContext context) {
			return context.getAlias(source);
		}

		@Override
		public String toString() {
			return render(RenderContext.EMPTY);
		}
	}

	/**
	 * {@code COUNT(…)} selection.
	 *
	 * @param source
	 * @param distinct
	 */
	record CountSelection(Entity source, boolean distinct) implements Selection {

		@Override
		public String render(RenderContext context) {
			return "COUNT(%s%s)".formatted(distinct ? "DISTINCT " : "", context.getAlias(source));
		}

		@Override
		public String toString() {
			return render(RenderContext.EMPTY);
		}
	}

	/**
	 * Expression selection.
	 *
	 * @param resultType
	 * @param multiselect
	 */
	record ConstructorExpression(String resultType, Multiselect multiselect) implements Selection {

		@Override
		public String render(RenderContext context) {

			return "new %s(%s)".formatted(resultType, multiselect.render(new ConstructorContext(context)));
		}

		@Override
		public String toString() {
			return render(RenderContext.EMPTY);
		}
	}

	/**
	 * Multi-select selecting one or many property paths.
	 *
	 * @param source
	 * @param paths
	 */
	record Multiselect(Origin source, Collection<JpqlQueryBuilder.PathExpression> paths) implements Selection {

		@Override
		public String render(RenderContext context) {

			StringBuilder builder = new StringBuilder();

			for (PathExpression path : paths) {

				if (!builder.isEmpty()) {
					builder.append(", ");
				}

				builder.append(path.render(context));
				if (!context.isConstructorContext()) {
					builder.append(" ").append(path.getPropertyPath().getSegment());
				}
			}

			return builder.toString();
		}

		@Override
		public String toString() {
			return render(RenderContext.EMPTY);
		}
	}

	/**
	 * {@code WHERE} predicate.
	 */
	public interface Predicate {

		/**
		 * Render the predicate given {@link RenderContext}.
		 *
		 * @param context
		 * @return
		 */
		String render(RenderContext context);

		/**
		 * {@code OR}-concatenate this predicate with {@code other}.
		 *
		 * @param other
		 * @return a composed predicate combining this and {@code other} using the OR operator.
		 */
		default Predicate or(Predicate other) {
			return new OrPredicate(this, other);
		}

		/**
		 * {@code AND}-concatenate this predicate with {@code other}.
		 *
		 * @param other
		 * @return a composed predicate combining this and {@code other} using the AND operator.
		 */
		default Predicate and(Predicate other) { // don't like the structuring of this and the nest() thing
			return new AndPredicate(this, other);
		}

		/**
		 * Wrap this predicate with parenthesis {@code (…)} to nest it without affecting AND/OR concatenation precedence.
		 *
		 * @return a nested variant of this predicate.
		 */
		default Predicate nest() {
			return new NestedPredicate(this);
		}
	}

	/**
	 * Interface specifying an expression that can be rendered to {@code String}.
	 */
	public interface Expression {

		/**
		 * Render the expression given {@link RenderContext}.
		 *
		 * @param context
		 * @return
		 */
		String render(RenderContext context);
	}

	/**
	 * Extension to {@link Expression} that contains a {@link PropertyPath}. Typically used to represent a selection
	 * expression or an expression used within sorting or {@code WHERE} clauses.
	 */
	public interface PathExpression extends Expression {

		/**
		 * @return the associated {@link PropertyPath}.
		 */
		PropertyPath getPropertyPath();
	}

	/**
	 * {@code SELECT} statement.
	 */
	public static class Select extends AbstractJpqlQuery {

		private final Selection selection;

		private final Entity entity;

		private final Map<String, Join> joins = new LinkedHashMap<>();

		private final List<Expression> orderBy = new ArrayList<>();

		private Select(Selection selection, Entity entity) {
			this.selection = selection;
			this.entity = entity;
		}

		/**
		 * Append a join to this select.
		 *
		 * @param join
		 * @return
		 */
		public Select join(Join join) {

			if (join.source() instanceof Join parent) {
				join(parent);
			}

			this.joins.put(join.joinType() + "_" + join.getName() + "_" + join.path(), join);
			return this;
		}

		/**
		 * Append an order-by expression to this select.
		 *
		 * @param orderBy
		 * @return
		 */
		public Select orderBy(Expression orderBy) {
			this.orderBy.add(orderBy);
			return this;
		}

		@Override
		String render() {

			Map<Origin, String> aliases = new LinkedHashMap<>();
			aliases.put(entity, entity.alias);

			RenderContext renderContext = new RenderContext(aliases);

			StringBuilder where = new StringBuilder();
			StringBuilder orderby = new StringBuilder();
			StringBuilder result = new StringBuilder(
					"SELECT %s FROM %s %s".formatted(selection.render(renderContext), entity.getEntity(), entity.getAlias()));

			if (getWhere() != null) {
				where.append(" WHERE ").append(getWhere().render(renderContext));
			}

			if (!orderBy.isEmpty()) {

				StringBuilder builder = new StringBuilder();

				for (Expression order : orderBy) {
					if (!builder.isEmpty()) {
						builder.append(", ");
					}

					builder.append(order.render(renderContext));
				}

				orderby.append(" ORDER BY ").append(builder);
			}

			aliases.keySet().forEach(key -> {

				if (key instanceof Join js) {
					join(js);
				}
			});

			for (Join join : joins.values()) {
				result.append(" ").append(join.joinType()).append(" ").append(renderContext.getAlias(join.source())).append(".")
						.append(join.path()).append(" ").append(renderContext.getAlias(join));
			}

			result.append(where).append(orderby);

			return result.toString();
		}
	}

	/**
	 * Abstract base class for JPQL queries.
	 */
	public static abstract class AbstractJpqlQuery {

		private @Nullable Predicate where;

		public AbstractJpqlQuery where(Predicate predicate) {
			this.where = predicate;
			return this;
		}

		@Nullable
		public Predicate getWhere() {
			return where;
		}

		abstract String render();

		@Override
		public String toString() {
			return render();
		}
	}

	record OrderExpression(Expression sortExpression, Sort.Order order) implements Expression {

		@Override
		public String render(RenderContext context) {

			StringBuilder builder = new StringBuilder();

			builder.append(sortExpression.render(context));
			builder.append(" ");

			builder.append(order.isDescending() ? TOKEN_DESC : TOKEN_ASC);

			if (order.getNullHandling() == Sort.NullHandling.NULLS_FIRST) {
				builder.append(" NULLS FIRST");
			} else if (order.getNullHandling() == Sort.NullHandling.NULLS_LAST) {
				builder.append(" NULLS LAST");
			}

			return builder.toString();
		}
	}

	/**
	 * Context used during rendering.
	 */
	public static class RenderContext {

		public static final RenderContext EMPTY = new RenderContext(Collections.emptyMap()) {

			@Override
			public String getAlias(Origin source) {
				return "";
			}
		};

		private final Map<Origin, String> aliases;
		private int counter;

		RenderContext(Map<Origin, String> aliases) {
			this.aliases = aliases;
		}

		/**
		 * Obtain an alias for {@link Origin}. Unknown selection origins are associated with the enclosing statement if they
		 * are used for the first time.
		 *
		 * @param source
		 * @return
		 */
		public String getAlias(Origin source) {

			return aliases.computeIfAbsent(source, it -> JpqlQueryBuilder.getAlias(source.getName(), s -> {
				return !aliases.containsValue(s);
			}, () -> "join_" + (counter++)));
		}

		/**
		 * Prefix {@code fragment} with the alias for {@link Origin}. Unknown selection origins are associated with the
		 * enclosing statement if they are used for the first time.
		 *
		 * @param source
		 * @return
		 */
		public String prefixWithAlias(Origin source, String fragment) {

			String alias = getAlias(source);
			return ObjectUtils.isEmpty(source) ? fragment : alias + "." + fragment;
		}

		public boolean isConstructorContext() {
			return false;
		}
	}

	static class ConstructorContext extends RenderContext {

		ConstructorContext(RenderContext rootContext) {
			super(rootContext.aliases);
		}

		@Override
		public boolean isConstructorContext() {
			return true;
		}
	}

	/**
	 * An origin that is used to select data from. selection origins are used with paths to define where a path is
	 * anchored.
	 */
	public interface Origin {

		/**
		 * Returns the simple name of the origin (e.g. {@link Class#getSimpleName()} or JOIN path name).
		 *
		 * @return the simple name of the origin (e.g. {@link Class#getSimpleName()})
		 */
		String getName();
	}

	/**
	 * An origin that is used to select data from. selection origins are used with paths to define where a path is
	 * anchored.
	 */
	public interface Bindable {

		boolean isRoot();
	}

	/**
	 * The root entity.
	 */
	public static final class Entity implements Origin {

		private final String entity;
		private final String simpleName;
		private final String alias;

		/**
		 * @param entity fully-qualified entity name.
		 * @param simpleName simple class name.
		 * @param alias alias to use.
		 */
		Entity(String entity, String simpleName, String alias) {
			this.entity = entity;
			this.simpleName = simpleName;
			this.alias = alias;
		}

		public String getEntity() {
			return entity;
		}

		@Override
		public String getName() {
			return simpleName;
		}

		public String getAlias() {
			return alias;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj == null || obj.getClass() != this.getClass()) {
				return false;
			}
			var that = (Entity) obj;
			return Objects.equals(this.entity, that.entity) && Objects.equals(this.simpleName, that.simpleName)
					&& Objects.equals(this.alias, that.alias);
		}

		@Override
		public int hashCode() {
			return Objects.hash(entity, simpleName, alias);
		}

		@Override
		public String toString() {
			return "Entity[" + "entity=" + entity + ", " + "simpleName=" + simpleName + ", " + "alias=" + alias + ']';
		}

	}

	/**
	 * A joined entity or element collection.
	 */
	public static final class Join implements Origin, Expression {

		private final Origin source;
		private final String joinType;
		private final String path;

		/**
		 * @param source
		 * @param joinType
		 * @param path
		 */
		Join(Origin source, String joinType, String path) {
			this.source = source;
			this.joinType = joinType;
			this.path = path;
		}

		@Override
		public String getName() {
			return path;
		}

		@Override
		public String render(RenderContext context) {
			return "%s %s %s".formatted(joinType, context.getAlias(source), path);
		}

		public Origin source() {
			return source;
		}

		public String joinType() {
			return joinType;
		}

		public String path() {
			return path;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj == null || obj.getClass() != this.getClass()) {
				return false;
			}
			var that = (Join) obj;
			return Objects.equals(this.source, that.source) && Objects.equals(this.joinType, that.joinType)
					&& Objects.equals(this.path, that.path);
		}

		@Override
		public int hashCode() {
			return Objects.hash(source, joinType, path);
		}

		@Override
		public String toString() {
			return "Join[" + "source=" + source + ", " + "joinType=" + joinType + ", " + "path=" + path + ']';
		}

	}

	/**
	 * Fluent interface to build a {@link Predicate}.
	 */
	public interface WhereStep {

		/**
		 * Create a {@code BETWEEN … AND …} predicate.
		 *
		 * @param lower lower boundary.
		 * @param upper upper boundary.
		 * @return
		 */
		Predicate between(Expression lower, Expression upper);

		/**
		 * Create a greater {@code > …} predicate.
		 *
		 * @param value the comparison value.
		 * @return
		 */
		Predicate gt(Expression value);

		/**
		 * Create a greater-or-equals {@code >= …} predicate.
		 *
		 * @param value the comparison value.
		 * @return
		 */
		Predicate gte(Expression value);

		/**
		 * Create a less {@code < …} predicate.
		 *
		 * @param value the comparison value.
		 * @return
		 */
		Predicate lt(Expression value);

		/**
		 * Create a less-or-equals {@code <= …} predicate.
		 *
		 * @param value the comparison value.
		 * @return
		 */
		Predicate lte(Expression value);

		/**
		 * Create a {@code IS NULL} predicate.
		 *
		 * @return
		 */
		Predicate isNull();

		/**
		 * Create a {@code IS NOT NULL} predicate.
		 *
		 * @return
		 */
		Predicate isNotNull();

		/**
		 * Create a {@code IS TRUE} predicate.
		 *
		 * @return
		 */
		Predicate isTrue();

		/**
		 * Create a {@code IS FALSE} predicate.
		 *
		 * @return
		 */
		Predicate isFalse();

		/**
		 * Create a {@code IS EMPTY} predicate.
		 *
		 * @return
		 */
		Predicate isEmpty();

		/**
		 * Create a {@code IS NOT EMPTY} predicate.
		 *
		 * @return
		 */
		Predicate isNotEmpty();

		/**
		 * Create a {@code IN} predicate.
		 *
		 * @param value
		 * @return
		 */
		Predicate in(Expression value);

		/**
		 * Create a {@code NOT IN} predicate.
		 *
		 * @param value
		 * @return
		 */
		Predicate notIn(Expression value);

		/**
		 * Create a {@code MEMBER OF &lt;collection&gt;} predicate.
		 *
		 * @param value
		 * @return
		 */
		Predicate memberOf(Expression value);

		/**
		 * Create a {@code NOT MEMBER OF &lt;collection&gt;} predicate.
		 *
		 * @param value
		 * @return
		 */
		Predicate notMemberOf(Expression value);

		default Predicate like(String value, String escape) {
			return like(expression(value), escape);
		}

		/**
		 * Create a {@code LIKE … ESCAPE} predicate.
		 *
		 * @param value
		 * @return
		 */
		Predicate like(Expression value, String escape);

		/**
		 * Create a {@code NOT LIKE … ESCAPE} predicate.
		 *
		 * @param value
		 * @return
		 */
		Predicate notLike(Expression value, String escape);

		/**
		 * Create a {@code =} (equals) predicate.
		 *
		 * @param value
		 * @return
		 */
		Predicate eq(Expression value);

		/**
		 * Create a {@code &lt;&gt;} (not equals) predicate.
		 *
		 * @param value
		 * @return
		 */
		Predicate neq(Expression value);
	}

	record LiteralExpression(String expression) implements Expression {

		@Override
		public String render(RenderContext context) {
			return expression;
		}

		@Override
		public String toString() {
			return render(RenderContext.EMPTY);
		}
	}

	record StringLiteralExpression(String literal) implements Expression {

		@Override
		public String render(RenderContext context) {
			return "'%s'".formatted(literal.replaceAll("'", "''"));
		}

		public String raw() {
			return literal;
		}

		@Override
		public String toString() {
			return render(RenderContext.EMPTY);
		}
	}

	record ParameterExpression(ParameterPlaceholder parameter) implements Expression {

		@Override
		public String render(RenderContext context) {
			return parameter.placeholder;
		}

		@Override
		public String toString() {
			return render(RenderContext.EMPTY);
		}
	}

	record FunctionExpression(String function, List<Expression> arguments) implements Expression {

		@Override
		public String render(RenderContext context) {

			StringBuilder builder = new StringBuilder();

			for (Expression argument : arguments) {

				if (!builder.isEmpty()) {
					builder.append(", ");
				}

				builder.append(argument.render(context));
			}

			return "%s(%s)".formatted(function, builder);
		}

		@Override
		public String toString() {
			return render(RenderContext.EMPTY);
		}
	}

	record OperatorPredicate(Expression path, String operator, Expression predicate) implements Predicate {

		@Override
		public String render(RenderContext context) {
			return "%s %s %s".formatted(path.render(context), operator, predicate.render(context));
		}

		@Override
		public String toString() {
			return render(RenderContext.EMPTY);
		}
	}

	record MemberOfPredicate(Expression path, String operator, Expression predicate) implements Predicate {

		@Override
		public String render(RenderContext context) {
			return "%s %s %s".formatted(predicate.render(context), operator, path.render(context));
		}

		@Override
		public String toString() {
			return render(RenderContext.EMPTY);
		}
	}

	record LhsPredicate(Expression path, String predicate) implements Predicate {

		@Override
		public String render(RenderContext context) {
			return "%s %s".formatted(path.render(context), predicate);
		}

		@Override
		public String toString() {
			return render(RenderContext.EMPTY);
		}
	}

	record BetweenPredicate(Expression path, Expression lower, Expression upper) implements Predicate {

		@Override
		public String render(RenderContext context) {
			return "%s BETWEEN %s AND %s".formatted(path.render(context), lower.render(context), upper.render(context));
		}

		@Override
		public String toString() {
			return render(RenderContext.EMPTY);
		}
	}

	record LikePredicate(Expression left, String operator, Expression right, String escape) implements Predicate {

		@Override
		public String render(RenderContext context) {
			return "%s %s %s ESCAPE '%s'".formatted(left.render(context), operator, right.render(context), escape);
		}

		@Override
		public String toString() {
			return render(RenderContext.EMPTY);
		}
	}

	record InPredicate(Expression path, String operator, Expression predicate) implements Predicate {

		@Override
		public String render(RenderContext context) {

			// TODO: should we rather wrap it with nested or check if its a nested predicate before we call render
			return "%s %s (%s)".formatted(path.render(context), operator, predicate.render(context));
		}

		@Override
		public String toString() {
			return render(RenderContext.EMPTY);
		}
	}

	record AndPredicate(Predicate left, Predicate right) implements Predicate {

		@Override
		public String render(RenderContext context) {
			return "%s AND %s".formatted(left.render(context), right.render(context));
		}

		@Override
		public String toString() {
			return render(RenderContext.EMPTY);
		}
	}

	record OrPredicate(Predicate left, Predicate right) implements Predicate {

		@Override
		public String render(RenderContext context) {
			return "%s OR %s".formatted(left.render(context), right.render(context));
		}

		@Override
		public String toString() {
			return render(RenderContext.EMPTY);
		}
	}

	record NestedPredicate(Predicate delegate) implements Predicate {

		@Override
		public String render(RenderContext context) {
			return "(%s)".formatted(delegate.render(context));
		}

		@Override
		public String toString() {
			return render(RenderContext.EMPTY);
		}
	}

	/**
	 * Value object capturing a property path and its origin.
	 *
	 * @param path
	 * @param origin
	 * @param onTheJoin whether the path should target the join itself instead of matching {@link PropertyPath}.
	 */
	record PathAndOrigin(PropertyPath path, Origin origin, boolean onTheJoin) implements PathExpression {

		@Override
		public PropertyPath getPropertyPath() {
			return path;
		}

		@Override
		public String render(RenderContext context) {

			if (path().hasNext() || !onTheJoin()) {
				return context.prefixWithAlias(origin(), path().toDotPath());
			} else {
				return context.getAlias(origin());
			}
		}
	}

	/**
	 * Value object capturing parameter placeholder.
	 *
	 * @param placeholder
	 */
	public record ParameterPlaceholder(String placeholder) {

		public ParameterPlaceholder {
			Assert.hasText(placeholder, "Placeholder must not be null nor empty");
		}

		/**
		 * Factory method to create a parameter placeholder using a parameter {@code index}.
		 *
		 * @param index the parameter index.
		 * @return an indexed parameter placeholder.
		 */
		public static ParameterPlaceholder indexed(int index) {
			return new ParameterPlaceholder("?%s".formatted(index));
		}

		/**
		 * Factory method to create a parameter placeholder using a parameter {@code name}.
		 *
		 * @param name the parameter name.
		 * @return a named parameter placeholder.
		 */
		public static ParameterPlaceholder named(String name) {

			Assert.hasText(name, "Placeholder name must not be empty");
			return new ParameterPlaceholder(":%s".formatted(name));
		}
	}
}
