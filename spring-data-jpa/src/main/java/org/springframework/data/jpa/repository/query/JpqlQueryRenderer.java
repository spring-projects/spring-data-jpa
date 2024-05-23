/*
 * Copyright 2022-2024 the original author or authors.
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

import org.antlr.v4.runtime.tree.ParseTree;

import org.springframework.data.jpa.repository.query.JpqlParser.Reserved_wordContext;
import org.springframework.data.jpa.repository.query.QueryRenderer.QueryRendererBuilder;

/**
 * An ANTLR {@link org.antlr.v4.runtime.tree.ParseTreeVisitor} that renders a JPQL query without making any changes.
 *
 * @author Greg Turnquist
 * @author Christoph Strobl
 * @since 3.1
 */
@SuppressWarnings({ "ConstantConditions", "DuplicatedCode" })
class JpqlQueryRenderer extends JpqlBaseVisitor<QueryRendererBuilder> {

	@Override
	public QueryRendererBuilder visitStart(JpqlParser.StartContext ctx) {
		return visit(ctx.ql_statement());
	}

	@Override
	public QueryRendererBuilder visitQl_statement(JpqlParser.Ql_statementContext ctx) {

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
	public QueryRendererBuilder visitSelect_statement(JpqlParser.Select_statementContext ctx) {

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

		return builder;
	}

	@Override
	public QueryRendererBuilder visitUpdate_statement(JpqlParser.Update_statementContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(visit(ctx.update_clause()));

		if (ctx.where_clause() != null) {
			builder.append(visit(ctx.where_clause()));
		}

		return builder;
	}

	@Override
	public QueryRendererBuilder visitDelete_statement(JpqlParser.Delete_statementContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.delete_clause()));

		if (ctx.where_clause() != null) {
			builder.appendExpression(visit(ctx.where_clause()));
		}

		return builder;
	}

	@Override
	public QueryRendererBuilder visitFrom_clause(JpqlParser.From_clauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(JpaQueryParsingToken.expression(ctx.FROM()));
		builder.appendInline(visit(ctx.identification_variable_declaration()));

		if (!ctx.identificationVariableDeclarationOrCollectionMemberDeclaration().isEmpty()) {

			builder.append(TOKEN_COMMA);
			builder.appendExpression(QueryRendererBuilder
					.concat(ctx.identificationVariableDeclarationOrCollectionMemberDeclaration(), this::visit, TOKEN_COMMA));
		}

		return builder;
	}

	@Override
	public QueryRendererBuilder visitIdentificationVariableDeclarationOrCollectionMemberDeclaration(
			JpqlParser.IdentificationVariableDeclarationOrCollectionMemberDeclarationContext ctx) {

		if (ctx.identification_variable_declaration() != null) {
			return visit(ctx.identification_variable_declaration());
		} else if (ctx.collection_member_declaration() != null) {
			return visit(ctx.collection_member_declaration());
		} else {
			return QueryRenderer.builder();
		}
	}

	@Override
	public QueryRendererBuilder visitIdentification_variable_declaration(
			JpqlParser.Identification_variable_declarationContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.range_variable_declaration()));

		ctx.join().forEach(joinContext -> {
			builder.append(visit(joinContext));
		});

		ctx.fetch_join().forEach(fetchJoinContext -> {
			builder.append(visit(fetchJoinContext));
		});

		return builder;
	}

	@Override
	public QueryRendererBuilder visitRange_variable_declaration(JpqlParser.Range_variable_declarationContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.entity_name()));

		if (ctx.AS() != null) {
			builder.append(JpaQueryParsingToken.expression(ctx.AS()));
		}

		builder.append(visit(ctx.identification_variable()));

		return builder;
	}

	@Override
	public QueryRendererBuilder visitJoin(JpqlParser.JoinContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(visit(ctx.join_spec()));
		builder.append(visit(ctx.join_association_path_expression()));
		if (ctx.AS() != null) {
			builder.append(JpaQueryParsingToken.expression(ctx.AS()));
		}
		builder.append(visit(ctx.identification_variable()));
		if (ctx.join_condition() != null) {
			builder.append(visit(ctx.join_condition()));
		}

		return builder;
	}

	@Override
	public QueryRendererBuilder visitFetch_join(JpqlParser.Fetch_joinContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(visit(ctx.join_spec()));
		builder.append(JpaQueryParsingToken.expression(ctx.FETCH()));
		builder.append(visit(ctx.join_association_path_expression()));

		return builder;
	}

	@Override
	public QueryRendererBuilder visitJoin_spec(JpqlParser.Join_specContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.LEFT() != null) {
			builder.append(JpaQueryParsingToken.expression(ctx.LEFT()));
		}
		if (ctx.OUTER() != null) {
			builder.append(JpaQueryParsingToken.expression(ctx.OUTER()));
		}
		if (ctx.INNER() != null) {
			builder.append(JpaQueryParsingToken.expression(ctx.INNER()));
		}
		if (ctx.JOIN() != null) {
			builder.append(JpaQueryParsingToken.expression(ctx.JOIN()));
		}

		return builder;
	}

	@Override
	public QueryRendererBuilder visitJoin_condition(JpqlParser.Join_conditionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(JpaQueryParsingToken.expression(ctx.ON()));
		builder.appendExpression(visit(ctx.conditional_expression()));

		return builder;
	}

	@Override
	public QueryRendererBuilder visitJoin_association_path_expression(
			JpqlParser.Join_association_path_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.TREAT() == null) {

			if (ctx.join_collection_valued_path_expression() != null) {
				builder.appendExpression(visit(ctx.join_collection_valued_path_expression()));
			} else if (ctx.join_single_valued_path_expression() != null) {
				builder.appendExpression(visit(ctx.join_single_valued_path_expression()));
			}
		} else {
			if (ctx.join_collection_valued_path_expression() != null) {

				builder.append(JpaQueryParsingToken.token(ctx.TREAT()));
				builder.append(TOKEN_OPEN_PAREN);
				builder.appendInline(visit(ctx.join_collection_valued_path_expression()));
				builder.append(JpaQueryParsingToken.expression(ctx.AS()));
				builder.appendInline(visit(ctx.subtype()));
				builder.append(TOKEN_CLOSE_PAREN);
			} else if (ctx.join_single_valued_path_expression() != null) {

				builder.append(JpaQueryParsingToken.token(ctx.TREAT()));
				builder.append(TOKEN_OPEN_PAREN);
				builder.appendInline(visit(ctx.join_single_valued_path_expression()));
				builder.append(JpaQueryParsingToken.expression(ctx.AS()));
				builder.appendInline(visit(ctx.subtype()));
				builder.append(TOKEN_CLOSE_PAREN);
			}
		}

		return builder;
	}

	@Override
	public QueryRendererBuilder visitJoin_collection_valued_path_expression(
			JpqlParser.Join_collection_valued_path_expressionContext ctx) {

		List<ParseTree> items = new ArrayList<>(3 + ctx.single_valued_embeddable_object_field().size());

		items.add(ctx.identification_variable());
		items.addAll(ctx.single_valued_embeddable_object_field());
		items.add(ctx.collection_valued_field());

		return QueryRendererBuilder.concat(items, this::visit, TOKEN_DOT);
	}

	@Override
	public QueryRendererBuilder visitJoin_single_valued_path_expression(
			JpqlParser.Join_single_valued_path_expressionContext ctx) {

		List<ParseTree> items = new ArrayList<>(3 + ctx.single_valued_embeddable_object_field().size());

		items.add(ctx.identification_variable());
		items.addAll(ctx.single_valued_embeddable_object_field());
		items.add(ctx.single_valued_object_field());

		return QueryRendererBuilder.concat(items, this::visit, TOKEN_DOT);
	}

	@Override
	public QueryRendererBuilder visitCollection_member_declaration(JpqlParser.Collection_member_declarationContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(JpaQueryParsingToken.token(ctx.IN()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(visit(ctx.collection_valued_path_expression()));
		builder.append(TOKEN_CLOSE_PAREN);

		if (ctx.AS() != null) {
			builder.append(JpaQueryParsingToken.expression(ctx.AS()));
		}

		builder.appendExpression(visit(ctx.identification_variable()));

		return builder;
	}

	@Override
	public QueryRendererBuilder visitQualified_identification_variable(
			JpqlParser.Qualified_identification_variableContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.map_field_identification_variable() != null) {
			builder.append(visit(ctx.map_field_identification_variable()));
		} else if (ctx.identification_variable() != null) {

			builder.append(JpaQueryParsingToken.expression(ctx.ENTRY()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.identification_variable()));
			builder.append(TOKEN_CLOSE_PAREN);
		}

		return builder;
	}

	@Override
	public QueryRendererBuilder visitMap_field_identification_variable(
			JpqlParser.Map_field_identification_variableContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.KEY() != null) {

			builder.append(JpaQueryParsingToken.token(ctx.KEY()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.identification_variable()));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.VALUE() != null) {

			builder.append(JpaQueryParsingToken.token(ctx.VALUE()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.identification_variable()));
			builder.append(TOKEN_CLOSE_PAREN);
		}

		return builder;
	}

	@Override
	public QueryRendererBuilder visitSingle_valued_path_expression(JpqlParser.Single_valued_path_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.qualified_identification_variable() != null) {
			builder.append(visit(ctx.qualified_identification_variable()));
		} else if (ctx.qualified_identification_variable() != null) {

			builder.append(JpaQueryParsingToken.token(ctx.TREAT()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.qualified_identification_variable()));
			builder.append(JpaQueryParsingToken.expression(ctx.AS()));
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
	public QueryRendererBuilder visitGeneral_identification_variable(
			JpqlParser.General_identification_variableContext ctx) {

		if (ctx.identification_variable() != null) {
			return visit(ctx.identification_variable());
		} else if (ctx.map_field_identification_variable() != null) {
			return visit(ctx.map_field_identification_variable());
		}

		return QueryRenderer.builder();
	}

	@Override
	public QueryRendererBuilder visitGeneral_subpath(JpqlParser.General_subpathContext ctx) {

		if (ctx.simple_subpath() != null) {
			return visit(ctx.simple_subpath());
		} else if (ctx.treated_subpath() != null) {

			List<ParseTree> items = new ArrayList<>(1 + ctx.single_valued_object_field().size());

			items.add(ctx.treated_subpath());
			items.addAll(ctx.single_valued_object_field());
			return QueryRendererBuilder.concat(items, this::visit, TOKEN_DOT);
		}

		return QueryRenderer.builder();
	}

	@Override
	public QueryRendererBuilder visitSimple_subpath(JpqlParser.Simple_subpathContext ctx) {

		List<ParseTree> items = new ArrayList<>(1 + ctx.single_valued_object_field().size());

		items.add(ctx.general_identification_variable());
		items.addAll(ctx.single_valued_object_field());
		return QueryRendererBuilder.concat(items, this::visit, TOKEN_DOT);
	}

	@Override
	public QueryRendererBuilder visitTreated_subpath(JpqlParser.Treated_subpathContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(JpaQueryParsingToken.token(ctx.TREAT()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(visit(ctx.general_subpath()));
		builder.append(JpaQueryParsingToken.expression(ctx.AS()));
		builder.appendInline(visit(ctx.subtype()));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryRendererBuilder visitState_field_path_expression(JpqlParser.State_field_path_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendInline(visit(ctx.general_subpath()));
		builder.append(TOKEN_DOT);
		builder.appendInline(visit(ctx.state_field()));

		return builder;
	}

	@Override
	public QueryRendererBuilder visitState_valued_path_expression(JpqlParser.State_valued_path_expressionContext ctx) {

		if (ctx.state_field_path_expression() != null) {
			return visit(ctx.state_field_path_expression());
		} else if (ctx.general_identification_variable() != null) {
			return visit(ctx.general_identification_variable());
		}

		return QueryRenderer.builder();
	}

	@Override
	public QueryRendererBuilder visitSingle_valued_object_path_expression(
			JpqlParser.Single_valued_object_path_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendInline(visit(ctx.general_subpath()));
		builder.append(TOKEN_DOT);
		builder.appendInline(visit(ctx.single_valued_object_field()));

		return builder;
	}

	@Override
	public QueryRendererBuilder visitCollection_valued_path_expression(
			JpqlParser.Collection_valued_path_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendInline(visit(ctx.general_subpath()));
		builder.append(TOKEN_DOT);
		builder.appendInline(visit(ctx.collection_value_field()));

		return builder;
	}

	@Override
	public QueryRendererBuilder visitUpdate_clause(JpqlParser.Update_clauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(JpaQueryParsingToken.expression(ctx.UPDATE()));
		builder.appendExpression(visit(ctx.entity_name()));

		if (ctx.AS() != null) {
			builder.append(JpaQueryParsingToken.expression(ctx.AS()));
		}

		if (ctx.identification_variable() != null) {
			builder.appendExpression(visit(ctx.identification_variable()));
		}

		builder.append(JpaQueryParsingToken.expression(ctx.SET()));
		builder.append(QueryRendererBuilder.concat(ctx.update_item(), this::visit, TOKEN_COMMA));

		return builder;
	}

	@Override
	public QueryRendererBuilder visitUpdate_item(JpqlParser.Update_itemContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		List<ParseTree> items = new ArrayList<>(2 + ctx.single_valued_embeddable_object_field().size());

		if (ctx.identification_variable() != null) {
			items.add(ctx.identification_variable());
		}

		items.addAll(ctx.single_valued_embeddable_object_field());

		if (ctx.state_field() != null) {
			items.add(ctx.state_field());
		} else if (ctx.single_valued_object_field() != null) {
			items.add(ctx.single_valued_object_field());
		}

		builder.appendInline(QueryRendererBuilder.concat(items, this::visit, TOKEN_DOT));
		builder.append(TOKEN_EQUALS);
		builder.appendInline(visit(ctx.new_value()));

		return builder;
	}

	@Override
	public QueryRendererBuilder visitNew_value(JpqlParser.New_valueContext ctx) {

		if (ctx.scalar_expression() != null) {
			return visit(ctx.scalar_expression());
		} else if (ctx.simple_entity_expression() != null) {
			return visit(ctx.simple_entity_expression());
		} else if (ctx.NULL() != null) {
			return QueryRendererBuilder.from(JpaQueryParsingToken.expression(ctx.NULL()));
		} else {
			return QueryRenderer.builder();
		}
	}

	@Override
	public QueryRendererBuilder visitDelete_clause(JpqlParser.Delete_clauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(JpaQueryParsingToken.expression(ctx.DELETE()));
		builder.append(JpaQueryParsingToken.expression(ctx.FROM()));
		builder.appendExpression(visit(ctx.entity_name()));
		if (ctx.AS() != null) {
			builder.append(JpaQueryParsingToken.expression(ctx.AS()));
		}
		if (ctx.identification_variable() != null) {
			builder.appendExpression(visit(ctx.identification_variable()));
		}

		return builder;
	}

	@Override
	public QueryRendererBuilder visitSelect_clause(JpqlParser.Select_clauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(JpaQueryParsingToken.expression(ctx.SELECT()));

		if (ctx.DISTINCT() != null) {
			builder.append(JpaQueryParsingToken.expression(ctx.DISTINCT()));
		}

		builder.append(QueryRendererBuilder.concat(ctx.select_item(), this::visit, TOKEN_COMMA));

		return builder;
	}

	@Override
	public QueryRendererBuilder visitSelect_item(JpqlParser.Select_itemContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.select_expression()));

		if (ctx.AS() != null || ctx.result_variable() != null) {

			if (ctx.AS() != null) {
				builder.append(JpaQueryParsingToken.expression(ctx.AS()));
			}

			if (ctx.result_variable() != null) {
				builder.appendExpression(visit(ctx.result_variable()));
			}
		}

		return builder;
	}

	@Override
	public QueryRendererBuilder visitSelect_expression(JpqlParser.Select_expressionContext ctx) {

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

				builder.append(JpaQueryParsingToken.token(ctx.OBJECT()));
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
	public QueryRendererBuilder visitConstructor_expression(JpqlParser.Constructor_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(JpaQueryParsingToken.expression(ctx.NEW()));
		builder.append(visit(ctx.constructor_name()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(QueryRendererBuilder.concat(ctx.constructor_item(), this::visit, TOKEN_COMMA));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryRendererBuilder visitConstructor_item(JpqlParser.Constructor_itemContext ctx) {

		if (ctx.single_valued_path_expression() != null) {
			return visit(ctx.single_valued_path_expression());
		} else if (ctx.scalar_expression() != null) {
			return visit(ctx.scalar_expression());
		} else if (ctx.aggregate_expression() != null) {
			return visit(ctx.aggregate_expression());
		} else if (ctx.identification_variable() != null) {
			return visit(ctx.identification_variable());
		} else if (ctx.literal() != null) {
			return visit(ctx.literal());
		}

		return QueryRenderer.builder();
	}

	@Override
	public QueryRendererBuilder visitAggregate_expression(JpqlParser.Aggregate_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.AVG() != null || ctx.MAX() != null || ctx.MIN() != null || ctx.SUM() != null) {

			if (ctx.AVG() != null) {
				builder.append(JpaQueryParsingToken.token(ctx.AVG()));
			}
			if (ctx.MAX() != null) {
				builder.append(JpaQueryParsingToken.token(ctx.MAX()));
			}
			if (ctx.MIN() != null) {
				builder.append(JpaQueryParsingToken.token(ctx.MIN()));
			}
			if (ctx.SUM() != null) {
				builder.append(JpaQueryParsingToken.token(ctx.SUM()));
			}

			builder.append(TOKEN_OPEN_PAREN);

			if (ctx.DISTINCT() != null) {
				builder.append(JpaQueryParsingToken.expression(ctx.DISTINCT()));
			}

			builder.appendInline(visit(ctx.state_valued_path_expression()));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.COUNT() != null) {

			builder.append(JpaQueryParsingToken.token(ctx.COUNT()));
			builder.append(TOKEN_OPEN_PAREN);
			if (ctx.DISTINCT() != null) {
				builder.append(JpaQueryParsingToken.expression(ctx.DISTINCT()));
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
	public QueryRendererBuilder visitWhere_clause(JpqlParser.Where_clauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(JpaQueryParsingToken.expression(ctx.WHERE()));
		builder.appendExpression(visit(ctx.conditional_expression()));

		return builder;
	}

	@Override
	public QueryRendererBuilder visitGroupby_clause(JpqlParser.Groupby_clauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(JpaQueryParsingToken.expression(ctx.GROUP()));
		builder.append(JpaQueryParsingToken.expression(ctx.BY()));
		builder.appendExpression(QueryRendererBuilder.concat(ctx.groupby_item(), this::visit, TOKEN_COMMA));

		return builder;
	}

	@Override
	public QueryRendererBuilder visitGroupby_item(JpqlParser.Groupby_itemContext ctx) {

		if (ctx.single_valued_path_expression() != null) {
			return visit(ctx.single_valued_path_expression());
		} else if (ctx.identification_variable() != null) {
			return visit(ctx.identification_variable());
		}

		return QueryRenderer.builder();
	}

	@Override
	public QueryRendererBuilder visitHaving_clause(JpqlParser.Having_clauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(JpaQueryParsingToken.expression(ctx.HAVING()));
		builder.appendExpression(visit(ctx.conditional_expression()));

		return builder;
	}

	@Override
	public QueryRendererBuilder visitOrderby_clause(JpqlParser.Orderby_clauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(JpaQueryParsingToken.expression(ctx.ORDER()));
		builder.append(JpaQueryParsingToken.expression(ctx.BY()));
		builder.appendExpression(QueryRendererBuilder.concat(ctx.orderby_item(), this::visit, TOKEN_COMMA));

		return builder;
	}

	@Override
	public QueryRendererBuilder visitOrderby_item(JpqlParser.Orderby_itemContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.state_field_path_expression() != null) {
			builder.appendExpression(visit(ctx.state_field_path_expression()));
		} else if (ctx.general_identification_variable() != null) {
			builder.appendExpression(visit(ctx.general_identification_variable()));
		} else if (ctx.result_variable() != null) {
			builder.appendExpression(visit(ctx.result_variable()));
		}

		if (ctx.ASC() != null) {
			builder.append(JpaQueryParsingToken.expression(ctx.ASC()));
		}
		if (ctx.DESC() != null) {
			builder.append(JpaQueryParsingToken.expression(ctx.DESC()));
		}

		return builder;
	}

	@Override
	public QueryRendererBuilder visitSubquery(JpqlParser.SubqueryContext ctx) {

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
	public QueryRendererBuilder visitSubquery_from_clause(JpqlParser.Subquery_from_clauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(JpaQueryParsingToken.expression(ctx.FROM()));
		builder.appendExpression(
				QueryRendererBuilder.concat(ctx.subselect_identification_variable_declaration(), this::visit, TOKEN_COMMA));

		return builder;
	}

	@Override
	public QueryRendererBuilder visitSubselect_identification_variable_declaration(
			JpqlParser.Subselect_identification_variable_declarationContext ctx) {
		return super.visitSubselect_identification_variable_declaration(ctx);
	}

	@Override
	public QueryRendererBuilder visitDerived_path_expression(JpqlParser.Derived_path_expressionContext ctx) {
		return super.visitDerived_path_expression(ctx);
	}

	@Override
	public QueryRendererBuilder visitGeneral_derived_path(JpqlParser.General_derived_pathContext ctx) {
		return super.visitGeneral_derived_path(ctx);
	}

	@Override
	public QueryRendererBuilder visitSimple_derived_path(JpqlParser.Simple_derived_pathContext ctx) {
		return super.visitSimple_derived_path(ctx);
	}

	@Override
	public QueryRendererBuilder visitTreated_derived_path(JpqlParser.Treated_derived_pathContext ctx) {
		return super.visitTreated_derived_path(ctx);
	}

	@Override
	public QueryRendererBuilder visitDerived_collection_member_declaration(
			JpqlParser.Derived_collection_member_declarationContext ctx) {
		return super.visitDerived_collection_member_declaration(ctx);
	}

	@Override
	public QueryRendererBuilder visitSimple_select_clause(JpqlParser.Simple_select_clauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(JpaQueryParsingToken.expression(ctx.SELECT()));
		if (ctx.DISTINCT() != null) {
			builder.append(JpaQueryParsingToken.expression(ctx.DISTINCT()));
		}
		builder.appendExpression(visit(ctx.simple_select_expression()));

		return builder;
	}

	@Override
	public QueryRendererBuilder visitSimple_select_expression(JpqlParser.Simple_select_expressionContext ctx) {

		if (ctx.single_valued_path_expression() != null) {
			return visit(ctx.single_valued_path_expression());
		} else if (ctx.scalar_expression() != null) {
			return visit(ctx.scalar_expression());
		} else if (ctx.aggregate_expression() != null) {
			return visit(ctx.aggregate_expression());
		} else if (ctx.identification_variable() != null) {
			return visit(ctx.identification_variable());
		}

		return QueryRenderer.builder();
	}

	@Override
	public QueryRendererBuilder visitScalar_expression(JpqlParser.Scalar_expressionContext ctx) {

		if (ctx.arithmetic_expression() != null) {
			return visit(ctx.arithmetic_expression());
		} else if (ctx.string_expression() != null) {
			return visit(ctx.string_expression());
		} else if (ctx.enum_expression() != null) {
			return visit(ctx.enum_expression());
		} else if (ctx.datetime_expression() != null) {
			return visit(ctx.datetime_expression());
		} else if (ctx.boolean_expression() != null) {
			return visit(ctx.boolean_expression());
		} else if (ctx.case_expression() != null) {
			return visit(ctx.case_expression());
		} else if (ctx.entity_type_expression() != null) {
			return visit(ctx.entity_type_expression());
		}

		return QueryRenderer.builder();
	}

	@Override
	public QueryRendererBuilder visitConditional_expression(JpqlParser.Conditional_expressionContext ctx) {

		if (ctx.conditional_expression() != null) {
			QueryRendererBuilder builder = QueryRenderer.builder();

			builder.appendExpression(visit(ctx.conditional_expression()));
			builder.append(JpaQueryParsingToken.expression(ctx.OR()));
			builder.appendExpression(visit(ctx.conditional_term()));

			return builder;
		} else {
			return visit(ctx.conditional_term());
		}
	}

	@Override
	public QueryRendererBuilder visitConditional_term(JpqlParser.Conditional_termContext ctx) {

		if (ctx.conditional_term() != null) {
			QueryRendererBuilder builder = QueryRenderer.builder();

			builder.appendExpression(visit(ctx.conditional_term()));
			builder.append(JpaQueryParsingToken.expression(ctx.AND()));
			builder.appendExpression(visit(ctx.conditional_factor()));

			return builder;
		} else {
			return visit(ctx.conditional_factor());
		}
	}

	@Override
	public QueryRendererBuilder visitConditional_factor(JpqlParser.Conditional_factorContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.NOT() != null) {
			builder.append(JpaQueryParsingToken.expression(ctx.NOT()));
		}

		builder.append(visit(ctx.conditional_primary()));

		return builder;
	}

	@Override
	public QueryRendererBuilder visitConditional_primary(JpqlParser.Conditional_primaryContext ctx) {

		if (ctx.simple_cond_expression() != null) {
			return visit(ctx.simple_cond_expression());
		}

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.conditional_expression() != null) {

			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.conditional_expression()));
			builder.append(TOKEN_CLOSE_PAREN);
		}

		return builder;
	}

	@Override
	public QueryRendererBuilder visitSimple_cond_expression(JpqlParser.Simple_cond_expressionContext ctx) {

		if (ctx.comparison_expression() != null) {
			return visit(ctx.comparison_expression());
		} else if (ctx.between_expression() != null) {
			return visit(ctx.between_expression());
		} else if (ctx.in_expression() != null) {
			return visit(ctx.in_expression());
		} else if (ctx.like_expression() != null) {
			return visit(ctx.like_expression());
		} else if (ctx.null_comparison_expression() != null) {
			return visit(ctx.null_comparison_expression());
		} else if (ctx.empty_collection_comparison_expression() != null) {
			return visit(ctx.empty_collection_comparison_expression());
		} else if (ctx.collection_member_expression() != null) {
			return visit(ctx.collection_member_expression());
		} else if (ctx.exists_expression() != null) {
			return visit(ctx.exists_expression());
		}

		return QueryRenderer.builder();
	}

	@Override
	public QueryRendererBuilder visitBetween_expression(JpqlParser.Between_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.arithmetic_expression(0) != null) {

			builder.appendExpression(visit(ctx.arithmetic_expression(0)));

			if (ctx.NOT() != null) {
				builder.append(JpaQueryParsingToken.expression(ctx.NOT()));
			}

			builder.append(JpaQueryParsingToken.expression(ctx.BETWEEN()));
			builder.appendExpression(visit(ctx.arithmetic_expression(1)));
			builder.append(JpaQueryParsingToken.expression(ctx.AND()));
			builder.appendExpression(visit(ctx.arithmetic_expression(2)));

		} else if (ctx.string_expression(0) != null) {

			builder.appendExpression(visit(ctx.string_expression(0)));

			if (ctx.NOT() != null) {
				builder.append(JpaQueryParsingToken.expression(ctx.NOT()));
			}

			builder.append(JpaQueryParsingToken.expression(ctx.BETWEEN()));
			builder.appendExpression(visit(ctx.string_expression(1)));
			builder.append(JpaQueryParsingToken.expression(ctx.AND()));
			builder.appendExpression(visit(ctx.string_expression(2)));

		} else if (ctx.datetime_expression(0) != null) {

			builder.appendExpression(visit(ctx.datetime_expression(0)));

			if (ctx.NOT() != null) {
				builder.append(JpaQueryParsingToken.expression(ctx.NOT()));
			}

			builder.append(JpaQueryParsingToken.expression(ctx.BETWEEN()));
			builder.appendExpression(visit(ctx.datetime_expression(1)));
			builder.append(JpaQueryParsingToken.expression(ctx.AND()));
			builder.appendExpression(visit(ctx.datetime_expression(2)));
		}

		return builder;
	}

	@Override
	public QueryRendererBuilder visitIn_expression(JpqlParser.In_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.state_valued_path_expression() != null) {
			builder.appendExpression(visit(ctx.state_valued_path_expression()));
		}
		if (ctx.type_discriminator() != null) {
			builder.appendExpression(visit(ctx.type_discriminator()));
		}
		if (ctx.NOT() != null) {
			builder.append(JpaQueryParsingToken.expression(ctx.NOT()));
		}
		if (ctx.IN() != null) {
			builder.append(JpaQueryParsingToken.expression(ctx.IN()));
		}

		if (ctx.in_item() != null && !ctx.in_item().isEmpty()) {

			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(QueryRendererBuilder.concat(ctx.in_item(), this::visit, TOKEN_COMMA));
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
	public QueryRendererBuilder visitIn_item(JpqlParser.In_itemContext ctx) {

		if (ctx.literal() != null) {
			return visit(ctx.literal());
		} else if (ctx.single_valued_input_parameter() != null) {
			return visit(ctx.single_valued_input_parameter());
		}

		return QueryRenderer.builder();
	}

	@Override
	public QueryRendererBuilder visitLike_expression(JpqlParser.Like_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.string_expression()));

		if (ctx.NOT() != null) {
			builder.append(JpaQueryParsingToken.expression(ctx.NOT()));
		}
		builder.append(JpaQueryParsingToken.expression(ctx.LIKE()));
		builder.appendExpression(visit(ctx.pattern_value()));

		if (ctx.ESCAPE() != null) {

			builder.append(JpaQueryParsingToken.expression(ctx.ESCAPE()));
			builder.appendExpression(visit(ctx.escape_character()));
		}

		return builder;
	}

	@Override
	public QueryRendererBuilder visitNull_comparison_expression(JpqlParser.Null_comparison_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.single_valued_path_expression() != null) {
			builder.appendExpression(visit(ctx.single_valued_path_expression()));
		} else if (ctx.input_parameter() != null) {
			builder.appendExpression(visit(ctx.input_parameter()));
		}

		builder.append(JpaQueryParsingToken.expression(ctx.IS()));
		if (ctx.NOT() != null) {
			builder.append(JpaQueryParsingToken.expression(ctx.NOT()));
		}
		builder.append(JpaQueryParsingToken.expression(ctx.NULL()));

		return builder;
	}

	@Override
	public QueryRendererBuilder visitEmpty_collection_comparison_expression(
			JpqlParser.Empty_collection_comparison_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.collection_valued_path_expression()));
		builder.append(JpaQueryParsingToken.expression(ctx.IS()));
		if (ctx.NOT() != null) {
			builder.append(JpaQueryParsingToken.expression(ctx.NOT()));
		}
		builder.append(JpaQueryParsingToken.expression(ctx.EMPTY()));

		return builder;
	}

	@Override
	public QueryRendererBuilder visitCollection_member_expression(JpqlParser.Collection_member_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.entity_or_value_expression()));
		if (ctx.NOT() != null) {
			builder.append(JpaQueryParsingToken.expression(ctx.NOT()));
		}
		builder.append(JpaQueryParsingToken.expression(ctx.MEMBER()));
		if (ctx.OF() != null) {
			builder.append(JpaQueryParsingToken.expression(ctx.OF()));
		}
		builder.append(visit(ctx.collection_valued_path_expression()));

		return builder;
	}

	@Override
	public QueryRendererBuilder visitEntity_or_value_expression(JpqlParser.Entity_or_value_expressionContext ctx) {

		if (ctx.single_valued_object_path_expression() != null) {
			return visit(ctx.single_valued_object_path_expression());
		} else if (ctx.state_field_path_expression() != null) {
			return visit(ctx.state_field_path_expression());
		} else if (ctx.simple_entity_or_value_expression() != null) {
			return visit(ctx.simple_entity_or_value_expression());
		}

		return QueryRenderer.builder();
	}

	@Override
	public QueryRendererBuilder visitSimple_entity_or_value_expression(
			JpqlParser.Simple_entity_or_value_expressionContext ctx) {

		if (ctx.identification_variable() != null) {
			return visit(ctx.identification_variable());
		} else if (ctx.input_parameter() != null) {
			return visit(ctx.input_parameter());
		} else if (ctx.literal() != null) {
			return visit(ctx.literal());
		}

		return QueryRenderer.builder();
	}

	@Override
	public QueryRendererBuilder visitExists_expression(JpqlParser.Exists_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.NOT() != null) {
			builder.append(JpaQueryParsingToken.expression(ctx.NOT()));
		}

		builder.append(JpaQueryParsingToken.expression(ctx.EXISTS()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(visit(ctx.subquery()));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryRendererBuilder visitAll_or_any_expression(JpqlParser.All_or_any_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.ALL() != null) {
			builder.append(JpaQueryParsingToken.expression(ctx.ALL()));
		} else if (ctx.ANY() != null) {
			builder.append(JpaQueryParsingToken.expression(ctx.ANY()));
		} else if (ctx.SOME() != null) {
			builder.append(JpaQueryParsingToken.expression(ctx.SOME()));
		}

		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(visit(ctx.subquery()));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryRendererBuilder visitComparison_expression(JpqlParser.Comparison_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (!ctx.string_expression().isEmpty()) {

			builder.appendExpression(visit(ctx.string_expression(0)));
			builder.appendExpression(visit(ctx.comparison_operator()));

			if (ctx.string_expression(1) != null) {
				builder.appendExpression(visit(ctx.string_expression(1)));
			} else {
				builder.appendExpression(visit(ctx.all_or_any_expression()));
			}
		} else if (!ctx.boolean_expression().isEmpty()) {

			builder.appendInline(visit(ctx.boolean_expression(0)));
			builder.append(JpaQueryParsingToken.ventilated(ctx.op));

			if (ctx.boolean_expression(1) != null) {
				builder.appendExpression(visit(ctx.boolean_expression(1)));
			} else {
				builder.appendExpression(visit(ctx.all_or_any_expression()));
			}
		} else if (!ctx.enum_expression().isEmpty()) {

			builder.appendInline(visit(ctx.enum_expression(0)));
			builder.append(JpaQueryParsingToken.ventilated(ctx.op));

			if (ctx.enum_expression(1) != null) {
				builder.appendExpression(visit(ctx.enum_expression(1)));
			} else {
				builder.appendExpression(visit(ctx.all_or_any_expression()));
			}
		} else if (!ctx.datetime_expression().isEmpty()) {

			builder.appendExpression(visit(ctx.datetime_expression(0)));
			builder.appendExpression(visit(ctx.comparison_operator()));

			if (ctx.datetime_expression(1) != null) {
				builder.appendExpression(visit(ctx.datetime_expression(1)));
			} else {
				builder.appendExpression(visit(ctx.all_or_any_expression()));
			}
		} else if (!ctx.entity_expression().isEmpty()) {

			builder.appendInline(visit(ctx.entity_expression(0)));
			builder.append(JpaQueryParsingToken.ventilated(ctx.op));

			if (ctx.entity_expression(1) != null) {
				builder.appendExpression(visit(ctx.entity_expression(1)));
			} else {
				builder.appendExpression(visit(ctx.all_or_any_expression()));
			}
		} else if (!ctx.arithmetic_expression().isEmpty()) {

			builder.appendExpression(visit(ctx.arithmetic_expression(0)));
			builder.appendExpression(visit(ctx.comparison_operator()));

			if (ctx.arithmetic_expression(1) != null) {
				builder.appendExpression(visit(ctx.arithmetic_expression(1)));
			} else {
				builder.appendExpression(visit(ctx.all_or_any_expression()));
			}
		} else if (!ctx.entity_type_expression().isEmpty()) {

			builder.appendInline(visit(ctx.entity_type_expression(0)));
			builder.append(JpaQueryParsingToken.ventilated(ctx.op));
			builder.appendExpression(visit(ctx.entity_type_expression(1)));
		}

		return builder;
	}

	@Override
	public QueryRendererBuilder visitComparison_operator(JpqlParser.Comparison_operatorContext ctx) {
		return QueryRendererBuilder.from(JpaQueryParsingToken.token(ctx.op));
	}

	@Override
	public QueryRendererBuilder visitArithmetic_expression(JpqlParser.Arithmetic_expressionContext ctx) {

		if (ctx.arithmetic_expression() != null) {

			QueryRendererBuilder builder = QueryRenderer.builder();
			builder.append(visit(ctx.arithmetic_expression()));
			builder.append(JpaQueryParsingToken.ventilated(ctx.op));
			builder.append(visit(ctx.arithmetic_term()));
			return builder;

		} else {
			return visit(ctx.arithmetic_term());
		}
	}

	@Override
	public QueryRendererBuilder visitArithmetic_term(JpqlParser.Arithmetic_termContext ctx) {

		if (ctx.arithmetic_term() != null) {

			QueryRendererBuilder builder = QueryRenderer.builder();
			builder.appendInline(visit(ctx.arithmetic_term()));
			builder.append(JpaQueryParsingToken.ventilated(ctx.op));
			builder.append(visit(ctx.arithmetic_factor()));

			return builder;
		} else {
			return visit(ctx.arithmetic_factor());
		}
	}

	@Override
	public QueryRendererBuilder visitArithmetic_factor(JpqlParser.Arithmetic_factorContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.op != null) {
			builder.append(JpaQueryParsingToken.token(ctx.op));
		}

		builder.append(visit(ctx.arithmetic_primary()));

		return builder;
	}

	@Override
	public QueryRendererBuilder visitArithmetic_primary(JpqlParser.Arithmetic_primaryContext ctx) {

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
	public QueryRendererBuilder visitString_expression(JpqlParser.String_expressionContext ctx) {

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
	public QueryRendererBuilder visitDatetime_expression(JpqlParser.Datetime_expressionContext ctx) {

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
	public QueryRendererBuilder visitBoolean_expression(JpqlParser.Boolean_expressionContext ctx) {

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
	public QueryRendererBuilder visitEnum_expression(JpqlParser.Enum_expressionContext ctx) {

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
	public QueryRendererBuilder visitEntity_expression(JpqlParser.Entity_expressionContext ctx) {

		if (ctx.single_valued_object_path_expression() != null) {
			return visit(ctx.single_valued_object_path_expression());
		} else if (ctx.simple_entity_expression() != null) {
			return visit(ctx.simple_entity_expression());
		}

		return QueryRenderer.builder();
	}

	@Override
	public QueryRendererBuilder visitSimple_entity_expression(JpqlParser.Simple_entity_expressionContext ctx) {

		if (ctx.identification_variable() != null) {
			return visit(ctx.identification_variable());
		} else if (ctx.input_parameter() != null) {
			return visit(ctx.input_parameter());
		}

		return QueryRenderer.builder();
	}

	@Override
	public QueryRendererBuilder visitEntity_type_expression(JpqlParser.Entity_type_expressionContext ctx) {

		if (ctx.type_discriminator() != null) {
			return visit(ctx.type_discriminator());
		} else if (ctx.entity_type_literal() != null) {
			return visit(ctx.entity_type_literal());
		} else if (ctx.input_parameter() != null) {
			return visit(ctx.input_parameter());
		}

		return QueryRenderer.builder();
	}

	@Override
	public QueryRendererBuilder visitType_discriminator(JpqlParser.Type_discriminatorContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(JpaQueryParsingToken.token(ctx.TYPE()));
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
	public QueryRendererBuilder visitFunctions_returning_numerics(JpqlParser.Functions_returning_numericsContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.LENGTH() != null) {

			builder.append(JpaQueryParsingToken.token(ctx.LENGTH()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.string_expression(0)));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.LOCATE() != null) {

			builder.append(JpaQueryParsingToken.token(ctx.LOCATE()));
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

			builder.append(JpaQueryParsingToken.token(ctx.ABS()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.arithmetic_expression(0)));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.CEILING() != null) {

			builder.append(JpaQueryParsingToken.token(ctx.CEILING()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.arithmetic_expression(0)));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.EXP() != null) {

			builder.append(JpaQueryParsingToken.token(ctx.EXP()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.arithmetic_expression(0)));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.FLOOR() != null) {

			builder.append(JpaQueryParsingToken.token(ctx.FLOOR()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.arithmetic_expression(0)));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.LN() != null) {

			builder.append(JpaQueryParsingToken.token(ctx.LN()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.arithmetic_expression(0)));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.SIGN() != null) {

			builder.append(JpaQueryParsingToken.token(ctx.SIGN()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.arithmetic_expression(0)));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.SQRT() != null) {

			builder.append(JpaQueryParsingToken.token(ctx.SQRT()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.arithmetic_expression(0)));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.MOD() != null) {

			builder.append(JpaQueryParsingToken.token(ctx.MOD()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.arithmetic_expression(0)));
			builder.append(TOKEN_COMMA);
			builder.appendInline(visit(ctx.arithmetic_expression(1)));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.POWER() != null) {

			builder.append(JpaQueryParsingToken.token(ctx.POWER()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.arithmetic_expression(0)));
			builder.append(TOKEN_COMMA);
			builder.appendInline(visit(ctx.arithmetic_expression(1)));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.ROUND() != null) {

			builder.append(JpaQueryParsingToken.token(ctx.ROUND()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.arithmetic_expression(0)));
			builder.append(TOKEN_COMMA);
			builder.appendInline(visit(ctx.arithmetic_expression(1)));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.SIZE() != null) {

			builder.append(JpaQueryParsingToken.token(ctx.SIZE()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.collection_valued_path_expression()));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.INDEX() != null) {

			builder.append(JpaQueryParsingToken.token(ctx.INDEX()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.identification_variable()));
			builder.append(TOKEN_CLOSE_PAREN);
		}

		return builder;
	}

	@Override
	public QueryRendererBuilder visitFunctions_returning_datetime(JpqlParser.Functions_returning_datetimeContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.CURRENT_DATE() != null) {
			builder.append(JpaQueryParsingToken.expression(ctx.CURRENT_DATE()));
		} else if (ctx.CURRENT_TIME() != null) {
			builder.append(JpaQueryParsingToken.expression(ctx.CURRENT_TIME()));
		} else if (ctx.CURRENT_TIMESTAMP() != null) {
			builder.append(JpaQueryParsingToken.expression(ctx.CURRENT_TIMESTAMP()));
		} else if (ctx.LOCAL() != null) {

			builder.append(JpaQueryParsingToken.expression(ctx.LOCAL()));

			if (ctx.DATE() != null) {
				builder.append(JpaQueryParsingToken.expression(ctx.DATE()));
			} else if (ctx.TIME() != null) {
				builder.append(JpaQueryParsingToken.expression(ctx.TIME()));
			} else if (ctx.DATETIME() != null) {
				builder.append(JpaQueryParsingToken.expression(ctx.DATETIME()));
			}
		}

		return builder;
	}

	@Override
	public QueryRendererBuilder visitFunctions_returning_strings(JpqlParser.Functions_returning_stringsContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.CONCAT() != null) {

			builder.append(JpaQueryParsingToken.token(ctx.CONCAT()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(QueryRendererBuilder.concat(ctx.string_expression(), this::visit, TOKEN_COMMA));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.SUBSTRING() != null) {

			builder.append(JpaQueryParsingToken.token(ctx.SUBSTRING()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.append(visit(ctx.string_expression(0)));
			builder.appendInline(QueryRendererBuilder.concat(ctx.arithmetic_expression(), this::visit, TOKEN_COMMA));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.TRIM() != null) {

			builder.append(JpaQueryParsingToken.token(ctx.TRIM()));
			builder.append(TOKEN_OPEN_PAREN);
			if (ctx.trim_specification() != null) {
				builder.appendExpression(visit(ctx.trim_specification()));
			}
			if (ctx.trim_character() != null) {
				builder.appendExpression(visit(ctx.trim_character()));
			}
			if (ctx.FROM() != null) {
				builder.append(JpaQueryParsingToken.expression(ctx.FROM()));
			}
			builder.append(visit(ctx.string_expression(0)));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.LOWER() != null) {

			builder.append(JpaQueryParsingToken.token(ctx.LOWER()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.string_expression(0)));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.UPPER() != null) {

			builder.append(JpaQueryParsingToken.token(ctx.UPPER()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.append(visit(ctx.string_expression(0)));
			builder.append(TOKEN_CLOSE_PAREN);
		}

		return builder;
	}

	@Override
	public QueryRendererBuilder visitTrim_specification(JpqlParser.Trim_specificationContext ctx) {

		if (ctx.LEADING() != null) {
			return QueryRendererBuilder.from(JpaQueryParsingToken.expression(ctx.LEADING()));
		} else if (ctx.TRAILING() != null) {
			return QueryRendererBuilder.from(JpaQueryParsingToken.expression(ctx.TRAILING()));
		} else {
			return QueryRendererBuilder.from(JpaQueryParsingToken.expression(ctx.BOTH()));
		}
	}

	@Override
	public QueryRendererBuilder visitFunction_invocation(JpqlParser.Function_invocationContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(JpaQueryParsingToken.token(ctx.FUNCTION()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(visit(ctx.function_name()));
		if (!ctx.function_arg().isEmpty()) {
			builder.append(TOKEN_COMMA);
			builder.appendInline(QueryRendererBuilder.concat(ctx.function_arg(), this::visit, TOKEN_COMMA));
		}
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryRendererBuilder visitExtract_datetime_field(JpqlParser.Extract_datetime_fieldContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(JpaQueryParsingToken.expression(ctx.EXTRACT()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendExpression(visit(ctx.datetime_field()));
		builder.append(JpaQueryParsingToken.expression(ctx.FROM()));
		builder.appendInline(visit(ctx.datetime_expression()));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryRendererBuilder visitDatetime_field(JpqlParser.Datetime_fieldContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public QueryRendererBuilder visitExtract_datetime_part(JpqlParser.Extract_datetime_partContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(JpaQueryParsingToken.expression(ctx.EXTRACT()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendExpression(visit(ctx.datetime_part()));
		builder.append(JpaQueryParsingToken.expression(ctx.FROM()));
		builder.append(visit(ctx.datetime_expression()));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryRendererBuilder visitDatetime_part(JpqlParser.Datetime_partContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public QueryRendererBuilder visitFunction_arg(JpqlParser.Function_argContext ctx) {

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
	public QueryRendererBuilder visitCase_expression(JpqlParser.Case_expressionContext ctx) {

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
	public QueryRendererBuilder visitGeneral_case_expression(JpqlParser.General_case_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(JpaQueryParsingToken.expression(ctx.CASE()));
		builder.appendExpression(QueryRendererBuilder.concatExpressions(ctx.when_clause(), this::visit, TOKEN_NONE));

		builder.append(JpaQueryParsingToken.expression(ctx.ELSE()));
		builder.appendExpression(visit(ctx.scalar_expression()));
		builder.append(JpaQueryParsingToken.expression(ctx.END()));

		return builder;
	}

	@Override
	public QueryRendererBuilder visitWhen_clause(JpqlParser.When_clauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(JpaQueryParsingToken.expression(ctx.WHEN()));
		builder.appendExpression(visit(ctx.conditional_expression()));
		builder.append(JpaQueryParsingToken.expression(ctx.THEN()));
		builder.appendExpression(visit(ctx.scalar_expression()));

		return builder;
	}

	@Override
	public QueryRendererBuilder visitSimple_case_expression(JpqlParser.Simple_case_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(JpaQueryParsingToken.expression(ctx.CASE()));
		builder.appendExpression(visit(ctx.case_operand()));
		builder.appendExpression(QueryRendererBuilder.concatExpressions(ctx.simple_when_clause(), this::visit, TOKEN_NONE));

		builder.append(JpaQueryParsingToken.expression(ctx.ELSE()));
		builder.appendExpression(visit(ctx.scalar_expression()));
		builder.append(JpaQueryParsingToken.expression(ctx.END()));

		return builder;
	}

	@Override
	public QueryRendererBuilder visitCase_operand(JpqlParser.Case_operandContext ctx) {

		if (ctx.state_valued_path_expression() != null) {
			return visit(ctx.state_valued_path_expression());
		} else {
			return visit(ctx.type_discriminator());
		}
	}

	@Override
	public QueryRendererBuilder visitSimple_when_clause(JpqlParser.Simple_when_clauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(JpaQueryParsingToken.expression(ctx.WHEN()));
		builder.appendExpression(visit(ctx.scalar_expression(0)));
		builder.append(JpaQueryParsingToken.expression(ctx.THEN()));
		builder.appendExpression(visit(ctx.scalar_expression(1)));

		return builder;
	}

	@Override
	public QueryRendererBuilder visitCoalesce_expression(JpqlParser.Coalesce_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(JpaQueryParsingToken.token(ctx.COALESCE()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(QueryRendererBuilder.concat(ctx.scalar_expression(), this::visit, TOKEN_COMMA));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryRendererBuilder visitNullif_expression(JpqlParser.Nullif_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(JpaQueryParsingToken.expression(ctx.NULLIF()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(visit(ctx.scalar_expression(0)));
		builder.append(TOKEN_COMMA);
		builder.appendInline(visit(ctx.scalar_expression(1)));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryRendererBuilder visitTrim_character(JpqlParser.Trim_characterContext ctx) {

		if (ctx.CHARACTER() != null) {
			return QueryRendererBuilder.from(JpaQueryParsingToken.expression(ctx.CHARACTER()));
		} else if (ctx.character_valued_input_parameter() != null) {
			return visit(ctx.character_valued_input_parameter());
		} else {
			return QueryRenderer.builder();
		}
	}

	@Override
	public QueryRendererBuilder visitIdentification_variable(JpqlParser.Identification_variableContext ctx) {

		if (ctx.IDENTIFICATION_VARIABLE() != null) {
			return QueryRendererBuilder.from(JpaQueryParsingToken.expression(ctx.IDENTIFICATION_VARIABLE()));
		} else if (ctx.f != null) {
			return QueryRendererBuilder.from(JpaQueryParsingToken.token(ctx.f));
		} else {
			return QueryRenderer.builder();
		}
	}

	@Override
	public QueryRendererBuilder visitConstructor_name(JpqlParser.Constructor_nameContext ctx) {
		return visit(ctx.entity_name());
	}

	@Override
	public QueryRendererBuilder visitLiteral(JpqlParser.LiteralContext ctx) {

		if (ctx.STRINGLITERAL() != null) {
			return QueryRendererBuilder.from(JpaQueryParsingToken.expression(ctx.STRINGLITERAL()));
		} else if (ctx.JAVASTRINGLITERAL() != null) {
			return QueryRendererBuilder.from(JpaQueryParsingToken.expression(ctx.JAVASTRINGLITERAL()));
		} else if (ctx.INTLITERAL() != null) {
			return QueryRendererBuilder.from(JpaQueryParsingToken.expression(ctx.INTLITERAL()));
		} else if (ctx.FLOATLITERAL() != null) {
			return QueryRendererBuilder.from(JpaQueryParsingToken.expression(ctx.FLOATLITERAL()));
		} else if (ctx.LONGLITERAL() != null) {
			return QueryRendererBuilder.from(JpaQueryParsingToken.expression(ctx.LONGLITERAL()));
		} else if (ctx.boolean_literal() != null) {
			return visit(ctx.boolean_literal());
		} else if (ctx.entity_type_literal() != null) {
			return visit(ctx.entity_type_literal());
		}

		return QueryRenderer.builder();
	}

	@Override
	public QueryRendererBuilder visitInput_parameter(JpqlParser.Input_parameterContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.INTLITERAL() != null) {

			builder.append(TOKEN_QUESTION_MARK);
			builder.append(JpaQueryParsingToken.token(ctx.INTLITERAL()));
		} else if (ctx.identification_variable() != null) {

			builder.append(TOKEN_COLON);
			builder.appendInline(visit(ctx.identification_variable()));
		}

		return builder;
	}

	@Override
	public QueryRendererBuilder visitPattern_value(JpqlParser.Pattern_valueContext ctx) {
		return visit(ctx.string_expression());
	}

	@Override
	public QueryRendererBuilder visitDate_time_timestamp_literal(JpqlParser.Date_time_timestamp_literalContext ctx) {
		return QueryRendererBuilder.from(JpaQueryParsingToken.expression(ctx.STRINGLITERAL()));
	}

	@Override
	public QueryRendererBuilder visitEntity_type_literal(JpqlParser.Entity_type_literalContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public QueryRendererBuilder visitEscape_character(JpqlParser.Escape_characterContext ctx) {
		return QueryRendererBuilder.from(JpaQueryParsingToken.expression(ctx.CHARACTER()));
	}

	@Override
	public QueryRendererBuilder visitNumeric_literal(JpqlParser.Numeric_literalContext ctx) {

		if (ctx.INTLITERAL() != null) {
			return QueryRendererBuilder.from(JpaQueryParsingToken.expression(ctx.INTLITERAL()));
		} else if (ctx.FLOATLITERAL() != null) {
			return QueryRendererBuilder.from(JpaQueryParsingToken.expression(ctx.FLOATLITERAL()));
		} else if (ctx.LONGLITERAL() != null) {
			return QueryRendererBuilder.from(JpaQueryParsingToken.expression(ctx.LONGLITERAL()));
		} else {
			return QueryRenderer.builder();
		}
	}

	@Override
	public QueryRendererBuilder visitBoolean_literal(JpqlParser.Boolean_literalContext ctx) {

		if (ctx.TRUE() != null) {
			return QueryRendererBuilder.from(JpaQueryParsingToken.expression(ctx.TRUE()));
		} else if (ctx.FALSE() != null) {
			return QueryRendererBuilder.from(JpaQueryParsingToken.expression(ctx.FALSE()));
		} else {
			return QueryRenderer.builder();
		}
	}

	@Override
	public QueryRendererBuilder visitEnum_literal(JpqlParser.Enum_literalContext ctx) {
		return visit(ctx.state_field_path_expression());
	}

	@Override
	public QueryRendererBuilder visitString_literal(JpqlParser.String_literalContext ctx) {

		if (ctx.CHARACTER() != null) {
			return QueryRendererBuilder.from(JpaQueryParsingToken.expression(ctx.CHARACTER()));
		} else if (ctx.STRINGLITERAL() != null) {
			return QueryRendererBuilder.from(JpaQueryParsingToken.expression(ctx.STRINGLITERAL()));
		} else {
			return QueryRenderer.builder();
		}
	}

	@Override
	public QueryRendererBuilder visitSingle_valued_embeddable_object_field(
			JpqlParser.Single_valued_embeddable_object_fieldContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public QueryRendererBuilder visitSubtype(JpqlParser.SubtypeContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public QueryRendererBuilder visitCollection_valued_field(JpqlParser.Collection_valued_fieldContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public QueryRendererBuilder visitSingle_valued_object_field(JpqlParser.Single_valued_object_fieldContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public QueryRendererBuilder visitState_field(JpqlParser.State_fieldContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public QueryRendererBuilder visitCollection_value_field(JpqlParser.Collection_value_fieldContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public QueryRendererBuilder visitEntity_name(JpqlParser.Entity_nameContext ctx) {
		return QueryRendererBuilder.concat(ctx.reserved_word(), this::visitReserved_word, TOKEN_DOT);
	}

	@Override
	public QueryRendererBuilder visitResult_variable(JpqlParser.Result_variableContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public QueryRendererBuilder visitSuperquery_identification_variable(
			JpqlParser.Superquery_identification_variableContext ctx) {
		return visit(ctx.identification_variable());
	}

	@Override
	public QueryRendererBuilder visitCollection_valued_input_parameter(
			JpqlParser.Collection_valued_input_parameterContext ctx) {
		return visit(ctx.input_parameter());
	}

	@Override
	public QueryRendererBuilder visitSingle_valued_input_parameter(JpqlParser.Single_valued_input_parameterContext ctx) {
		return visit(ctx.input_parameter());
	}

	@Override
	public QueryRendererBuilder visitFunction_name(JpqlParser.Function_nameContext ctx) {
		return visit(ctx.string_literal());
	}

	@Override
	public QueryRendererBuilder visitCharacter_valued_input_parameter(
			JpqlParser.Character_valued_input_parameterContext ctx) {

		if (ctx.CHARACTER() != null) {
			return QueryRendererBuilder.from(JpaQueryParsingToken.expression(ctx.CHARACTER()));
		} else if (ctx.input_parameter() != null) {
			return visit(ctx.input_parameter());
		} else {
			return QueryRenderer.builder();
		}
	}

	@Override
	public QueryRendererBuilder visitReserved_word(Reserved_wordContext ctx) {
		if (ctx.IDENTIFICATION_VARIABLE() != null) {
			return QueryRendererBuilder.from(JpaQueryParsingToken.token(ctx.IDENTIFICATION_VARIABLE()));
		} else if (ctx.f != null) {
			return QueryRendererBuilder.from(JpaQueryParsingToken.token(ctx.f));
		} else {
			return QueryRenderer.builder();
		}
	}
}
