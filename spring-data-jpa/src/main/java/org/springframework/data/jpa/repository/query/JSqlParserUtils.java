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

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A utility class for JSqlParser.
 *
 * @author Diego Krupitza
 * @author Greg Turnquist
 * @since 2.7.0
 */
public final class JSqlParserUtils {

	private JSqlParserUtils() {}

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
