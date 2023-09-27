/*
 * Copyright 2023 the original author or authors.
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

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Path;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.springframework.data.jpa.domain.JpaSort;

/**
 * Parses the content of {@link JpaSort#unsafe(String...)} as a JPQL {@literal orderby_item} and renders that into a JPA
 * Criteria {@link Expression}.
 *
 * @author Greg Turnquist
 * @since 3.2
 */
class JpqlOrderByExtractor extends JpqlBaseVisitor<JpqlOrderByExtractor.JpqlOrderByItem> {

	private From<?, ?> from;

	JpqlOrderByExtractor(From<?, ?> from) {
		this.from = from;
	}

	/**
	 * Extract the {@link org.springframework.data.jpa.domain.JpaSort.JpaOrder}'s property and parse it as a JPQL
	 * {@literal orderby_item}.
	 *
	 * @param jpaOrder
	 * @return criteriaExpression
	 * @since 3.2
	 */
	Expression<?> extractCriteriaExpression(JpaSort.JpaOrder jpaOrder) {

		JpqlLexer jpaOrderLexer = new JpqlLexer(CharStreams.fromString(jpaOrder.getProperty()));
		JpqlParser jpaOrderParser = new JpqlParser(new CommonTokenStream(jpaOrderLexer));

		return expression(visit(jpaOrderParser.orderby_item()));
	}

	/**
	 * Base token return type of the ANTLR visitor used to traverse the {@literal sortExpression}.
	 */
	interface JpqlOrderByItem {}

	/**
	 * A token that encloses an {@link Expression}-based token, e.g. "LENGTH(firstname)".
	 *
	 * @param expression
	 */
	private record JpqlOrderByItemExpressionToken(Expression<?> expression) implements JpqlOrderByItem {
	}

	/**
	 * A token that encloses a string-based token name, e.g. "firstname", that is usually turned into
	 * {@literal from.get(token)}.
	 *
	 * @param token
	 */
	private record JpqlOrderByItemNamedToken(String token) implements JpqlOrderByItem {
	}

	/**
	 * Given a particular {@link JpqlOrderByItem}, transform it into a Jakarta {@link Expression}.
	 *
	 * @param token
	 * @return Expression
	 */
	private Expression<?> expression(JpqlOrderByItem token) {

		if (token instanceof JpqlOrderByItemExpressionToken expressionToken) {
			return expressionToken.expression();
		} else if (token instanceof JpqlOrderByItemNamedToken namedToken) {
			return from.get(namedToken.token());
		} else {
			if (token != null) {
				throw new IllegalArgumentException("We can't handle a " + token.getClass() + "!");
			} else {
				throw new IllegalArgumentException("We can't handle a null token!");
			}
		}
	}

	/**
	 * Convert a generic {@link JpqlOrderByItem} token into a {@link JpqlOrderByItemNamedToken} and then extract its
	 * string token value.
	 *
	 * @param token
	 * @return string value
	 * @since 3.2
	 */
	private String token(JpqlOrderByItem token) {

		if (token instanceof JpqlOrderByItemNamedToken namedToken) {
			return namedToken.token();
		} else {
			if (token != null) {
				throw new IllegalArgumentException("We can't handle a " + token.getClass() + "!");
			} else {
				throw new IllegalArgumentException("We can't handle a null token!");
			}
		}
	}

	@Override
	public JpqlOrderByItem visitOrderby_item(JpqlParser.Orderby_itemContext ctx) {

		if (ctx.state_field_path_expression() != null) {
			return visit(ctx.state_field_path_expression());
		} else if (ctx.general_identification_variable() != null) {
			return visit(ctx.general_identification_variable());
		} else if (ctx.result_variable() != null) {
			return visit(ctx.result_variable());
		} else {
			return null;
		}
	}

	@Override
	public JpqlOrderByItem visitState_field_path_expression(JpqlParser.State_field_path_expressionContext ctx) {

		Path<?> path = (Path<?>) expression(visit(ctx.general_subpath()));

		path = path.get(token(visit(ctx.state_field())));

		return new JpqlOrderByItemExpressionToken(path);
	}

	@Override
	public JpqlOrderByItem visitGeneral_identification_variable(JpqlParser.General_identification_variableContext ctx) {

		if (ctx.identification_variable() != null) {
			return visit(ctx.identification_variable());
		} else {
			return null;
		}
	}

	@Override
	public JpqlOrderByItem visitSimple_subpath(JpqlParser.Simple_subpathContext ctx) {

		Path<?> path = (Path<?>) expression(visit(ctx.general_identification_variable()));

		for (JpqlParser.Single_valued_object_fieldContext singleValuedObjectFieldContext : ctx
				.single_valued_object_field()) {
			path = path.get(token(visit(singleValuedObjectFieldContext)));
		}

		return new JpqlOrderByItemExpressionToken(path);
	}

	@Override
	public JpqlOrderByItem visitResult_variable(JpqlParser.Result_variableContext ctx) {
		return super.visitResult_variable(ctx);
	}

	@Override
	public JpqlOrderByItem visitIdentification_variable(JpqlParser.Identification_variableContext ctx) {

		if (ctx.IDENTIFICATION_VARIABLE() != null) {
			return new JpqlOrderByItemNamedToken(ctx.IDENTIFICATION_VARIABLE().getText());
		} else {
			return new JpqlOrderByItemNamedToken(ctx.f.getText());
		}
	}
}
