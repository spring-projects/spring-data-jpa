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

import static org.springframework.data.jpa.repository.query.JpaQueryParsingToken.*;

import java.util.ArrayList;
import java.util.List;

/**
 * An ANTLR {@link org.antlr.v4.runtime.tree.ParseTreeVisitor} that renders a JPQL query without making any changes.
 *
 * @author Greg Turnquist
 * @since 3.1
 */
class JpqlQueryRenderer extends JpqlBaseVisitor<List<JpaQueryParsingToken>> {

	@Override
	public List<JpaQueryParsingToken> visitStart(JpqlParser.StartContext ctx) {
		return visit(ctx.ql_statement());
	}

	@Override
	public List<JpaQueryParsingToken> visitQl_statement(JpqlParser.Ql_statementContext ctx) {

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
	public List<JpaQueryParsingToken> visitSelect_statement(JpqlParser.Select_statementContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

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

		if (ctx.orderby_clause() != null) {
			tokens.addAll(visit(ctx.orderby_clause()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitUpdate_statement(JpqlParser.Update_statementContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.update_clause()));

		if (ctx.where_clause() != null) {
			tokens.addAll(visit(ctx.where_clause()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitDelete_statement(JpqlParser.Delete_statementContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.delete_clause()));

		if (ctx.where_clause() != null) {
			tokens.addAll(visit(ctx.where_clause()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitFrom_clause(JpqlParser.From_clauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.FROM(), true));
		tokens.addAll(visit(ctx.identification_variable_declaration()));

		ctx.identificationVariableDeclarationOrCollectionMemberDeclaration()
				.forEach(identificationVariableDeclarationOrCollectionMemberDeclarationContext -> {
					NOSPACE(tokens);
					tokens.add(TOKEN_COMMA);
					tokens.addAll(visit(identificationVariableDeclarationOrCollectionMemberDeclarationContext));
				});
		SPACE(tokens);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitIdentificationVariableDeclarationOrCollectionMemberDeclaration(
			JpqlParser.IdentificationVariableDeclarationOrCollectionMemberDeclarationContext ctx) {

		if (ctx.identification_variable_declaration() != null) {
			return visit(ctx.identification_variable_declaration());
		} else if (ctx.collection_member_declaration() != null) {
			return visit(ctx.collection_member_declaration());
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitIdentification_variable_declaration(
			JpqlParser.Identification_variable_declarationContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

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
	public List<JpaQueryParsingToken> visitRange_variable_declaration(JpqlParser.Range_variable_declarationContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.entity_name()));

		if (ctx.AS() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.AS()));
		}

		tokens.addAll(visit(ctx.identification_variable()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitJoin(JpqlParser.JoinContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.join_spec()));
		tokens.addAll(visit(ctx.join_association_path_expression()));
		if (ctx.AS() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.AS()));
		}
		tokens.addAll(visit(ctx.identification_variable()));
		if (ctx.join_condition() != null) {
			tokens.addAll(visit(ctx.join_condition()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitFetch_join(JpqlParser.Fetch_joinContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.join_spec()));
		tokens.add(new JpaQueryParsingToken(ctx.FETCH()));
		tokens.addAll(visit(ctx.join_association_path_expression()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitJoin_spec(JpqlParser.Join_specContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.LEFT() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.LEFT()));
		}
		if (ctx.OUTER() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.OUTER()));
		}
		if (ctx.INNER() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.INNER()));
		}
		if (ctx.JOIN() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.JOIN()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitJoin_condition(JpqlParser.Join_conditionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.ON()));
		tokens.addAll(visit(ctx.conditional_expression()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitJoin_association_path_expression(
			JpqlParser.Join_association_path_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.TREAT() == null) {

			if (ctx.join_collection_valued_path_expression() != null) {
				tokens.addAll(visit(ctx.join_collection_valued_path_expression()));
			} else if (ctx.join_single_valued_path_expression() != null) {
				tokens.addAll(visit(ctx.join_single_valued_path_expression()));
			}
		} else {
			if (ctx.join_collection_valued_path_expression() != null) {

				tokens.add(new JpaQueryParsingToken(ctx.TREAT(), false));
				tokens.add(TOKEN_OPEN_PAREN);
				tokens.addAll(visit(ctx.join_collection_valued_path_expression()));
				tokens.add(new JpaQueryParsingToken(ctx.AS()));
				tokens.addAll(visit(ctx.subtype()));
				NOSPACE(tokens);
				tokens.add(TOKEN_CLOSE_PAREN);
			} else if (ctx.join_single_valued_path_expression() != null) {

				tokens.add(new JpaQueryParsingToken(ctx.TREAT(), false));
				tokens.add(TOKEN_OPEN_PAREN);
				tokens.addAll(visit(ctx.join_single_valued_path_expression()));
				tokens.add(new JpaQueryParsingToken(ctx.AS()));
				tokens.addAll(visit(ctx.subtype()));
				NOSPACE(tokens);
				tokens.add(TOKEN_CLOSE_PAREN);
			}
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitJoin_collection_valued_path_expression(
			JpqlParser.Join_collection_valued_path_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.identification_variable()));
		NOSPACE(tokens);
		tokens.add(TOKEN_DOT);

		ctx.single_valued_embeddable_object_field().forEach(singleValuedEmbeddableObjectFieldContext -> {
			tokens.addAll(visit(singleValuedEmbeddableObjectFieldContext));
			NOSPACE(tokens);
			tokens.add(TOKEN_DOT);
		});

		tokens.addAll(visit(ctx.collection_valued_field()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitJoin_single_valued_path_expression(
			JpqlParser.Join_single_valued_path_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.identification_variable()));
		tokens.add(TOKEN_DOT);

		ctx.single_valued_embeddable_object_field().forEach(singleValuedEmbeddableObjectFieldContext -> {
			tokens.addAll(visit(singleValuedEmbeddableObjectFieldContext));
			tokens.add(TOKEN_DOT);
		});

		tokens.addAll(visit(ctx.single_valued_object_field()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitCollection_member_declaration(
			JpqlParser.Collection_member_declarationContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.IN(), false));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.collection_valued_path_expression()));
		NOSPACE(tokens);
		tokens.add(TOKEN_CLOSE_PAREN);

		if (ctx.AS() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.AS()));
		}

		tokens.addAll(visit(ctx.identification_variable()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitQualified_identification_variable(
			JpqlParser.Qualified_identification_variableContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.map_field_identification_variable() != null) {
			tokens.addAll(visit(ctx.map_field_identification_variable()));
		} else if (ctx.identification_variable() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.ENTRY()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.identification_variable()));
			tokens.add(TOKEN_CLOSE_PAREN);
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitMap_field_identification_variable(
			JpqlParser.Map_field_identification_variableContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.KEY() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.KEY(), false));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.identification_variable()));
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.VALUE() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.VALUE(), false));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.identification_variable()));
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSingle_valued_path_expression(
			JpqlParser.Single_valued_path_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.qualified_identification_variable() != null) {
			tokens.addAll(visit(ctx.qualified_identification_variable()));
		} else if (ctx.qualified_identification_variable() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.TREAT(), false));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.qualified_identification_variable()));
			tokens.add(new JpaQueryParsingToken(ctx.AS()));
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
	public List<JpaQueryParsingToken> visitGeneral_identification_variable(
			JpqlParser.General_identification_variableContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.identification_variable() != null) {
			tokens.addAll(visit(ctx.identification_variable()));
		} else if (ctx.map_field_identification_variable() != null) {
			tokens.addAll(visit(ctx.map_field_identification_variable()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitGeneral_subpath(JpqlParser.General_subpathContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.simple_subpath() != null) {
			tokens.addAll(visit(ctx.simple_subpath()));
		} else if (ctx.treated_subpath() != null) {

			tokens.addAll(visit(ctx.treated_subpath()));

			ctx.single_valued_object_field().forEach(singleValuedObjectFieldContext -> {
				tokens.add(TOKEN_DOT);
				tokens.addAll(visit(singleValuedObjectFieldContext));
			});
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSimple_subpath(JpqlParser.Simple_subpathContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.general_identification_variable()));
		NOSPACE(tokens);

		ctx.single_valued_object_field().forEach(singleValuedObjectFieldContext -> {
			tokens.add(TOKEN_DOT);
			tokens.addAll(visit(singleValuedObjectFieldContext));
			NOSPACE(tokens);
		});
		SPACE(tokens);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitTreated_subpath(JpqlParser.Treated_subpathContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.TREAT(), false));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.general_subpath()));
		SPACE(tokens);
		tokens.add(new JpaQueryParsingToken(ctx.AS()));
		tokens.addAll(visit(ctx.subtype()));
		NOSPACE(tokens);
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitState_field_path_expression(
			JpqlParser.State_field_path_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.general_subpath()));
		NOSPACE(tokens);
		tokens.add(TOKEN_DOT);
		tokens.addAll(visit(ctx.state_field()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitState_valued_path_expression(
			JpqlParser.State_valued_path_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.state_field_path_expression() != null) {
			tokens.addAll(visit(ctx.state_field_path_expression()));
		} else if (ctx.general_identification_variable() != null) {
			tokens.addAll(visit(ctx.general_identification_variable()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSingle_valued_object_path_expression(
			JpqlParser.Single_valued_object_path_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.general_subpath()));
		NOSPACE(tokens);
		tokens.add(TOKEN_DOT);
		tokens.addAll(visit(ctx.single_valued_object_field()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitCollection_valued_path_expression(
			JpqlParser.Collection_valued_path_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.general_subpath()));
		NOSPACE(tokens);
		tokens.add(TOKEN_DOT);
		tokens.addAll(visit(ctx.collection_value_field()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitUpdate_clause(JpqlParser.Update_clauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.UPDATE()));
		tokens.addAll(visit(ctx.entity_name()));

		if (ctx.AS() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.AS()));
		}
		if (ctx.identification_variable() != null) {
			tokens.addAll(visit(ctx.identification_variable()));
		}

		tokens.add(new JpaQueryParsingToken(ctx.SET()));

		ctx.update_item().forEach(updateItemContext -> {
			tokens.addAll(visit(updateItemContext));
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitUpdate_item(JpqlParser.Update_itemContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.identification_variable() != null) {
			tokens.addAll(visit(ctx.identification_variable()));
			NOSPACE(tokens);
			tokens.add(TOKEN_DOT);
		}

		ctx.single_valued_embeddable_object_field().forEach(singleValuedEmbeddableObjectFieldContext -> {
			tokens.addAll(visit(singleValuedEmbeddableObjectFieldContext));
			NOSPACE(tokens);
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
	public List<JpaQueryParsingToken> visitNew_value(JpqlParser.New_valueContext ctx) {

		if (ctx.scalar_expression() != null) {
			return visit(ctx.scalar_expression());
		} else if (ctx.simple_entity_expression() != null) {
			return visit(ctx.simple_entity_expression());
		} else if (ctx.NULL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.NULL()));
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitDelete_clause(JpqlParser.Delete_clauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.DELETE()));
		tokens.add(new JpaQueryParsingToken(ctx.FROM()));
		tokens.addAll(visit(ctx.entity_name()));
		if (ctx.AS() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.AS()));
		}
		if (ctx.identification_variable() != null) {
			tokens.addAll(visit(ctx.identification_variable()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSelect_clause(JpqlParser.Select_clauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.SELECT()));

		if (ctx.DISTINCT() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.DISTINCT()));
		}

		ctx.select_item().forEach(selectItemContext -> {
			tokens.addAll(visit(selectItemContext));
			NOSPACE(tokens);
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);
		SPACE(tokens);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSelect_item(JpqlParser.Select_itemContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.select_expression()));
		SPACE(tokens);

		if (ctx.AS() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.AS()));
		}

		if (ctx.result_variable() != null) {
			tokens.addAll(visit(ctx.result_variable()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSelect_expression(JpqlParser.Select_expressionContext ctx) {

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

				List<JpaQueryParsingToken> tokens = new ArrayList<>();

				tokens.add(new JpaQueryParsingToken(ctx.OBJECT(), false));
				tokens.add(TOKEN_OPEN_PAREN);
				tokens.addAll(visit(ctx.identification_variable()));
				NOSPACE(tokens);
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
	public List<JpaQueryParsingToken> visitConstructor_expression(JpqlParser.Constructor_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.NEW()));
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
	public List<JpaQueryParsingToken> visitConstructor_item(JpqlParser.Constructor_itemContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

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
	public List<JpaQueryParsingToken> visitAggregate_expression(JpqlParser.Aggregate_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.AVG() != null || ctx.MAX() != null || ctx.MIN() != null || ctx.SUM() != null) {

			if (ctx.AVG() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.AVG(), false));
			}
			if (ctx.MAX() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.MAX(), false));
			}
			if (ctx.MIN() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.MIN(), false));
			}
			if (ctx.SUM() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.SUM(), false));
			}

			tokens.add(TOKEN_OPEN_PAREN);

			if (ctx.DISTINCT() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.DISTINCT()));
			}

			tokens.addAll(visit(ctx.state_valued_path_expression()));
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.COUNT() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.COUNT(), false));
			tokens.add(TOKEN_OPEN_PAREN);
			if (ctx.DISTINCT() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.DISTINCT()));
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
	public List<JpaQueryParsingToken> visitWhere_clause(JpqlParser.Where_clauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.WHERE(), true));
		tokens.addAll(visit(ctx.conditional_expression()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitGroupby_clause(JpqlParser.Groupby_clauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.GROUP()));
		tokens.add(new JpaQueryParsingToken(ctx.BY()));
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
	public List<JpaQueryParsingToken> visitGroupby_item(JpqlParser.Groupby_itemContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.single_valued_path_expression() != null) {
			tokens.addAll(visit(ctx.single_valued_path_expression()));
		} else if (ctx.identification_variable() != null) {
			tokens.addAll(visit(ctx.identification_variable()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitHaving_clause(JpqlParser.Having_clauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.HAVING()));
		tokens.addAll(visit(ctx.conditional_expression()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitOrderby_clause(JpqlParser.Orderby_clauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.ORDER()));
		tokens.add(new JpaQueryParsingToken(ctx.BY()));

		ctx.orderby_item().forEach(orderbyItemContext -> {
			tokens.addAll(visit(orderbyItemContext));
			NOSPACE(tokens);
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitOrderby_item(JpqlParser.Orderby_itemContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.state_field_path_expression() != null) {
			tokens.addAll(visit(ctx.state_field_path_expression()));
		} else if (ctx.general_identification_variable() != null) {
			tokens.addAll(visit(ctx.general_identification_variable()));
		} else if (ctx.result_variable() != null) {
			tokens.addAll(visit(ctx.result_variable()));
		}

		if (ctx.ASC() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.ASC()));
		}
		if (ctx.DESC() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.DESC()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSubquery(JpqlParser.SubqueryContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

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
	public List<JpaQueryParsingToken> visitSubquery_from_clause(JpqlParser.Subquery_from_clauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.FROM()));
		ctx.subselect_identification_variable_declaration().forEach(subselectIdentificationVariableDeclarationContext -> {
			tokens.addAll(visit(subselectIdentificationVariableDeclarationContext));
			NOSPACE(tokens);
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);
		SPACE(tokens);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSubselect_identification_variable_declaration(
			JpqlParser.Subselect_identification_variable_declarationContext ctx) {
		return super.visitSubselect_identification_variable_declaration(ctx);
	}

	@Override
	public List<JpaQueryParsingToken> visitDerived_path_expression(JpqlParser.Derived_path_expressionContext ctx) {
		return super.visitDerived_path_expression(ctx);
	}

	@Override
	public List<JpaQueryParsingToken> visitGeneral_derived_path(JpqlParser.General_derived_pathContext ctx) {
		return super.visitGeneral_derived_path(ctx);
	}

	@Override
	public List<JpaQueryParsingToken> visitSimple_derived_path(JpqlParser.Simple_derived_pathContext ctx) {
		return super.visitSimple_derived_path(ctx);
	}

	@Override
	public List<JpaQueryParsingToken> visitTreated_derived_path(JpqlParser.Treated_derived_pathContext ctx) {
		return super.visitTreated_derived_path(ctx);
	}

	@Override
	public List<JpaQueryParsingToken> visitDerived_collection_member_declaration(
			JpqlParser.Derived_collection_member_declarationContext ctx) {
		return super.visitDerived_collection_member_declaration(ctx);
	}

	@Override
	public List<JpaQueryParsingToken> visitSimple_select_clause(JpqlParser.Simple_select_clauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.SELECT()));
		if (ctx.DISTINCT() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.DISTINCT()));
		}
		tokens.addAll(visit(ctx.simple_select_expression()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSimple_select_expression(JpqlParser.Simple_select_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

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
	public List<JpaQueryParsingToken> visitScalar_expression(JpqlParser.Scalar_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

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
	public List<JpaQueryParsingToken> visitConditional_expression(JpqlParser.Conditional_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.conditional_expression() != null) {
			tokens.addAll(visit(ctx.conditional_expression()));
			tokens.add(new JpaQueryParsingToken(ctx.OR()));
			tokens.addAll(visit(ctx.conditional_term()));
		} else {
			tokens.addAll(visit(ctx.conditional_term()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitConditional_term(JpqlParser.Conditional_termContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.conditional_term() != null) {
			tokens.addAll(visit(ctx.conditional_term()));
			tokens.add(new JpaQueryParsingToken(ctx.AND()));
			tokens.addAll(visit(ctx.conditional_factor()));
		} else {
			tokens.addAll(visit(ctx.conditional_factor()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitConditional_factor(JpqlParser.Conditional_factorContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.NOT() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.NOT()));
		}

		JpqlParser.Conditional_primaryContext conditionalPrimary = ctx.conditional_primary();
		List<JpaQueryParsingToken> visitedConditionalPrimary = visit(conditionalPrimary);
		tokens.addAll(visitedConditionalPrimary);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitConditional_primary(JpqlParser.Conditional_primaryContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

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
	public List<JpaQueryParsingToken> visitSimple_cond_expression(JpqlParser.Simple_cond_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

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
	public List<JpaQueryParsingToken> visitBetween_expression(JpqlParser.Between_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.arithmetic_expression(0) != null) {

			tokens.addAll(visit(ctx.arithmetic_expression(0)));

			if (ctx.NOT() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.NOT()));
			}

			tokens.add(new JpaQueryParsingToken(ctx.BETWEEN()));
			tokens.addAll(visit(ctx.arithmetic_expression(1)));
			tokens.add(new JpaQueryParsingToken(ctx.AND()));
			tokens.addAll(visit(ctx.arithmetic_expression(2)));

		} else if (ctx.string_expression(0) != null) {

			tokens.addAll(visit(ctx.string_expression(0)));

			if (ctx.NOT() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.NOT()));
			}

			tokens.add(new JpaQueryParsingToken(ctx.BETWEEN()));
			tokens.addAll(visit(ctx.string_expression(1)));
			tokens.add(new JpaQueryParsingToken(ctx.AND()));
			tokens.addAll(visit(ctx.string_expression(2)));

		} else if (ctx.datetime_expression(0) != null) {

			tokens.addAll(visit(ctx.datetime_expression(0)));

			if (ctx.NOT() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.NOT()));
			}

			tokens.add(new JpaQueryParsingToken(ctx.BETWEEN()));
			tokens.addAll(visit(ctx.datetime_expression(1)));
			tokens.add(new JpaQueryParsingToken(ctx.AND()));
			tokens.addAll(visit(ctx.datetime_expression(2)));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitIn_expression(JpqlParser.In_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.state_valued_path_expression() != null) {
			tokens.addAll(visit(ctx.state_valued_path_expression()));
		}
		if (ctx.type_discriminator() != null) {
			tokens.addAll(visit(ctx.type_discriminator()));
		}
		if (ctx.NOT() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.NOT()));
		}
		if (ctx.IN() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.IN()));
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
	public List<JpaQueryParsingToken> visitIn_item(JpqlParser.In_itemContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.literal() != null) {
			tokens.addAll(visit(ctx.literal()));
		} else if (ctx.single_valued_input_parameter() != null) {
			tokens.addAll(visit(ctx.single_valued_input_parameter()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitLike_expression(JpqlParser.Like_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.string_expression()));
		if (ctx.NOT() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.NOT()));
		}
		tokens.add(new JpaQueryParsingToken(ctx.LIKE()));
		tokens.addAll(visit(ctx.pattern_value()));

		if (ctx.ESCAPE() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.ESCAPE()));
			tokens.addAll(visit(ctx.escape_character()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitNull_comparison_expression(JpqlParser.Null_comparison_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.single_valued_path_expression() != null) {
			tokens.addAll(visit(ctx.single_valued_path_expression()));
		} else if (ctx.input_parameter() != null) {
			tokens.addAll(visit(ctx.input_parameter()));
		}

		tokens.add(new JpaQueryParsingToken(ctx.IS()));
		if (ctx.NOT() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.NOT()));
		}
		tokens.add(new JpaQueryParsingToken(ctx.NULL()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitEmpty_collection_comparison_expression(
			JpqlParser.Empty_collection_comparison_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.collection_valued_path_expression()));
		tokens.add(new JpaQueryParsingToken(ctx.IS()));
		if (ctx.NOT() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.NOT()));
		}
		tokens.add(new JpaQueryParsingToken(ctx.EMPTY()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitCollection_member_expression(
			JpqlParser.Collection_member_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.entity_or_value_expression()));
		if (ctx.NOT() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.NOT()));
		}
		tokens.add(new JpaQueryParsingToken(ctx.MEMBER()));
		if (ctx.OF() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.OF()));
		}
		tokens.addAll(visit(ctx.collection_valued_path_expression()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitEntity_or_value_expression(JpqlParser.Entity_or_value_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

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
	public List<JpaQueryParsingToken> visitSimple_entity_or_value_expression(
			JpqlParser.Simple_entity_or_value_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

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
	public List<JpaQueryParsingToken> visitExists_expression(JpqlParser.Exists_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.NOT() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.NOT()));
		}
		tokens.add(new JpaQueryParsingToken(ctx.EXISTS()));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.subquery()));
		NOSPACE(tokens);
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitAll_or_any_expression(JpqlParser.All_or_any_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.ALL() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.ALL()));
		} else if (ctx.ANY() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.ANY()));
		} else if (ctx.SOME() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.SOME()));
		}

		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.subquery()));
		NOSPACE(tokens);
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitComparison_expression(JpqlParser.Comparison_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

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
			tokens.add(new JpaQueryParsingToken(ctx.op));

			if (ctx.boolean_expression(1) != null) {
				tokens.addAll(visit(ctx.boolean_expression(1)));
			} else {
				tokens.addAll(visit(ctx.all_or_any_expression()));
			}
		} else if (!ctx.enum_expression().isEmpty()) {

			tokens.addAll(visit(ctx.enum_expression(0)));
			tokens.add(new JpaQueryParsingToken(ctx.op));

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
			tokens.add(new JpaQueryParsingToken(ctx.op));

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
			tokens.add(new JpaQueryParsingToken(ctx.op));
			tokens.addAll(visit(ctx.entity_type_expression(1)));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitComparison_operator(JpqlParser.Comparison_operatorContext ctx) {
		return List.of(new JpaQueryParsingToken(ctx.op));
	}

	@Override
	public List<JpaQueryParsingToken> visitArithmetic_expression(JpqlParser.Arithmetic_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.arithmetic_expression() != null) {

			tokens.addAll(visit(ctx.arithmetic_expression()));
			tokens.add(new JpaQueryParsingToken(ctx.op));
			tokens.addAll(visit(ctx.arithmetic_term()));

		} else {
			tokens.addAll(visit(ctx.arithmetic_term()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitArithmetic_term(JpqlParser.Arithmetic_termContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.arithmetic_term() != null) {

			tokens.addAll(visit(ctx.arithmetic_term()));
			NOSPACE(tokens);
			tokens.add(new JpaQueryParsingToken(ctx.op, false));
			tokens.addAll(visit(ctx.arithmetic_factor()));
		} else {
			tokens.addAll(visit(ctx.arithmetic_factor()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitArithmetic_factor(JpqlParser.Arithmetic_factorContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.op != null) {
			tokens.add(new JpaQueryParsingToken(ctx.op));
		}
		tokens.addAll(visit(ctx.arithmetic_primary()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitArithmetic_primary(JpqlParser.Arithmetic_primaryContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

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
	public List<JpaQueryParsingToken> visitString_expression(JpqlParser.String_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

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
	public List<JpaQueryParsingToken> visitDatetime_expression(JpqlParser.Datetime_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

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
	public List<JpaQueryParsingToken> visitBoolean_expression(JpqlParser.Boolean_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

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
	public List<JpaQueryParsingToken> visitEnum_expression(JpqlParser.Enum_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

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
	public List<JpaQueryParsingToken> visitEntity_expression(JpqlParser.Entity_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.single_valued_object_path_expression() != null) {
			tokens.addAll(visit(ctx.single_valued_object_path_expression()));
		} else if (ctx.simple_entity_expression() != null) {
			tokens.addAll(visit(ctx.simple_entity_expression()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSimple_entity_expression(JpqlParser.Simple_entity_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.identification_variable() != null) {
			tokens.addAll(visit(ctx.identification_variable()));
		} else if (ctx.input_parameter() != null) {
			tokens.addAll(visit(ctx.input_parameter()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitEntity_type_expression(JpqlParser.Entity_type_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

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
	public List<JpaQueryParsingToken> visitType_discriminator(JpqlParser.Type_discriminatorContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.TYPE(), false));
		tokens.add(TOKEN_OPEN_PAREN);

		if (ctx.general_identification_variable() != null) {
			tokens.addAll(visit(ctx.general_identification_variable()));
		} else if (ctx.single_valued_object_path_expression() != null) {
			tokens.addAll(visit(ctx.single_valued_object_path_expression()));
		} else if (ctx.input_parameter() != null) {
			tokens.addAll(visit(ctx.input_parameter()));
		}
		NOSPACE(tokens);
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitFunctions_returning_numerics(
			JpqlParser.Functions_returning_numericsContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.LENGTH() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.LENGTH(), false));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.string_expression(0)));
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.LOCATE() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.LOCATE(), false));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.string_expression(0)));
			NOSPACE(tokens);
			tokens.add(TOKEN_COMMA);
			tokens.addAll(visit(ctx.string_expression(1)));
			NOSPACE(tokens);
			if (ctx.arithmetic_expression() != null) {
				tokens.add(TOKEN_COMMA);
				tokens.addAll(visit(ctx.arithmetic_expression(0)));
				NOSPACE(tokens);
			}
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.ABS() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.ABS(), false));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.arithmetic_expression(0)));
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.CEILING() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.CEILING(), false));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.arithmetic_expression(0)));
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.EXP() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.EXP(), false));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.arithmetic_expression(0)));
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.FLOOR() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.FLOOR(), false));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.arithmetic_expression(0)));
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.LN() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.LN(), false));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.arithmetic_expression(0)));
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.SIGN() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.SIGN(), false));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.arithmetic_expression(0)));
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.SQRT() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.SQRT(), false));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.arithmetic_expression(0)));
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.MOD() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.MOD(), false));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.arithmetic_expression(0)));
			NOSPACE(tokens);
			tokens.add(TOKEN_COMMA);
			tokens.addAll(visit(ctx.arithmetic_expression(1)));
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.POWER() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.POWER(), false));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.arithmetic_expression(0)));
			NOSPACE(tokens);
			tokens.add(TOKEN_COMMA);
			tokens.addAll(visit(ctx.arithmetic_expression(1)));
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.ROUND() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.ROUND(), false));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.arithmetic_expression(0)));
			NOSPACE(tokens);
			tokens.add(TOKEN_COMMA);
			tokens.addAll(visit(ctx.arithmetic_expression(1)));
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.SIZE() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.SIZE(), false));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.collection_valued_path_expression()));
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.INDEX() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.INDEX(), false));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.identification_variable()));
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitFunctions_returning_datetime(
			JpqlParser.Functions_returning_datetimeContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.CURRENT_DATE() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.CURRENT_DATE()));
		} else if (ctx.CURRENT_TIME() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.CURRENT_TIME()));
		} else if (ctx.CURRENT_TIMESTAMP() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.CURRENT_TIMESTAMP()));
		} else if (ctx.LOCAL() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.LOCAL()));

			if (ctx.DATE() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.DATE()));
			} else if (ctx.TIME() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.TIME()));
			} else if (ctx.DATETIME() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.DATETIME()));
			}
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitFunctions_returning_strings(
			JpqlParser.Functions_returning_stringsContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.CONCAT() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.CONCAT(), false));
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

			tokens.add(new JpaQueryParsingToken(ctx.SUBSTRING(), false));
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

			tokens.add(new JpaQueryParsingToken(ctx.TRIM(), false));
			tokens.add(TOKEN_OPEN_PAREN);
			if (ctx.trim_specification() != null) {
				tokens.addAll(visit(ctx.trim_specification()));
			}
			if (ctx.trim_character() != null) {
				tokens.addAll(visit(ctx.trim_character()));
			}
			if (ctx.FROM() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.FROM()));
			}
			tokens.addAll(visit(ctx.string_expression(0)));
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.LOWER() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.LOWER(), false));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.string_expression(0)));
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.UPPER() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.UPPER(), false));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.string_expression(0)));
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitTrim_specification(JpqlParser.Trim_specificationContext ctx) {

		if (ctx.LEADING() != null) {
			return List.of(new JpaQueryParsingToken(ctx.LEADING()));
		} else if (ctx.TRAILING() != null) {
			return List.of(new JpaQueryParsingToken(ctx.TRAILING()));
		} else {
			return List.of(new JpaQueryParsingToken(ctx.BOTH()));
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitFunction_invocation(JpqlParser.Function_invocationContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.FUNCTION(), false));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.function_name()));
		NOSPACE(tokens);
		ctx.function_arg().forEach(functionArgContext -> {
			tokens.add(TOKEN_COMMA);
			tokens.addAll(visit(functionArgContext));
			NOSPACE(tokens);
		});
		NOSPACE(tokens);
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitExtract_datetime_field(JpqlParser.Extract_datetime_fieldContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.EXTRACT()));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.datetime_field()));
		tokens.add(new JpaQueryParsingToken(ctx.FROM()));
		tokens.addAll(visit(ctx.datetime_expression()));
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitDatetime_field(JpqlParser.Datetime_fieldContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public List<JpaQueryParsingToken> visitExtract_datetime_part(JpqlParser.Extract_datetime_partContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.EXTRACT()));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.datetime_part()));
		tokens.add(new JpaQueryParsingToken(ctx.FROM()));
		tokens.addAll(visit(ctx.datetime_expression()));
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitDatetime_part(JpqlParser.Datetime_partContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public List<JpaQueryParsingToken> visitFunction_arg(JpqlParser.Function_argContext ctx) {

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
	public List<JpaQueryParsingToken> visitCase_expression(JpqlParser.Case_expressionContext ctx) {

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
	public List<JpaQueryParsingToken> visitGeneral_case_expression(JpqlParser.General_case_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.CASE()));

		ctx.when_clause().forEach(whenClauseContext -> {
			tokens.addAll(visit(whenClauseContext));
		});

		tokens.add(new JpaQueryParsingToken(ctx.ELSE()));
		tokens.addAll(visit(ctx.scalar_expression()));
		tokens.add(new JpaQueryParsingToken(ctx.END()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitWhen_clause(JpqlParser.When_clauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.WHEN()));
		tokens.addAll(visit(ctx.conditional_expression()));
		tokens.add(new JpaQueryParsingToken(ctx.THEN()));
		tokens.addAll(visit(ctx.scalar_expression()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSimple_case_expression(JpqlParser.Simple_case_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.CASE()));
		tokens.addAll(visit(ctx.case_operand()));

		ctx.simple_when_clause().forEach(simpleWhenClauseContext -> {
			tokens.addAll(visit(simpleWhenClauseContext));
		});

		tokens.add(new JpaQueryParsingToken(ctx.ELSE()));
		tokens.addAll(visit(ctx.scalar_expression()));
		tokens.add(new JpaQueryParsingToken(ctx.END()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitCase_operand(JpqlParser.Case_operandContext ctx) {

		if (ctx.state_valued_path_expression() != null) {
			return visit(ctx.state_valued_path_expression());
		} else {
			return visit(ctx.type_discriminator());
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitSimple_when_clause(JpqlParser.Simple_when_clauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.WHEN()));
		tokens.addAll(visit(ctx.scalar_expression(0)));
		tokens.add(new JpaQueryParsingToken(ctx.THEN()));
		tokens.addAll(visit(ctx.scalar_expression(1)));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitCoalesce_expression(JpqlParser.Coalesce_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.COALESCE(), false));
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
	public List<JpaQueryParsingToken> visitNullif_expression(JpqlParser.Nullif_expressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.NULLIF()));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.scalar_expression(0)));
		tokens.add(TOKEN_COMMA);
		tokens.addAll(visit(ctx.scalar_expression(1)));
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitTrim_character(JpqlParser.Trim_characterContext ctx) {

		if (ctx.CHARACTER() != null) {
			return List.of(new JpaQueryParsingToken(ctx.CHARACTER()));
		} else if (ctx.character_valued_input_parameter() != null) {
			return visit(ctx.character_valued_input_parameter());
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitIdentification_variable(JpqlParser.Identification_variableContext ctx) {

		if (ctx.IDENTIFICATION_VARIABLE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.IDENTIFICATION_VARIABLE()));
		} else if (ctx.f != null) {
			return List.of(new JpaQueryParsingToken(ctx.f));
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitConstructor_name(JpqlParser.Constructor_nameContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.state_field_path_expression()));
		NOSPACE(tokens);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitLiteral(JpqlParser.LiteralContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.STRINGLITERAL() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.STRINGLITERAL()));
		} else if (ctx.INTLITERAL() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.INTLITERAL()));
		} else if (ctx.FLOATLITERAL() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.FLOATLITERAL()));
		} else if (ctx.boolean_literal() != null) {
			tokens.addAll(visit(ctx.boolean_literal()));
		} else if (ctx.entity_type_literal() != null) {
			tokens.addAll(visit(ctx.entity_type_literal()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitInput_parameter(JpqlParser.Input_parameterContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.INTLITERAL() != null) {

			tokens.add(TOKEN_QUESTION_MARK);
			tokens.add(new JpaQueryParsingToken(ctx.INTLITERAL()));
		} else if (ctx.identification_variable() != null) {

			tokens.add(TOKEN_COLON);
			tokens.addAll(visit(ctx.identification_variable()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitPattern_value(JpqlParser.Pattern_valueContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.string_expression()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitDate_time_timestamp_literal(
			JpqlParser.Date_time_timestamp_literalContext ctx) {
		return List.of(new JpaQueryParsingToken(ctx.STRINGLITERAL()));
	}

	@Override
	public List<JpaQueryParsingToken> visitEntity_type_literal(JpqlParser.Entity_type_literalContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public List<JpaQueryParsingToken> visitEscape_character(JpqlParser.Escape_characterContext ctx) {
		return List.of(new JpaQueryParsingToken(ctx.CHARACTER()));
	}

	@Override
	public List<JpaQueryParsingToken> visitNumeric_literal(JpqlParser.Numeric_literalContext ctx) {

		if (ctx.INTLITERAL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.INTLITERAL()));
		} else if (ctx.FLOATLITERAL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.FLOATLITERAL()));
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitBoolean_literal(JpqlParser.Boolean_literalContext ctx) {

		if (ctx.TRUE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.TRUE()));
		} else if (ctx.FALSE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.FALSE()));
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitEnum_literal(JpqlParser.Enum_literalContext ctx) {
		return visit(ctx.state_field_path_expression());
	}

	@Override
	public List<JpaQueryParsingToken> visitString_literal(JpqlParser.String_literalContext ctx) {

		if (ctx.CHARACTER() != null) {
			return List.of(new JpaQueryParsingToken(ctx.CHARACTER()));
		} else if (ctx.STRINGLITERAL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.STRINGLITERAL()));
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitSingle_valued_embeddable_object_field(
			JpqlParser.Single_valued_embeddable_object_fieldContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public List<JpaQueryParsingToken> visitSubtype(JpqlParser.SubtypeContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public List<JpaQueryParsingToken> visitCollection_valued_field(JpqlParser.Collection_valued_fieldContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public List<JpaQueryParsingToken> visitSingle_valued_object_field(JpqlParser.Single_valued_object_fieldContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public List<JpaQueryParsingToken> visitState_field(JpqlParser.State_fieldContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public List<JpaQueryParsingToken> visitCollection_value_field(JpqlParser.Collection_value_fieldContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public List<JpaQueryParsingToken> visitEntity_name(JpqlParser.Entity_nameContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		ctx.identification_variable().forEach(identificationVariableContext -> {
			tokens.addAll(visit(identificationVariableContext));
			NOSPACE(tokens);
			tokens.add(TOKEN_DOT);
		});
		CLIP(tokens);
		SPACE(tokens);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitResult_variable(JpqlParser.Result_variableContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public List<JpaQueryParsingToken> visitSuperquery_identification_variable(
			JpqlParser.Superquery_identification_variableContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public List<JpaQueryParsingToken> visitCollection_valued_input_parameter(
			JpqlParser.Collection_valued_input_parameterContext ctx) {
		return visit(ctx.input_parameter());
	}

	@Override
	public List<JpaQueryParsingToken> visitSingle_valued_input_parameter(
			JpqlParser.Single_valued_input_parameterContext ctx) {
		return visit(ctx.input_parameter());
	}

	@Override
	public List<JpaQueryParsingToken> visitFunction_name(JpqlParser.Function_nameContext ctx) {
		return visit(ctx.string_literal());
	}

	@Override
	public List<JpaQueryParsingToken> visitCharacter_valued_input_parameter(
			JpqlParser.Character_valued_input_parameterContext ctx) {

		if (ctx.CHARACTER() != null) {
			return List.of(new JpaQueryParsingToken(ctx.CHARACTER()));
		} else if (ctx.input_parameter() != null) {
			return visit(ctx.input_parameter());
		} else {
			return List.of();
		}
	}
}
