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

import static java.util.regex.Pattern.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Simple utility class to create JPA queries.
 * 
 * @author Oliver Gierke
 */
public abstract class QueryUtils {

	public static final String COUNT_QUERY_STRING = "select count(%s) from %s x";
	public static final String EXISTS_QUERY_STRING = "select count(%s) from %s x where x.%s = :id";

	public static final String DELETE_ALL_QUERY_STRING = "delete from %s x";
	public static final String READ_ALL_QUERY = "select x from %s x";
	private static final String DEFAULT_ALIAS = "x";
	private static final String COUNT_REPLACEMENT = "select count($3$5) $4$5$6";

	private static final Pattern ALIAS_MATCH;
	private static final Pattern COUNT_MATCH;

	private static final String IDENTIFIER = "[\\p{Alnum}._$]+";
	private static final String IDENTIFIER_GROUP = String.format("(%s)", IDENTIFIER);

	private static final String LEFT_JOIN = "left (outer )?join " + IDENTIFIER + " (as )?" + IDENTIFIER_GROUP;
	private static final Pattern LEFT_JOIN_PATTERN = Pattern.compile(LEFT_JOIN, Pattern.CASE_INSENSITIVE);

	static {

		StringBuilder builder = new StringBuilder();
		builder.append("(?<=from)"); // from as starting delimiter
		builder.append("(?: )+"); // at least one space separating
		builder.append(IDENTIFIER_GROUP); // Entity name, can be qualified (any
		builder.append("(?: as)*"); // exclude possible "as" keyword
		builder.append("(?: )+"); // at least one space separating
		builder.append("(\\w*)"); // the actual alias

		ALIAS_MATCH = compile(builder.toString(), CASE_INSENSITIVE);

		builder = new StringBuilder();
		builder.append("(select\\s+((distinct )?.+?)\\s+)?(from\\s+");
		builder.append(IDENTIFIER);
		builder.append("(?:\\s+as)?\\s+)");
		builder.append(IDENTIFIER_GROUP);
		builder.append("(.*)");

		COUNT_MATCH = compile(builder.toString(), CASE_INSENSITIVE);
	}

	/**
	 * Private constructor to prevent instantiation.
	 */
	private QueryUtils() {

	}

	/**
	 * Returns the query string for the given class name.
	 * 
	 * @param template
	 * @param entityName
	 * @return
	 */
	public static String getQueryString(String template, String entityName) {

		Assert.hasText(entityName, "Entity name must not be null or empty!");

		return String.format(template, entityName);
	}

	/**
	 * Adds {@literal order by} clause to the JPQL query. Uses the {@link #DEFAULT_ALIAS} to bind the sorting property to.
	 * 
	 * @param query
	 * @param alias
	 * @param sort
	 * @return
	 */
	public static String applySorting(String query, Sort sort) {

		return applySorting(query, sort, DEFAULT_ALIAS);
	}

	/**
	 * Adds {@literal order by} clause to the JPQL query.
	 * 
	 * @param query
	 * @param sort
	 * @param alias
	 * @return
	 */
	public static String applySorting(String query, Sort sort, String alias) {

		Assert.hasText(query);

		if (null == sort || !sort.iterator().hasNext()) {
			return query;
		}

		StringBuilder builder = new StringBuilder(query);

		if (!query.contains("order by")) {
			builder.append(" order by ");
		} else {
			builder.append(", ");
		}

		Set<String> aliases = getOuterJoinAliases(query);

		for (Order order : sort) {
			builder.append(getOrderClause(aliases, alias, order));
		}

		builder.delete(builder.length() - 2, builder.length());

		return builder.toString();
	}

	/**
	 * Returns the order clause for the given {@link Order}. Will prefix the clause with the given alias if the referenced
	 * property refers to a join alias.
	 * 
	 * @param joinAliases the join aliases of the original query.
	 * @param alias the alias for the root entity.
	 * @param order the order object to build the clause for.
	 * @return
	 */
	private static String getOrderClause(Set<String> joinAliases, String alias, Order order) {

		String property = order.getProperty();
		boolean qualifyReference = true;

		for (String joinAlias : joinAliases) {
			if (property.startsWith(joinAlias)) {
				qualifyReference = false;
				break;
			}
		}

		return String.format("%s%s %s, ", qualifyReference ? alias + "." : "", property, toJpaDirection(order));
	}

	/**
	 * Returns the aliases used for {@code left (outer) join}s.
	 * 
	 * @param query
	 * @return
	 */
	static Set<String> getOuterJoinAliases(String query) {

		Set<String> result = new HashSet<String>();
		Matcher matcher = LEFT_JOIN_PATTERN.matcher(query);

		while (matcher.find()) {

			String alias = matcher.group(3);
			if (StringUtils.hasText(alias)) {
				result.add(alias);
			}
		}

		return result;
	}

	private static String toJpaDirection(Order order) {
		return order.getDirection().name().toLowerCase(Locale.US);
	}

	/**
	 * Resolves the alias for the entity to be retrieved from the given JPA query.
	 * 
	 * @param query
	 * @return
	 */
	public static String detectAlias(String query) {

		Matcher matcher = ALIAS_MATCH.matcher(query);

		return matcher.find() ? matcher.group(2) : null;
	}

	/**
	 * Creates a where-clause referencing the given entities and appends it to the given query string. Binds the given
	 * entities to the query.
	 * 
	 * @param <T>
	 * @param queryString
	 * @param entities
	 * @param entityManager
	 * @return
	 */
	public static <T> Query applyAndBind(String queryString, Iterable<T> entities, EntityManager entityManager) {

		Assert.notNull(queryString);
		Assert.notNull(entities);
		Assert.notNull(entityManager);

		Iterator<T> iterator = entities.iterator();

		if (!iterator.hasNext()) {
			return entityManager.createQuery(queryString);
		}

		String alias = detectAlias(queryString);
		StringBuilder builder = new StringBuilder(queryString);
		builder.append(" where");

		int i = 0;

		while (iterator.hasNext()) {

			iterator.next();

			builder.append(String.format(" %s = ?%d", alias, ++i));

			if (iterator.hasNext()) {
				builder.append(" or");
			}
		}

		Query query = entityManager.createQuery(builder.toString());

		iterator = entities.iterator();
		i = 0;

		while (iterator.hasNext()) {
			query.setParameter(++i, iterator.next());
		}

		return query;
	}

	/**
	 * Creates a count projected query from the given orginal query.
	 * 
	 * @param originalQuery must not be {@literal null} or empty
	 * @return
	 */
	public static String createCountQueryFor(String originalQuery) {

		Assert.hasText(originalQuery);

		Matcher matcher = COUNT_MATCH.matcher(originalQuery);
		return matcher.replaceFirst(COUNT_REPLACEMENT);
	}

	/**
	 * Returns whether the given {@link Query} contains named parameters.
	 * 
	 * @param query
	 * @return
	 */
	public static boolean hasNamedParameter(Query query) {

		for (Parameter<?> parameter : query.getParameters()) {
			if (parameter.getName() != null) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Turns the given {@link Sort} into {@link javax.persistence.criteria.Order}s.
	 * 
	 * @param sort the {@link Sort} instance to be transformed into JPA {@link javax.persistence.criteria.Order}s.
	 * @param root must not be {@literal null}.
	 * @param cb must not be {@literal null}.
	 * @return
	 */
	public static List<javax.persistence.criteria.Order> toOrders(Sort sort, Root<?> root, CriteriaBuilder cb) {

		List<javax.persistence.criteria.Order> orders = new ArrayList<javax.persistence.criteria.Order>();

		if (sort == null) {
			return orders;
		}

		Assert.notNull(root);
		Assert.notNull(cb);

		for (org.springframework.data.domain.Sort.Order order : sort) {
			orders.add(toJpaOrder(order, root, cb));
		}

		return orders;
	}

	/**
	 * Creates a criteria API {@link javax.persistence.criteria.Order} from the given {@link Order}.
	 * 
	 * @param order the order to transform into a JPA {@link javax.persistence.criteria.Order}
	 * @param root the {@link Root} the {@link Order} expression is based on
	 * @param cb the {@link CriteriaBuilder} to build the {@link javax.persistence.criteria.Order} with
	 * @return
	 */
	private static javax.persistence.criteria.Order toJpaOrder(Order order, Root<?> root, CriteriaBuilder cb) {

		Expression<?> expression = toExpressionRecursively(root, PropertyPath.from(order.getProperty(), root.getJavaType()));
		return order.isAscending() ? cb.asc(expression) : cb.desc(expression);
	}

	@SuppressWarnings("unchecked")
	static <T> Expression<T> toExpressionRecursively(From<?, ?> from, PropertyPath property) {

		if (property.isCollection()) {
			Join<Object, Object> join = from.join(property.getSegment());
			return (Expression<T>) (property.hasNext() ? toExpressionRecursively((From<?, ?>) join, property.next()) : join);
		} else {
			Path<Object> path = from.get(property.getSegment());
			return (Expression<T>) (property.hasNext() ? toExpressionRecursively(path, property.next()) : path);
		}
	}

	static Expression<Object> toExpressionRecursively(Path<Object> path, PropertyPath property) {

		Path<Object> result = path.get(property.getSegment());
		return property.hasNext() ? toExpressionRecursively(result, property.next()) : result;
	}
}
