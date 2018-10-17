/*
 * Copyright 2008-2018 the original author or authors.
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
import static javax.persistence.metamodel.Attribute.PersistentAttributeType.*;

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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Attribute.PersistentAttributeType;
import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.PluralAttribute;

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
 * Simple utility class to create JPA queries.
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
 */
public abstract class QueryUtils {

	public static final String COUNT_QUERY_STRING = "select count(%s) from %s x";
	public static final String DELETE_ALL_QUERY_STRING = "delete from %s x";

	// Used Regex/Unicode categories (see http://www.unicode.org/reports/tr18/#General_Category_Property):
	// Z Separator
	// Cc Control
	// Cf Format
	// P Punctuation
	private static final String IDENTIFIER = "[._[\\P{Z}&&\\P{Cc}&&\\P{Cf}&&\\P{P}]]+";
	static final String COLON_NO_DOUBLE_COLON = "(?<![:\\\\]):";
	static final String IDENTIFIER_GROUP = String.format("(%s)", IDENTIFIER);

	private static final String COUNT_REPLACEMENT_TEMPLATE = "select count(%s) $5$6$7";
	private static final String SIMPLE_COUNT_VALUE = "$2";
	private static final String COMPLEX_COUNT_VALUE = "$3$6";
	private static final String ORDER_BY_PART = "(?iu)\\s+order\\s+by\\s+.*$";

	private static final Pattern ALIAS_MATCH;
	private static final Pattern COUNT_MATCH;
	private static final Pattern PROJECTION_CLAUSE = Pattern.compile("select\\s+(.+)\\s+from", Pattern.CASE_INSENSITIVE);

	private static final Pattern NO_DIGITS = Pattern.compile("\\D+");

	private static final String JOIN = "join\\s+(fetch\\s+)?" + IDENTIFIER + "\\s+(as\\s+)?" + IDENTIFIER_GROUP;
	private static final Pattern JOIN_PATTERN = Pattern.compile(JOIN, Pattern.CASE_INSENSITIVE);

	private static final String EQUALS_CONDITION_STRING = "%s.%s = :%s";
	private static final Pattern ORDER_BY = Pattern.compile(".*order\\s+by\\s+.*", CASE_INSENSITIVE);

	private static final Pattern NAMED_PARAMETER = Pattern
			.compile(COLON_NO_DOUBLE_COLON + IDENTIFIER + "|\\#" + IDENTIFIER, CASE_INSENSITIVE);

	private static final Pattern CONSTRUCTOR_EXPRESSION;

	private static final Map<PersistentAttributeType, Class<? extends Annotation>> ASSOCIATION_TYPES;

	private static final int QUERY_JOIN_ALIAS_GROUP_INDEX = 3;
	private static final int VARIABLE_NAME_GROUP_INDEX = 4;

	private static final Pattern PUNCTATION_PATTERN = Pattern.compile(".*((?![\\._])[\\p{Punct}|\\s])");
	private static final Pattern FUNCTION_PATTERN;

	private static final String UNSAFE_PROPERTY_REFERENCE = "Sort expression '%s' must only contain property references or "
			+ "aliases used in the select clause. If you really want to use something other than that for sorting, please use "
			+ "JpaSort.unsafe(…)!";

	static {

		StringBuilder builder = new StringBuilder();
		builder.append("(?<=from)"); // from as starting delimiter
		builder.append("(?:\\s)+"); // at least one space separating
		builder.append(IDENTIFIER_GROUP); // Entity name, can be qualified (any
		builder.append("(?:\\sas)*"); // exclude possible "as" keyword
		builder.append("(?:\\s)+"); // at least one space separating
		builder.append("(?!(?:where))(\\w+)"); // the actual alias

		ALIAS_MATCH = compile(builder.toString(), CASE_INSENSITIVE);

		builder = new StringBuilder();
		builder.append("(select\\s+((distinct )?(.+?)?)\\s+)?(from\\s+");
		builder.append(IDENTIFIER);
		builder.append("(?:\\s+as)?\\s+)");
		builder.append(IDENTIFIER_GROUP);
		builder.append("(.*)");

		COUNT_MATCH = compile(builder.toString(), CASE_INSENSITIVE);

		Map<PersistentAttributeType, Class<? extends Annotation>> persistentAttributeTypes = new HashMap<PersistentAttributeType, Class<? extends Annotation>>();
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
		builder.append("\\w+\\s*\\([\\w\\.,\\s'=]+\\)");
		// the potential alias
		builder.append("\\s+[as|AS]+\\s+(([\\w\\.]+))");

		FUNCTION_PATTERN = compile(builder.toString());
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

		Assert.hasText(entityName, "Entity name must not be null or empty!");

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

		Assert.hasText(query, "Query must not be null or empty!");

		if (sort.isUnsorted()) {
			return query;
		}

		StringBuilder builder = new StringBuilder(query);

		if (!ORDER_BY.matcher(query).matches()) {
			builder.append(" order by ");
		} else {
			builder.append(", ");
		}

		Set<String> aliases = getOuterJoinAliases(query);
		Set<String> functionAliases = getFunctionAliases(query);

		for (Order order : sort) {
			builder.append(getOrderClause(aliases, functionAliases, alias, order)).append(", ");
		}

		builder.delete(builder.length() - 2, builder.length());

		return builder.toString();
	}

	/**
	 * Returns the order clause for the given {@link Order}. Will prefix the clause with the given alias if the referenced
	 * property refers to a join alias, i.e. starts with {@code $alias.}.
	 *
	 * @param joinAliases the join aliases of the original query. Must not be {@literal null}.
	 * @param alias the alias for the root entity. May be {@literal null}.
	 * @param order the order object to build the clause for. Must not be {@literal null}.
	 * @return a String containing a order clause. Guaranteed to be not {@literal null}.
	 */
	private static String getOrderClause(Set<String> joinAliases, Set<String> functionAlias, @Nullable String alias,
			Order order) {

		String property = order.getProperty();

		checkSortExpression(order);

		if (functionAlias.contains(property)) {
			return String.format("%s %s", property, toJpaDirection(order));
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

		Set<String> result = new HashSet<String>();
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

		Matcher matcher = ALIAS_MATCH.matcher(query);

		return matcher.find() ? matcher.group(2) : null;
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

		Assert.notNull(queryString, "Querystring must not be null!");
		Assert.notNull(entities, "Iterable of entities must not be null!");
		Assert.notNull(entityManager, "EntityManager must not be null!");

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
	 * @deprecated use {@link DeclaredQuery#deriveCountQuery(String, String)} instead.
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
	 * @deprecated use {@link DeclaredQuery#deriveCountQuery(String, String)} instead.
	 */
	@Deprecated
	public static String createCountQueryFor(String originalQuery, @Nullable String countProjection) {

		Assert.hasText(originalQuery, "OriginalQuery must not be null or empty!");

		Matcher matcher = COUNT_MATCH.matcher(originalQuery);
		String countQuery = null;

		if (countProjection == null) {

			String variable = matcher.matches() ? matcher.group(VARIABLE_NAME_GROUP_INDEX) : null;
			boolean useVariable = variable != null && StringUtils.hasText(variable) && !variable.startsWith("new")
					&& !variable.startsWith("count(") && !variable.contains(",");

			String replacement = useVariable ? SIMPLE_COUNT_VALUE : COMPLEX_COUNT_VALUE;
			countQuery = matcher.replaceFirst(String.format(COUNT_REPLACEMENT_TEMPLATE, replacement));
		} else {
			countQuery = matcher.replaceFirst(String.format(COUNT_REPLACEMENT_TEMPLATE, countProjection));
		}

		return countQuery.replaceFirst(ORDER_BY_PART, "");
	}

	/**
	 * Returns whether the given {@link Query} contains named parameters.
	 *
	 * @param query Must not be {@literal null}.
	 * @return whether the given {@link Query} contains named parameters.
	 */
	public static boolean hasNamedParameter(Query query) {

		Assert.notNull(query, "Query must not be null!");

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
	 * Turns the given {@link Sort} into {@link javax.persistence.criteria.Order}s.
	 *
	 * @param sort the {@link Sort} instance to be transformed into JPA {@link javax.persistence.criteria.Order}s.
	 * @param from must not be {@literal null}.
	 * @param cb must not be {@literal null}.
	 * @return a {@link List} of {@link javax.persistence.criteria.Order}s.
	 */
	public static List<javax.persistence.criteria.Order> toOrders(Sort sort, From<?, ?> from, CriteriaBuilder cb) {

		if (sort.isUnsorted()) {
			return Collections.emptyList();
		}

		Assert.notNull(from, "From must not be null!");
		Assert.notNull(cb, "CriteriaBuilder must not be null!");

		List<javax.persistence.criteria.Order> orders = new ArrayList<>();

		for (org.springframework.data.domain.Sort.Order order : sort) {
			orders.add(toJpaOrder(order, from, cb));
		}

		return orders;
	}

	/**
	 * Returns whether the given JPQL query contains a constructor expression.
	 *
	 * @param query must not be {@literal null} or empty.
	 * @return
	 * @since 1.10
	 */
	public static boolean hasConstructorExpression(String query) {

		Assert.hasText(query, "Query must not be null or empty!");

		return CONSTRUCTOR_EXPRESSION.matcher(query).find();
	}

	/**
	 * Returns the projection part of the query, i.e. everything between {@code select} and {@code from}.
	 *
	 * @param query must not be {@literal null} or empty.
	 * @return
	 * @since 1.10.2
	 */
	public static String getProjection(String query) {

		Assert.hasText(query, "Query must not be null or empty!");

		Matcher matcher = PROJECTION_CLAUSE.matcher(query);
		String projection = matcher.find() ? matcher.group(1) : "";
		return projection.trim();
	}

	/**
	 * Creates a criteria API {@link javax.persistence.criteria.Order} from the given {@link Order}.
	 *
	 * @param order the order to transform into a JPA {@link javax.persistence.criteria.Order}
	 * @param from the {@link From} the {@link Order} expression is based on
	 * @param cb the {@link CriteriaBuilder} to build the {@link javax.persistence.criteria.Order} with
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static javax.persistence.criteria.Order toJpaOrder(Order order, From<?, ?> from, CriteriaBuilder cb) {

		PropertyPath property = PropertyPath.from(order.getProperty(), from.getJavaType());
		Expression<?> expression = toExpressionRecursively(from, property);

		if (order.isIgnoreCase() && String.class.equals(expression.getJavaType())) {
			Expression<String> lower = cb.lower((Expression<String>) expression);
			return order.isAscending() ? cb.asc(lower) : cb.desc(lower);
		} else {
			return order.isAscending() ? cb.asc(expression) : cb.desc(expression);
		}
	}

	@SuppressWarnings("unchecked")
	static <T> Expression<T> toExpressionRecursively(From<?, ?> from, PropertyPath property) {
		return toExpressionRecursively(from, property, false);
	}

	@SuppressWarnings("unchecked")
	static <T> Expression<T> toExpressionRecursively(From<?, ?> from, PropertyPath property, boolean isForSelection) {

		Bindable<?> propertyPathModel;
		Bindable<?> model = from.getModel();
		String segment = property.getSegment();

		if (model instanceof ManagedType) {

			/*
			 *  Required to keep support for EclipseLink 2.4.x. TODO: Remove once we drop that (probably Dijkstra M1)
			 *  See: https://bugs.eclipse.org/bugs/show_bug.cgi?id=413892
			 */
			propertyPathModel = (Bindable<?>) ((ManagedType<?>) model).getAttribute(segment);
		} else {
			propertyPathModel = from.get(segment).getModel();
		}

		if (requiresJoin(propertyPathModel, model instanceof PluralAttribute, !property.hasNext(), isForSelection)
				&& !isAlreadyFetched(from, segment)) {
			Join<?, ?> join = getOrCreateJoin(from, segment);
			return (Expression<T>) (property.hasNext() ? toExpressionRecursively(join, property.next(), isForSelection)
					: join);
		} else {
			Path<Object> path = from.get(segment);
			return (Expression<T>) (property.hasNext() ? toExpressionRecursively(path, property.next()) : path);
		}
	}

	/**
	 * Returns whether the given {@code propertyPathModel} requires the creation of a join. This is the case if we find a
	 * optional association.
	 *
	 * @param propertyPathModel may be {@literal null}.
	 * @param isPluralAttribute is the attribute of Collection type?
	 * @param isLeafProperty is this the final property navigated by a {@link PropertyPath}?
	 * @param isForSelection is the property navigated for the selection part of the query?
	 * @return wether an outer join is to be used for integrating this attribute in a query.
	 */
	private static boolean requiresJoin(@Nullable Bindable<?> propertyPathModel, boolean isPluralAttribute,
			boolean isLeafProperty, boolean isForSelection) {

		if (propertyPathModel == null && isPluralAttribute) {
			return true;
		}

		if (!(propertyPathModel instanceof Attribute)) {
			return false;
		}

		Attribute<?, ?> attribute = (Attribute<?, ?>) propertyPathModel;

		if (!ASSOCIATION_TYPES.containsKey(attribute.getPersistentAttributeType())) {
			return false;
		}

		// if this path is part of the select list we need to generate an explicit outer join in order to prevent Hibernate
		// to use an inner join instead.
		// see https://hibernate.atlassian.net/browse/HHH-12999.
		if (isLeafProperty && !isForSelection && !attribute.isCollection()) {
			return false;
		}

		Class<? extends Annotation> associationAnnotation = ASSOCIATION_TYPES.get(attribute.getPersistentAttributeType());

		if (associationAnnotation == null) {
			return true;
		}

		Member member = attribute.getJavaMember();

		if (!(member instanceof AnnotatedElement)) {
			return true;
		}

		Annotation annotation = AnnotationUtils.getAnnotation((AnnotatedElement) member, associationAnnotation);
		return annotation == null ? true : (boolean) AnnotationUtils.getValue(annotation, "optional");
	}

	static Expression<Object> toExpressionRecursively(Path<Object> path, PropertyPath property) {

		Path<Object> result = path.get(property.getSegment());
		return property.hasNext() ? toExpressionRecursively(result, property.next()) : result;
	}

	/**
	 * Returns an existing join for the given attribute if one already exists or creates a new one if not.
	 *
	 * @param from the {@link From} to get the current joins from.
	 * @param attribute the {@link Attribute} to look for in the current joins.
	 * @return will never be {@literal null}.
	 */
	private static Join<?, ?> getOrCreateJoin(From<?, ?> from, String attribute) {

		for (Join<?, ?> join : from.getJoins()) {

			boolean sameName = join.getAttribute().getName().equals(attribute);

			if (sameName && join.getJoinType().equals(JoinType.LEFT)) {
				return join;
			}
		}

		return from.join(attribute, JoinType.LEFT);
	}

	/**
	 * Return whether the given {@link From} contains a fetch declaration for the attribute with the given name.
	 *
	 * @param from the {@link From} to check for fetches.
	 * @param attribute the attribute name to check.
	 * @return
	 */
	private static boolean isAlreadyFetched(From<?, ?> from, String attribute) {

		for (Fetch<?, ?> fetch : from.getFetches()) {

			boolean sameName = fetch.getAttribute().getName().equals(attribute);

			if (sameName && fetch.getJoinType().equals(JoinType.LEFT)) {
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
	private static void checkSortExpression(Order order) {

		if (order instanceof JpaOrder && ((JpaOrder) order).isUnsafe()) {
			return;
		}

		if (PUNCTATION_PATTERN.matcher(order.getProperty()).find()) {
			throw new InvalidDataAccessApiUsageException(String.format(UNSAFE_PROPERTY_REFERENCE, order));
		}
	}
}
