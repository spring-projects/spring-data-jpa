/*
 * Copyright 2008-2024 the original author or authors.
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

import static jakarta.persistence.metamodel.Attribute.PersistentAttributeType.*;
import static java.util.regex.Pattern.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Parameter;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.Attribute.PersistentAttributeType;
import jakarta.persistence.metamodel.Bindable;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.JpaSort.JpaOrder;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.util.Streamable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Simple utility class to create JPA queries using the default implementation of a custom parser.
 *
 * @author Oliver Gierke
 * @author Kevin Raymond
 * @author Thomas Darimont
 * @author Komi Innocent
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Sébastien Péralta
 * @author Jens Schauder
 * @author Nils Borrmann
 * @author Reda.Housni-Alaoui
 * @author Florian Lüdiger
 * @author Grégoire Druant
 * @author Mohammad Hewedy
 * @author Andriy Redko
 * @author Peter Großmann
 * @author Greg Turnquist
 * @author Diego Krupitza
 * @author Jędrzej Biedrzycki
 * @author Darin Manica
 * @author Simon Paradies
 * @author Vladislav Yukharin
 * @author Chris Fraser
 * @author Donghun Shin
 * @author Pranav HS
 * @author Eduard Dudar
 * @author Yanming Zhou
 */
public abstract class QueryUtils {

	public static final String COUNT_QUERY_STRING = "select count(%s) from %s x";
	public static final String DELETE_ALL_QUERY_STRING = "delete from %s x";
	public static final String DELETE_ALL_QUERY_BY_ID_STRING = "delete from %s x where %s in :ids";

	// Used Regex/Unicode categories (see https://www.unicode.org/reports/tr18/#General_Category_Property):
	// Z Separator
	// Cc Control
	// Cf Format
	// Punct Punctuation
	private static final String IDENTIFIER = "[._$[\\P{Z}&&\\P{Cc}&&\\P{Cf}&&\\P{Punct}]]+";
	static final String COLON_NO_DOUBLE_COLON = "(?<![:\\\\]):";
	static final String IDENTIFIER_GROUP = String.format("(%s)", IDENTIFIER);

	private static final String COUNT_REPLACEMENT_TEMPLATE = "select count(%s) $5$6$7";
	private static final String SIMPLE_COUNT_VALUE = "$2";
	private static final String COMPLEX_COUNT_VALUE = "$3 $6";
	private static final String COMPLEX_COUNT_LAST_VALUE = "$6";
	private static final Pattern ORDER_BY_PART = Pattern.compile("(?iu)\\s+order\\s+by\\s+.*", CASE_INSENSITIVE | DOTALL);

	private static final Pattern ALIAS_MATCH;
	private static final Pattern COUNT_MATCH;
	private static final Pattern STARTS_WITH_PAREN = Pattern.compile("^\\s*\\(");
	private static final Pattern PARENS_TO_REMOVE = Pattern.compile("(\\(.*\\bfrom\\b[^)]+\\))",
			CASE_INSENSITIVE | DOTALL | MULTILINE);
	private static final Pattern PROJECTION_CLAUSE = Pattern.compile("select\\s+(?:distinct\\s+)?(.+)\\s+from",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern NO_DIGITS = Pattern.compile("\\D+");

	private static final String JOIN = "join\\s+(fetch\\s+)?" + IDENTIFIER + "\\s+(as\\s+)?" + IDENTIFIER_GROUP;
	private static final Pattern JOIN_PATTERN = Pattern.compile(JOIN, Pattern.CASE_INSENSITIVE);

	private static final String EQUALS_CONDITION_STRING = "%s.%s = :%s";
	private static final Pattern ORDER_BY = Pattern.compile("(order\\s+by\\s+)", CASE_INSENSITIVE);
	private static final Pattern ORDER_BY_IN_WINDOW_OR_SUBSELECT = Pattern
			.compile("\\([\\s\\S]*order\\s+by\\s[\\s\\S]*\\)", CASE_INSENSITIVE);

	private static final Pattern NAMED_PARAMETER = Pattern.compile(COLON_NO_DOUBLE_COLON + IDENTIFIER + "|#" + IDENTIFIER,
			CASE_INSENSITIVE);

	private static final Pattern CONSTRUCTOR_EXPRESSION;

	static final Map<PersistentAttributeType, Class<? extends Annotation>> ASSOCIATION_TYPES;

	private static final int QUERY_JOIN_ALIAS_GROUP_INDEX = 3;
	private static final int VARIABLE_NAME_GROUP_INDEX = 4;
	private static final int COMPLEX_COUNT_FIRST_INDEX = 3;

	private static final Pattern PUNCTATION_PATTERN = Pattern.compile(".*((?![._])[\\p{Punct}|\\s])");
	private static final Pattern FUNCTION_PATTERN;
	private static final Pattern FIELD_ALIAS_PATTERN;

	private static final String UNSAFE_PROPERTY_REFERENCE = "Sort expression '%s' must only contain property references or "
			+ "aliases used in the select clause; If you really want to use something other than that for sorting, please use "
			+ "JpaSort.unsafe(…)";

	static {

		StringBuilder builder = new StringBuilder();
		builder.append("(?<=\\bfrom)"); // from as starting delimiter
		builder.append("(?:\\s)+"); // at least one space separating
		builder.append(IDENTIFIER_GROUP); // Entity name, can be qualified (any
		builder.append("(?:\\sas)*"); // exclude possible "as" keyword
		builder.append("(?:\\s)+"); // at least one space separating
		builder.append("(?!(?:where|group\\s*by|order\\s*by))(\\w+)"); // the actual alias

		ALIAS_MATCH = compile(builder.toString(), CASE_INSENSITIVE);

		builder = new StringBuilder();
		builder.append("\\s*");
		builder.append("(select\\s+((distinct)?((?s).+?)?)\\s+)?(from\\s+");
		builder.append(IDENTIFIER);
		builder.append("(?:\\s+as)?\\s*)");
		builder.append(IDENTIFIER_GROUP);
		builder.append("(.*)");

		COUNT_MATCH = compile(builder.toString(), CASE_INSENSITIVE | DOTALL);

		Map<PersistentAttributeType, Class<? extends Annotation>> persistentAttributeTypes = new HashMap<>();
		persistentAttributeTypes.put(ONE_TO_ONE, OneToOne.class);
		persistentAttributeTypes.put(ONE_TO_MANY, null);
		persistentAttributeTypes.put(MANY_TO_ONE, ManyToOne.class);
		persistentAttributeTypes.put(MANY_TO_MANY, null);
		persistentAttributeTypes.put(ELEMENT_COLLECTION, null);

		ASSOCIATION_TYPES = Collections.unmodifiableMap(persistentAttributeTypes);

		builder = new StringBuilder();
		builder.append("select");
		builder.append("\\s+"); // at least one space separating
		builder.append("(.*\\s+)?"); // anything in between (e.g. distinct) at least one space separating
		builder.append("new");
		builder.append("\\s+"); // at least one space separating
		builder.append(IDENTIFIER);
		builder.append("\\s*"); // zero to unlimited space separating
		builder.append("\\(");
		builder.append(".*");
		builder.append("\\)");

		CONSTRUCTOR_EXPRESSION = compile(builder.toString(), CASE_INSENSITIVE + DOTALL);

		builder = new StringBuilder();
		// any function call including parameters within the brackets
		builder.append("\\w+\\s*\\([\\w\\.,\\s'=:;\\\\?]+\\)");
		// the potential alias
		builder.append("\\s+[as|AS]+\\s+(([\\w\\.]+))");

		FUNCTION_PATTERN = compile(builder.toString());

		builder = new StringBuilder();
		builder.append("\\s+"); // at least one space
		builder.append("[^\\s\\(\\)]+"); // No white char no bracket
		builder.append("\\s+[as|AS]+\\s+(([\\w\\.]+))"); // the potential alias

		FIELD_ALIAS_PATTERN = compile(builder.toString());

	}

	/**
	 * Private constructor to prevent instantiation.
	 */
	private QueryUtils() {

	}

	/**
	 * Returns the query string to execute an exists query for the given id attributes.
	 *
	 * @param entityName the name of the entity to create the query for, must not be {@literal null}.
	 * @param countQueryPlaceHolder the placeholder for the count clause, must not be {@literal null}.
	 * @param idAttributes the id attributes for the entity, must not be {@literal null}.
	 */
	public static String getExistsQueryString(String entityName, String countQueryPlaceHolder,
			Iterable<String> idAttributes) {

		String whereClause = Streamable.of(idAttributes).stream() //
				.map(idAttribute -> String.format(EQUALS_CONDITION_STRING, "x", idAttribute, idAttribute)) //
				.collect(Collectors.joining(" AND ", " WHERE ", ""));

		return String.format(COUNT_QUERY_STRING, countQueryPlaceHolder, entityName) + whereClause;
	}

	/**
	 * Returns the query string for the given class name.
	 *
	 * @param template must not be {@literal null}.
	 * @param entityName must not be {@literal null}.
	 * @return the template with placeholders replaced by the {@literal entityName}. Guaranteed to be not {@literal null}.
	 */
	public static String getQueryString(String template, String entityName) {

		Assert.hasText(entityName, "Entity name must not be null or empty");

		return String.format(template, entityName);
	}

	/**
	 * Adds {@literal order by} clause to the JPQL query. Uses the first alias to bind the sorting property to.
	 *
	 * @param query the query string to which sorting is applied
	 * @param sort the sort specification to apply.
	 * @return the modified query string.
	 */
	public static String applySorting(String query, Sort sort) {
		return applySorting(query, sort, detectAlias(query));
	}

	/**
	 * Adds {@literal order by} clause to the JPQL query.
	 *
	 * @param query the query string to which sorting is applied. Must not be {@literal null} or empty.
	 * @param sort the sort specification to apply.
	 * @param alias the alias to be used in the order by clause. May be {@literal null} or empty.
	 * @return the modified query string.
	 */
	public static String applySorting(String query, Sort sort, @Nullable String alias) {

		Assert.hasText(query, "Query must not be null or empty");

		if (sort.isUnsorted()) {
			return query;
		}

		StringBuilder builder = new StringBuilder(query);

		if (hasOrderByClause(query)) {
			builder.append(", ");
		} else {
			builder.append(" order by ");
		}

		Set<String> joinAliases = getOuterJoinAliases(query);
		Set<String> selectionAliases = getFunctionAliases(query);
		selectionAliases.addAll(getFieldAliases(query));

		String orderClauses = sort.stream()
			.map(order -> getOrderClause(joinAliases, selectionAliases, alias, order))
			.collect(Collectors.joining(", "));

		builder.append(orderClauses);

		return builder.toString();
	}

	/**
	 * Returns {@code true} if the query has {@code order by} clause. The query has {@code order by} clause if there is an
	 * {@code order by} which is not part of window clause.
	 *
	 * @param query the analysed query string
	 * @return {@code true} if the query has {@code order by} clause, {@code false} otherwise
	 */
	private static boolean hasOrderByClause(String query) {
		return countOccurrences(ORDER_BY, query) > countOccurrences(ORDER_BY_IN_WINDOW_OR_SUBSELECT, query);
	}

	/**
	 * Counts the number of occurrences of the pattern in the string
	 *
	 * @param pattern regex with a group to match
	 * @param string analysed string
	 * @return the number of occurrences of the pattern in the string
	 */
	private static int countOccurrences(Pattern pattern, String string) {

		Matcher matcher = pattern.matcher(string);

		int occurrences = 0;
		while (matcher.find()) {
			occurrences++;
		}
		return occurrences;
	}

	/**
	 * Returns the order clause for the given {@link Order}. Will prefix the clause with the given alias if the referenced
	 * property refers to a join alias, i.e. starts with {@code $alias.}.
	 *
	 * @param joinAliases the join aliases of the original query. Must not be {@literal null}.
	 * @param alias the alias for the root entity. May be {@literal null}.
	 * @param order the order object to build the clause for. Must not be {@literal null}.
	 * @return a String containing an order clause. Guaranteed to be not {@literal null}.
	 */
	private static String getOrderClause(Set<String> joinAliases, Set<String> selectionAlias, @Nullable String alias,
			Order order) {

		String property = order.getProperty();

		checkSortExpression(order);

		if (selectionAlias.contains(property)) {

			return String.format("%s %s", //
					order.isIgnoreCase() ? String.format("lower(%s)", property) : property, //
					toJpaDirection(order));
		}

		boolean qualifyReference = !property.contains("("); // ( indicates a function

		for (String joinAlias : joinAliases) {

			if (property.startsWith(joinAlias.concat("."))) {

				qualifyReference = false;
				break;
			}
		}

		String reference = qualifyReference && StringUtils.hasText(alias) ? String.format("%s.%s", alias, property)
				: property;
		String wrapped = order.isIgnoreCase() ? String.format("lower(%s)", reference) : reference;

		return String.format("%s %s", wrapped, toJpaDirection(order));
	}

	/**
	 * Returns the aliases used for {@code left (outer) join}s.
	 *
	 * @param query a query string to extract the aliases of joins from. Must not be {@literal null}.
	 * @return a {@literal Set} of aliases used in the query. Guaranteed to be not {@literal null}.
	 */
	static Set<String> getOuterJoinAliases(String query) {

		Set<String> result = new HashSet<>();
		Matcher matcher = JOIN_PATTERN.matcher(query);

		while (matcher.find()) {

			String alias = matcher.group(QUERY_JOIN_ALIAS_GROUP_INDEX);
			if (StringUtils.hasText(alias)) {
				result.add(alias);
			}
		}

		return result;
	}

	/**
	 * Returns the aliases used for fields in the query.
	 *
	 * @param query a {@literal String} containing a query. Must not be {@literal null}.
	 * @return a {@literal Set} containing all found aliases. Guaranteed to be not {@literal null}.
	 */
	private static Set<String> getFieldAliases(String query) {

		Set<String> result = new HashSet<>();
		Matcher matcher = FIELD_ALIAS_PATTERN.matcher(query);

		while (matcher.find()) {
			String alias = matcher.group(1);

			if (StringUtils.hasText(alias)) {
				result.add(alias);
			}
		}
		return result;
	}

	/**
	 * Returns the aliases used for aggregate functions like {@code SUM, COUNT, ...}.
	 *
	 * @param query a {@literal String} containing a query. Must not be {@literal null}.
	 * @return a {@literal Set} containing all found aliases. Guaranteed to be not {@literal null}.
	 */
	static Set<String> getFunctionAliases(String query) {

		Set<String> result = new HashSet<>();
		Matcher matcher = FUNCTION_PATTERN.matcher(query);

		while (matcher.find()) {

			String alias = matcher.group(1);

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
	 * @param query must not be {@literal null}.
	 * @return Might return {@literal null}.
	 * @deprecated use {@link DeclaredQuery#getAlias()} instead.
	 */
	@Nullable
	@Deprecated
	public static String detectAlias(String query) {

		String alias = null;
		Matcher matcher = ALIAS_MATCH.matcher(removeSubqueries(query));
		while (matcher.find()) {
			alias = matcher.group(2);
		}
		return alias;
	}

	/**
	 * Remove subqueries from the query, in order to identify the correct alias in order by clauses. If the entire query
	 * is surrounded by parenthesis, the outermost parenthesis are not removed.
	 *
	 * @param query
	 * @return query with all subqueries removed.
	 */
	static String removeSubqueries(String query) {

		if (!StringUtils.hasText(query)) {
			return query;
		}

		List<Integer> opens = new ArrayList<>();
		List<Integer> closes = new ArrayList<>();
		List<Boolean> closeMatches = new ArrayList<>();

		for (int i = 0; i < query.length(); i++) {

			char c = query.charAt(i);
			if (c == '(') {
				opens.add(i);
			} else if (c == ')') {
				closes.add(i);
				closeMatches.add(Boolean.FALSE);
			}
		}

		StringBuilder sb = new StringBuilder(query);
		boolean startsWithParen = STARTS_WITH_PAREN.matcher(query).find();
		for (int i = opens.size() - 1; i >= (startsWithParen ? 1 : 0); i--) {

			Integer open = opens.get(i);
			Integer close = findClose(open, closes, closeMatches) + 1;

			if (close > open) {

				String subquery = sb.substring(open, close);
				Matcher matcher = PARENS_TO_REMOVE.matcher(subquery);
				if (matcher.find()) {
					sb.replace(open, close, new String(new char[close - open]).replace('\0', ' '));
				}
			}
		}

		return sb.toString();
	}

	private static Integer findClose(final Integer open, final List<Integer> closes, final List<Boolean> closeMatches) {

		for (int i = 0; i < closes.size(); i++) {

			int close = closes.get(i);
			if (close > open && !closeMatches.get(i)) {
				closeMatches.set(i, Boolean.TRUE);
				return close;
			}
		}

		return -1;
	}

	/**
	 * Creates a where-clause referencing the given entities and appends it to the given query string. Binds the given
	 * entities to the query.
	 *
	 * @param <T> type of the entities.
	 * @param queryString must not be {@literal null}.
	 * @param entities must not be {@literal null}.
	 * @param entityManager must not be {@literal null}.
	 * @return Guaranteed to be not {@literal null}.
	 */

	public static <T> Query applyAndBind(String queryString, Iterable<T> entities, EntityManager entityManager) {

		Assert.notNull(queryString, "Querystring must not be null");
		Assert.notNull(entities, "Iterable of entities must not be null");
		Assert.notNull(entityManager, "EntityManager must not be null");

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
	 * Creates a count projected query from the given original query.
	 *
	 * @param originalQuery must not be {@literal null} or empty.
	 * @return Guaranteed to be not {@literal null}.
	 * @deprecated use {@link DeclaredQuery#deriveCountQuery(String)} instead.
	 */
	@Deprecated
	public static String createCountQueryFor(String originalQuery) {
		return createCountQueryFor(originalQuery, null);
	}

	/**
	 * Creates a count projected query from the given original query.
	 *
	 * @param originalQuery must not be {@literal null}.
	 * @param countProjection may be {@literal null}.
	 * @return a query String to be used a count query for pagination. Guaranteed to be not {@literal null}.
	 * @since 1.6
	 * @deprecated use {@link DeclaredQuery#deriveCountQuery(String)} instead.
	 */
	@Deprecated
	public static String createCountQueryFor(String originalQuery, @Nullable String countProjection) {
		return createCountQueryFor(originalQuery, countProjection, false);
	}

	/**
	 * Creates a count projected query from the given original query.
	 *
	 * @param originalQuery must not be {@literal null}.
	 * @param countProjection may be {@literal null}.
	 * @param nativeQuery whether the underlying query is a native query.
	 * @return a query String to be used a count query for pagination. Guaranteed to be not {@literal null}.
	 * @since 2.7.8
	 */
	static String createCountQueryFor(String originalQuery, @Nullable String countProjection, boolean nativeQuery) {

		Assert.hasText(originalQuery, "OriginalQuery must not be null or empty");

		Matcher matcher = COUNT_MATCH.matcher(originalQuery);
		String countQuery;

		if (countProjection == null) {

			String variable = matcher.matches() ? matcher.group(VARIABLE_NAME_GROUP_INDEX) : null;
			boolean useVariable = StringUtils.hasText(variable) //
					&& !variable.startsWith("new") // select [new com.example.User...
					&& !variable.startsWith(" new") // select distinct[ new com.example.User...
					&& !variable.startsWith("count(") // select [count(...
					&& !variable.contains(",");

			String complexCountValue = matcher.matches() && StringUtils.hasText(matcher.group(COMPLEX_COUNT_FIRST_INDEX))
					? COMPLEX_COUNT_VALUE
					: COMPLEX_COUNT_LAST_VALUE;

			String replacement = useVariable ? SIMPLE_COUNT_VALUE : complexCountValue;

			if (variable != null && (nativeQuery && (variable.contains(",") || "*".equals(variable)))) {
				replacement = "1";
			} else {

				String alias = QueryUtils.detectAlias(originalQuery);
				if ("*".equals(variable) && alias != null) {
					replacement = alias;
				}
			}

			countQuery = matcher.replaceFirst(String.format(COUNT_REPLACEMENT_TEMPLATE, replacement));
		} else {
			countQuery = matcher.replaceFirst(String.format(COUNT_REPLACEMENT_TEMPLATE, countProjection));
		}

		return ORDER_BY_PART.matcher(countQuery).replaceFirst("");
	}

	/**
	 * Returns whether the given {@link Query} contains named parameters.
	 *
	 * @param query Must not be {@literal null}.
	 * @return whether the given {@link Query} contains named parameters.
	 */
	public static boolean hasNamedParameter(Query query) {

		Assert.notNull(query, "Query must not be null");

		for (Parameter<?> parameter : query.getParameters()) {

			String name = parameter.getName();

			// Hibernate 3 specific hack as it returns the index as String for the name.
			if (name != null && NO_DIGITS.matcher(name).find()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns whether the given query contains named parameters.
	 *
	 * @param query can be {@literal null} or empty.
	 * @return whether the given query contains named parameters.
	 */
	@Deprecated
	static boolean hasNamedParameter(@Nullable String query) {
		return StringUtils.hasText(query) && NAMED_PARAMETER.matcher(query).find();
	}

	/**
	 * Turns the given {@link Sort} into {@link jakarta.persistence.criteria.Order}s.
	 *
	 * @param sort the {@link Sort} instance to be transformed into JPA {@link jakarta.persistence.criteria.Order}s.
	 * @param from must not be {@literal null}.
	 * @param cb must not be {@literal null}.
	 * @return a {@link List} of {@link jakarta.persistence.criteria.Order}s.
	 */
	public static List<jakarta.persistence.criteria.Order> toOrders(Sort sort, From<?, ?> from, CriteriaBuilder cb) {

		if (sort.isUnsorted()) {
			return Collections.emptyList();
		}

		Assert.notNull(from, "From must not be null");
		Assert.notNull(cb, "CriteriaBuilder must not be null");

		List<jakarta.persistence.criteria.Order> orders = new ArrayList<>();

		for (org.springframework.data.domain.Sort.Order order : sort) {
			orders.add(toJpaOrder(order, from, cb));
		}

		return orders;
	}

	/**
	 * Returns whether the given JPQL query contains a constructor expression.
	 *
	 * @param query must not be {@literal null} or empty.
	 * @return whether the given JPQL query contains a constructor expression.
	 * @since 1.10
	 */
	public static boolean hasConstructorExpression(String query) {

		Assert.hasText(query, "Query must not be null or empty");

		return CONSTRUCTOR_EXPRESSION.matcher(query).find();
	}

	/**
	 * Returns the projection part of the query, i.e. everything between {@code select} and {@code from}.
	 *
	 * @param query must not be {@literal null} or empty.
	 * @return the projection part of the query.
	 * @since 1.10.2
	 */
	public static String getProjection(String query) {

		Assert.hasText(query, "Query must not be null or empty");

		Matcher matcher = PROJECTION_CLAUSE.matcher(query);
		String projection = matcher.find() ? matcher.group(1) : "";
		return projection.trim();
	}

	/**
	 * Creates a criteria API {@link jakarta.persistence.criteria.Order} from the given {@link Order}.
	 *
	 * @param order the order to transform into a JPA {@link jakarta.persistence.criteria.Order}
	 * @param from the {@link From} the {@link Order} expression is based on
	 * @param cb the {@link CriteriaBuilder} to build the {@link jakarta.persistence.criteria.Order} with
	 * @return Guaranteed to be not {@literal null}.
	 */
	@SuppressWarnings("unchecked")
	private static jakarta.persistence.criteria.Order toJpaOrder(Order order, From<?, ?> from, CriteriaBuilder cb) {

		PropertyPath property = PropertyPath.from(order.getProperty(), from.getJavaType());
		Expression<?> expression = toExpressionRecursively(from, property);

		if (order.getNullHandling() != Sort.NullHandling.NATIVE) {
			throw new UnsupportedOperationException("Applying Null Precedence using Criteria Queries is not yet supported.");
		}

		if (order.isIgnoreCase() && String.class.equals(expression.getJavaType())) {
			Expression<String> upper = cb.lower((Expression<String>) expression);
			return order.isAscending() ? cb.asc(upper) : cb.desc(upper);
		} else {
			return order.isAscending() ? cb.asc(expression) : cb.desc(expression);
		}
	}

	static <T> Expression<T> toExpressionRecursively(From<?, ?> from, PropertyPath property) {
		return toExpressionRecursively(from, property, false);
	}

	static <T> Expression<T> toExpressionRecursively(From<?, ?> from, PropertyPath property, boolean isForSelection) {
		return toExpressionRecursively(from, property, isForSelection, false);
	}

	/**
	 * Creates an expression with proper inner and left joins by recursively navigating the path
	 *
	 * @param from the {@link From}
	 * @param property the property path
	 * @param isForSelection is the property navigated for the selection or ordering part of the query?
	 * @param hasRequiredOuterJoin has a parent already required an outer join?
	 * @param <T> the type of the expression
	 * @return the expression
	 */
	@SuppressWarnings("unchecked")
	static <T> Expression<T> toExpressionRecursively(From<?, ?> from, PropertyPath property, boolean isForSelection,
			boolean hasRequiredOuterJoin) {

		String segment = property.getSegment();

		boolean isLeafProperty = !property.hasNext();

		boolean requiresOuterJoin = requiresOuterJoin(from, property, isForSelection, hasRequiredOuterJoin);

		// if it does not require an outer join and is a leaf, simply get the segment
		if (!requiresOuterJoin && isLeafProperty) {
			return from.get(segment);
		}

		// get or create the join
		JoinType joinType = requiresOuterJoin ? JoinType.LEFT : JoinType.INNER;
		Join<?, ?> join = getOrCreateJoin(from, segment, joinType);

		// if it's a leaf, return the join
		if (isLeafProperty) {
			return (Expression<T>) join;
		}

		PropertyPath nextProperty = Objects.requireNonNull(property.next(), "An element of the property path is null");

		// recurse with the next property
		return toExpressionRecursively(join, nextProperty, isForSelection, requiresOuterJoin);
	}

	/**
	 * Checks if this attribute requires an outer join. This is the case e.g. if it hadn't already been fetched with an
	 * inner join and if it's an optional association, and if previous paths has already required outer joins. It also
	 * ensures outer joins are used even when Hibernate defaults to inner joins (HHH-12712 and HHH-12999).
	 *
	 * @param from the {@link From} to check for fetches.
	 * @param property the property path
	 * @param isForSelection is the property navigated for the selection or ordering part of the query? if true, we need
	 *          to generate an explicit outer join in order to prevent Hibernate to use an inner join instead. see
	 *          https://hibernate.atlassian.net/browse/HHH-12999
	 * @param hasRequiredOuterJoin has a parent already required an outer join?
	 * @return whether an outer join is to be used for integrating this attribute in a query.
	 */
	static boolean requiresOuterJoin(From<?, ?> from, PropertyPath property, boolean isForSelection,
			boolean hasRequiredOuterJoin) {

		// already inner joined so outer join is useless
		if (isAlreadyInnerJoined(from, property.getSegment())) {
			return false;
		}

		Bindable<?> model = from.getModel();
		ManagedType<?> managedType = getManagedTypeForModel(model);
		Bindable<?> propertyPathModel = getModelForPath(property, managedType, from);

		// is the attribute of Collection type?
		boolean isPluralAttribute = model instanceof PluralAttribute;

		if (propertyPathModel == null && isPluralAttribute) {
			return true;
		}

		if (!(propertyPathModel instanceof Attribute<?, ?> attribute)) {
			return false;
		}

		// not a persistent attribute type association (@OneToOne, @ManyToOne)
		if (!ASSOCIATION_TYPES.containsKey(attribute.getPersistentAttributeType())) {
			return false;
		}

		boolean isCollection = attribute.isCollection();
		// if this path is an optional one to one attribute navigated from the not owning side we also need an
		// explicit outer join to avoid https://hibernate.atlassian.net/browse/HHH-12712
		// and https://github.com/eclipse-ee4j/jpa-api/issues/170
		boolean isInverseOptionalOneToOne = PersistentAttributeType.ONE_TO_ONE == attribute.getPersistentAttributeType()
				&& StringUtils.hasText(getAnnotationProperty(attribute, "mappedBy", ""));

		boolean isLeafProperty = !property.hasNext();
		if (isLeafProperty && !isForSelection && !isCollection && !isInverseOptionalOneToOne && !hasRequiredOuterJoin) {
			return false;
		}

		return hasRequiredOuterJoin || getAnnotationProperty(attribute, "optional", true);
	}

	static <T> T getAnnotationProperty(Attribute<?, ?> attribute, String propertyName, T defaultValue) {

		Class<? extends Annotation> associationAnnotation = ASSOCIATION_TYPES.get(attribute.getPersistentAttributeType());

		if (associationAnnotation == null) {
			return defaultValue;
		}

		Member member = attribute.getJavaMember();

		if (!(member instanceof AnnotatedElement annotatedMember)) {
			return defaultValue;
		}

		Annotation annotation = AnnotationUtils.getAnnotation(annotatedMember, associationAnnotation);
		return annotation == null ? defaultValue : (T) AnnotationUtils.getValue(annotation, propertyName);
	}

	/**
	 * Returns an existing join for the given attribute if one already exists or creates a new one if not.
	 *
	 * @param from the {@link From} to get the current joins from.
	 * @param attribute the {@link Attribute} to look for in the current joins.
	 * @param joinType the join type to create if none was found
	 * @return will never be {@literal null}.
	 */
	static Join<?, ?> getOrCreateJoin(From<?, ?> from, String attribute, JoinType joinType) {

		for (Join<?, ?> join : from.getJoins()) {

			if (join.getAttribute().getName().equals(attribute)) {
				return join;
			}
		}
		return from.join(attribute, joinType);
	}

	/**
	 * Return whether the given {@link From} contains an inner join for the attribute with the given name.
	 *
	 * @param from the {@link From} to check for joins.
	 * @param attribute the attribute name to check.
	 * @return true if the attribute has already been inner joined
	 */
	static boolean isAlreadyInnerJoined(From<?, ?> from, String attribute) {

		for (Fetch<?, ?> fetch : from.getFetches()) {

			if (fetch.getAttribute().getName().equals(attribute) //
					&& fetch.getJoinType().equals(JoinType.INNER)) {
				return true;
			}
		}

		for (Join<?, ?> join : from.getJoins()) {

			if (join.getAttribute().getName().equals(attribute) //
					&& join.getJoinType().equals(JoinType.INNER)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Check any given {@link JpaOrder#isUnsafe()} order for presence of at least one property offending the
	 * {@link #PUNCTATION_PATTERN} and throw an {@link Exception} indicating potential unsafe order by expression.
	 *
	 * @param order
	 */
	static void checkSortExpression(Order order) {

		if (order instanceof JpaOrder jpaOrder && jpaOrder.isUnsafe()) {
			return;
		}

		if (PUNCTATION_PATTERN.matcher(order.getProperty()).find()) {
			throw new InvalidDataAccessApiUsageException(String.format(UNSAFE_PROPERTY_REFERENCE, order));
		}
	}

	/**
	 * Get the {@link Bindable model} that corresponds to the given path utilizing the given {@link ManagedType} if
	 * present or resolving the model from the {@link Path#getModel() path} by creating it via {@link From#get(String)} in
	 * case where the type signature may be erased by some vendors if the attribute contains generics.
	 *
	 * @param path the current {@link PropertyPath} segment.
	 * @param managedType primary source for the resulting {@link Bindable}. Can be {@literal null}.
	 * @param fallback must not be {@literal null}.
	 * @return the corresponding {@link Bindable} of {@literal null}.
	 * @see <a href=
	 *      "https://hibernate.atlassian.net/browse/HHH-16144">https://hibernate.atlassian.net/browse/HHH-16144</a>
	 * @see <a href=
	 *      "https://github.com/jakartaee/persistence/issues/562">https://github.com/jakartaee/persistence/issues/562</a>
	 */
	@Nullable
	private static Bindable<?> getModelForPath(PropertyPath path, @Nullable ManagedType<?> managedType,
			Path<?> fallback) {

		String segment = path.getSegment();
		if (managedType != null) {
			try {
				return (Bindable<?>) managedType.getAttribute(segment);
			} catch (IllegalArgumentException ex) {
				// ManagedType may be erased for some vendor if the attribute is declared as generic
			}
		}

		return fallback.get(segment).getModel();
	}

	/**
	 * Required for EclipseLink: we try to avoid using from.get as EclipseLink produces an inner join regardless of which
	 * join operation is specified next
	 *
	 * @see <a href=
	 *      "https://bugs.eclipse.org/bugs/show_bug.cgi?id=413892">https://bugs.eclipse.org/bugs/show_bug.cgi?id=413892</a>
	 * @param model
	 * @return
	 */
	@Nullable
	static ManagedType<?> getManagedTypeForModel(Bindable<?> model) {

		if (model instanceof ManagedType<?> managedType) {
			return managedType;
		}

		if (!(model instanceof SingularAttribute<?, ?> singularAttribute)) {
			return null;
		}

		return singularAttribute.getType() instanceof ManagedType<?> managedType ? managedType : null;
	}
}
