/*
 * Copyright 2022-2025 the original author or authors.
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

import static org.springframework.data.jpa.repository.query.JSqlParserUtils.*;
import static org.springframework.data.jpa.repository.query.QueryUtils.*;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.ParseException;
import net.sf.jsqlparser.parser.feature.Feature;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.merge.Merge;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.Values;
import net.sf.jsqlparser.statement.update.Update;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import org.jspecify.annotations.Nullable;

import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.SerializationUtils;
import org.springframework.util.StringUtils;

/**
 * The implementation of {@link QueryEnhancer} using JSqlParser.
 *
 * @author Diego Krupitza
 * @author Greg Turnquist
 * @author Geoffrey Deremetz
 * @author Yanming Zhou
 * @author Christoph Strobl
 * @since 2.7.0
 */
public class JSqlParserQueryEnhancer implements QueryEnhancer {

	private final QueryProvider query;
	private final Statement statement;
	private final ParsedType parsedType;
	private final boolean hasConstructorExpression;
	private final @Nullable String primaryAlias;
	private final String projection;
	private final Set<String> joinAliases;
	private final Set<String> selectAliases;
	private final byte @Nullable [] serialized;

	/**
	 * @param query the query we want to enhance. Must not be {@literal null}.
	 */
	public JSqlParserQueryEnhancer(QueryProvider query) {

		this.query = query;
		this.statement = parseStatement(query.getQueryString(), Statement.class);

		this.parsedType = detectParsedType(statement);
		this.hasConstructorExpression = QueryUtils.hasConstructorExpression(query.getQueryString());
		this.primaryAlias = detectAlias(this.parsedType, this.statement);
		this.projection = detectProjection(this.statement);
		this.selectAliases = Collections.unmodifiableSet(getSelectionAliases(this.statement));
		this.joinAliases = Collections.unmodifiableSet(getJoinAliases(this.statement));
		byte[] tmp = SerializationUtils.serialize(this.statement);
		// this.serialized = tmp != null ? tmp : new byte[0];
		this.serialized = SerializationUtils.serialize(this.statement);
	}

	/**
	 * Parses a query string with JSqlParser.
	 *
	 * @param sql the query to parse
	 * @param classOfT the query to parse
	 * @return the parsed query
	 */
	static <T extends Statement> T parseStatement(String sql, Class<T> classOfT) {

		try {

			CCJSqlParser parser = CCJSqlParserUtil.newParser(sql);
			boolean allowComplex = parser.getConfiguration().getAsBoolean(Feature.allowComplexParsing);
			try {
				return classOfT.cast(parser.withAllowComplexParsing(true).Statement());
			} catch (ParseException ex) {
				if (allowComplex && CCJSqlParserUtil.getNestingDepth(sql) <= CCJSqlParserUtil.ALLOWED_NESTING_DEPTH) {
					// beware: the parser must not be reused, but needs to be re-initiated
					parser = CCJSqlParserUtil.newParser(sql);
					return classOfT.cast(parser.withAllowComplexParsing(true).Statement());
				} else {
					throw ex;
				}
			}

		} catch (ParseException e) {
			throw new IllegalArgumentException("The query you provided is not a valid SQL Query", e);
		}
	}

	/**
	 * Resolves the alias for the entity to be retrieved from the given JPA query. Note that you only provide valid Query
	 * strings. Things such as <code>from User u</code> will throw an {@link IllegalArgumentException}.
	 *
	 * @return Might return {@literal null}.
	 */
	private static @Nullable String detectAlias(ParsedType parsedType, Statement statement) {

		if (ParsedType.MERGE.equals(parsedType)) {

			Merge mergeStatement = (Merge) statement;

			Alias alias = mergeStatement.getUsingAlias();
			return alias == null ? null : alias.getName();

		}

		if (ParsedType.SELECT.equals(parsedType)) {

			Select selectStatement = (Select) statement;

			/*
			 * For all the other types ({@link ValuesStatement} and {@link SetOperationList}) it does not make sense to provide
			 * alias since:
			 * ValuesStatement has no alias
			 * SetOperation can have multiple alias for each operation item
			 */
			if (!(selectStatement instanceof PlainSelect selectBody)) {
				return null;
			}

			if (selectBody.getFromItem() == null) {
				return null;
			}

			Alias alias = selectBody.getFromItem().getAlias();
			return alias == null ? null : alias.getName();
		}

		return null;
	}

	/**
	 * Returns the aliases used inside the selection part in the query.
	 *
	 * @return a {@literal Set} containing all found aliases. Guaranteed to be not {@literal null}.
	 */
	private static Set<String> getSelectionAliases(Statement statement) {

		if (!(statement instanceof PlainSelect select) || CollectionUtils.isEmpty(select.getSelectItems())) {
			return Collections.emptySet();
		}

		Set<String> set = new HashSet<>(select.getSelectItems().size());

		for (SelectItem<?> selectItem : select.getSelectItems()) {
			Alias alias = selectItem.getAlias();
			if (alias != null) {
				set.add(alias.getName());
			}
		}

		return set;
	}

	/**
	 * Returns the aliases used for {@code join}s.
	 *
	 * @return a {@literal Set} of aliases used in the query. Guaranteed to be not {@literal null}.
	 */
	private static Set<String> getJoinAliases(Statement statement) {

		if (!(statement instanceof PlainSelect selectBody) || CollectionUtils.isEmpty(selectBody.getJoins())) {
			return Collections.emptySet();
		}

		Set<String> set = new HashSet<>(selectBody.getJoins().size());

		for (Join join : selectBody.getJoins()) {
			Alias alias = join.getRightItem().getAlias();
			if (alias != null) {
				set.add(alias.getName());
			}
		}

		return set;

	}

	private static String detectProjection(Statement statement) {

		if (!(statement instanceof Select select)) {
			return "";
		}

		if (select instanceof Values) {
			return "";
		}

		Select selectBody = select;

		if (select instanceof SetOperationList setOperationList) {

			// using the first one since for setoperations the projection has to be the same
			selectBody = setOperationList.getSelects().get(0);

			if (!(selectBody instanceof PlainSelect)) {
				return "";
			}
		}

		StringJoiner joiner = new StringJoiner(", ");
		for (SelectItem<?> selectItem : ((PlainSelect) selectBody).getSelectItems()) {
			joiner.add(selectItem.toString());
		}
		return joiner.toString().trim();

	}

	/**
	 * Detects what type of query is provided.
	 *
	 * @return the parsed type
	 */
	private static ParsedType detectParsedType(Statement statement) {

		if (statement instanceof Insert) {
			return ParsedType.INSERT;
		} else if (statement instanceof Update) {
			return ParsedType.UPDATE;
		} else if (statement instanceof Delete) {
			return ParsedType.DELETE;
		} else if (statement instanceof Select) {
			return ParsedType.SELECT;
		} else if (statement instanceof Merge) {
			return ParsedType.MERGE;
		} else {
			return ParsedType.OTHER;
		}
	}

	@Override
	public boolean hasConstructorExpression() {
		return hasConstructorExpression;
	}

	@Override
	public @Nullable String detectAlias() {
		return this.primaryAlias;
	}

	@Override
	public String getProjection() {
		return this.projection;
	}

	public Set<String> getSelectionAliases() {
		return selectAliases;
	}

	@Override
	public QueryProvider getQuery() {
		return this.query;
	}

	@Override
	public String rewrite(QueryRewriteInformation rewriteInformation) {
		return doApplySorting(rewriteInformation.getSort(), primaryAlias);
	}

	private String doApplySorting(Sort sort, @Nullable String alias) {
		String queryString = query.getQueryString();
		Assert.hasText(queryString, "Query must not be null or empty");

		if (this.parsedType != ParsedType.SELECT || sort.isUnsorted()) {
			return queryString;
		}

		return applySorting(deserializeRequired(this.serialized, Select.class), sort, alias);
	}

	private String applySorting(@Nullable Select selectStatement, Sort sort, @Nullable String alias) {

		if (selectStatement instanceof SetOperationList setOperationList) {
			return applySortingToSetOperationList(setOperationList, sort);
		}

		if (!(selectStatement instanceof PlainSelect selectBody)) {
			if (selectStatement != null) {
				return selectStatement.toString();
			} else {
				throw new IllegalArgumentException("Select must not be null");
			}
		}

		List<OrderByElement> orderByElements = new ArrayList<>(16);
		for (Sort.Order order : sort) {
			orderByElements.add(getOrderClause(joinAliases, selectAliases, alias, order));
		}

		if (CollectionUtils.isEmpty(selectBody.getOrderByElements())) {
			selectBody.setOrderByElements(orderByElements);
		} else {
			selectBody.getOrderByElements().addAll(orderByElements);
		}

		return selectStatement.toString();
	}

	@Override
	public String createCountQueryFor(@Nullable String countProjection) {

		if (this.parsedType != ParsedType.SELECT) {
			return this.query.getQueryString();
		}

		Assert.hasText(this.query.getQueryString(), "OriginalQuery must not be null or empty");

		Statement statement = (Statement) deserialize(this.serialized);
		/*
		  We only support count queries for {@link PlainSelect}.
		 */
		if (!(statement instanceof PlainSelect selectBody)) {
			return this.query.getQueryString();
		}

		return createCountQueryFor(selectBody, countProjection, primaryAlias);
	}

	private static String createCountQueryFor(PlainSelect selectBody, @Nullable String countProjection,
			@Nullable String primaryAlias) {

		// remove order by
		selectBody.setOrderByElements(null);

		if (StringUtils.hasText(countProjection)) {

			selectBody.setSelectItems(
					Collections.singletonList(SelectItem.from(getJSqlCount(Collections.singletonList(countProjection), false))));
		} else {

			boolean distinct = selectBody.getDistinct() != null;
			selectBody.setDistinct(null); // reset possible distinct

			Function jSqlCount = getJSqlCount(
					Collections.singletonList(countPropertyNameForSelection(selectBody.getSelectItems(), distinct, primaryAlias)),
					distinct);
			selectBody.setSelectItems(Collections.singletonList(SelectItem.from(jSqlCount)));
		}

		return selectBody.toString();
	}

	/**
	 * Returns the {@link SetOperationList} as a string query with {@link Sort}s applied in the right order.
	 *
	 * @param setOperationListStatement
	 * @param sort
	 * @return
	 */
	private static String applySortingToSetOperationList(SetOperationList setOperationListStatement, Sort sort) {

		// special case: ValuesStatements are detected as nested OperationListStatements
		for (Select select : setOperationListStatement.getSelects()) {
			if (select instanceof Values) {
				return setOperationListStatement.toString();
			}
		}

		List<OrderByElement> orderByElements = new ArrayList<>(16);
		for (Sort.Order order : sort) {
			orderByElements.add(getOrderClause(Collections.emptySet(), Collections.emptySet(), null, order));
		}

		if (setOperationListStatement.getOrderByElements() == null) {
			setOperationListStatement.setOrderByElements(orderByElements);
		} else {
			setOperationListStatement.getOrderByElements().addAll(orderByElements);
		}

		return setOperationListStatement.toString();
	}

	/**
	 * Returns the order clause for the given {@link Sort.Order}. Will prefix the clause with the given alias if the
	 * referenced property refers to a join alias, i.e. starts with {@code $alias.}.
	 *
	 * @param joinAliases the join aliases of the original query. Must not be {@literal null}.
	 * @param alias the alias for the root entity. May be {@literal null}.
	 * @param order the order object to build the clause for. Must not be {@literal null}.
	 * @return a {@link OrderByElement} containing an order clause. Guaranteed to be not {@literal null}.
	 */
	private static OrderByElement getOrderClause(Set<String> joinAliases, Set<String> selectionAliases,
			@Nullable String alias, Sort.Order order) {

		OrderByElement orderByElement = new OrderByElement();
		orderByElement.setAsc(order.getDirection().isAscending());
		orderByElement.setAscDescPresent(true);

		String property = order.getProperty();

		checkSortExpression(order);

		if (selectionAliases.contains(property)) {

			Expression orderExpression = order.isIgnoreCase() ? getJSqlLower(property) : new Column(property);
			orderByElement.setExpression(orderExpression);
			return orderByElement;
		}

		boolean qualifyReference = true;
		for (String joinAlias : joinAliases) {
			if (property.startsWith(joinAlias.concat("."))) {
				qualifyReference = false;
				break;
			}
		}

		boolean functionIndicator = property.contains("(");

		String reference = qualifyReference && !functionIndicator && StringUtils.hasText(alias) ? alias + "." + property
				: property;
		Expression orderExpression = order.isIgnoreCase() ? getJSqlLower(reference) : new Column(reference);
		orderByElement.setExpression(orderExpression);
		return orderByElement;
	}

	/**
	 * Get the count property if present in {@link SelectItem slected items}, {@literal *} or {@literal 1} for native ones
	 * and {@literal *} or the given {@literal tableAlias}.
	 *
	 * @param selectItems items from the select.
	 * @param distinct indicator if query for distinct values.
	 * @param tableAlias the table alias which can be {@literal null}.
	 * @return
	 */
	private static String countPropertyNameForSelection(List<SelectItem<?>> selectItems, boolean distinct,
			@Nullable String tableAlias) {

		if (onlyASingleColumnProjection(selectItems)) {

			SelectItem<?> singleProjection = selectItems.get(0);
			Column column = (Column) singleProjection.getExpression();
			return column.getFullyQualifiedName();
		}

		return distinct ? ((tableAlias != null ? tableAlias + "." : "") + "*") : "1";
	}

	/**
	 * Checks whether a given projection only contains a single column definition (aka without functions, etc.)
	 *
	 * @param projection the projection to analyse.
	 * @return {@code true} when the projection only contains a single column definition otherwise {@code false}.
	 */
	private static boolean onlyASingleColumnProjection(List<SelectItem<?>> projection) {

		// this is unfortunately the only way to check without any hacky & hard string regex magic
		return projection.size() == 1 && projection.get(0) instanceof SelectItem<?>
				&& ((projection.get(0)).getExpression()) instanceof Column;
	}

	/**
	 * An enum to represent the top level parsed statement of the provided query.
	 * <ul>
	 * <li>{@code ParsedType.DELETE}: means the top level statement is {@link Delete}</li>
	 * <li>{@code ParsedType.UPDATE}: means the top level statement is {@link Update}</li>
	 * <li>{@code ParsedType.SELECT}: means the top level statement is {@link Select}</li>
	 * <li>{@code ParsedType.INSERT}: means the top level statement is {@link Insert}</li>
	 * <li>{@code ParsedType.MERGE}: means the top level statement is {@link Merge}</li>
	 * <li>{@code ParsedType.OTHER}: means the top level statement is a different top-level type</li>
	 * </ul>
	 */
	enum ParsedType {
		DELETE, UPDATE, SELECT, INSERT, MERGE, OTHER
	}

	/**
	 * Deserialize the byte array into an object.
	 *
	 * @param bytes a serialized object
	 * @return the result of deserializing the bytes
	 */
	private static @Nullable Object deserialize(byte @Nullable [] bytes) {
		if (ObjectUtils.isEmpty(bytes)) {
			return null;
		}
		try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
			return ois.readObject();
		} catch (IOException ex) {
			throw new IllegalArgumentException("Failed to deserialize object", ex);
		} catch (ClassNotFoundException ex) {
			throw new IllegalStateException("Failed to deserialize object type", ex);
		}
	}

	private static <T> T deserializeRequired(byte @Nullable [] bytes, Class<T> type) {
		Object deserialize = deserialize(bytes);
		if (deserialize != null) {
			return type.cast(deserialize);
		}
		throw new IllegalStateException("Failed to deserialize object type");
	}

}
