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

import static org.springframework.data.jpa.repository.query.QueryParsingToken.*;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;

/**
 * An ANTLR {@link org.antlr.v4.runtime.tree.ParseTreeVisitor} that transforms a parsed JPQL query.
 *
 * @author Greg Turnquist
 * @since 3.1
 */
class JpqlQueryTransformer extends JpqlBaseVisitor<List<QueryParsingToken>> {

	@Nullable private Sort sort;
	private boolean countQuery;

	@Nullable private String countProjection;

	@Nullable private String alias = null;

	private List<QueryParsingToken> projection = null;

	private boolean hasConstructorExpression = false;

	JpqlQueryTransformer() {
		this(null, false, null);
	}

	JpqlQueryTransformer(@Nullable Sort sort) {
		this(sort, false, null);
	}

	JpqlQueryTransformer(boolean countQuery, @Nullable String countProjection) {
		this(null, countQuery, countProjection);
	}

	private JpqlQueryTransformer(@Nullable Sort sort, boolean countQuery, @Nullable String countProjection) {

		this.sort = sort;
		this.countQuery = countQuery;
		this.countProjection = countProjection;
	}

	@Nullable
	public String getAlias() {
		return this.alias;
	}

	public List<QueryParsingToken> getProjection() {
		return this.projection;
	}

	public boolean hasConstructorExpression() {
		return this.hasConstructorExpression;
	}

	@Override
	public List<QueryParsingToken> visitStart(JpqlParser.StartContext ctx) {
		return visit(ctx.ql_statement());
	}

	@Override
	public List<QueryParsingToken> visitQl_statement(JpqlParser.Ql_statementContext ctx) {

		if (ctx.select_statement() != null) {
			return visit(ctx.select_statement());
		} else if (ctx.update_statement() != null) {
			return visit(ctx.update_statement());
		} else if (ctx.delete_statement() != null) {
			return visit(ctx.delete_statement());
		} else {
			return List.of();
		}
	}

	@Override
	public List<QueryParsingToken> visitSelect_statement(JpqlParser.Select_statementContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.select_clause()));
		tokens.addAll(visit(ctx.from_clause()));

		if (ctx.where_clause() != null) {
			tokens.addAll(visit(ctx.where_clause()));
		}

		if (ctx.groupby_clause() != null) {
			tokens.addAll(visit(ctx.groupby_clause()));
		}

		if (ctx.having_clause() != null) {
			tokens.addAll(visit(ctx.having_clause()));
		}

		if (!countQuery) {

			if (ctx.orderby_clause() != null) {
				tokens.addAll(visit(ctx.orderby_clause()));
			}

			if (this.sort != null && this.sort.isSorted()) {

				if (ctx.orderby_clause() != null) {

					NOSPACE(tokens);
					tokens.add(TOKEN_COMMA);
				} else {

					SPACE(tokens);
					tokens.add(TOKEN_ORDER_BY);
				}

				this.sort.forEach(order -> {

					QueryParser.checkSortExpression(order);

					if (order.isIgnoreCase()) {
						tokens.add(TOKEN_LOWER_FUNC);
					}
					tokens.add(new QueryParsingToken(() -> {

						if (order.getProperty().contains("(")) {
							return order.getProperty();
						}

						return this.alias + "." + order.getProperty();
					}, true));
					if (order.isIgnoreCase()) {
						NOSPACE(tokens);
						tokens.add(TOKEN_CLOSE_PAREN);
					}
					tokens.add(order.isDescending() ? TOKEN_DESC : TOKEN_ASC);
					tokens.add(TOKEN_COMMA);
				});
				CLIP(tokens);
			}
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitUpdate_statement(JpqlParser.Update_statementContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.update_clause()));

		if (ctx.where_clause() != null) {
			tokens.addAll(visit(ctx.where_clause()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitDelete_statement(JpqlParser.Delete_statementContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.delete_clause()));

		if (ctx.where_clause() != null) {
			tokens.addAll(visit(ctx.where_clause()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitFrom_clause(JpqlParser.From_clauseContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.FROM(), true));

		ctx.identification_variable_declaration().forEach(identificationVariableDeclarationContext -> {
			tokens.addAll(visit(identificationVariableDeclarationContext));
		});

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitIdentification_variable_declaration(
			JpqlParser.Identification_variable_declarationContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.range_variable_declaration()));

		ctx.join().forEach(joinContext -> {
			tokens.addAll(visit(joinContext));
		});
		ctx.fetch_join().forEach(fetchJoinContext -> {
			tokens.addAll(visit(fetchJoinContext));
		});

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitRange_variable_declaration(JpqlParser.Range_variable_declarationContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.entity_name()));

		if (ctx.AS() != null) {
			tokens.add(new QueryParsingToken(ctx.AS()));
		}

		tokens.addAll(visit(ctx.identification_variable()));

		if (this.alias == null) {
			this.alias = tokens.get(tokens.size() - 1).getToken();
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitJoin(JpqlParser.JoinContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.join_spec()));
		tokens.addAll(visit(ctx.join_association_path_expression()));
		if (ctx.AS() != null) {
			tokens.add(new QueryParsingToken(ctx.AS()));
		}
		tokens.addAll(visit(ctx.identification_variable()));
		if (ctx.join_condition() != null) {
			tokens.addAll(visit(ctx.join_condition()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitFetch_join(JpqlParser.Fetch_joinContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.join_spec()));
		tokens.add(new QueryParsingToken(ctx.FETCH()));
		tokens.addAll(visit(ctx.join_association_path_expression()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitJoin_spec(JpqlParser.Join_specContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.LEFT() != null) {
			tokens.add(new QueryParsingToken(ctx.LEFT()));
		}
		if (ctx.OUTER() != null) {
			tokens.add(new QueryParsingToken(ctx.OUTER()));
		}
		if (ctx.INNER() != null) {
			tokens.add(new QueryParsingToken(ctx.INNER()));
		}
		if (ctx.JOIN() != null) {
			tokens.add(new QueryParsingToken(ctx.JOIN()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitJoin_condition(JpqlParser.Join_conditionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.ON()));
		tokens.addAll(visit(ctx.conditional_expression()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitJoin_association_path_expression(
			JpqlParser.Join_association_path_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.TREAT() == null) {
			if (ctx.join_collection_valued_path_expression() != null) {
				tokens.addAll(visit(ctx.join_collection_valued_path_expression()));
			} else if (ctx.join_single_valued_path_expression() != null) {
				tokens.addAll(visit(ctx.join_single_valued_path_expression()));
			}
		} else {
			if (ctx.join_collection_valued_path_expression() != null) {

				tokens.add(new QueryParsingToken(ctx.TREAT()));
				tokens.add(TOKEN_OPEN_PAREN);
				tokens.addAll(visit(ctx.join_collection_valued_path_expression()));
				tokens.add(new QueryParsingToken(ctx.AS()));
				tokens.addAll(visit(ctx.subtype()));
				tokens.add(TOKEN_CLOSE_PAREN);
			} else if (ctx.join_single_valued_path_expression() != null) {

				tokens.add(new QueryParsingToken(ctx.TREAT()));
				tokens.add(TOKEN_OPEN_PAREN);
				tokens.addAll(visit(ctx.join_single_valued_path_expression()));
				tokens.add(new QueryParsingToken(ctx.AS()));
				tokens.addAll(visit(ctx.subtype()));
				tokens.add(TOKEN_CLOSE_PAREN);
			}
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitJoin_collection_valued_path_expression(
			JpqlParser.Join_collection_valued_path_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.identification_variable()));
		NOSPACE(tokens);
		tokens.add(TOKEN_DOT);

		ctx.single_valued_embeddable_object_field().forEach(singleValuedEmbeddableObjectFieldContext -> {
			tokens.addAll(visit(singleValuedEmbeddableObjectFieldContext));
			tokens.add(TOKEN_DOT);
		});

		tokens.addAll(visit(ctx.collection_valued_field()));

		return NOSPACE_ALL_BUT_LAST_ELEMENT(tokens, true);
	}

	@Override
	public List<QueryParsingToken> visitJoin_single_valued_path_expression(
			JpqlParser.Join_single_valued_path_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.identification_variable()));
		tokens.add(TOKEN_DOT);

		ctx.single_valued_embeddable_object_field().forEach(singleValuedEmbeddableObjectFieldContext -> {
			tokens.addAll(visit(singleValuedEmbeddableObjectFieldContext));
			tokens.add(TOKEN_DOT);
		});

		tokens.addAll(visit(ctx.single_valued_object_field()));

		return NOSPACE_ALL_BUT_LAST_ELEMENT(tokens, true);
	}

	@Override
	public List<QueryParsingToken> visitCollection_member_declaration(
			JpqlParser.Collection_member_declarationContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.IN()));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.collection_valued_path_expression()));
		tokens.add(TOKEN_CLOSE_PAREN);
		if (ctx.AS() != null) {
			tokens.add(new QueryParsingToken(ctx.AS()));
		}
		tokens.addAll(visit(ctx.identification_variable()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitQualified_identification_variable(
			JpqlParser.Qualified_identification_variableContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.map_field_identification_variable() != null) {
			tokens.addAll(visit(ctx.map_field_identification_variable()));
		} else if (ctx.identification_variable() != null) {

			tokens.add(new QueryParsingToken(ctx.ENTRY()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.identification_variable()));
			tokens.add(TOKEN_CLOSE_PAREN);
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitMap_field_identification_variable(
			JpqlParser.Map_field_identification_variableContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.KEY() != null) {

			tokens.add(new QueryParsingToken(ctx.KEY()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.identification_variable()));
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.VALUE() != null) {

			tokens.add(new QueryParsingToken(ctx.VALUE()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.identification_variable()));
			tokens.add(TOKEN_CLOSE_PAREN);
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitSingle_valued_path_expression(
			JpqlParser.Single_valued_path_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.qualified_identification_variable() != null) {
			tokens.addAll(visit(ctx.qualified_identification_variable()));
		} else if (ctx.qualified_identification_variable() != null) {

			tokens.add(new QueryParsingToken(ctx.TREAT(), false));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.qualified_identification_variable()));
			tokens.add(new QueryParsingToken(ctx.AS()));
			tokens.addAll(visit(ctx.subtype()));
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.state_field_path_expression() != null) {
			tokens.addAll(visit(ctx.state_field_path_expression()));
		} else if (ctx.single_valued_object_path_expression() != null) {
			tokens.addAll(visit(ctx.single_valued_object_path_expression()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitGeneral_identification_variable(
			JpqlParser.General_identification_variableContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.identification_variable() != null) {
			tokens.addAll(visit(ctx.identification_variable()));
		} else if (ctx.map_field_identification_variable() != null) {
			tokens.addAll(visit(ctx.map_field_identification_variable()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitGeneral_subpath(JpqlParser.General_subpathContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.simple_subpath() != null) {
			tokens.addAll(visit(ctx.simple_subpath()));
		} else if (ctx.treated_subpath() != null) {

			tokens.addAll(visit(ctx.treated_subpath()));
			ctx.single_valued_object_field().forEach(singleValuedObjectFieldContext -> {
				tokens.add(TOKEN_DOT);
				tokens.addAll(visit(singleValuedObjectFieldContext));
				NOSPACE(tokens);
			});
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitSimple_subpath(JpqlParser.Simple_subpathContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.general_identification_variable()));

		ctx.single_valued_object_field().forEach(singleValuedObjectFieldContext -> {
			tokens.add(TOKEN_DOT);
			tokens.addAll(visit(singleValuedObjectFieldContext));
		});

		return NOSPACE_ALL_BUT_LAST_ELEMENT(tokens, false);
	}

	@Override
	public List<QueryParsingToken> visitTreated_subpath(JpqlParser.Treated_subpathContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.TREAT()));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.general_subpath()));
		tokens.add(new QueryParsingToken(ctx.AS()));
		tokens.addAll(visit(ctx.subtype()));
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitState_field_path_expression(JpqlParser.State_field_path_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.general_subpath()));
		NOSPACE(tokens);
		tokens.add(TOKEN_DOT);
		tokens.addAll(visit(ctx.state_field()));

		return NOSPACE_ALL_BUT_LAST_ELEMENT(tokens, true);
	}

	@Override
	public List<QueryParsingToken> visitState_valued_path_expression(JpqlParser.State_valued_path_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.state_field_path_expression() != null) {
			tokens.addAll(visit(ctx.state_field_path_expression()));
		} else if (ctx.general_identification_variable() != null) {
			tokens.addAll(visit(ctx.general_identification_variable()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitSingle_valued_object_path_expression(
			JpqlParser.Single_valued_object_path_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.general_subpath()));
		NOSPACE(tokens);
		tokens.add(TOKEN_DOT);
		tokens.addAll(visit(ctx.single_valued_object_field()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitCollection_valued_path_expression(
			JpqlParser.Collection_valued_path_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.general_subpath()));
		NOSPACE(tokens);
		tokens.add(TOKEN_DOT);
		tokens.addAll(visit(ctx.collection_value_field()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitUpdate_clause(JpqlParser.Update_clauseContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.UPDATE()));
		tokens.addAll(visit(ctx.entity_name()));
		if (ctx.AS() != null) {
			tokens.add(new QueryParsingToken(ctx.AS()));
		}
		if (ctx.identification_variable() != null) {
			tokens.addAll(visit(ctx.identification_variable()));
		}
		tokens.add(new QueryParsingToken(ctx.SET()));
		ctx.update_item().forEach(updateItemContext -> {
			tokens.addAll(visit(updateItemContext));
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitUpdate_item(JpqlParser.Update_itemContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.identification_variable() != null) {
			tokens.addAll(visit(ctx.identification_variable()));
			tokens.add(TOKEN_DOT);
		}

		ctx.single_valued_embeddable_object_field().forEach(singleValuedEmbeddableObjectFieldContext -> {
			tokens.addAll(visit(singleValuedEmbeddableObjectFieldContext));
			tokens.add(TOKEN_DOT);
		});

		if (ctx.state_field() != null) {
			tokens.addAll(visit(ctx.state_field()));
		} else if (ctx.single_valued_object_field() != null) {
			tokens.addAll(visit(ctx.single_valued_object_field()));
		}

		tokens.add(TOKEN_EQUALS);
		tokens.addAll(visit(ctx.new_value()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitNew_value(JpqlParser.New_valueContext ctx) {

		if (ctx.scalar_expression() != null) {
			return visit(ctx.scalar_expression());
		} else if (ctx.simple_entity_expression() != null) {
			return visit(ctx.simple_entity_expression());
		} else if (ctx.NULL() != null) {
			return List.of(new QueryParsingToken(ctx.NULL()));
		} else {
			return List.of();
		}
	}

	@Override
	public List<QueryParsingToken> visitDelete_clause(JpqlParser.Delete_clauseContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.DELETE()));
		tokens.add(new QueryParsingToken(ctx.FROM()));
		tokens.addAll(visit(ctx.entity_name()));
		if (ctx.AS() != null) {
			tokens.add(new QueryParsingToken(ctx.AS()));
		}
		if (ctx.identification_variable() != null) {
			tokens.addAll(visit(ctx.identification_variable()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitSelect_clause(JpqlParser.Select_clauseContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.SELECT()));

		if (countQuery) {
			tokens.add(TOKEN_COUNT_FUNC);
		}

		if (ctx.DISTINCT() != null) {
			tokens.add(new QueryParsingToken(ctx.DISTINCT()));
		}

		List<QueryParsingToken> selectItemTokens = new ArrayList<>();

		ctx.select_item().forEach(selectItemContext -> {
			selectItemTokens.addAll(visit(selectItemContext));
			NOSPACE(selectItemTokens);
			selectItemTokens.add(TOKEN_COMMA);
		});
		CLIP(selectItemTokens);
		SPACE(selectItemTokens);

		if (countQuery) {

			if (countProjection != null) {
				tokens.add(new QueryParsingToken(countProjection));
			} else {

				if (ctx.DISTINCT() != null) {

					if (selectItemTokens.stream().anyMatch(jpqlToken -> jpqlToken.getToken().contains("new"))) {
						// constructor
						tokens.add(new QueryParsingToken(() -> this.alias));
					} else {
						// keep all the select items to distinct against
						tokens.addAll(selectItemTokens);
					}
				} else {
					tokens.add(new QueryParsingToken(() -> this.alias));
				}
			}

			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else {
			tokens.addAll(selectItemTokens);
		}

		if (projection == null) {
			this.projection = selectItemTokens;
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitSelect_item(JpqlParser.Select_itemContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.select_expression()));
		SPACE(tokens);

		if (ctx.AS() != null) {
			tokens.add(new QueryParsingToken(ctx.AS()));
		}

		if (ctx.result_variable() != null) {
			tokens.addAll(visit(ctx.result_variable()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitSelect_expression(JpqlParser.Select_expressionContext ctx) {

		if (ctx.single_valued_path_expression() != null) {
			return visit(ctx.single_valued_path_expression());
		} else if (ctx.scalar_expression() != null) {
			return visit(ctx.scalar_expression());
		} else if (ctx.aggregate_expression() != null) {
			return visit(ctx.aggregate_expression());
		} else if (ctx.identification_variable() != null) {

			if (ctx.OBJECT() == null) {
				return visit(ctx.identification_variable());
			} else {

				List<QueryParsingToken> tokens = new ArrayList<>();

				tokens.add(new QueryParsingToken(ctx.OBJECT()));
				tokens.add(TOKEN_OPEN_PAREN);
				tokens.addAll(visit(ctx.identification_variable()));
				tokens.add(TOKEN_CLOSE_PAREN);

				return tokens;
			}
		} else if (ctx.constructor_expression() != null) {
			return visit(ctx.constructor_expression());
		} else {
			return List.of();
		}
	}

	@Override
	public List<QueryParsingToken> visitConstructor_expression(JpqlParser.Constructor_expressionContext ctx) {

		this.hasConstructorExpression = true;

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.NEW()));
		tokens.addAll(visit(ctx.constructor_name()));
		tokens.add(TOKEN_OPEN_PAREN);

		ctx.constructor_item().forEach(constructorItemContext -> {
			tokens.addAll(visit(constructorItemContext));
			NOSPACE(tokens);
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);

		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitConstructor_item(JpqlParser.Constructor_itemContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.single_valued_path_expression() != null) {
			tokens.addAll(visit(ctx.single_valued_path_expression()));
		} else if (ctx.scalar_expression() != null) {
			tokens.addAll(visit(ctx.scalar_expression()));
		} else if (ctx.aggregate_expression() != null) {
			tokens.addAll(visit(ctx.aggregate_expression()));
		} else if (ctx.identification_variable() != null) {
			tokens.addAll(visit(ctx.identification_variable()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitAggregate_expression(JpqlParser.Aggregate_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.AVG() != null || ctx.MAX() != null || ctx.MIN() != null || ctx.SUM() != null) {

			if (ctx.AVG() != null) {
				tokens.add(new QueryParsingToken(ctx.AVG(), false));
			}
			if (ctx.MAX() != null) {
				tokens.add(new QueryParsingToken(ctx.MAX(), false));
			}
			if (ctx.MIN() != null) {
				tokens.add(new QueryParsingToken(ctx.MIN(), false));
			}
			if (ctx.SUM() != null) {
				tokens.add(new QueryParsingToken(ctx.SUM(), false));
			}

			tokens.add(TOKEN_OPEN_PAREN);

			if (ctx.DISTINCT() != null) {
				tokens.add(new QueryParsingToken(ctx.DISTINCT()));
			}

			tokens.addAll(visit(ctx.state_valued_path_expression()));
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.COUNT() != null) {

			tokens.add(new QueryParsingToken(ctx.COUNT(), false));
			tokens.add(TOKEN_OPEN_PAREN);
			if (ctx.DISTINCT() != null) {
				tokens.add(new QueryParsingToken(ctx.DISTINCT()));
			}
			if (ctx.identification_variable() != null) {
				tokens.addAll(visit(ctx.identification_variable()));
			} else if (ctx.state_valued_path_expression() != null) {
				tokens.addAll(visit(ctx.state_valued_path_expression()));
			} else if (ctx.single_valued_object_path_expression() != null) {
				tokens.addAll(visit(ctx.single_valued_object_path_expression()));
			}
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.function_invocation() != null) {
			tokens.addAll(visit(ctx.function_invocation()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitWhere_clause(JpqlParser.Where_clauseContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.WHERE(), true));
		tokens.addAll(visit(ctx.conditional_expression()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitGroupby_clause(JpqlParser.Groupby_clauseContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.GROUP()));
		tokens.add(new QueryParsingToken(ctx.BY()));
		ctx.groupby_item().forEach(groupbyItemContext -> {
			tokens.addAll(visit(groupbyItemContext));
			NOSPACE(tokens);
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);
		SPACE(tokens);

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitGroupby_item(JpqlParser.Groupby_itemContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.single_valued_path_expression() != null) {
			tokens.addAll(visit(ctx.single_valued_path_expression()));
		} else if (ctx.identification_variable() != null) {
			tokens.addAll(visit(ctx.identification_variable()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitHaving_clause(JpqlParser.Having_clauseContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.HAVING()));
		tokens.addAll(visit(ctx.conditional_expression()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitOrderby_clause(JpqlParser.Orderby_clauseContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.ORDER()));
		tokens.add(new QueryParsingToken(ctx.BY()));

		ctx.orderby_item().forEach(orderbyItemContext -> {
			tokens.addAll(visit(orderbyItemContext));
			NOSPACE(tokens);
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitOrderby_item(JpqlParser.Orderby_itemContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.state_field_path_expression() != null) {
			tokens.addAll(visit(ctx.state_field_path_expression()));
		} else if (ctx.general_identification_variable() != null) {
			tokens.addAll(visit(ctx.general_identification_variable()));
		} else if (ctx.result_variable() != null) {
			tokens.addAll(visit(ctx.result_variable()));
		}

		if (ctx.ASC() != null) {
			tokens.add(new QueryParsingToken(ctx.ASC()));
		}
		if (ctx.DESC() != null) {
			tokens.add(new QueryParsingToken(ctx.DESC()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitSubquery(JpqlParser.SubqueryContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.simple_select_clause()));
		tokens.addAll(visit(ctx.subquery_from_clause()));
		if (ctx.where_clause() != null) {
			tokens.addAll(visit(ctx.where_clause()));
		}
		if (ctx.groupby_clause() != null) {
			tokens.addAll(visit(ctx.groupby_clause()));
		}
		if (ctx.having_clause() != null) {
			tokens.addAll(visit(ctx.having_clause()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitSubquery_from_clause(JpqlParser.Subquery_from_clauseContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.FROM()));
		ctx.subselect_identification_variable_declaration().forEach(subselectIdentificationVariableDeclarationContext -> {
			tokens.addAll(visit(subselectIdentificationVariableDeclarationContext));
			NOSPACE(tokens);
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitSubselect_identification_variable_declaration(
			JpqlParser.Subselect_identification_variable_declarationContext ctx) {
		return super.visitSubselect_identification_variable_declaration(ctx);
	}

	@Override
	public List<QueryParsingToken> visitDerived_path_expression(JpqlParser.Derived_path_expressionContext ctx) {
		return super.visitDerived_path_expression(ctx);
	}

	@Override
	public List<QueryParsingToken> visitGeneral_derived_path(JpqlParser.General_derived_pathContext ctx) {
		return super.visitGeneral_derived_path(ctx);
	}

	@Override
	public List<QueryParsingToken> visitSimple_derived_path(JpqlParser.Simple_derived_pathContext ctx) {
		return super.visitSimple_derived_path(ctx);
	}

	@Override
	public List<QueryParsingToken> visitTreated_derived_path(JpqlParser.Treated_derived_pathContext ctx) {
		return super.visitTreated_derived_path(ctx);
	}

	@Override
	public List<QueryParsingToken> visitDerived_collection_member_declaration(
			JpqlParser.Derived_collection_member_declarationContext ctx) {
		return super.visitDerived_collection_member_declaration(ctx);
	}

	@Override
	public List<QueryParsingToken> visitSimple_select_clause(JpqlParser.Simple_select_clauseContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.SELECT()));
		if (ctx.DISTINCT() != null) {
			tokens.add(new QueryParsingToken(ctx.DISTINCT()));
		}
		tokens.addAll(visit(ctx.simple_select_expression()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitSimple_select_expression(JpqlParser.Simple_select_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.single_valued_path_expression() != null) {
			tokens.addAll(visit(ctx.single_valued_path_expression()));
		} else if (ctx.scalar_expression() != null) {
			tokens.addAll(visit(ctx.scalar_expression()));
		} else if (ctx.aggregate_expression() != null) {
			tokens.addAll(visit(ctx.aggregate_expression()));
		} else if (ctx.identification_variable() != null) {
			tokens.addAll(visit(ctx.identification_variable()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitScalar_expression(JpqlParser.Scalar_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.arithmetic_expression() != null) {
			tokens.addAll(visit(ctx.arithmetic_expression()));
		} else if (ctx.string_expression() != null) {
			tokens.addAll(visit(ctx.string_expression()));
		} else if (ctx.enum_expression() != null) {
			tokens.addAll(visit(ctx.enum_expression()));
		} else if (ctx.datetime_expression() != null) {
			tokens.addAll(visit(ctx.datetime_expression()));
		} else if (ctx.boolean_expression() != null) {
			tokens.addAll(visit(ctx.boolean_expression()));
		} else if (ctx.case_expression() != null) {
			tokens.addAll(visit(ctx.case_expression()));
		} else if (ctx.entity_type_expression() != null) {
			tokens.addAll(visit(ctx.entity_type_expression()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitConditional_expression(JpqlParser.Conditional_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.conditional_expression() != null) {
			tokens.addAll(visit(ctx.conditional_expression()));
			tokens.add(new QueryParsingToken(ctx.OR()));
			tokens.addAll(visit(ctx.conditional_term()));
		} else {
			tokens.addAll(visit(ctx.conditional_term()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitConditional_term(JpqlParser.Conditional_termContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.conditional_term() != null) {
			tokens.addAll(visit(ctx.conditional_term()));
			tokens.add(new QueryParsingToken(ctx.AND()));
			tokens.addAll(visit(ctx.conditional_factor()));
		} else {
			tokens.addAll(visit(ctx.conditional_factor()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitConditional_factor(JpqlParser.Conditional_factorContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.NOT() != null) {
			tokens.add(new QueryParsingToken(ctx.NOT()));
		}

		JpqlParser.Conditional_primaryContext conditionalPrimary = ctx.conditional_primary();
		List<QueryParsingToken> visitedConditionalPrimary = visit(conditionalPrimary);
		tokens.addAll(visitedConditionalPrimary);

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitConditional_primary(JpqlParser.Conditional_primaryContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.simple_cond_expression() != null) {
			tokens.addAll(visit(ctx.simple_cond_expression()));
		} else if (ctx.conditional_expression() != null) {

			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.conditional_expression()));
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitSimple_cond_expression(JpqlParser.Simple_cond_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.comparison_expression() != null) {
			tokens.addAll(visit(ctx.comparison_expression()));
		} else if (ctx.between_expression() != null) {
			tokens.addAll(visit(ctx.between_expression()));
		} else if (ctx.in_expression() != null) {
			tokens.addAll(visit(ctx.in_expression()));
		} else if (ctx.like_expression() != null) {
			tokens.addAll(visit(ctx.like_expression()));
		} else if (ctx.null_comparison_expression() != null) {
			tokens.addAll(visit(ctx.null_comparison_expression()));
		} else if (ctx.empty_collection_comparison_expression() != null) {
			tokens.addAll(visit(ctx.empty_collection_comparison_expression()));
		} else if (ctx.collection_member_expression() != null) {
			tokens.addAll(visit(ctx.collection_member_expression()));
		} else if (ctx.exists_expression() != null) {
			tokens.addAll(visit(ctx.exists_expression()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitBetween_expression(JpqlParser.Between_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.arithmetic_expression(0) != null) {

			tokens.addAll(visit(ctx.arithmetic_expression(0)));

			if (ctx.NOT() != null) {
				tokens.add(new QueryParsingToken(ctx.NOT()));
			}

			tokens.add(new QueryParsingToken(ctx.BETWEEN()));
			tokens.addAll(visit(ctx.arithmetic_expression(1)));
			tokens.add(new QueryParsingToken(ctx.AND()));
			tokens.addAll(visit(ctx.arithmetic_expression(2)));

		} else if (ctx.string_expression(0) != null) {

			tokens.addAll(visit(ctx.string_expression(0)));

			if (ctx.NOT() != null) {
				tokens.add(new QueryParsingToken(ctx.NOT()));
			}

			tokens.add(new QueryParsingToken(ctx.BETWEEN()));
			tokens.addAll(visit(ctx.string_expression(1)));
			tokens.add(new QueryParsingToken(ctx.AND()));
			tokens.addAll(visit(ctx.string_expression(2)));

		} else if (ctx.datetime_expression(0) != null) {

			tokens.addAll(visit(ctx.datetime_expression(0)));

			if (ctx.NOT() != null) {
				tokens.add(new QueryParsingToken(ctx.NOT()));
			}

			tokens.add(new QueryParsingToken(ctx.BETWEEN()));
			tokens.addAll(visit(ctx.datetime_expression(1)));
			tokens.add(new QueryParsingToken(ctx.AND()));
			tokens.addAll(visit(ctx.datetime_expression(2)));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitIn_expression(JpqlParser.In_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.state_valued_path_expression() != null) {
			tokens.addAll(visit(ctx.state_valued_path_expression()));
		}
		if (ctx.type_discriminator() != null) {
			tokens.addAll(visit(ctx.type_discriminator()));
		}
		if (ctx.NOT() != null) {
			tokens.add(new QueryParsingToken(ctx.NOT()));
		}
		if (ctx.IN() != null) {
			tokens.add(new QueryParsingToken(ctx.IN()));
		}

		if (ctx.in_item() != null && !ctx.in_item().isEmpty()) {

			tokens.add(TOKEN_OPEN_PAREN);

			ctx.in_item().forEach(inItemContext -> {

				tokens.addAll(visit(inItemContext));
				NOSPACE(tokens);
				tokens.add(TOKEN_COMMA);
			});
			CLIP(tokens);

			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.subquery() != null) {

			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.subquery()));
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.collection_valued_input_parameter() != null) {
			tokens.addAll(visit(ctx.collection_valued_input_parameter()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitIn_item(JpqlParser.In_itemContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.literal() != null) {
			tokens.addAll(visit(ctx.literal()));
		} else if (ctx.single_valued_input_parameter() != null) {
			tokens.addAll(visit(ctx.single_valued_input_parameter()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitLike_expression(JpqlParser.Like_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.string_expression()));
		if (ctx.NOT() != null) {
			tokens.add(new QueryParsingToken(ctx.NOT()));
		}
		tokens.add(new QueryParsingToken(ctx.LIKE()));
		tokens.addAll(visit(ctx.pattern_value()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitNull_comparison_expression(JpqlParser.Null_comparison_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.single_valued_path_expression() != null) {
			tokens.addAll(visit(ctx.single_valued_path_expression()));
		} else if (ctx.input_parameter() != null) {
			tokens.addAll(visit(ctx.input_parameter()));
		}

		tokens.add(new QueryParsingToken(ctx.IS()));
		if (ctx.NOT() != null) {
			tokens.add(new QueryParsingToken(ctx.NOT()));
		}
		tokens.add(new QueryParsingToken(ctx.NULL()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitEmpty_collection_comparison_expression(
			JpqlParser.Empty_collection_comparison_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.collection_valued_path_expression()));
		tokens.add(new QueryParsingToken(ctx.IS()));
		if (ctx.NOT() != null) {
			tokens.add(new QueryParsingToken(ctx.NOT()));
		}
		tokens.add(new QueryParsingToken(ctx.EMPTY()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitCollection_member_expression(JpqlParser.Collection_member_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.entity_or_value_expression()));
		if (ctx.NOT() != null) {
			tokens.add(new QueryParsingToken(ctx.NOT()));
		}
		tokens.add(new QueryParsingToken(ctx.MEMBER()));
		if (ctx.OF() != null) {
			tokens.add(new QueryParsingToken(ctx.OF()));
		}
		tokens.addAll(visit(ctx.collection_valued_path_expression()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitEntity_or_value_expression(JpqlParser.Entity_or_value_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.single_valued_object_path_expression() != null) {
			tokens.addAll(visit(ctx.single_valued_object_path_expression()));
		} else if (ctx.state_field_path_expression() != null) {
			tokens.addAll(visit(ctx.state_field_path_expression()));
		} else if (ctx.simple_entity_or_value_expression() != null) {
			tokens.addAll(visit(ctx.simple_entity_or_value_expression()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitSimple_entity_or_value_expression(
			JpqlParser.Simple_entity_or_value_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.identification_variable() != null) {
			tokens.addAll(visit(ctx.identification_variable()));
		} else if (ctx.input_parameter() != null) {
			tokens.addAll(visit(ctx.input_parameter()));
		} else if (ctx.literal() != null) {
			tokens.addAll(visit(ctx.literal()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitExists_expression(JpqlParser.Exists_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.NOT() != null) {
			tokens.add(new QueryParsingToken(ctx.NOT()));
		}
		tokens.add(new QueryParsingToken(ctx.EXISTS()));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.subquery()));
		NOSPACE(tokens);
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitAll_or_any_expression(JpqlParser.All_or_any_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.ALL() != null) {
			tokens.add(new QueryParsingToken(ctx.ALL()));
		} else if (ctx.ANY() != null) {
			tokens.add(new QueryParsingToken(ctx.ANY()));
		} else if (ctx.SOME() != null) {
			tokens.add(new QueryParsingToken(ctx.SOME()));
		}
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.subquery()));
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitComparison_expression(JpqlParser.Comparison_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (!ctx.string_expression().isEmpty()) {

			tokens.addAll(visit(ctx.string_expression(0)));
			tokens.addAll(visit(ctx.comparison_operator()));

			if (ctx.string_expression(1) != null) {
				tokens.addAll(visit(ctx.string_expression(1)));
			} else {
				tokens.addAll(visit(ctx.all_or_any_expression()));
			}
		} else if (!ctx.boolean_expression().isEmpty()) {

			tokens.addAll(visit(ctx.boolean_expression(0)));
			tokens.add(new QueryParsingToken(ctx.op));

			if (ctx.boolean_expression(1) != null) {
				tokens.addAll(visit(ctx.boolean_expression(1)));
			} else {
				tokens.addAll(visit(ctx.all_or_any_expression()));
			}
		} else if (!ctx.enum_expression().isEmpty()) {

			tokens.addAll(visit(ctx.enum_expression(0)));
			tokens.add(new QueryParsingToken(ctx.op));

			if (ctx.enum_expression(1) != null) {
				tokens.addAll(visit(ctx.enum_expression(1)));
			} else {
				tokens.addAll(visit(ctx.all_or_any_expression()));
			}
		} else if (!ctx.datetime_expression().isEmpty()) {

			tokens.addAll(visit(ctx.datetime_expression(0)));
			tokens.addAll(visit(ctx.comparison_operator()));

			if (ctx.datetime_expression(1) != null) {
				tokens.addAll(visit(ctx.datetime_expression(1)));
			} else {
				tokens.addAll(visit(ctx.all_or_any_expression()));
			}
		} else if (!ctx.entity_expression().isEmpty()) {

			tokens.addAll(visit(ctx.entity_expression(0)));
			tokens.add(new QueryParsingToken(ctx.op));

			if (ctx.entity_expression(1) != null) {
				tokens.addAll(visit(ctx.entity_expression(1)));
			} else {
				tokens.addAll(visit(ctx.all_or_any_expression()));
			}
		} else if (!ctx.arithmetic_expression().isEmpty()) {

			tokens.addAll(visit(ctx.arithmetic_expression(0)));
			tokens.addAll(visit(ctx.comparison_operator()));

			if (ctx.arithmetic_expression(1) != null) {
				tokens.addAll(visit(ctx.arithmetic_expression(1)));
			} else {
				tokens.addAll(visit(ctx.all_or_any_expression()));
			}
		} else if (!ctx.entity_type_expression().isEmpty()) {

			tokens.addAll(visit(ctx.entity_type_expression(0)));
			tokens.add(new QueryParsingToken(ctx.op));
			tokens.addAll(visit(ctx.entity_type_expression(1)));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitComparison_operator(JpqlParser.Comparison_operatorContext ctx) {
		return List.of(new QueryParsingToken(ctx.op));
	}

	@Override
	public List<QueryParsingToken> visitArithmetic_expression(JpqlParser.Arithmetic_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.arithmetic_expression() != null) {

			tokens.addAll(visit(ctx.arithmetic_expression()));
			tokens.add(new QueryParsingToken(ctx.op));
			tokens.addAll(visit(ctx.arithmetic_term()));

		} else {
			tokens.addAll(visit(ctx.arithmetic_term()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitArithmetic_term(JpqlParser.Arithmetic_termContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.arithmetic_term() != null) {

			tokens.addAll(visit(ctx.arithmetic_term()));
			tokens.add(new QueryParsingToken(ctx.op));
			tokens.addAll(visit(ctx.arithmetic_factor()));
		} else {
			tokens.addAll(visit(ctx.arithmetic_factor()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitArithmetic_factor(JpqlParser.Arithmetic_factorContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.op != null) {
			tokens.add(new QueryParsingToken(ctx.op));
		}
		tokens.addAll(visit(ctx.arithmetic_primary()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitArithmetic_primary(JpqlParser.Arithmetic_primaryContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.state_valued_path_expression() != null) {
			tokens.addAll(visit(ctx.state_valued_path_expression()));
		} else if (ctx.numeric_literal() != null) {
			tokens.addAll(visit(ctx.numeric_literal()));
		} else if (ctx.arithmetic_expression() != null) {

			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.arithmetic_expression()));
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.input_parameter() != null) {
			tokens.addAll(visit(ctx.input_parameter()));
		} else if (ctx.functions_returning_numerics() != null) {
			tokens.addAll(visit(ctx.functions_returning_numerics()));
		} else if (ctx.aggregate_expression() != null) {
			tokens.addAll(visit(ctx.aggregate_expression()));
		} else if (ctx.case_expression() != null) {
			tokens.addAll(visit(ctx.case_expression()));
		} else if (ctx.function_invocation() != null) {
			tokens.addAll(visit(ctx.function_invocation()));
		} else if (ctx.subquery() != null) {

			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.subquery()));
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitString_expression(JpqlParser.String_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.state_valued_path_expression() != null) {
			tokens.addAll(visit(ctx.state_valued_path_expression()));
		} else if (ctx.string_literal() != null) {
			tokens.addAll(visit(ctx.string_literal()));
		} else if (ctx.input_parameter() != null) {
			tokens.addAll(visit(ctx.input_parameter()));
		} else if (ctx.functions_returning_strings() != null) {
			tokens.addAll(visit(ctx.functions_returning_strings()));
		} else if (ctx.aggregate_expression() != null) {
			tokens.addAll(visit(ctx.aggregate_expression()));
		} else if (ctx.case_expression() != null) {
			tokens.addAll(visit(ctx.case_expression()));
		} else if (ctx.function_invocation() != null) {
			tokens.addAll(visit(ctx.function_invocation()));
		} else if (ctx.subquery() != null) {

			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.subquery()));
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitDatetime_expression(JpqlParser.Datetime_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.state_valued_path_expression() != null) {
			tokens.addAll(visit(ctx.state_valued_path_expression()));
		} else if (ctx.input_parameter() != null) {
			tokens.addAll(visit(ctx.input_parameter()));
		} else if (ctx.input_parameter() != null) {
			tokens.addAll(visit(ctx.input_parameter()));
		} else if (ctx.functions_returning_datetime() != null) {
			tokens.addAll(visit(ctx.functions_returning_datetime()));
		} else if (ctx.aggregate_expression() != null) {
			tokens.addAll(visit(ctx.aggregate_expression()));
		} else if (ctx.case_expression() != null) {
			tokens.addAll(visit(ctx.case_expression()));
		} else if (ctx.function_invocation() != null) {
			tokens.addAll(visit(ctx.function_invocation()));
		} else if (ctx.date_time_timestamp_literal() != null) {
			tokens.addAll(visit(ctx.date_time_timestamp_literal()));
		} else if (ctx.subquery() != null) {

			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.subquery()));
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitBoolean_expression(JpqlParser.Boolean_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.state_valued_path_expression() != null) {
			tokens.addAll(visit(ctx.state_valued_path_expression()));
		} else if (ctx.boolean_literal() != null) {
			tokens.addAll(visit(ctx.boolean_literal()));
		} else if (ctx.input_parameter() != null) {
			tokens.addAll(visit(ctx.input_parameter()));
		} else if (ctx.case_expression() != null) {
			tokens.addAll(visit(ctx.case_expression()));
		} else if (ctx.function_invocation() != null) {
			tokens.addAll(visit(ctx.function_invocation()));
		} else if (ctx.subquery() != null) {

			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.subquery()));
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitEnum_expression(JpqlParser.Enum_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.state_valued_path_expression() != null) {
			tokens.addAll(visit(ctx.state_valued_path_expression()));
		} else if (ctx.enum_literal() != null) {
			tokens.addAll(visit(ctx.enum_literal()));
		} else if (ctx.input_parameter() != null) {
			tokens.addAll(visit(ctx.input_parameter()));
		} else if (ctx.case_expression() != null) {
			tokens.addAll(visit(ctx.case_expression()));
		} else if (ctx.subquery() != null) {

			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.subquery()));
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitEntity_expression(JpqlParser.Entity_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.single_valued_object_path_expression() != null) {
			tokens.addAll(visit(ctx.single_valued_object_path_expression()));
		} else if (ctx.simple_entity_expression() != null) {
			tokens.addAll(visit(ctx.simple_entity_expression()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitSimple_entity_expression(JpqlParser.Simple_entity_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.identification_variable() != null) {
			tokens.addAll(visit(ctx.identification_variable()));
		} else if (ctx.input_parameter() != null) {
			tokens.addAll(visit(ctx.input_parameter()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitEntity_type_expression(JpqlParser.Entity_type_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.type_discriminator() != null) {
			tokens.addAll(visit(ctx.type_discriminator()));
		} else if (ctx.entity_type_literal() != null) {
			tokens.addAll(visit(ctx.entity_type_literal()));
		} else if (ctx.input_parameter() != null) {
			tokens.addAll(visit(ctx.input_parameter()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitType_discriminator(JpqlParser.Type_discriminatorContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.TYPE()));
		tokens.add(TOKEN_OPEN_PAREN);
		if (ctx.general_identification_variable() != null) {
			tokens.addAll(visit(ctx.general_identification_variable()));
		} else if (ctx.single_valued_object_path_expression() != null) {
			tokens.addAll(visit(ctx.single_valued_object_path_expression()));
		} else if (ctx.input_parameter() != null) {
			tokens.addAll(visit(ctx.input_parameter()));
		}
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitFunctions_returning_numerics(JpqlParser.Functions_returning_numericsContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.LENGTH() != null) {

			tokens.add(new QueryParsingToken(ctx.LENGTH()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.string_expression(0)));
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.LOCATE() != null) {

			tokens.add(new QueryParsingToken(ctx.LOCATE()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.string_expression(0)));
			tokens.add(TOKEN_COMMA);
			tokens.addAll(visit(ctx.string_expression(1)));
			if (ctx.arithmetic_expression() != null) {
				tokens.add(TOKEN_COMMA);
				tokens.addAll(visit(ctx.arithmetic_expression(0)));
			}
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.ABS() != null) {

			tokens.add(new QueryParsingToken(ctx.ABS()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.arithmetic_expression(0)));
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.CEILING() != null) {

			tokens.add(new QueryParsingToken(ctx.CEILING()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.arithmetic_expression(0)));
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.EXP() != null) {

			tokens.add(new QueryParsingToken(ctx.EXP()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.arithmetic_expression(0)));
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.FLOOR() != null) {

			tokens.add(new QueryParsingToken(ctx.FLOOR()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.arithmetic_expression(0)));
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.LN() != null) {

			tokens.add(new QueryParsingToken(ctx.LN()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.arithmetic_expression(0)));
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.SIGN() != null) {

			tokens.add(new QueryParsingToken(ctx.SIGN()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.arithmetic_expression(0)));
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.SQRT() != null) {

			tokens.add(new QueryParsingToken(ctx.SQRT()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.arithmetic_expression(0)));
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.MOD() != null) {

			tokens.add(new QueryParsingToken(ctx.MOD()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.arithmetic_expression(0)));
			tokens.add(TOKEN_COMMA);
			tokens.addAll(visit(ctx.arithmetic_expression(1)));
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.POWER() != null) {

			tokens.add(new QueryParsingToken(ctx.POWER()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.arithmetic_expression(0)));
			tokens.add(TOKEN_COMMA);
			tokens.addAll(visit(ctx.arithmetic_expression(1)));
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.ROUND() != null) {

			tokens.add(new QueryParsingToken(ctx.ROUND()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.arithmetic_expression(0)));
			tokens.add(TOKEN_COMMA);
			tokens.addAll(visit(ctx.arithmetic_expression(1)));
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.SIZE() != null) {

			tokens.add(new QueryParsingToken(ctx.SIZE()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.collection_valued_path_expression()));
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.INDEX() != null) {

			tokens.add(new QueryParsingToken(ctx.INDEX()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.identification_variable()));
			tokens.add(TOKEN_CLOSE_PAREN);
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitFunctions_returning_datetime(JpqlParser.Functions_returning_datetimeContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.CURRENT_DATE() != null) {
			tokens.add(new QueryParsingToken(ctx.CURRENT_DATE()));
		} else if (ctx.CURRENT_TIME() != null) {
			tokens.add(new QueryParsingToken(ctx.CURRENT_TIME()));
		} else if (ctx.CURRENT_TIMESTAMP() != null) {
			tokens.add(new QueryParsingToken(ctx.CURRENT_TIMESTAMP()));
		} else if (ctx.LOCAL() != null) {

			tokens.add(new QueryParsingToken(ctx.LOCAL()));

			if (ctx.DATE() != null) {
				tokens.add(new QueryParsingToken(ctx.DATE()));
			} else if (ctx.TIME() != null) {
				tokens.add(new QueryParsingToken(ctx.TIME()));
			} else if (ctx.DATETIME() != null) {
				tokens.add(new QueryParsingToken(ctx.DATETIME()));
			}
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitFunctions_returning_strings(JpqlParser.Functions_returning_stringsContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.CONCAT() != null) {

			tokens.add(new QueryParsingToken(ctx.CONCAT(), false));
			tokens.add(TOKEN_OPEN_PAREN);
			ctx.string_expression().forEach(stringExpressionContext -> {
				tokens.addAll(visit(stringExpressionContext));
				NOSPACE(tokens);
				tokens.add(TOKEN_COMMA);
			});
			CLIP(tokens);
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.SUBSTRING() != null) {

			tokens.add(new QueryParsingToken(ctx.SUBSTRING(), false));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.string_expression(0)));
			NOSPACE(tokens);
			ctx.arithmetic_expression().forEach(arithmeticExpressionContext -> {
				tokens.addAll(visit(arithmeticExpressionContext));
				NOSPACE(tokens);
				tokens.add(TOKEN_COMMA);
			});
			CLIP(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.TRIM() != null) {

			tokens.add(new QueryParsingToken(ctx.TRIM(), false));
			tokens.add(TOKEN_OPEN_PAREN);
			if (ctx.trim_specification() != null) {
				tokens.addAll(visit(ctx.trim_specification()));
			}
			if (ctx.trim_character() != null) {
				tokens.addAll(visit(ctx.trim_character()));
			}
			if (ctx.FROM() != null) {
				tokens.add(new QueryParsingToken(ctx.FROM()));
			}
			tokens.addAll(visit(ctx.string_expression(0)));
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.LOWER() != null) {

			tokens.add(new QueryParsingToken(ctx.LOWER(), false));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.string_expression(0)));
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.UPPER() != null) {

			tokens.add(new QueryParsingToken(ctx.UPPER(), false));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.string_expression(0)));
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitTrim_specification(JpqlParser.Trim_specificationContext ctx) {

		if (ctx.LEADING() != null) {
			return List.of(new QueryParsingToken(ctx.LEADING()));
		} else if (ctx.TRAILING() != null) {
			return List.of(new QueryParsingToken(ctx.TRAILING()));
		} else {
			return List.of(new QueryParsingToken(ctx.BOTH()));
		}
	}

	@Override
	public List<QueryParsingToken> visitFunction_invocation(JpqlParser.Function_invocationContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.FUNCTION()));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.function_name()));
		ctx.function_arg().forEach(functionArgContext -> {
			tokens.add(TOKEN_COMMA);
			tokens.addAll(visit(functionArgContext));
		});
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitExtract_datetime_field(JpqlParser.Extract_datetime_fieldContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.EXTRACT()));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.datetime_field()));
		tokens.add(new QueryParsingToken(ctx.FROM()));
		tokens.addAll(visit(ctx.datetime_expression()));
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitDatetime_field(JpqlParser.Datetime_fieldContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public List<QueryParsingToken> visitExtract_datetime_part(JpqlParser.Extract_datetime_partContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.EXTRACT()));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.datetime_part()));
		tokens.add(new QueryParsingToken(ctx.FROM()));
		tokens.addAll(visit(ctx.datetime_expression()));
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitDatetime_part(JpqlParser.Datetime_partContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public List<QueryParsingToken> visitFunction_arg(JpqlParser.Function_argContext ctx) {

		if (ctx.literal() != null) {
			return visit(ctx.literal());
		} else if (ctx.state_valued_path_expression() != null) {
			return visit(ctx.state_valued_path_expression());
		} else if (ctx.input_parameter() != null) {
			return visit(ctx.input_parameter());
		} else {
			return visit(ctx.scalar_expression());
		}
	}

	@Override
	public List<QueryParsingToken> visitCase_expression(JpqlParser.Case_expressionContext ctx) {

		if (ctx.general_case_expression() != null) {
			return visit(ctx.general_case_expression());
		} else if (ctx.simple_case_expression() != null) {
			return visit(ctx.simple_case_expression());
		} else if (ctx.coalesce_expression() != null) {
			return visit(ctx.coalesce_expression());
		} else {
			return visit(ctx.nullif_expression());
		}
	}

	@Override
	public List<QueryParsingToken> visitGeneral_case_expression(JpqlParser.General_case_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.CASE()));

		ctx.when_clause().forEach(whenClauseContext -> {
			tokens.addAll(visit(whenClauseContext));
		});

		tokens.add(new QueryParsingToken(ctx.ELSE()));
		tokens.addAll(visit(ctx.scalar_expression()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitWhen_clause(JpqlParser.When_clauseContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.WHEN()));
		tokens.addAll(visit(ctx.conditional_expression()));
		tokens.add(new QueryParsingToken(ctx.THEN()));
		tokens.addAll(visit(ctx.scalar_expression()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitSimple_case_expression(JpqlParser.Simple_case_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.CASE()));
		tokens.addAll(visit(ctx.case_operand()));

		ctx.simple_when_clause().forEach(simpleWhenClauseContext -> {
			tokens.addAll(visit(simpleWhenClauseContext));
		});

		tokens.add(new QueryParsingToken(ctx.ELSE()));
		tokens.addAll(visit(ctx.scalar_expression()));
		tokens.add(new QueryParsingToken(ctx.END()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitCase_operand(JpqlParser.Case_operandContext ctx) {

		if (ctx.state_valued_path_expression() != null) {
			return visit(ctx.state_valued_path_expression());
		} else {
			return visit(ctx.type_discriminator());
		}
	}

	@Override
	public List<QueryParsingToken> visitSimple_when_clause(JpqlParser.Simple_when_clauseContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.WHEN()));
		tokens.addAll(visit(ctx.scalar_expression(0)));
		tokens.add(new QueryParsingToken(ctx.THEN()));
		tokens.addAll(visit(ctx.scalar_expression(1)));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitCoalesce_expression(JpqlParser.Coalesce_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.COALESCE(), false));
		tokens.add(TOKEN_OPEN_PAREN);
		ctx.scalar_expression().forEach(scalarExpressionContext -> {
			tokens.addAll(visit(scalarExpressionContext));
			NOSPACE(tokens);
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitNullif_expression(JpqlParser.Nullif_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.NULLIF()));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.scalar_expression(0)));
		tokens.add(TOKEN_COMMA);
		tokens.addAll(visit(ctx.scalar_expression(1)));
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitTrim_character(JpqlParser.Trim_characterContext ctx) {

		if (ctx.CHARACTER() != null) {
			return List.of(new QueryParsingToken(ctx.CHARACTER()));
		} else if (ctx.character_valued_input_parameter() != null) {
			return visit(ctx.character_valued_input_parameter());
		} else {
			return List.of();
		}
	}

	@Override
	public List<QueryParsingToken> visitIdentification_variable(JpqlParser.Identification_variableContext ctx) {

		if (ctx.IDENTIFICATION_VARIABLE() != null) {
			return List.of(new QueryParsingToken(ctx.IDENTIFICATION_VARIABLE()));
		} else if (ctx.COUNT() != null) {
			return List.of(new QueryParsingToken(ctx.COUNT()));
		} else if (ctx.ORDER() != null) {
			return List.of(new QueryParsingToken(ctx.ORDER()));
		} else if (ctx.KEY() != null) {
			return List.of(new QueryParsingToken(ctx.KEY()));
		} else if (ctx.spel_expression() != null) {
			return visit(ctx.spel_expression());
		} else {
			return List.of();
		}
	}

	@Override
	public List<QueryParsingToken> visitConstructor_name(JpqlParser.Constructor_nameContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.state_field_path_expression()));
		NOSPACE(tokens);

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitLiteral(JpqlParser.LiteralContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.STRINGLITERAL() != null) {
			tokens.add(new QueryParsingToken(ctx.STRINGLITERAL()));
		} else if (ctx.INTLITERAL() != null) {
			tokens.add(new QueryParsingToken(ctx.INTLITERAL()));
		} else if (ctx.FLOATLITERAL() != null) {
			tokens.add(new QueryParsingToken(ctx.FLOATLITERAL()));
		} else if (ctx.boolean_literal() != null) {
			tokens.addAll(visit(ctx.boolean_literal()));
		} else if (ctx.entity_type_literal() != null) {
			tokens.addAll(visit(ctx.entity_type_literal()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitInput_parameter(JpqlParser.Input_parameterContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.INTLITERAL() != null) {

			tokens.add(TOKEN_QUESTION_MARK);
			tokens.add(new QueryParsingToken(ctx.INTLITERAL()));
		} else if (ctx.identification_variable() != null) {

			tokens.add(TOKEN_COLON);
			tokens.addAll(visit(ctx.identification_variable()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitPattern_value(JpqlParser.Pattern_valueContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.string_expression()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitDate_time_timestamp_literal(JpqlParser.Date_time_timestamp_literalContext ctx) {
		return List.of(new QueryParsingToken(ctx.STRINGLITERAL()));
	}

	@Override
	public List<QueryParsingToken> visitEntity_type_literal(JpqlParser.Entity_type_literalContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public List<QueryParsingToken> visitEscape_character(JpqlParser.Escape_characterContext ctx) {
		return List.of(new QueryParsingToken(ctx.CHARACTER()));
	}

	@Override
	public List<QueryParsingToken> visitNumeric_literal(JpqlParser.Numeric_literalContext ctx) {

		if (ctx.INTLITERAL() != null) {
			return List.of(new QueryParsingToken(ctx.INTLITERAL()));
		} else if (ctx.FLOATLITERAL() != null) {
			return List.of(new QueryParsingToken(ctx.FLOATLITERAL()));
		} else {
			return List.of();
		}
	}

	@Override
	public List<QueryParsingToken> visitBoolean_literal(JpqlParser.Boolean_literalContext ctx) {

		if (ctx.TRUE() != null) {
			return List.of(new QueryParsingToken(ctx.TRUE()));
		} else if (ctx.FALSE() != null) {
			return List.of(new QueryParsingToken(ctx.FALSE()));
		} else {
			return List.of();
		}
	}

	@Override
	public List<QueryParsingToken> visitEnum_literal(JpqlParser.Enum_literalContext ctx) {
		return visit(ctx.state_field_path_expression());
	}

	@Override
	public List<QueryParsingToken> visitString_literal(JpqlParser.String_literalContext ctx) {

		if (ctx.CHARACTER() != null) {
			return List.of(new QueryParsingToken(ctx.CHARACTER()));
		} else if (ctx.STRINGLITERAL() != null) {
			return List.of(new QueryParsingToken(ctx.STRINGLITERAL()));
		} else {
			return List.of();
		}
	}

	@Override
	public List<QueryParsingToken> visitSingle_valued_embeddable_object_field(
			JpqlParser.Single_valued_embeddable_object_fieldContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public List<QueryParsingToken> visitSubtype(JpqlParser.SubtypeContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public List<QueryParsingToken> visitCollection_valued_field(JpqlParser.Collection_valued_fieldContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public List<QueryParsingToken> visitSingle_valued_object_field(JpqlParser.Single_valued_object_fieldContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public List<QueryParsingToken> visitState_field(JpqlParser.State_fieldContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public List<QueryParsingToken> visitCollection_value_field(JpqlParser.Collection_value_fieldContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public List<QueryParsingToken> visitEntity_name(JpqlParser.Entity_nameContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		ctx.identification_variable().forEach(identificationVariableContext -> {
			tokens.addAll(visit(identificationVariableContext));
			tokens.add(TOKEN_DOT);
		});
		CLIP(tokens);

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitResult_variable(JpqlParser.Result_variableContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public List<QueryParsingToken> visitSuperquery_identification_variable(
			JpqlParser.Superquery_identification_variableContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public List<QueryParsingToken> visitCollection_valued_input_parameter(
			JpqlParser.Collection_valued_input_parameterContext ctx) {
		return visit(ctx.input_parameter());
	}

	@Override
	public List<QueryParsingToken> visitSingle_valued_input_parameter(
			JpqlParser.Single_valued_input_parameterContext ctx) {
		return visit(ctx.input_parameter());
	}

	@Override
	public List<QueryParsingToken> visitFunction_name(JpqlParser.Function_nameContext ctx) {
		return visit(ctx.string_literal());
	}

	@Override
	public List<QueryParsingToken> visitSpel_expression(JpqlParser.Spel_expressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.prefix.equals("#{#")) { // #{#entityName}

			tokens.add(new QueryParsingToken(ctx.prefix));
			ctx.identification_variable().forEach(identificationVariableContext -> {
				tokens.addAll(visit(identificationVariableContext));
				tokens.add(TOKEN_DOT);
			});
			CLIP(tokens);
			tokens.add(TOKEN_CLOSE_BRACE);

		} else if (ctx.prefix.equals("#{#[")) { // #{[0]}

			tokens.add(new QueryParsingToken(ctx.prefix));
			tokens.add(new QueryParsingToken(ctx.INTLITERAL()));
			tokens.add(TOKEN_CLOSE_SQUARE_BRACKET_BRACE);

		} else if (ctx.prefix.equals("#{")) {// #{escape([0])} or #{escape('foo')}

			tokens.add(new QueryParsingToken(ctx.prefix));
			tokens.addAll(visit(ctx.identification_variable(0)));
			tokens.add(TOKEN_OPEN_PAREN);

			if (ctx.string_literal() != null) {
				tokens.addAll(visit(ctx.string_literal()));
			} else if (ctx.INTLITERAL() != null) {

				tokens.add(TOKEN_OPEN_SQUARE_BRACKET);
				tokens.add(new QueryParsingToken(ctx.INTLITERAL()));
				tokens.add(TOKEN_CLOSE_SQUARE_BRACKET);
			}

			tokens.add(TOKEN_CLOSE_PAREN_BRACE);
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitCharacter_valued_input_parameter(
			JpqlParser.Character_valued_input_parameterContext ctx) {

		if (ctx.CHARACTER() != null) {
			return List.of(new QueryParsingToken(ctx.CHARACTER()));
		} else if (ctx.input_parameter() != null) {
			return visit(ctx.input_parameter());
		} else {
			return List.of();
		}
	}
}
