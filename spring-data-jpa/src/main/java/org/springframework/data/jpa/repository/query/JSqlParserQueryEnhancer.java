/*
 * Copyright 2022-2023 the original author or authors.
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

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.merge.Merge;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.WithItem;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.values.ValuesStatement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * The implementation of {@link QueryEnhancer} using JSqlParser.
 *
 * @author Diego Krupitza
 * @author Greg Turnquist
 * @author Geoffrey Deremetz
 * @since 2.7.0
 */
public class JSqlParserQueryEnhancer implements QueryEnhancer {

	private final DeclaredQuery query;
	private final ParsedType parsedType;

	/**
	 * @param query the query we want to enhance. Must not be {@literal null}.
	 */
	public JSqlParserQueryEnhancer(DeclaredQuery query) {

		this.query = query;
		this.parsedType = detectParsedType();
	}

	/**
	 * Detects what type of query is provided.
	 *
	 * @return the parsed type
	 */
	private ParsedType detectParsedType() {

		try {
			Statement statement = CCJSqlParserUtil.parse(this.query.getQueryString());

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
				return ParsedType.SELECT;
			}
		} catch (JSQLParserException e) {
			throw new IllegalArgumentException("The query you provided is not a valid SQL Query!", e);
		}
	}

	@Override
	public String applySorting(Sort sort, @Nullable String alias) {

		String queryString = query.getQueryString();
		Assert.hasText(queryString, "Query must not be null or empty");

		if (this.parsedType != ParsedType.SELECT) {
			return queryString;
		}

		if (sort.isUnsorted()) {
			return queryString;
		}

		Select selectStatement = parseSelectStatement(queryString);

		if (selectStatement.getSelectBody()instanceof SetOperationList setOperationList) {
			return applySortingToSetOperationList(setOperationList, sort);
		} else if (!(selectStatement.getSelectBody() instanceof PlainSelect)) {
			return queryString;
		}

		PlainSelect selectBody = (PlainSelect) selectStatement.getSelectBody();

		final Set<String> joinAliases = getJoinAliases(selectBody);

		final Set<String> selectionAliases = getSelectionAliases(selectBody);

		List<OrderByElement> orderByElements = sort.stream() //
				.map(order -> getOrderClause(joinAliases, selectionAliases, alias, order)) //
				.toList();

		if (CollectionUtils.isEmpty(selectBody.getOrderByElements())) {
			selectBody.setOrderByElements(new ArrayList<>());
		}

		selectBody.getOrderByElements().addAll(orderByElements);

		return selectBody.toString();

	}

	/**
	 * Returns the {@link SetOperationList} as a string query with {@link Sort}s applied in the right order.
	 *
	 * @param setOperationListStatement
	 * @param sort
	 * @return
	 */
	private String applySortingToSetOperationList(SetOperationList setOperationListStatement, Sort sort) {

		// special case: ValuesStatements are detected as nested OperationListStatements
		if (setOperationListStatement.getSelects().stream().anyMatch(ValuesStatement.class::isInstance)) {
			return setOperationListStatement.toString();
		}

		// if (CollectionUtils.isEmpty(setOperationListStatement.getOrderByElements())) {
		if (setOperationListStatement.getOrderByElements() == null) {
			setOperationListStatement.setOrderByElements(new ArrayList<>());
		}

		List<OrderByElement> orderByElements = sort.stream() //
				.map(order -> getOrderClause(Collections.emptySet(), Collections.emptySet(), null, order)) //
				.toList();
		setOperationListStatement.getOrderByElements().addAll(orderByElements);

		return setOperationListStatement.toString();
	}

	/**
	 * Returns the aliases used inside the selection part in the query.
	 *
	 * @param selectBody a {@link PlainSelect} containing a query. Must not be {@literal null}.
	 * @return a {@literal Set} containing all found aliases. Guaranteed to be not {@literal null}.
	 */
	private Set<String> getSelectionAliases(PlainSelect selectBody) {

		if (CollectionUtils.isEmpty(selectBody.getSelectItems())) {
			return new HashSet<>();
		}

		return selectBody.getSelectItems().stream() //
				.filter(SelectExpressionItem.class::isInstance) //
				.map(item -> ((SelectExpressionItem) item).getAlias()) //
				.filter(Objects::nonNull) //
				.map(Alias::getName) //
				.collect(Collectors.toSet());
	}

	/**
	 * Returns the aliases used inside the selection part in the query.
	 *
	 * @return a {@literal Set} containing all found aliases. Guaranteed to be not {@literal null}.
	 */
	Set<String> getSelectionAliases() {

		if (this.parsedType != ParsedType.SELECT) {
			return new HashSet<>();
		}

		Select selectStatement = parseSelectStatement(this.query.getQueryString());
		PlainSelect selectBody = (PlainSelect) selectStatement.getSelectBody();
		return this.getSelectionAliases(selectBody);
	}

	/**
	 * Returns the aliases used for {@code join}s.
	 *
	 * @param query a query string to extract the aliases of joins from. Must not be {@literal null}.
	 * @return a {@literal Set} of aliases used in the query. Guaranteed to be not {@literal null}.
	 */
	private Set<String> getJoinAliases(String query) {

		if (this.parsedType != ParsedType.SELECT) {
			return new HashSet<>();
		}

		Select selectStatement = parseSelectStatement(query);
		if (selectStatement.getSelectBody()instanceof PlainSelect selectBody) {
			return getJoinAliases(selectBody);
		}

		return new HashSet<>();
	}

	/**
	 * Returns the aliases used for {@code join}s.
	 *
	 * @param selectBody the selection body to extract the aliases of joins from. Must not be {@literal null}.
	 * @return a {@literal Set} of aliases used in the query. Guaranteed to be not {@literal null}.
	 */
	private Set<String> getJoinAliases(PlainSelect selectBody) {

		if (CollectionUtils.isEmpty(selectBody.getJoins())) {
			return new HashSet<>();
		}

		return selectBody.getJoins().stream() //
				.map(join -> join.getRightItem().getAlias()) //
				.filter(Objects::nonNull) //
				.map(Alias::getName) //
				.collect(Collectors.toSet());
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
	private OrderByElement getOrderClause(final Set<String> joinAliases, final Set<String> selectionAliases,
			@Nullable final String alias, final Sort.Order order) {

		final OrderByElement orderByElement = new OrderByElement();
		orderByElement.setAsc(order.getDirection().isAscending());
		orderByElement.setAscDescPresent(true);

		final String property = order.getProperty();

		checkSortExpression(order);

		if (selectionAliases.contains(property)) {
			Expression orderExpression = order.isIgnoreCase() ? getJSqlLower(property) : new Column(property);

			orderByElement.setExpression(orderExpression);
			return orderByElement;
		}

		boolean qualifyReference = joinAliases //
				.parallelStream() //
				.map(joinAlias -> joinAlias.concat(".")) //
				.noneMatch(property::startsWith);

		boolean functionIndicator = property.contains("(");

		String reference = qualifyReference && !functionIndicator && StringUtils.hasText(alias)
				? String.format("%s.%s", alias, property)
				: property;
		Expression orderExpression = order.isIgnoreCase() ? getJSqlLower(reference) : new Column(reference);
		orderByElement.setExpression(orderExpression);
		return orderByElement;
	}

	@Override
	public String detectAlias() {
		return detectAlias(this.query.getQueryString());
	}

	/**
	 * Resolves the alias for the entity to be retrieved from the given JPA query. Note that you only provide valid Query
	 * strings. Things such as <code>from User u</code> will throw an {@link IllegalArgumentException}.
	 *
	 * @param query must not be {@literal null}.
	 * @return Might return {@literal null}.
	 */
	@Nullable
	private String detectAlias(String query) {

		if (ParsedType.MERGE.equals(this.parsedType)) {

			Merge mergeStatement = parseSelectStatement(query, Merge.class);
			return detectAlias(mergeStatement);

		} else if (ParsedType.SELECT.equals(this.parsedType)) {

			Select selectStatement = parseSelectStatement(query);

			/*
			For all the other types ({@link ValuesStatement} and {@link SetOperationList}) it does not make sense to provide
			alias since:
			* ValuesStatement has no alias
			* SetOperation can have multiple alias for each operation item
			 */
			if (!(selectStatement.getSelectBody() instanceof PlainSelect)) {
				return null;
			}

			PlainSelect selectBody = (PlainSelect) selectStatement.getSelectBody();
			return detectAlias(selectBody);
		}

		return null;
	}

	/**
	 * Resolves the alias for the entity to be retrieved from the given {@link PlainSelect}. Note that you only provide
	 * valid Query strings. Things such as <code>from User u</code> will throw an {@link IllegalArgumentException}.
	 *
	 * @param selectBody must not be {@literal null}.
	 * @return Might return {@literal null}.
	 */
	@Nullable
	private String detectAlias(PlainSelect selectBody) {

		if (selectBody.getFromItem() == null) {
			return null;
		}

		Alias alias = selectBody.getFromItem().getAlias();
		return alias == null ? null : alias.getName();
	}

	/**
	 * Resolves the alias for the given {@link Merge} statement.
	 *
	 * @param mergeStatement must not be {@literal null}.
	 * @return Might return {@literal null}.
	 */
	@Nullable
	private String detectAlias(Merge mergeStatement) {

		Alias alias = mergeStatement.getUsingAlias();
		return alias == null ? null : alias.getName();
	}

	@Override
	public String createCountQueryFor(@Nullable String countProjection) {

		if (this.parsedType != ParsedType.SELECT) {
			return this.query.getQueryString();
		}

		Assert.hasText(this.query.getQueryString(), "OriginalQuery must not be null or empty");

		Select selectStatement = parseSelectStatement(this.query.getQueryString());

		/*
		  We only support count queries for {@link PlainSelect}.
		 */
		if (!(selectStatement.getSelectBody() instanceof PlainSelect)) {
			return this.query.getQueryString();
		}

		PlainSelect selectBody = (PlainSelect) selectStatement.getSelectBody();

		// remove order by
		selectBody.setOrderByElements(null);

		if (StringUtils.hasText(countProjection)) {

			Function jSqlCount = getJSqlCount(Collections.singletonList(countProjection), false);
			selectBody.setSelectItems(Collections.singletonList(new SelectExpressionItem(jSqlCount)));
			return selectBody.toString();
		}

		boolean distinct = selectBody.getDistinct() != null;
		selectBody.setDistinct(null); // reset possible distinct

		String tableAlias = detectAlias(selectBody);

		// is never null
		List<SelectItem> selectItems = selectBody.getSelectItems();

		if (onlyASingleColumnProjection(selectItems)) {

			SelectExpressionItem singleProjection = (SelectExpressionItem) selectItems.get(0);

			Column column = (Column) singleProjection.getExpression();
			String countProp = column.getFullyQualifiedName();

			Function jSqlCount = getJSqlCount(Collections.singletonList(countProp), distinct);
			selectBody.setSelectItems(Collections.singletonList(new SelectExpressionItem(jSqlCount)));
			return selectBody.toString();
		}

		String countProp = query.isNativeQuery() ? (distinct ? "*" : "1") : tableAlias == null ? "*" : tableAlias;

		Function jSqlCount = getJSqlCount(Collections.singletonList(countProp), distinct);
		selectBody.setSelectItems(Collections.singletonList(new SelectExpressionItem(jSqlCount)));

		if (CollectionUtils.isEmpty(selectStatement.getWithItemsList())) {
			return selectBody.toString();
		}

		String withStatements = selectStatement.getWithItemsList().stream() //
				.map(WithItem::toString) //
				.collect(Collectors.joining(","));

		return "with " + withStatements + "\n" + selectBody;
	}

	@Override
	public String getProjection() {

		if (this.parsedType != ParsedType.SELECT) {
			return "";
		}

		Assert.hasText(query.getQueryString(), "Query must not be null or empty");

		Select selectStatement = parseSelectStatement(query.getQueryString());

		if (selectStatement.getSelectBody() instanceof ValuesStatement) {
			return "";
		}

		SelectBody selectBody = selectStatement.getSelectBody();

		if (selectStatement.getSelectBody()instanceof SetOperationList setOperationList) {

			// using the first one since for setoperations the projection has to be the same
			selectBody = setOperationList.getSelects().get(0);

			if (!(selectBody instanceof PlainSelect)) {
				return "";
			}
		}

		return ((PlainSelect) selectBody).getSelectItems() //
				.stream() //
				.map(Object::toString) //
				.collect(Collectors.joining(", ")).trim();
	}

	@Override
	public Set<String> getJoinAliases() {
		return this.getJoinAliases(this.query.getQueryString());
	}

	/**
	 * Parses a query string with JSqlParser.
	 *
	 * @param query the query to parse
	 * @return the parsed query
	 */
	private <T extends Statement> T parseSelectStatement(String query, Class<T> classOfT) {

		try {
			return classOfT.cast(CCJSqlParserUtil.parse(query));
		} catch (JSQLParserException e) {
			throw new IllegalArgumentException("The query you provided is not a valid SQL Query", e);
		}
	}

	/**
	 * Parses a query string with JSqlParser.
	 *
	 * @param query the query to parse
	 * @return the parsed query
	 */
	private Select parseSelectStatement(String query) {
		return parseSelectStatement(query, Select.class);
	}

	/**
	 * Checks whether a given projection only contains a single column definition (aka without functions, etc.)
	 *
	 * @param projection the projection to analyse
	 * @return <code>true</code> when the projection only contains a single column definition otherwise <code>false</code>
	 */
	private boolean onlyASingleColumnProjection(List<SelectItem> projection) {

		// this is unfortunately the only way to check without any hacky & hard string regex magic
		return projection.size() == 1 && projection.get(0) instanceof SelectExpressionItem
				&& (((SelectExpressionItem) projection.get(0)).getExpression()) instanceof Column;
	}

	@Override
	public DeclaredQuery getQuery() {
		return this.query;
	}

	/**
	 * An enum to represent the top level parsed statement of the provided query.
	 * <ul>
	 * <li>{@code ParsedType.DELETE}: means the top level statement is {@link Delete}</li>
	 * <li>{@code ParsedType.UPDATE}: means the top level statement is {@link Update}</li>
	 * <li>{@code ParsedType.SELECT}: means the top level statement is {@link Select}</li>
	 * <li>{@code ParsedType.INSERT}: means the top level statement is {@link Insert}</li>
	 * <li>{@code ParsedType.MERGE}: means the top level statement is {@link Merge}</li>
	 * </ul>
	 */
	enum ParsedType {
		DELETE, UPDATE, SELECT, INSERT, MERGE;
	}

}
