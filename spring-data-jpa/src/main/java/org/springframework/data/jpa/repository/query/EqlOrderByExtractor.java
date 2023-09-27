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
 * Parses the content of {@link JpaSort#unsafe(String...)} as an EQL {@literal orderby_item} and renders that into a JPA
 * Criteria {@link Expression}.
 *
 * @author Greg Turnquist
 * @since 3.2
 */
class EqlOrderByExtractor extends EqlBaseVisitor<JpaOrderByToken> {

	private From<?, ?> from;

	EqlOrderByExtractor(From<?, ?> from) {
		this.from = from;
	}

	/**
	 * Extract the {@link JpaSort.JpaOrder}'s property and parse it as a EQL {@literal orderby_item}.
	 *
	 * @param jpaOrder
	 * @return criteriaExpression
	 * @since 3.2
	 */
	Expression<?> extractCriteriaExpression(JpaSort.JpaOrder jpaOrder) {

		EqlLexer jpaOrderLexer = new EqlLexer(CharStreams.fromString(jpaOrder.getProperty()));
		EqlParser jpaOrderParser = new EqlParser(new CommonTokenStream(jpaOrderLexer));

		return expression(visit(jpaOrderParser.orderby_item()));
	}

	/**
	 * Given a particular {@link JpaOrderByToken}, transform it into a Jakarta {@link Expression}.
	 *
	 * @param token
	 * @return Expression
	 */
	private Expression<?> expression(JpaOrderByToken token) {

		if (token instanceof JpaOrderByExpressionToken expressionToken) {
			return expressionToken.expression();
		} else if (token instanceof JpaOrderByNamedToken namedToken) {
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
	 * Convert a generic {@link JpaOrderByToken} token into a {@link JpaOrderByNamedToken} and then extract its string
	 * token value.
	 *
	 * @param token
	 * @return string value
	 * @since 3.2
	 */
	private String token(JpaOrderByToken token) {

		if (token instanceof JpaOrderByNamedToken namedToken) {
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
	public JpaOrderByToken visitOrderby_item(EqlParser.Orderby_itemContext ctx) {

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
	public JpaOrderByToken visitState_field_path_expression(EqlParser.State_field_path_expressionContext ctx) {

		Path<?> path = (Path<?>) expression(visit(ctx.general_subpath()));

		path = path.get(token(visit(ctx.state_field())));

		return new JpaOrderByExpressionToken(path);
	}

	@Override
	public JpaOrderByToken visitGeneral_identification_variable(EqlParser.General_identification_variableContext ctx) {

		if (ctx.identification_variable() != null) {
			return visit(ctx.identification_variable());
		} else {
			return null;
		}
	}

	@Override
	public JpaOrderByToken visitSimple_subpath(EqlParser.Simple_subpathContext ctx) {

		Path<?> path = (Path<?>) expression(visit(ctx.general_identification_variable()));

		for (EqlParser.Single_valued_object_fieldContext singleValuedObjectFieldContext : ctx
				.single_valued_object_field()) {
			path = path.get(token(visit(singleValuedObjectFieldContext)));
		}

		return new JpaOrderByExpressionToken(path);
	}

	@Override
	public JpaOrderByToken visitResult_variable(EqlParser.Result_variableContext ctx) {
		return super.visitResult_variable(ctx);
	}

	@Override
	public JpaOrderByToken visitIdentification_variable(EqlParser.Identification_variableContext ctx) {

		if (ctx.IDENTIFICATION_VARIABLE() != null) {
			return new JpaOrderByNamedToken(ctx.IDENTIFICATION_VARIABLE().getText());
		} else {
			return new JpaOrderByNamedToken(ctx.f.getText());
		}
	}
}
