/*
 * Copyright 2022 the original author or authors.
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

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A utility class for JSqlParser.
 *
 * @author Diego Krupitza
 */
public final class JSqlParserUtils {

	private JSqlParserUtils() {
	}

	/**
	 * Generates a JSqlParser table from an entity name and an optional alias name
	 *
	 * @param entityName the name of the table
	 * @param alias the optional alias. Might be {@literal null} or empty
	 * @return the newly generated table
	 */
	public static Table getTableWithAlias(String entityName, String alias) {
		Table table = new Table(entityName);
		return StringUtils.hasText(alias) ? table.withAlias(new Alias(alias)) : table;
	}

	/**
	 * Concatenates a list of expression with <code>AND</code>.
	 *
	 * @param expressions the list of expressions to concatenate. Has to be non empty and with size >= 2
	 * @return the root of the concatenated expression
	 */
	public static AndExpression concatenateWithAndExpression(List<Expression> expressions) {

		if (CollectionUtils.isEmpty(expressions) || expressions.size() == 1) {
			throw new IllegalArgumentException(
					"The list of expression has to be at least of length 2! Otherwise it is not possible to concatinate with an");
		}

		AndExpression rootAndExpression = new AndExpression();
		AndExpression currentLocation = rootAndExpression;

		// traverse the list with looking 1 element ahead
		for (int i = 0; i < expressions.size(); i++) {
			Expression currentExpression = expressions.get(i);
			if (currentLocation.getLeftExpression() == null) {
				currentLocation.setLeftExpression(currentExpression);
			} else if (currentLocation.getRightExpression() == null && i == expressions.size() - 1) {
				currentLocation.setRightExpression(currentExpression);
			} else {
				AndExpression nextAndExpression = new AndExpression();
				nextAndExpression.setLeftExpression(currentExpression);

				currentLocation.setRightExpression(nextAndExpression);
				currentLocation = (AndExpression) currentLocation.getRightExpression();
			}
		}

		return rootAndExpression;
	}

	/**
	 * Generates a count function call, based on the {@code countFields}.
	 *
	 * @param countFields the non-empty list of fields that are used for counting
	 * @param distinct if it should be a distinct count
	 * @return the generated count function call
	 */
	public static Function getJSqlCount(final List<String> countFields, final boolean distinct) {
		List<Expression> countColumns = countFields //
				.stream() //
				.map(Column::new) //
				.collect(Collectors.toList());

		ExpressionList countExpression = new ExpressionList(countColumns);
		return new Function() //
				.withName("count") //
				.withParameters(countExpression) //
				.withDistinct(distinct);
	}

	/**
	 * Generates a lower function call, based on the {@code column}.
	 *
	 * @param column the non-empty column to use as param for lower
	 * @return the generated lower function call
	 */
	public static Function getJSqlLower(String column) {
		List<Expression> expressions = Collections.singletonList(new Column(column));
		ExpressionList lowerParamExpression = new ExpressionList(expressions);
		return new Function() //
				.withName("lower") //
				.withParameters(lowerParamExpression);
	}

}
