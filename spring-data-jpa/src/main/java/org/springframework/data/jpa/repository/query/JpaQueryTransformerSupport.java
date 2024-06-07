package org.springframework.data.jpa.repository.query;

import static org.springframework.data.jpa.repository.query.QueryTokens.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * Transformational operations needed to support either {@link HqlSortedQueryTransformer} or
 * {@link JpqlSortedQueryTransformer}.
 *
 * @author Greg Turnquist
 * @author Donghun Shin
 * @since 3.1
 */
class JpaQueryTransformerSupport {

	private static final Pattern PUNCTUATION_PATTERN = Pattern.compile(".*((?![._])[\\p{Punct}|\\s])");

	private static final String UNSAFE_PROPERTY_REFERENCE = "Sort expression '%s' must only contain property references or "
			+ "aliases used in the select clause; If you really want to use something other than that for sorting, please use "
			+ "JpaSort.unsafe(…)";

	private final Set<String> projectionAliases = new HashSet<>();

	/**
	 * Register an {@literal alias} so it can later be evaluated when applying {@link Sort}s.
	 *
	 * @param token
	 */
	void registerAlias(String token) {
		projectionAliases.add(token);
	}

	void registerAlias(QueryToken token) {
		projectionAliases.add(token.value());
	}

	/**
	 * Using the primary {@literal FROM} clause's alias and a {@link Sort}, construct all the {@literal ORDER BY}
	 * arguments.
	 *
	 * @param primaryFromAlias
	 * @param sort
	 * @return
	 */
	List<QueryToken> orderBy(String primaryFromAlias, Sort sort) {

		List<QueryToken> tokens = new ArrayList<>();

		sort.forEach(order -> {

			checkSortExpression(order);

			StringBuilder builder = new StringBuilder();

			if (order.isIgnoreCase()) {
				builder.append(TOKEN_LOWER_FUNC.value());
			}

			builder.append(generateOrderByArgument(primaryFromAlias, order));

			if (order.isIgnoreCase()) {
				builder.append(TOKEN_CLOSE_PAREN);
			}
			builder.append(" ");

			builder.append(order.isDescending() ? TOKEN_DESC : TOKEN_ASC);

			if (!tokens.isEmpty()) {
				tokens.add(TOKEN_COMMA);
			}

			tokens.add(QueryTokens.token(builder.toString()));
		});

		return tokens;
	}

	/**
	 * Check any given {@link JpaSort.JpaOrder#isUnsafe()} order for presence of at least one property offending the
	 * {@link #PUNCTUATION_PATTERN} and throw an {@link Exception} indicating potential unsafe order by expression.
	 *
	 * @param order
	 */
	private void checkSortExpression(Sort.Order order) {

		if (order instanceof JpaSort.JpaOrder jpaOrder && jpaOrder.isUnsafe()) {
			return;
		}

		if (PUNCTUATION_PATTERN.matcher(order.getProperty()).find()) {
			throw new InvalidDataAccessApiUsageException(String.format(UNSAFE_PROPERTY_REFERENCE, order));
		}
	}

	/**
	 * Using the {@code primaryFromAlias} and the {@link org.springframework.data.domain.Sort.Order}, construct a suitable
	 * argument to be added to an {@literal ORDER BY} expression.
	 *
	 * @param primaryFromAlias
	 * @param order
	 * @return
	 */
	private String generateOrderByArgument(@Nullable String primaryFromAlias, Sort.Order order) {

		if (shouldPrefixWithAlias(order, primaryFromAlias)) {
			return primaryFromAlias + "." + order.getProperty();
		} else {
			return order.getProperty();
		}
	}

	/**
	 * Determine when an {@link org.springframework.data.domain.Sort.Order} parameter should be prefixed with the primary
	 * FROM clause's alias.
	 *
	 * @param order
	 * @param primaryFromAlias
	 * @return boolean whether or not to apply the primary FROM clause's alias as a prefix
	 */
	private boolean shouldPrefixWithAlias(Sort.Order order, @Nullable String primaryFromAlias) {

		// If there is no primary alias
		if (ObjectUtils.isEmpty(primaryFromAlias)) {
			return false;
		}

		// If the Sort contains a function
		if (order.getProperty().contains("(")) {
			return false;
		}

		// If the Sort starts with the primary alias
		if (order.getProperty().startsWith(primaryFromAlias + ".")) {
			return false;
		}

		// If the Sort references an alias directly
		if (projectionAliases.contains(order.getProperty())) {
			return false;
		}

		// If the Sort property starts with an alias
		if (projectionAliases.stream().anyMatch(alias -> order.getProperty().startsWith(alias + "."))) {
			return false;
		}

		return true;
	}
}
