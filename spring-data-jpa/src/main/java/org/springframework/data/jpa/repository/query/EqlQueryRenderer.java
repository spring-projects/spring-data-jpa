/*
 * Copyright 2023-2024 the original author or authors.
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

import static org.springframework.data.jpa.repository.query.QueryTokens.*;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.tree.ParseTree;

import org.springframework.data.jpa.repository.query.QueryRenderer.QueryRendererBuilder;

/**
 * An ANTLR {@link org.antlr.v4.runtime.tree.ParseTreeVisitor} that renders an EQL query without making any changes.
 *
 * @author Greg Turnquist
 * @author Christoph Strobl
 * @since 3.2
 */
@SuppressWarnings({ "ConstantConditions", "DuplicatedCode" })
class EqlQueryRenderer extends EqlBaseVisitor<QueryTokenStream> {

	@Override
	public QueryTokenStream visitStart(EqlParser.StartContext ctx) {
		return visit(ctx.ql_statement());
	}

	@Override
	public QueryTokenStream visitQl_statement(EqlParser.Ql_statementContext ctx) {

		if (ctx.select_statement() != null) {
			return visit(ctx.select_statement());
		} else if (ctx.update_statement() != null) {
			return visit(ctx.update_statement());
		} else if (ctx.delete_statement() != null) {
			return visit(ctx.delete_statement());
		} else {
			return QueryRenderer.builder();
		}
	}

	@Override
	public QueryTokenStream visitSelect_statement(EqlParser.Select_statementContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.select_clause()));
		builder.appendExpression(visit(ctx.from_clause()));

		if (ctx.where_clause() != null) {
			builder.appendExpression(visit(ctx.where_clause()));
		}

		if (ctx.groupby_clause() != null) {
			builder.appendExpression(visit(ctx.groupby_clause()));
		}

		if (ctx.having_clause() != null) {
			builder.appendExpression(visit(ctx.having_clause()));
		}

		if (ctx.orderby_clause() != null) {
			builder.appendExpression(visit(ctx.orderby_clause()));
		}

		for (int i = 0; i < ctx.setOperator().size(); i++) {

			builder.appendExpression(visit(ctx.setOperator(i)));
			builder.appendExpression(visit(ctx.select_statement(i)));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitSetOperator(EqlParser.SetOperatorContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.UNION() != null) {
			builder.append(QueryTokens.expression(ctx.UNION()));
		} else if (ctx.INTERSECT() != null) {
			builder.append(QueryTokens.expression(ctx.INTERSECT()));
		} else if (ctx.EXCEPT() != null) {
			builder.append(QueryTokens.expression(ctx.EXCEPT()));
		}

		if (ctx.ALL() != null) {
			builder.append(QueryTokens.expression(ctx.ALL()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitUpdate_statement(EqlParser.Update_statementContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.update_clause()));

		if (ctx.where_clause() != null) {
			builder.appendExpression(visit(ctx.where_clause()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitDelete_statement(EqlParser.Delete_statementContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.delete_clause()));

		if (ctx.where_clause() != null) {
			builder.appendExpression(visit(ctx.where_clause()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitFrom_clause(EqlParser.From_clauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.FROM()));
		builder.appendInline(visit(ctx.identification_variable_declaration()));

		if (!ctx.identificationVariableDeclarationOrCollectionMemberDeclaration().isEmpty()) {
			builder.append(TOKEN_COMMA);
		}

		builder.appendExpression(QueryTokenStream
				.concat(ctx.identificationVariableDeclarationOrCollectionMemberDeclaration(), this::visit, TOKEN_COMMA));

		return builder;
	}

	@Override
	public QueryTokenStream visitIdentificationVariableDeclarationOrCollectionMemberDeclaration(
			EqlParser.IdentificationVariableDeclarationOrCollectionMemberDeclarationContext ctx) {

		if (ctx.identification_variable_declaration() != null) {
			return visit(ctx.identification_variable_declaration());
		} else if (ctx.collection_member_declaration() != null) {
			return visit(ctx.collection_member_declaration());
		} else if (ctx.subquery() != null) {

			QueryRendererBuilder nested = QueryRenderer.builder();
			nested.append(TOKEN_OPEN_PAREN);
			nested.appendInline(visit(ctx.subquery()));
			nested.append(TOKEN_CLOSE_PAREN);

			QueryRendererBuilder builder = QueryRenderer.builder();
			builder.appendExpression(nested);
			builder.appendExpression(visit(ctx.identification_variable()));

			return builder;
		} else {
			return QueryRenderer.builder();
		}
	}

	@Override
	public QueryTokenStream visitIdentification_variable_declaration(
			EqlParser.Identification_variable_declarationContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(visit(ctx.range_variable_declaration()));

		ctx.join().forEach(joinContext -> {
			builder.append(visit(joinContext));
		});
		ctx.fetch_join().forEach(fetchJoinContext -> {
			builder.append(visit(fetchJoinContext));
		});

		return builder;
	}

	@Override
	public QueryTokenStream visitRange_variable_declaration(EqlParser.Range_variable_declarationContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.entity_name() != null) {
			builder.appendExpression(visit(ctx.entity_name()));
		} else if (ctx.function_invocation() != null) {
			builder.appendExpression(visit(ctx.function_invocation()));
		}

		if (ctx.AS() != null) {
			builder.append(QueryTokens.expression(ctx.AS()));
		}

		builder.appendExpression(visit(ctx.identification_variable()));

		return builder;
	}

	@Override
	public QueryTokenStream visitJoin(EqlParser.JoinContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.join_spec()));
		builder.appendExpression(visit(ctx.join_association_path_expression()));

		if (ctx.AS() != null) {
			builder.append(QueryTokens.expression(ctx.AS()));
		}
		if (ctx.identification_variable() != null) {
			builder.appendExpression(visit(ctx.identification_variable()));
		}
		if (ctx.join_condition() != null) {
			builder.appendExpression(visit(ctx.join_condition()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitFetch_join(EqlParser.Fetch_joinContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.join_spec()));
		builder.append(QueryTokens.expression(ctx.FETCH()));
		builder.appendExpression(visit(ctx.join_association_path_expression()));

		if (ctx.AS() != null) {
			builder.append(QueryTokens.expression(ctx.AS()));
		}
		if (ctx.identification_variable() != null) {
			builder.appendExpression(visit(ctx.identification_variable()));
		}
		if (ctx.join_condition() != null) {
			builder.appendExpression(visit(ctx.join_condition()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitJoin_spec(EqlParser.Join_specContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.LEFT() != null) {
			builder.append(QueryTokens.expression(ctx.LEFT()));
		}
		if (ctx.OUTER() != null) {
			builder.append(QueryTokens.expression(ctx.OUTER()));
		}
		if (ctx.INNER() != null) {
			builder.append(QueryTokens.expression(ctx.INNER()));
		}
		if (ctx.JOIN() != null) {
			builder.append(QueryTokens.expression(ctx.JOIN()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitJoin_condition(EqlParser.Join_conditionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.ON()));
		builder.appendExpression(visit(ctx.conditional_expression()));

		return builder;
	}

	@Override
	public QueryTokenStream visitJoin_association_path_expression(
			EqlParser.Join_association_path_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.TREAT() == null) {

			if (ctx.join_collection_valued_path_expression() != null) {
				builder.appendExpression(visit(ctx.join_collection_valued_path_expression()));
			} else if (ctx.join_single_valued_path_expression() != null) {
				builder.appendExpression(visit(ctx.join_single_valued_path_expression()));
			}
		} else {
			if (ctx.join_collection_valued_path_expression() != null) {

				QueryRendererBuilder nested = QueryRenderer.builder();

				nested.append(QueryTokens.token(ctx.TREAT()));
				nested.append(TOKEN_OPEN_PAREN);
				nested.appendInline(visit(ctx.join_collection_valued_path_expression()));
				nested.append(QueryTokens.expression(ctx.AS()));
				nested.appendInline(visit(ctx.subtype()));
				nested.append(TOKEN_CLOSE_PAREN);

				builder.appendExpression(nested);
			} else if (ctx.join_single_valued_path_expression() != null) {

				QueryRendererBuilder nested = QueryRenderer.builder();

				nested.append(QueryTokens.token(ctx.TREAT()));
				nested.append(TOKEN_OPEN_PAREN);
				nested.appendInline(visit(ctx.join_single_valued_path_expression()));
				nested.append(QueryTokens.expression(ctx.AS()));
				nested.appendInline(visit(ctx.subtype()));
				nested.append(TOKEN_CLOSE_PAREN);

				builder.appendExpression(nested);
			}
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitJoin_collection_valued_path_expression(
			EqlParser.Join_collection_valued_path_expressionContext ctx) {

		List<ParseTree> items = new ArrayList<>(2 + ctx.single_valued_embeddable_object_field().size());
		if (ctx.identification_variable() != null) {
			items.add(ctx.identification_variable());
		}

		items.addAll(ctx.single_valued_embeddable_object_field());
		items.add(ctx.collection_valued_field());

		return QueryTokenStream.concat(items, this::visit, TOKEN_DOT);
	}

	@Override
	public QueryTokenStream visitJoin_single_valued_path_expression(
			EqlParser.Join_single_valued_path_expressionContext ctx) {

		List<ParseTree> items = new ArrayList<>(2 + ctx.single_valued_embeddable_object_field().size());
		if (ctx.identification_variable() != null) {
			items.add(ctx.identification_variable());
		}

		items.addAll(ctx.single_valued_embeddable_object_field());
		items.add(ctx.single_valued_object_field());

		return QueryTokenStream.concat(items, this::visit, TOKEN_DOT);
	}

	@Override
	public QueryTokenStream visitCollection_member_declaration(EqlParser.Collection_member_declarationContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.IN()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(visit(ctx.collection_valued_path_expression()));
		builder.append(TOKEN_CLOSE_PAREN);

		if (ctx.AS() != null) {
			builder.append(QueryTokens.expression(ctx.AS()));
		}

		builder.appendExpression(visit(ctx.identification_variable()));

		return builder;
	}

	@Override
	public QueryTokenStream visitQualified_identification_variable(
			EqlParser.Qualified_identification_variableContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.map_field_identification_variable() != null) {
			builder.append(visit(ctx.map_field_identification_variable()));
		} else if (ctx.identification_variable() != null) {

			builder.append(QueryTokens.expression(ctx.ENTRY()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.append(visit(ctx.identification_variable()));
			builder.append(TOKEN_CLOSE_PAREN);
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitMap_field_identification_variable(
			EqlParser.Map_field_identification_variableContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.KEY() != null) {

			builder.append(QueryTokens.token(ctx.KEY()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.identification_variable()));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.VALUE() != null) {

			builder.append(QueryTokens.token(ctx.VALUE()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.identification_variable()));
			builder.append(TOKEN_CLOSE_PAREN);
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitSingle_valued_path_expression(EqlParser.Single_valued_path_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.qualified_identification_variable() != null) {
			builder.append(visit(ctx.qualified_identification_variable()));
		} else if (ctx.qualified_identification_variable() != null) {

			builder.append(QueryTokens.token(ctx.TREAT()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.qualified_identification_variable()));
			builder.append(QueryTokens.expression(ctx.AS()));
			builder.appendInline(visit(ctx.subtype()));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.state_field_path_expression() != null) {
			builder.append(visit(ctx.state_field_path_expression()));
		} else if (ctx.single_valued_object_path_expression() != null) {
			builder.append(visit(ctx.single_valued_object_path_expression()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitGeneral_identification_variable(
			EqlParser.General_identification_variableContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.identification_variable() != null) {
			builder.append(visit(ctx.identification_variable()));
		} else if (ctx.map_field_identification_variable() != null) {
			builder.append(visit(ctx.map_field_identification_variable()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitGeneral_subpath(EqlParser.General_subpathContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.simple_subpath() != null) {
			builder.appendInline(visit(ctx.simple_subpath()));
		} else if (ctx.treated_subpath() != null) {

			builder.appendInline(visit(ctx.treated_subpath()));
			builder.appendInline(QueryTokenStream.concat(ctx.single_valued_object_field(), this::visit, TOKEN_DOT));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitSimple_subpath(EqlParser.Simple_subpathContext ctx) {

		List<ParseTree> items = new ArrayList<>(1 + ctx.single_valued_object_field().size());
		items.add(ctx.general_identification_variable());
		items.addAll(ctx.single_valued_object_field());

		return QueryTokenStream.concat(items, this::visit, TOKEN_DOT);
	}

	@Override
	public QueryTokenStream visitTreated_subpath(EqlParser.Treated_subpathContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.TREAT()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(visit(ctx.general_subpath()));
		builder.append(QueryTokens.expression(ctx.AS()));
		builder.appendInline(visit(ctx.subtype()));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitState_field_path_expression(EqlParser.State_field_path_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendInline(visit(ctx.general_subpath()));
		builder.append(TOKEN_DOT);
		builder.appendInline(visit(ctx.state_field()));

		return builder;
	}

	@Override
	public QueryTokenStream visitState_valued_path_expression(EqlParser.State_valued_path_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.state_field_path_expression() != null) {
			builder.append(visit(ctx.state_field_path_expression()));
		} else if (ctx.general_identification_variable() != null) {
			builder.append(visit(ctx.general_identification_variable()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitSingle_valued_object_path_expression(
			EqlParser.Single_valued_object_path_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendInline(visit(ctx.general_subpath()));
		builder.append(TOKEN_DOT);
		builder.appendInline(visit(ctx.single_valued_object_field()));

		return builder;
	}

	@Override
	public QueryTokenStream visitCollection_valued_path_expression(
			EqlParser.Collection_valued_path_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendInline(visit(ctx.general_subpath()));
		builder.append(TOKEN_DOT);
		builder.appendInline(visit(ctx.collection_value_field()));

		return builder;
	}

	@Override
	public QueryTokenStream visitUpdate_clause(EqlParser.Update_clauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.UPDATE()));
		builder.appendExpression(visit(ctx.entity_name()));

		if (ctx.AS() != null) {
			builder.append(QueryTokens.expression(ctx.AS()));
		}

		if (ctx.identification_variable() != null) {
			builder.appendExpression(visit(ctx.identification_variable()));
		}

		builder.append(QueryTokens.expression(ctx.SET()));
		builder.appendExpression(QueryTokenStream.concat(ctx.update_item(), this::visit, TOKEN_COMMA));

		return builder;
	}

	@Override
	public QueryTokenStream visitUpdate_item(EqlParser.Update_itemContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		List<ParseTree> items = new ArrayList<>(3 + ctx.single_valued_embeddable_object_field().size());
		if (ctx.identification_variable() != null) {
			items.add(ctx.identification_variable());
		}

		items.addAll(ctx.single_valued_embeddable_object_field());

		if (ctx.state_field() != null) {
			items.add(ctx.state_field());
		} else if (ctx.single_valued_object_field() != null) {
			items.add(ctx.single_valued_object_field());
		}

		builder.appendInline(QueryTokenStream.concat(items, this::visit, TOKEN_DOT));
		builder.append(TOKEN_EQUALS);
		builder.append(visit(ctx.new_value()));

		return builder;
	}

	@Override
	public QueryTokenStream visitNew_value(EqlParser.New_valueContext ctx) {

		if (ctx.scalar_expression() != null) {
			return visit(ctx.scalar_expression());
		} else if (ctx.simple_entity_expression() != null) {
			return visit(ctx.simple_entity_expression());
		} else if (ctx.NULL() != null) {
			return QueryRendererBuilder.from(QueryTokens.expression(ctx.NULL()));
		} else {
			return QueryRenderer.builder();
		}
	}

	@Override
	public QueryTokenStream visitDelete_clause(EqlParser.Delete_clauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.DELETE()));
		builder.append(QueryTokens.expression(ctx.FROM()));
		builder.appendExpression(visit(ctx.entity_name()));

		if (ctx.AS() != null) {
			builder.append(QueryTokens.expression(ctx.AS()));
		}
		if (ctx.identification_variable() != null) {
			builder.appendExpression(visit(ctx.identification_variable()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitSelect_clause(EqlParser.Select_clauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.SELECT()));

		if (ctx.DISTINCT() != null) {
			builder.append(QueryTokens.expression(ctx.DISTINCT()));
		}

		builder.appendExpression(QueryTokenStream.concat(ctx.select_item(), this::visit, TOKEN_COMMA));

		return builder;
	}

	@Override
	public QueryTokenStream visitSelect_item(EqlParser.Select_itemContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.select_expression()));

		if (ctx.AS() != null) {
			builder.append(QueryTokens.expression(ctx.AS()));
		}

		if (ctx.result_variable() != null) {
			builder.appendExpression(visit(ctx.result_variable()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitSelect_expression(EqlParser.Select_expressionContext ctx) {

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

				QueryRendererBuilder builder = QueryRenderer.builder();

				builder.append(QueryTokens.token(ctx.OBJECT()));
				builder.append(TOKEN_OPEN_PAREN);
				builder.appendInline(visit(ctx.identification_variable()));
				builder.append(TOKEN_CLOSE_PAREN);

				return builder;
			}
		} else if (ctx.constructor_expression() != null) {
			return visit(ctx.constructor_expression());
		} else {
			return QueryRenderer.builder();
		}
	}

	@Override
	public QueryTokenStream visitConstructor_expression(EqlParser.Constructor_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.NEW()));
		builder.append(visit(ctx.constructor_name()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(QueryTokenStream.concat(ctx.constructor_item(), this::visit, TOKEN_COMMA));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitConstructor_item(EqlParser.Constructor_itemContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.single_valued_path_expression() != null) {
			builder.append(visit(ctx.single_valued_path_expression()));
		} else if (ctx.scalar_expression() != null) {
			builder.append(visit(ctx.scalar_expression()));
		} else if (ctx.aggregate_expression() != null) {
			builder.append(visit(ctx.aggregate_expression()));
		} else if (ctx.identification_variable() != null) {
			builder.append(visit(ctx.identification_variable()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitAggregate_expression(EqlParser.Aggregate_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.AVG() != null || ctx.MAX() != null || ctx.MIN() != null || ctx.SUM() != null) {

			if (ctx.AVG() != null) {
				builder.append(QueryTokens.token(ctx.AVG()));
			}
			if (ctx.MAX() != null) {
				builder.append(QueryTokens.token(ctx.MAX()));
			}
			if (ctx.MIN() != null) {
				builder.append(QueryTokens.token(ctx.MIN()));
			}
			if (ctx.SUM() != null) {
				builder.append(QueryTokens.token(ctx.SUM()));
			}

			builder.append(TOKEN_OPEN_PAREN);

			if (ctx.DISTINCT() != null) {
				builder.append(QueryTokens.expression(ctx.DISTINCT()));
			}

			builder.appendInline(visit(ctx.state_valued_path_expression()));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.COUNT() != null) {

			builder.append(QueryTokens.token(ctx.COUNT()));
			builder.append(TOKEN_OPEN_PAREN);
			if (ctx.DISTINCT() != null) {
				builder.append(QueryTokens.expression(ctx.DISTINCT()));
			}
			if (ctx.identification_variable() != null) {
				builder.appendInline(visit(ctx.identification_variable()));
			} else if (ctx.state_valued_path_expression() != null) {
				builder.appendInline(visit(ctx.state_valued_path_expression()));
			} else if (ctx.single_valued_object_path_expression() != null) {
				builder.appendInline(visit(ctx.single_valued_object_path_expression()));
			}
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.function_invocation() != null) {
			builder.append(visit(ctx.function_invocation()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitWhere_clause(EqlParser.Where_clauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.WHERE()));
		builder.appendExpression(visit(ctx.conditional_expression()));

		return builder;
	}

	@Override
	public QueryTokenStream visitGroupby_clause(EqlParser.Groupby_clauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.GROUP()));
		builder.append(QueryTokens.expression(ctx.BY()));
		builder.appendExpression(QueryTokenStream.concat(ctx.groupby_item(), this::visit, TOKEN_COMMA));

		return builder;
	}

	@Override
	public QueryTokenStream visitGroupby_item(EqlParser.Groupby_itemContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.single_valued_path_expression() != null) {
			builder.append(visit(ctx.single_valued_path_expression()));
		} else if (ctx.identification_variable() != null) {
			builder.append(visit(ctx.identification_variable()));
		} else if (ctx.scalar_expression() != null) {
			builder.append(visit(ctx.scalar_expression()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitHaving_clause(EqlParser.Having_clauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.HAVING()));
		builder.appendExpression(visit(ctx.conditional_expression()));

		return builder;
	}

	@Override
	public QueryTokenStream visitOrderby_clause(EqlParser.Orderby_clauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.ORDER()));
		builder.append(QueryTokens.expression(ctx.BY()));
		builder.append(QueryTokenStream.concat(ctx.orderby_item(), this::visit, TOKEN_COMMA));

		return builder;
	}

	@Override
	public QueryTokenStream visitOrderby_item(EqlParser.Orderby_itemContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.state_field_path_expression() != null) {
			builder.append(visit(ctx.state_field_path_expression()));
		} else if (ctx.general_identification_variable() != null) {
			builder.append(visit(ctx.general_identification_variable()));
		} else if (ctx.result_variable() != null) {
			builder.append(visit(ctx.result_variable()));
		} else if (ctx.string_expression() != null) {
			builder.append(visit(ctx.string_expression()));
		} else if (ctx.scalar_expression() != null) {
			builder.append(visit(ctx.scalar_expression()));
		}

		if (ctx.ASC() != null) {
			builder.append(QueryTokens.expression(ctx.ASC()));
		}
		if (ctx.DESC() != null) {
			builder.append(QueryTokens.expression(ctx.DESC()));
		}

		if (ctx.nullsPrecedence() != null) {
			builder.appendExpression(visit(ctx.nullsPrecedence()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitNullsPrecedence(EqlParser.NullsPrecedenceContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(TOKEN_NULLS);

		if (ctx.FIRST() != null) {
			builder.append(TOKEN_FIRST);
		} else if (ctx.LAST() != null) {
			builder.append(TOKEN_LAST);
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitSubquery(EqlParser.SubqueryContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.simple_select_clause()));
		builder.appendExpression(visit(ctx.subquery_from_clause()));
		if (ctx.where_clause() != null) {
			builder.appendExpression(visit(ctx.where_clause()));
		}
		if (ctx.groupby_clause() != null) {
			builder.appendExpression(visit(ctx.groupby_clause()));
		}
		if (ctx.having_clause() != null) {
			builder.appendExpression(visit(ctx.having_clause()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitSubquery_from_clause(EqlParser.Subquery_from_clauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.FROM()));
		builder.appendExpression(
				QueryTokenStream.concat(ctx.subselect_identification_variable_declaration(), this::visit, TOKEN_COMMA));

		return builder;
	}

	@Override
	public QueryTokenStream visitSubselect_identification_variable_declaration(
			EqlParser.Subselect_identification_variable_declarationContext ctx) {
		return super.visitSubselect_identification_variable_declaration(ctx);
	}

	@Override
	public QueryTokenStream visitDerived_path_expression(EqlParser.Derived_path_expressionContext ctx) {
		return super.visitDerived_path_expression(ctx);
	}

	@Override
	public QueryTokenStream visitGeneral_derived_path(EqlParser.General_derived_pathContext ctx) {
		return super.visitGeneral_derived_path(ctx);
	}

	@Override
	public QueryTokenStream visitSimple_derived_path(EqlParser.Simple_derived_pathContext ctx) {
		return super.visitSimple_derived_path(ctx);
	}

	@Override
	public QueryTokenStream visitTreated_derived_path(EqlParser.Treated_derived_pathContext ctx) {
		return super.visitTreated_derived_path(ctx);
	}

	@Override
	public QueryTokenStream visitDerived_collection_member_declaration(
			EqlParser.Derived_collection_member_declarationContext ctx) {
		return super.visitDerived_collection_member_declaration(ctx);
	}

	@Override
	public QueryTokenStream visitSimple_select_clause(EqlParser.Simple_select_clauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.SELECT()));
		if (ctx.DISTINCT() != null) {
			builder.append(QueryTokens.expression(ctx.DISTINCT()));
		}
		builder.append(visit(ctx.simple_select_expression()));

		return builder;
	}

	@Override
	public QueryTokenStream visitSimple_select_expression(EqlParser.Simple_select_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.single_valued_path_expression() != null) {
			builder.append(visit(ctx.single_valued_path_expression()));
		} else if (ctx.scalar_expression() != null) {
			builder.append(visit(ctx.scalar_expression()));
		} else if (ctx.aggregate_expression() != null) {
			builder.append(visit(ctx.aggregate_expression()));
		} else if (ctx.identification_variable() != null) {
			builder.append(visit(ctx.identification_variable()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitScalar_expression(EqlParser.Scalar_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.arithmetic_expression() != null) {
			builder.append(visit(ctx.arithmetic_expression()));
		} else if (ctx.string_expression() != null) {
			builder.append(visit(ctx.string_expression()));
		} else if (ctx.enum_expression() != null) {
			builder.append(visit(ctx.enum_expression()));
		} else if (ctx.datetime_expression() != null) {
			builder.append(visit(ctx.datetime_expression()));
		} else if (ctx.boolean_expression() != null) {
			builder.append(visit(ctx.boolean_expression()));
		} else if (ctx.case_expression() != null) {
			builder.append(visit(ctx.case_expression()));
		} else if (ctx.entity_type_expression() != null) {
			builder.append(visit(ctx.entity_type_expression()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitConditional_expression(EqlParser.Conditional_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.conditional_expression() != null) {
			builder.append(visit(ctx.conditional_expression()));
			builder.append(QueryTokens.expression(ctx.OR()));
			builder.append(visit(ctx.conditional_term()));
		} else {
			builder.append(visit(ctx.conditional_term()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitConditional_term(EqlParser.Conditional_termContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.conditional_term() != null) {
			builder.append(visit(ctx.conditional_term()));
			builder.append(QueryTokens.expression(ctx.AND()));
			builder.append(visit(ctx.conditional_factor()));
		} else {
			builder.append(visit(ctx.conditional_factor()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitConditional_factor(EqlParser.Conditional_factorContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.NOT() != null) {
			builder.append(QueryTokens.expression(ctx.NOT()));
		}

		builder.append(visit(ctx.conditional_primary()));

		return builder;
	}

	@Override
	public QueryTokenStream visitConditional_primary(EqlParser.Conditional_primaryContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.simple_cond_expression() != null) {
			builder.append(visit(ctx.simple_cond_expression()));
		} else if (ctx.conditional_expression() != null) {

			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.conditional_expression()));
			builder.append(TOKEN_CLOSE_PAREN);
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitSimple_cond_expression(EqlParser.Simple_cond_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.comparison_expression() != null) {
			builder.append(visit(ctx.comparison_expression()));
		} else if (ctx.between_expression() != null) {
			builder.append(visit(ctx.between_expression()));
		} else if (ctx.in_expression() != null) {
			builder.append(visit(ctx.in_expression()));
		} else if (ctx.like_expression() != null) {
			builder.append(visit(ctx.like_expression()));
		} else if (ctx.null_comparison_expression() != null) {
			builder.append(visit(ctx.null_comparison_expression()));
		} else if (ctx.empty_collection_comparison_expression() != null) {
			builder.append(visit(ctx.empty_collection_comparison_expression()));
		} else if (ctx.collection_member_expression() != null) {
			builder.append(visit(ctx.collection_member_expression()));
		} else if (ctx.exists_expression() != null) {
			builder.append(visit(ctx.exists_expression()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitBetween_expression(EqlParser.Between_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.arithmetic_expression(0) != null) {

			builder.append(visit(ctx.arithmetic_expression(0)));

			if (ctx.NOT() != null) {
				builder.append(QueryTokens.expression(ctx.NOT()));
			}

			builder.append(QueryTokens.expression(ctx.BETWEEN()));
			builder.appendExpression(visit(ctx.arithmetic_expression(1)));
			builder.append(QueryTokens.expression(ctx.AND()));
			builder.appendExpression(visit(ctx.arithmetic_expression(2)));

		} else if (ctx.string_expression(0) != null) {

			builder.appendExpression(visit(ctx.string_expression(0)));

			if (ctx.NOT() != null) {
				builder.append(QueryTokens.expression(ctx.NOT()));
			}

			builder.append(QueryTokens.expression(ctx.BETWEEN()));
			builder.appendExpression(visit(ctx.string_expression(1)));
			builder.append(QueryTokens.expression(ctx.AND()));
			builder.appendExpression(visit(ctx.string_expression(2)));

		} else if (ctx.datetime_expression(0) != null) {

			builder.append(visit(ctx.datetime_expression(0)));

			if (ctx.NOT() != null) {
				builder.append(QueryTokens.expression(ctx.NOT()));
			}

			builder.append(QueryTokens.expression(ctx.BETWEEN()));
			builder.appendExpression(visit(ctx.datetime_expression(1)));
			builder.append(QueryTokens.expression(ctx.AND()));
			builder.appendExpression(visit(ctx.datetime_expression(2)));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitIn_expression(EqlParser.In_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.state_valued_path_expression() != null) {
			builder.append(visit(ctx.state_valued_path_expression()));
		}
		if (ctx.type_discriminator() != null) {
			builder.append(visit(ctx.type_discriminator()));
		}
		if (ctx.NOT() != null) {
			builder.append(QueryTokens.expression(ctx.NOT()));
		}
		if (ctx.IN() != null) {
			builder.append(QueryTokens.expression(ctx.IN()));
		}

		if (ctx.in_item() != null && !ctx.in_item().isEmpty()) {

			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(QueryTokenStream.concat(ctx.in_item(), this::visit, TOKEN_COMMA));

			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.subquery() != null) {

			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.subquery()));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.collection_valued_input_parameter() != null) {
			builder.append(visit(ctx.collection_valued_input_parameter()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitIn_item(EqlParser.In_itemContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.literal() != null) {
			builder.append(visit(ctx.literal()));
		} else if (ctx.single_valued_input_parameter() != null) {
			builder.append(visit(ctx.single_valued_input_parameter()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitLike_expression(EqlParser.Like_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(visit(ctx.string_expression()));
		if (ctx.NOT() != null) {
			builder.append(QueryTokens.expression(ctx.NOT()));
		}
		builder.append(QueryTokens.expression(ctx.LIKE()));
		builder.appendExpression(visit(ctx.pattern_value()));

		if (ctx.ESCAPE() != null) {

			builder.append(QueryTokens.expression(ctx.ESCAPE()));
			builder.appendExpression(visit(ctx.escape_character()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitNull_comparison_expression(EqlParser.Null_comparison_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.single_valued_path_expression() != null) {
			builder.append(visit(ctx.single_valued_path_expression()));
		} else if (ctx.input_parameter() != null) {
			builder.append(visit(ctx.input_parameter()));
		} else if (ctx.nullif_expression() != null) {
			builder.append(visit(ctx.nullif_expression()));
		}

		if (ctx.op != null) {
			builder.append(QueryTokens.expression(ctx.op.getText()));
		} else {
			builder.append(QueryTokens.expression(ctx.IS()));
			if (ctx.NOT() != null) {
				builder.append(QueryTokens.expression(ctx.NOT()));
			}
		}
		builder.append(QueryTokens.expression(ctx.NULL()));

		return builder;
	}

	@Override
	public QueryTokenStream visitEmpty_collection_comparison_expression(
			EqlParser.Empty_collection_comparison_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(visit(ctx.collection_valued_path_expression()));
		builder.append(QueryTokens.expression(ctx.IS()));
		if (ctx.NOT() != null) {
			builder.append(QueryTokens.expression(ctx.NOT()));
		}
		builder.append(QueryTokens.expression(ctx.EMPTY()));

		return builder;
	}

	@Override
	public QueryTokenStream visitCollection_member_expression(EqlParser.Collection_member_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(visit(ctx.entity_or_value_expression()));
		if (ctx.NOT() != null) {
			builder.append(QueryTokens.expression(ctx.NOT()));
		}
		builder.append(QueryTokens.expression(ctx.MEMBER()));
		if (ctx.OF() != null) {
			builder.append(QueryTokens.expression(ctx.OF()));
		}
		builder.append(visit(ctx.collection_valued_path_expression()));

		return builder;
	}

	@Override
	public QueryTokenStream visitEntity_or_value_expression(EqlParser.Entity_or_value_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.single_valued_object_path_expression() != null) {
			builder.append(visit(ctx.single_valued_object_path_expression()));
		} else if (ctx.state_field_path_expression() != null) {
			builder.append(visit(ctx.state_field_path_expression()));
		} else if (ctx.simple_entity_or_value_expression() != null) {
			builder.append(visit(ctx.simple_entity_or_value_expression()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitSimple_entity_or_value_expression(
			EqlParser.Simple_entity_or_value_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.identification_variable() != null) {
			builder.append(visit(ctx.identification_variable()));
		} else if (ctx.input_parameter() != null) {
			builder.append(visit(ctx.input_parameter()));
		} else if (ctx.literal() != null) {
			builder.append(visit(ctx.literal()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitExists_expression(EqlParser.Exists_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.NOT() != null) {
			builder.append(QueryTokens.expression(ctx.NOT()));
		}

		builder.append(QueryTokens.expression(ctx.EXISTS()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(visit(ctx.subquery()));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitAll_or_any_expression(EqlParser.All_or_any_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.ALL() != null) {
			builder.append(QueryTokens.expression(ctx.ALL()));
		} else if (ctx.ANY() != null) {
			builder.append(QueryTokens.expression(ctx.ANY()));
		} else if (ctx.SOME() != null) {
			builder.append(QueryTokens.expression(ctx.SOME()));
		}

		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(visit(ctx.subquery()));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitStringComparison(EqlParser.StringComparisonContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendInline(visit(ctx.string_expression(0)));
		builder.append(visit(ctx.comparison_operator()));

		if (ctx.string_expression(1) != null) {
			builder.append(visit(ctx.string_expression(1)));
		} else {
			builder.append(visit(ctx.all_or_any_expression()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitBooleanComparison(EqlParser.BooleanComparisonContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendInline(visit(ctx.boolean_expression(0)));
		builder.append(QueryTokens.ventilated(ctx.op));

		if (ctx.boolean_expression(1) != null) {
			builder.append(visit(ctx.boolean_expression(1)));
		} else {
			builder.append(visit(ctx.all_or_any_expression()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitDirectBooleanCheck(EqlParser.DirectBooleanCheckContext ctx) {
		return visit(ctx.boolean_expression());
	}

	@Override
	public QueryTokenStream visitEnumComparison(EqlParser.EnumComparisonContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendInline(visit(ctx.enum_expression(0)));
		builder.append(QueryTokens.ventilated(ctx.op));

		if (ctx.enum_expression(1) != null) {
			builder.append(visit(ctx.enum_expression(1)));
		} else {
			builder.append(visit(ctx.all_or_any_expression()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitDatetimeComparison(EqlParser.DatetimeComparisonContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendInline(visit(ctx.datetime_expression(0)));
		builder.append(QueryTokens.ventilated(ctx.comparison_operator().op));

		if (ctx.datetime_expression(1) != null) {
			builder.append(visit(ctx.datetime_expression(1)));
		} else {
			builder.append(visit(ctx.all_or_any_expression()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitEntityComparison(EqlParser.EntityComparisonContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.entity_expression(0)));
		builder.append(QueryTokens.expression(ctx.op));

		if (ctx.entity_expression(1) != null) {
			builder.append(visit(ctx.entity_expression(1)));
		} else {
			builder.append(visit(ctx.all_or_any_expression()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitArithmeticComparison(EqlParser.ArithmeticComparisonContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(visit(ctx.arithmetic_expression(0)));
		builder.append(visit(ctx.comparison_operator()));

		if (ctx.arithmetic_expression(1) != null) {
			builder.append(visit(ctx.arithmetic_expression(1)));
		} else {
			builder.append(visit(ctx.all_or_any_expression()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitEntityTypeComparison(EqlParser.EntityTypeComparisonContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendInline(visit(ctx.entity_type_expression(0)));
		builder.append(QueryTokens.ventilated(ctx.op));
		builder.append(visit(ctx.entity_type_expression(1)));

		return builder;
	}

	@Override
	public QueryTokenStream visitRegexpComparison(EqlParser.RegexpComparisonContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.string_expression()));
		builder.append(QueryTokens.expression(ctx.REGEXP()));
		builder.appendExpression(visit(ctx.string_literal()));

		return builder;
	}

	@Override
	public QueryTokenStream visitComparison_operator(EqlParser.Comparison_operatorContext ctx) {
		return QueryRendererBuilder.from(QueryTokens.ventilated(ctx.op));
	}

	@Override
	public QueryTokenStream visitArithmetic_expression(EqlParser.Arithmetic_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.arithmetic_expression() != null) {

			builder.append(visit(ctx.arithmetic_expression()));
			builder.append(QueryTokens.expression(ctx.op));
			builder.append(visit(ctx.arithmetic_term()));

		} else {
			builder.append(visit(ctx.arithmetic_term()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitArithmetic_term(EqlParser.Arithmetic_termContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.arithmetic_term() != null) {

			builder.appendInline(visit(ctx.arithmetic_term()));
			builder.append(QueryTokens.ventilated(ctx.op));
			builder.append(visit(ctx.arithmetic_factor()));
		} else {
			builder.append(visit(ctx.arithmetic_factor()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitArithmetic_factor(EqlParser.Arithmetic_factorContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.op != null) {
			builder.append(QueryTokens.token(ctx.op));
		}
		builder.appendInline(visit(ctx.arithmetic_primary()));

		return builder;
	}

	@Override
	public QueryTokenStream visitArithmetic_primary(EqlParser.Arithmetic_primaryContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.state_valued_path_expression() != null) {
			builder.append(visit(ctx.state_valued_path_expression()));
		} else if (ctx.numeric_literal() != null) {
			builder.append(visit(ctx.numeric_literal()));
		} else if (ctx.arithmetic_expression() != null) {

			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.arithmetic_expression()));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.input_parameter() != null) {
			builder.append(visit(ctx.input_parameter()));
		} else if (ctx.functions_returning_numerics() != null) {
			builder.append(visit(ctx.functions_returning_numerics()));
		} else if (ctx.aggregate_expression() != null) {
			builder.append(visit(ctx.aggregate_expression()));
		} else if (ctx.case_expression() != null) {
			builder.append(visit(ctx.case_expression()));
		} else if (ctx.cast_function() != null) {
			builder.append(visit(ctx.cast_function()));
		} else if (ctx.function_invocation() != null) {
			builder.append(visit(ctx.function_invocation()));
		} else if (ctx.subquery() != null) {

			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.subquery()));
			builder.append(TOKEN_CLOSE_PAREN);
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitString_expression(EqlParser.String_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.state_valued_path_expression() != null) {
			builder.append(visit(ctx.state_valued_path_expression()));
		} else if (ctx.string_literal() != null) {
			builder.append(visit(ctx.string_literal()));
		} else if (ctx.input_parameter() != null) {
			builder.append(visit(ctx.input_parameter()));
		} else if (ctx.functions_returning_strings() != null) {
			builder.append(visit(ctx.functions_returning_strings()));
		} else if (ctx.aggregate_expression() != null) {
			builder.append(visit(ctx.aggregate_expression()));
		} else if (ctx.case_expression() != null) {
			builder.append(visit(ctx.case_expression()));
		} else if (ctx.function_invocation() != null) {
			builder.append(visit(ctx.function_invocation()));
		} else if (ctx.subquery() != null) {

			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.subquery()));
			builder.append(TOKEN_CLOSE_PAREN);
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitDatetime_expression(EqlParser.Datetime_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.state_valued_path_expression() != null) {
			builder.append(visit(ctx.state_valued_path_expression()));
		} else if (ctx.input_parameter() != null) {
			builder.append(visit(ctx.input_parameter()));
		} else if (ctx.input_parameter() != null) {
			builder.append(visit(ctx.input_parameter()));
		} else if (ctx.functions_returning_datetime() != null) {
			builder.append(visit(ctx.functions_returning_datetime()));
		} else if (ctx.aggregate_expression() != null) {
			builder.append(visit(ctx.aggregate_expression()));
		} else if (ctx.case_expression() != null) {
			builder.append(visit(ctx.case_expression()));
		} else if (ctx.function_invocation() != null) {
			builder.append(visit(ctx.function_invocation()));
		} else if (ctx.date_time_timestamp_literal() != null) {
			builder.append(visit(ctx.date_time_timestamp_literal()));
		} else if (ctx.subquery() != null) {

			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.subquery()));
			builder.append(TOKEN_CLOSE_PAREN);
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitBoolean_expression(EqlParser.Boolean_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.state_valued_path_expression() != null) {
			builder.append(visit(ctx.state_valued_path_expression()));
		} else if (ctx.boolean_literal() != null) {
			builder.append(visit(ctx.boolean_literal()));
		} else if (ctx.input_parameter() != null) {
			builder.append(visit(ctx.input_parameter()));
		} else if (ctx.case_expression() != null) {
			builder.append(visit(ctx.case_expression()));
		} else if (ctx.function_invocation() != null) {
			builder.append(visit(ctx.function_invocation()));
		} else if (ctx.subquery() != null) {

			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.subquery()));
			builder.append(TOKEN_CLOSE_PAREN);
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitEnum_expression(EqlParser.Enum_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.state_valued_path_expression() != null) {
			builder.append(visit(ctx.state_valued_path_expression()));
		} else if (ctx.enum_literal() != null) {
			builder.append(visit(ctx.enum_literal()));
		} else if (ctx.input_parameter() != null) {
			builder.append(visit(ctx.input_parameter()));
		} else if (ctx.case_expression() != null) {
			builder.append(visit(ctx.case_expression()));
		} else if (ctx.subquery() != null) {

			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.subquery()));
			builder.append(TOKEN_CLOSE_PAREN);
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitEntity_expression(EqlParser.Entity_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.single_valued_object_path_expression() != null) {
			builder.append(visit(ctx.single_valued_object_path_expression()));
		} else if (ctx.simple_entity_expression() != null) {
			builder.append(visit(ctx.simple_entity_expression()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitSimple_entity_expression(EqlParser.Simple_entity_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.identification_variable() != null) {
			builder.append(visit(ctx.identification_variable()));
		} else if (ctx.input_parameter() != null) {
			builder.append(visit(ctx.input_parameter()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitEntity_type_expression(EqlParser.Entity_type_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.type_discriminator() != null) {
			builder.append(visit(ctx.type_discriminator()));
		} else if (ctx.entity_type_literal() != null) {
			builder.append(visit(ctx.entity_type_literal()));
		} else if (ctx.input_parameter() != null) {
			builder.append(visit(ctx.input_parameter()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitType_discriminator(EqlParser.Type_discriminatorContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.TYPE()));
		builder.append(TOKEN_OPEN_PAREN);

		if (ctx.general_identification_variable() != null) {
			builder.appendInline(visit(ctx.general_identification_variable()));
		} else if (ctx.single_valued_object_path_expression() != null) {
			builder.appendInline(visit(ctx.single_valued_object_path_expression()));
		} else if (ctx.input_parameter() != null) {
			builder.appendInline(visit(ctx.input_parameter()));
		}
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitFunctions_returning_numerics(EqlParser.Functions_returning_numericsContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.LENGTH() != null) {

			builder.append(QueryTokens.token(ctx.LENGTH()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.string_expression(0)));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.LOCATE() != null) {

			builder.append(QueryTokens.token(ctx.LOCATE()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.string_expression(0)));
			builder.append(TOKEN_COMMA);
			builder.appendInline(visit(ctx.string_expression(1)));
			if (ctx.arithmetic_expression() != null) {
				builder.append(TOKEN_COMMA);
				builder.appendInline(visit(ctx.arithmetic_expression(0)));
			}
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.ABS() != null) {

			builder.append(QueryTokens.token(ctx.ABS()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.arithmetic_expression(0)));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.CEILING() != null) {

			builder.append(QueryTokens.token(ctx.CEILING()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.arithmetic_expression(0)));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.EXP() != null) {

			builder.append(QueryTokens.token(ctx.EXP()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.arithmetic_expression(0)));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.FLOOR() != null) {

			builder.append(QueryTokens.token(ctx.FLOOR()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.arithmetic_expression(0)));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.LN() != null) {

			builder.append(QueryTokens.token(ctx.LN()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.arithmetic_expression(0)));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.SIGN() != null) {

			builder.append(QueryTokens.token(ctx.SIGN()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.arithmetic_expression(0)));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.SQRT() != null) {

			builder.append(QueryTokens.token(ctx.SQRT()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.arithmetic_expression(0)));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.MOD() != null) {

			builder.append(QueryTokens.token(ctx.MOD()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.arithmetic_expression(0)));
			builder.append(TOKEN_COMMA);
			builder.appendInline(visit(ctx.arithmetic_expression(1)));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.POWER() != null) {

			builder.append(QueryTokens.token(ctx.POWER()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.arithmetic_expression(0)));
			builder.append(TOKEN_COMMA);
			builder.appendInline(visit(ctx.arithmetic_expression(1)));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.ROUND() != null) {

			builder.append(QueryTokens.token(ctx.ROUND()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.arithmetic_expression(0)));
			builder.append(TOKEN_COMMA);
			builder.appendInline(visit(ctx.arithmetic_expression(1)));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.SIZE() != null) {

			builder.append(QueryTokens.token(ctx.SIZE()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.collection_valued_path_expression()));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.INDEX() != null) {

			builder.append(QueryTokens.token(ctx.INDEX()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.identification_variable()));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.extract_datetime_field() != null) {
			builder.append(visit(ctx.extract_datetime_field()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitFunctions_returning_datetime(EqlParser.Functions_returning_datetimeContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.CURRENT_DATE() != null) {
			builder.append(QueryTokens.expression(ctx.CURRENT_DATE()));
		} else if (ctx.CURRENT_TIME() != null) {
			builder.append(QueryTokens.expression(ctx.CURRENT_TIME()));
		} else if (ctx.CURRENT_TIMESTAMP() != null) {
			builder.append(QueryTokens.expression(ctx.CURRENT_TIMESTAMP()));
		} else if (ctx.LOCAL() != null) {

			builder.append(QueryTokens.expression(ctx.LOCAL()));

			if (ctx.DATE() != null) {
				builder.append(QueryTokens.expression(ctx.DATE()));
			} else if (ctx.TIME() != null) {
				builder.append(QueryTokens.expression(ctx.TIME()));
			} else if (ctx.DATETIME() != null) {
				builder.append(QueryTokens.expression(ctx.DATETIME()));
			}
		} else if (ctx.extract_datetime_part() != null) {
			builder.append(visit(ctx.extract_datetime_part()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitFunctions_returning_strings(EqlParser.Functions_returning_stringsContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.CONCAT() != null) {

			builder.append(QueryTokens.token(ctx.CONCAT()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(QueryTokenStream.concat(ctx.string_expression(), this::visit, TOKEN_COMMA));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.SUBSTRING() != null) {

			builder.append(QueryTokens.token(ctx.SUBSTRING()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.string_expression(0)));
			builder.append(TOKEN_COMMA);
			builder.appendInline(QueryTokenStream.concat(ctx.arithmetic_expression(), this::visit, TOKEN_COMMA));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.TRIM() != null) {

			builder.append(QueryTokens.token(ctx.TRIM()));
			builder.append(TOKEN_OPEN_PAREN);
			if (ctx.trim_specification() != null) {
				builder.appendExpression(visit(ctx.trim_specification()));
			}
			if (ctx.trim_character() != null) {
				builder.appendExpression(visit(ctx.trim_character()));
			}
			if (ctx.FROM() != null) {
				builder.append(QueryTokens.expression(ctx.FROM()));
			}
			builder.appendInline(visit(ctx.string_expression(0)));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.LOWER() != null) {

			builder.append(QueryTokens.token(ctx.LOWER()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.string_expression(0)));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.UPPER() != null) {

			builder.append(QueryTokens.token(ctx.UPPER()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.string_expression(0)));
			builder.append(TOKEN_CLOSE_PAREN);
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitTrim_specification(EqlParser.Trim_specificationContext ctx) {

		if (ctx.LEADING() != null) {
			return QueryRendererBuilder.from(QueryTokens.expression(ctx.LEADING()));
		} else if (ctx.TRAILING() != null) {
			return QueryRendererBuilder.from(QueryTokens.expression(ctx.TRAILING()));
		} else {
			return QueryRendererBuilder.from(QueryTokens.expression(ctx.BOTH()));
		}
	}

	@Override
	public QueryTokenStream visitCast_function(EqlParser.Cast_functionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.CAST()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(visit(ctx.single_valued_path_expression()));
		builder.append(TOKEN_SPACE);
		builder.appendInline(visit(ctx.identification_variable()));

		if (ctx.numeric_literal() != null) {

			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(QueryTokenStream.concat(ctx.numeric_literal(), this::visit, TOKEN_COMMA));
			builder.append(TOKEN_CLOSE_PAREN);
		}
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitFunction_invocation(EqlParser.Function_invocationContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.FUNCTION() != null) {
			builder.append(QueryTokens.token(ctx.FUNCTION()));
		} else if (ctx.identification_variable() != null) {
			builder.appendInline(visit(ctx.identification_variable()));
		}
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(visit(ctx.function_name()));
		if (!ctx.function_arg().isEmpty()) {
			builder.append(TOKEN_COMMA);
		}

		builder.appendInline(QueryTokenStream.concat(ctx.function_arg(), this::visit, TOKEN_COMMA));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitExtract_datetime_field(EqlParser.Extract_datetime_fieldContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.EXTRACT()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendExpression(visit(ctx.datetime_field()));
		builder.append(QueryTokens.expression(ctx.FROM()));
		builder.appendInline(visit(ctx.datetime_expression()));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitDatetime_field(EqlParser.Datetime_fieldContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public QueryTokenStream visitExtract_datetime_part(EqlParser.Extract_datetime_partContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.EXTRACT()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendExpression(visit(ctx.datetime_part()));
		builder.append(QueryTokens.expression(ctx.FROM()));
		builder.appendInline(visit(ctx.datetime_expression()));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitDatetime_part(EqlParser.Datetime_partContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public QueryTokenStream visitFunction_arg(EqlParser.Function_argContext ctx) {

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
	public QueryTokenStream visitCase_expression(EqlParser.Case_expressionContext ctx) {

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
	public QueryTokenStream visitGeneral_case_expression(EqlParser.General_case_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.CASE()));

		ctx.when_clause().forEach(whenClauseContext -> {
			builder.appendExpression(visit(whenClauseContext));
		});

		builder.append(QueryTokens.expression(ctx.ELSE()));
		builder.appendExpression(visit(ctx.scalar_expression()));
		builder.append(QueryTokens.expression(ctx.END()));

		return builder;
	}

	@Override
	public QueryTokenStream visitWhen_clause(EqlParser.When_clauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.WHEN()));
		builder.append(visit(ctx.conditional_expression()));
		builder.append(QueryTokens.expression(ctx.THEN()));
		builder.append(visit(ctx.scalar_expression()));

		return builder;
	}

	@Override
	public QueryTokenStream visitSimple_case_expression(EqlParser.Simple_case_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.CASE()));
		builder.append(visit(ctx.case_operand()));

		ctx.simple_when_clause().forEach(simpleWhenClauseContext -> {
			builder.append(visit(simpleWhenClauseContext));
		});

		builder.append(QueryTokens.expression(ctx.ELSE()));
		builder.append(visit(ctx.scalar_expression()));
		builder.append(QueryTokens.expression(ctx.END()));

		return builder;
	}

	@Override
	public QueryTokenStream visitCase_operand(EqlParser.Case_operandContext ctx) {

		if (ctx.state_valued_path_expression() != null) {
			return visit(ctx.state_valued_path_expression());
		} else {
			return visit(ctx.type_discriminator());
		}
	}

	@Override
	public QueryTokenStream visitSimple_when_clause(EqlParser.Simple_when_clauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.WHEN()));
		builder.appendExpression(visit(ctx.scalar_expression(0)));
		builder.append(QueryTokens.expression(ctx.THEN()));
		builder.appendExpression(visit(ctx.scalar_expression(1)));

		return builder;
	}

	@Override
	public QueryTokenStream visitCoalesce_expression(EqlParser.Coalesce_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.COALESCE()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(QueryTokenStream.concat(ctx.scalar_expression(), this::visit, TOKEN_COMMA));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitNullif_expression(EqlParser.Nullif_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.NULLIF()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(visit(ctx.scalar_expression(0)));
		builder.append(TOKEN_COMMA);
		builder.appendInline(visit(ctx.scalar_expression(1)));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitTrim_character(EqlParser.Trim_characterContext ctx) {

		if (ctx.CHARACTER() != null) {
			return QueryRendererBuilder.from(QueryTokens.expression(ctx.CHARACTER()));
		} else if (ctx.character_valued_input_parameter() != null) {
			return visit(ctx.character_valued_input_parameter());
		} else {
			return QueryRenderer.builder();
		}
	}

	@Override
	public QueryTokenStream visitIdentification_variable(EqlParser.Identification_variableContext ctx) {

		if (ctx.IDENTIFICATION_VARIABLE() != null) {
			return QueryRendererBuilder.from(QueryTokens.expression(ctx.IDENTIFICATION_VARIABLE()));
		} else if (ctx.f != null) {
			return QueryRendererBuilder.from(QueryTokens.expression(ctx.f));
		} else {
			return QueryRenderer.builder();
		}
	}

	@Override
	public QueryTokenStream visitConstructor_name(EqlParser.Constructor_nameContext ctx) {
		return visit(ctx.entity_name());
	}

	@Override
	public QueryTokenStream visitLiteral(EqlParser.LiteralContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.STRINGLITERAL() != null) {
			builder.append(QueryTokens.expression(ctx.STRINGLITERAL()));
		} else if (ctx.INTLITERAL() != null) {
			builder.append(QueryTokens.expression(ctx.INTLITERAL()));
		} else if (ctx.FLOATLITERAL() != null) {
			builder.append(QueryTokens.expression(ctx.FLOATLITERAL()));
		} else if (ctx.LONGLITERAL() != null) {
			builder.append(QueryTokens.expression(ctx.LONGLITERAL()));
		} else if (ctx.boolean_literal() != null) {
			builder.append(visit(ctx.boolean_literal()));
		} else if (ctx.entity_type_literal() != null) {
			builder.append(visit(ctx.entity_type_literal()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitInput_parameter(EqlParser.Input_parameterContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.INTLITERAL() != null) {

			builder.append(TOKEN_QUESTION_MARK);
			builder.append(QueryTokens.token(ctx.INTLITERAL()));
		} else if (ctx.identification_variable() != null) {

			builder.append(TOKEN_COLON);
			builder.appendInline(visit(ctx.identification_variable()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitPattern_value(EqlParser.Pattern_valueContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(visit(ctx.string_expression()));

		return builder;
	}

	@Override
	public QueryTokenStream visitDate_time_timestamp_literal(EqlParser.Date_time_timestamp_literalContext ctx) {

		if (ctx.STRINGLITERAL() != null) {
			return QueryRendererBuilder.from(QueryTokens.expression(ctx.STRINGLITERAL()));
		} else if (ctx.DATELITERAL() != null) {
			return QueryRendererBuilder.from(QueryTokens.expression(ctx.DATELITERAL()));
		} else if (ctx.TIMELITERAL() != null) {
			return QueryRendererBuilder.from(QueryTokens.expression(ctx.TIMELITERAL()));
		} else if (ctx.TIMESTAMPLITERAL() != null) {
			return QueryRendererBuilder.from(QueryTokens.expression(ctx.TIMESTAMPLITERAL()));
		} else {
			return QueryRenderer.builder();
		}
	}

	@Override
	public QueryTokenStream visitEntity_type_literal(EqlParser.Entity_type_literalContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public QueryTokenStream visitEscape_character(EqlParser.Escape_characterContext ctx) {
		return QueryRendererBuilder.from(QueryTokens.token(ctx.CHARACTER()));
	}

	@Override
	public QueryTokenStream visitNumeric_literal(EqlParser.Numeric_literalContext ctx) {

		if (ctx.INTLITERAL() != null) {
			return QueryRendererBuilder.from(QueryTokens.token(ctx.INTLITERAL()));
		} else if (ctx.FLOATLITERAL() != null) {
			return QueryRendererBuilder.from(QueryTokens.token(ctx.FLOATLITERAL()));
		} else if (ctx.LONGLITERAL() != null) {
			return QueryRendererBuilder.from(QueryTokens.token(ctx.LONGLITERAL()));
		} else {
			return QueryRenderer.builder();
		}
	}

	@Override
	public QueryTokenStream visitBoolean_literal(EqlParser.Boolean_literalContext ctx) {

		if (ctx.TRUE() != null) {
			return QueryRendererBuilder.from(QueryTokens.token(ctx.TRUE()));
		} else if (ctx.FALSE() != null) {
			return QueryRendererBuilder.from(QueryTokens.token(ctx.FALSE()));
		} else {
			return QueryRenderer.builder();
		}
	}

	@Override
	public QueryTokenStream visitEnum_literal(EqlParser.Enum_literalContext ctx) {
		return visit(ctx.state_field_path_expression());
	}

	@Override
	public QueryTokenStream visitString_literal(EqlParser.String_literalContext ctx) {

		if (ctx.CHARACTER() != null) {
			return QueryRendererBuilder.from(QueryTokens.expression(ctx.CHARACTER()));
		} else if (ctx.STRINGLITERAL() != null) {
			return QueryRendererBuilder.from(QueryTokens.expression(ctx.STRINGLITERAL()));
		} else {
			return QueryRenderer.builder();
		}
	}

	@Override
	public QueryTokenStream visitSingle_valued_embeddable_object_field(
			EqlParser.Single_valued_embeddable_object_fieldContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public QueryTokenStream visitSubtype(EqlParser.SubtypeContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public QueryTokenStream visitCollection_valued_field(EqlParser.Collection_valued_fieldContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public QueryTokenStream visitSingle_valued_object_field(EqlParser.Single_valued_object_fieldContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public QueryTokenStream visitState_field(EqlParser.State_fieldContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public QueryTokenStream visitCollection_value_field(EqlParser.Collection_value_fieldContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public QueryTokenStream visitEntity_name(EqlParser.Entity_nameContext ctx) {
		return QueryTokenStream.concat(ctx.reserved_word(), this::visit, QueryRenderer::inline, TOKEN_DOT);
	}

	@Override
	public QueryTokenStream visitResult_variable(EqlParser.Result_variableContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public QueryTokenStream visitSuperquery_identification_variable(
			EqlParser.Superquery_identification_variableContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public QueryTokenStream visitCollection_valued_input_parameter(
			EqlParser.Collection_valued_input_parameterContext ctx) {
		return visit(ctx.input_parameter());
	}

	@Override
	public QueryTokenStream visitSingle_valued_input_parameter(EqlParser.Single_valued_input_parameterContext ctx) {
		return visit(ctx.input_parameter());
	}

	@Override
	public QueryTokenStream visitFunction_name(EqlParser.Function_nameContext ctx) {
		return visit(ctx.string_literal());
	}

	@Override
	public QueryTokenStream visitCharacter_valued_input_parameter(
			EqlParser.Character_valued_input_parameterContext ctx) {

		if (ctx.CHARACTER() != null) {
			return QueryRendererBuilder.from(QueryTokens.expression(ctx.CHARACTER()));
		} else if (ctx.input_parameter() != null) {
			return visit(ctx.input_parameter());
		} else {
			return QueryRenderer.builder();
		}
	}

	@Override
	public QueryTokenStream visitReserved_word(EqlParser.Reserved_wordContext ctx) {
		if (ctx.IDENTIFICATION_VARIABLE() != null) {
			return QueryRendererBuilder.from(QueryTokens.expression(ctx.IDENTIFICATION_VARIABLE()));
		} else if (ctx.f != null) {
			return QueryRendererBuilder.from(QueryTokens.expression(ctx.f));
		} else {
			return QueryRenderer.builder();
		}
	}
}
