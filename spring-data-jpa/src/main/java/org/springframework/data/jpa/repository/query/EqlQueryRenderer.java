/*
 * Copyright 2023-2025 the original author or authors.
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

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import org.springframework.data.jpa.repository.query.QueryRenderer.QueryRendererBuilder;
import org.springframework.util.CollectionUtils;

/**
 * An ANTLR {@link org.antlr.v4.runtime.tree.ParseTreeVisitor} that renders an EQL query without making any changes.
 *
 * @author Greg Turnquist
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 3.2
 */
@SuppressWarnings({ "ConstantConditions", "DuplicatedCode" })
class EqlQueryRenderer extends EqlBaseVisitor<QueryTokenStream> {

	/**
	 * Is this AST tree a {@literal subquery}?
	 *
	 * @return boolean
	 */
	static boolean isSubquery(ParserRuleContext ctx) {

		if (ctx instanceof EqlParser.SubqueryContext) {
			return true;
		} else if (ctx instanceof EqlParser.Update_statementContext) {
			return false;
		} else if (ctx instanceof EqlParser.Delete_statementContext) {
			return false;
		} else {
			return ctx.getParent() != null && isSubquery(ctx.getParent());
		}
	}

	/**
	 * Is this AST tree a {@literal set} query that has been added through {@literal UNION|INTERSECT|EXCEPT}?
	 *
	 * @return boolean
	 */
	static boolean isSetQuery(ParserRuleContext ctx) {

		if (ctx instanceof EqlParser.Set_fuctionContext) {
			return true;
		}

		return ctx.getParent() != null && isSetQuery(ctx.getParent());
	}

	@Override
	public QueryTokenStream visitStart(EqlParser.StartContext ctx) {
		return visit(ctx.ql_statement());
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

		if (ctx.subquery() != null) {

			QueryRendererBuilder nested = QueryRenderer.builder();
			nested.append(TOKEN_OPEN_PAREN);
			nested.appendInline(visit(ctx.subquery()));
			nested.append(TOKEN_CLOSE_PAREN);

			QueryRendererBuilder builder = QueryRenderer.builder();
			builder.appendExpression(nested);

			if (ctx.AS() != null) {
				builder.append(QueryTokens.expression(ctx.AS()));
			}

			if (ctx.identification_variable() != null) {
				builder.appendExpression(visit(ctx.identification_variable()));
			}

			return builder;
		}

		return super.visitIdentificationVariableDeclarationOrCollectionMemberDeclaration(ctx);
	}

	@Override
	public QueryTokenStream visitJoin_association_path_expression(EqlParser.Join_association_path_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.TREAT() == null) {

			if (ctx.join_collection_valued_path_expression() != null) {
				builder.appendExpression(visit(ctx.join_collection_valued_path_expression()));
			} else if (ctx.join_single_valued_path_expression() != null) {
				builder.appendExpression(visit(ctx.join_single_valued_path_expression()));
			}
		} else {
			QueryRendererBuilder nested = QueryRenderer.builder();

			if (ctx.join_collection_valued_path_expression() != null) {

				nested.appendExpression(visit(ctx.join_collection_valued_path_expression()));
				nested.append(QueryTokens.expression(ctx.AS()));
				nested.appendExpression(visit(ctx.subtype()));

			} else if (ctx.join_single_valued_path_expression() != null) {

				nested.appendExpression(visit(ctx.join_single_valued_path_expression()));
				nested.append(QueryTokens.expression(ctx.AS()));
				nested.appendExpression(visit(ctx.subtype()));
			}

			builder.append(QueryTokens.token(ctx.TREAT()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(nested);
			builder.append(TOKEN_CLOSE_PAREN);
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

		QueryRendererBuilder nested = QueryRenderer.builder();

		nested.append(QueryTokens.token(ctx.IN()));
		nested.append(TOKEN_OPEN_PAREN);
		nested.appendInline(visit(ctx.collection_valued_path_expression()));
		nested.append(TOKEN_CLOSE_PAREN);

		QueryRendererBuilder builder = QueryRenderer.builder();
		builder.appendExpression(nested);

		if (ctx.AS() != null) {
			builder.append(QueryTokens.expression(ctx.AS()));
		}

		if (ctx.identification_variable() != null) {
			builder.appendExpression(visit(ctx.identification_variable()));
		}

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
	public QueryTokenStream visitGeneral_subpath(EqlParser.General_subpathContext ctx) {

		if (ctx.simple_subpath() != null) {
			return visit(ctx.simple_subpath());
		} else if (ctx.treated_subpath() != null) {

			List<ParseTree> items = new ArrayList<>(1 + ctx.single_valued_object_field().size());

			items.add(ctx.treated_subpath());
			items.addAll(ctx.single_valued_object_field());
			return QueryTokenStream.concat(items, this::visit, TOKEN_DOT);
		}

		return QueryTokenStream.empty();
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
		QueryRendererBuilder nested = QueryRenderer.builder();

		nested.appendExpression(visit(ctx.general_subpath()));
		nested.append(QueryTokens.expression(ctx.AS()));
		nested.appendExpression(visit(ctx.subtype()));

		builder.append(QueryTokens.token(ctx.TREAT()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(nested);
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
		builder.append(QueryTokenStream.concat(ctx.update_item(), this::visit, TOKEN_COMMA));

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
	public QueryTokenStream visitSelect_clause(EqlParser.Select_clauseContext ctx) {

		QueryRendererBuilder builder = prepareSelectClause(ctx);

		builder.appendExpression(QueryTokenStream.concat(ctx.select_item(), this::visit, TOKEN_COMMA));

		return builder;
	}

	QueryRendererBuilder prepareSelectClause(EqlParser.Select_clauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.SELECT()));

		if (ctx.DISTINCT() != null) {
			builder.append(QueryTokens.expression(ctx.DISTINCT()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitSelect_expression(EqlParser.Select_expressionContext ctx) {

		if (ctx.identification_variable() != null && ctx.OBJECT() != null) {

			QueryRendererBuilder builder = QueryRenderer.builder();

			builder.append(QueryTokens.token(ctx.OBJECT()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.identification_variable()));
			builder.append(TOKEN_CLOSE_PAREN);

			return builder;
		}

		return super.visitSelect_expression(ctx);
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
	public QueryTokenStream visitGroupby_clause(EqlParser.Groupby_clauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.GROUP()));
		builder.append(QueryTokens.expression(ctx.BY()));
		builder.appendExpression(QueryTokenStream.concat(ctx.groupby_item(), this::visit, TOKEN_COMMA));

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
	public QueryTokenStream visitSubquery_from_clause(EqlParser.Subquery_from_clauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.FROM()));
		builder.appendExpression(
				QueryTokenStream.concat(ctx.subselect_identification_variable_declaration(), this::visit, TOKEN_COMMA));

		return builder;
	}

	@Override
	public QueryTokenStream visitConditional_primary(EqlParser.Conditional_primaryContext ctx) {

		if (ctx.conditional_expression() != null) {
			return QueryTokenStream.group(visit(ctx.conditional_expression()));
		}

		return super.visitConditional_primary(ctx);
	}

	@Override
	public QueryTokenStream visitIn_expression(EqlParser.In_expressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.string_expression() != null) {
			builder.appendExpression(visit(ctx.string_expression()));
		}

		if (ctx.type_discriminator() != null) {
			builder.appendExpression(visit(ctx.type_discriminator()));
		}

		if (ctx.NOT() != null) {
			builder.append(QueryTokens.expression(ctx.NOT()));
		}

		if (ctx.IN() != null) {
			builder.append(QueryTokens.expression(ctx.IN()));
		}

		if (ctx.in_item() != null && !ctx.in_item().isEmpty()) {
			builder.append(QueryTokenStream.group(QueryTokenStream.concat(ctx.in_item(), this::visit, TOKEN_COMMA)));
		} else if (ctx.subquery() != null) {
			builder.append(QueryTokenStream.group(visit(ctx.subquery())));
		} else if (ctx.collection_valued_input_parameter() != null) {
			builder.append(visit(ctx.collection_valued_input_parameter()));
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
		builder.append(QueryTokenStream.group(visit(ctx.subquery())));

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

		builder.append(QueryTokenStream.group(visit(ctx.subquery())));

		return builder;
	}

	@Override
	public QueryTokenStream visitArithmetic_factor(EqlParser.Arithmetic_factorContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.op != null) {
			builder.append(QueryTokens.token(ctx.op));
		}

		builder.append(visit(ctx.arithmetic_primary()));

		return builder;
	}

	@Override
	public QueryTokenStream visitArithmetic_primary(EqlParser.Arithmetic_primaryContext ctx) {

		if (ctx.arithmetic_expression() != null) {
			return QueryTokenStream.group(visit(ctx.arithmetic_expression()));
		} else if (ctx.subquery() != null) {
			return QueryTokenStream.group(visit(ctx.subquery()));
		}

		return super.visitArithmetic_primary(ctx);
	}

	@Override
	public QueryTokenStream visitString_expression(EqlParser.String_expressionContext ctx) {

		if (ctx.subquery() != null) {
			return QueryTokenStream.group(visit(ctx.subquery()));
		}

		return super.visitString_expression(ctx);
	}

	@Override
	public QueryTokenStream visitDatetime_expression(EqlParser.Datetime_expressionContext ctx) {

		if (ctx.subquery() != null) {
			return QueryTokenStream.group(visit(ctx.subquery()));
		}

		return super.visitDatetime_expression(ctx);
	}

	@Override
	public QueryTokenStream visitBoolean_expression(EqlParser.Boolean_expressionContext ctx) {

		if (ctx.subquery() != null) {
			return QueryTokenStream.group(visit(ctx.subquery()));
		}

		return super.visitBoolean_expression(ctx);
	}

	@Override
	public QueryTokenStream visitEnum_expression(EqlParser.Enum_expressionContext ctx) {

		if (ctx.subquery() != null) {
			return QueryTokenStream.group(visit(ctx.subquery()));
		}

		return super.visitEnum_expression(ctx);
	}

	@Override
	public QueryTokenStream visitType_discriminator(EqlParser.Type_discriminatorContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.general_identification_variable() != null) {
			builder.append(visit(ctx.general_identification_variable()));
		} else if (ctx.single_valued_object_path_expression() != null) {
			builder.append(visit(ctx.single_valued_object_path_expression()));
		} else if (ctx.input_parameter() != null) {
			builder.append(visit(ctx.input_parameter()));
		}

		return QueryTokenStream.ofFunction(ctx.TYPE(), builder);
	}

	@Override
	public QueryTokenStream visitFunctions_returning_numerics(EqlParser.Functions_returning_numericsContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.LENGTH() != null) {
			return QueryTokenStream.ofFunction(ctx.LENGTH(), visit(ctx.string_expression(0)));
		} else if (ctx.LOCATE() != null) {

			builder.appendInline(visit(ctx.string_expression(0)));
			builder.append(TOKEN_COMMA);
			builder.appendInline(visit(ctx.string_expression(1)));

			if (ctx.arithmetic_expression() != null) {
				builder.append(TOKEN_COMMA);
				builder.appendInline(visit(ctx.arithmetic_expression(0)));
			}

			return QueryTokenStream.ofFunction(ctx.LOCATE(), builder);
		} else if (ctx.ABS() != null) {
			return QueryTokenStream.ofFunction(ctx.ABS(), visit(ctx.arithmetic_expression(0)));
		} else if (ctx.CEILING() != null) {
			return QueryTokenStream.ofFunction(ctx.CEILING(), visit(ctx.arithmetic_expression(0)));
		} else if (ctx.EXP() != null) {
			return QueryTokenStream.ofFunction(ctx.EXP(), visit(ctx.arithmetic_expression(0)));
		} else if (ctx.FLOOR() != null) {
			return QueryTokenStream.ofFunction(ctx.FLOOR(), visit(ctx.arithmetic_expression(0)));
		} else if (ctx.LN() != null) {
			return QueryTokenStream.ofFunction(ctx.LN(), visit(ctx.arithmetic_expression(0)));
		} else if (ctx.SIGN() != null) {
			return QueryTokenStream.ofFunction(ctx.SIGN(), visit(ctx.arithmetic_expression(0)));
		} else if (ctx.SQRT() != null) {
			return QueryTokenStream.ofFunction(ctx.SQRT(), visit(ctx.arithmetic_expression(0)));
		} else if (ctx.MOD() != null) {

			builder.appendInline(visit(ctx.arithmetic_expression(0)));
			builder.append(TOKEN_COMMA);
			builder.appendInline(visit(ctx.arithmetic_expression(1)));

			return QueryTokenStream.ofFunction(ctx.MOD(), builder);
		} else if (ctx.POWER() != null) {

			builder.appendInline(visit(ctx.arithmetic_expression(0)));
			builder.append(TOKEN_COMMA);
			builder.appendInline(visit(ctx.arithmetic_expression(1)));

			return QueryTokenStream.ofFunction(ctx.POWER(), builder);
		} else if (ctx.ROUND() != null) {

			builder.appendInline(visit(ctx.arithmetic_expression(0)));
			builder.append(TOKEN_COMMA);
			builder.appendInline(visit(ctx.arithmetic_expression(1)));

			return QueryTokenStream.ofFunction(ctx.ROUND(), builder);
		} else if (ctx.SIZE() != null) {
			return QueryTokenStream.ofFunction(ctx.SIZE(), visit(ctx.collection_valued_path_expression()));
		} else if (ctx.INDEX() != null) {
			return QueryTokenStream.ofFunction(ctx.INDEX(), visit(ctx.identification_variable()));
		} else if (ctx.extract_datetime_field() != null) {
			builder.append(visit(ctx.extract_datetime_field()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitFunctions_returning_strings(EqlParser.Functions_returning_stringsContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.CONCAT() != null) {
			return QueryTokenStream.ofFunction(ctx.CONCAT(),
					QueryTokenStream.concat(ctx.string_expression(), this::visit, TOKEN_COMMA));
		} else if (ctx.SUBSTRING() != null) {

			builder.append(visit(ctx.string_expression(0)));
			builder.append(TOKEN_COMMA);
			builder.appendInline(QueryTokenStream.concat(ctx.arithmetic_expression(), this::visit, TOKEN_COMMA));

			return QueryTokenStream.ofFunction(ctx.SUBSTRING(), builder);
		} else if (ctx.TRIM() != null) {

			if (ctx.trim_specification() != null) {
				builder.appendExpression(visit(ctx.trim_specification()));
			}
			if (ctx.trim_character() != null) {
				builder.appendExpression(visit(ctx.trim_character()));
			}
			if (ctx.FROM() != null) {
				builder.append(QueryTokens.expression(ctx.FROM()));
			}

			builder.append(visit(ctx.string_expression(0)));

			return QueryTokenStream.ofFunction(ctx.TRIM(), builder);
		} else if (ctx.LOWER() != null) {
			return QueryTokenStream.ofFunction(ctx.LOWER(),
					QueryTokenStream.concat(ctx.string_expression(), this::visit, TOKEN_COMMA));
		} else if (ctx.UPPER() != null) {
			return QueryTokenStream.ofFunction(ctx.UPPER(),
					QueryTokenStream.concat(ctx.string_expression(), this::visit, TOKEN_COMMA));
		} else if (ctx.LEFT() != null) {

			builder.append(visit(ctx.string_expression(0)));
			builder.append(TOKEN_COMMA);
			builder.append(visit(ctx.arithmetic_expression(0)));

			return QueryTokenStream.ofFunction(ctx.LEFT(), builder);
		} else if (ctx.RIGHT() != null) {

			builder.appendInline(visit(ctx.string_expression(0)));
			builder.append(TOKEN_COMMA);
			builder.append(visit(ctx.arithmetic_expression(0)));

			return QueryTokenStream.ofFunction(ctx.RIGHT(), builder);
		} else if (ctx.REPLACE() != null) {
			return QueryTokenStream.ofFunction(ctx.REPLACE(),
					QueryTokenStream.concat(ctx.string_expression(), this::visit, TOKEN_COMMA));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitArithmetic_cast_function(EqlParser.Arithmetic_cast_functionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.string_expression()));
		if (ctx.AS() != null) {
			builder.append(QueryTokens.expression(ctx.AS()));
		}
		builder.append(QueryTokens.token(ctx.f));

		return QueryTokenStream.ofFunction(ctx.CAST(), builder);
	}

	@Override
	public QueryTokenStream visitType_cast_function(EqlParser.Type_cast_functionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.scalar_expression()));

		if (ctx.AS() != null) {
			builder.append(QueryTokens.expression(ctx.AS()));
		}

		builder.appendInline(visit(ctx.identification_variable()));

		if (!CollectionUtils.isEmpty(ctx.numeric_literal())) {

			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(QueryTokenStream.concat(ctx.numeric_literal(), this::visit, TOKEN_COMMA));
			builder.append(TOKEN_CLOSE_PAREN);
		}

		return QueryTokenStream.ofFunction(ctx.CAST(), builder);
	}

	@Override
	public QueryTokenStream visitString_cast_function(EqlParser.String_cast_functionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.scalar_expression()));
		if (ctx.AS() != null) {
			builder.append(QueryTokens.expression(ctx.AS()));
		}
		builder.append(QueryTokens.token(ctx.STRING()));

		return QueryTokenStream.ofFunction(ctx.CAST(), builder);
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

		QueryRendererBuilder nested = QueryRenderer.builder();

		nested.appendExpression(visit(ctx.datetime_field()));
		nested.append(QueryTokens.expression(ctx.FROM()));
		nested.appendExpression(visit(ctx.datetime_expression()));

		return QueryTokenStream.ofFunction(ctx.EXTRACT(), nested);
	}

	@Override
	public QueryTokenStream visitExtract_datetime_part(EqlParser.Extract_datetime_partContext ctx) {

		QueryRendererBuilder nested = QueryRenderer.builder();

		nested.appendExpression(visit(ctx.datetime_part()));
		nested.append(QueryTokens.expression(ctx.FROM()));
		nested.appendExpression(visit(ctx.datetime_expression()));

		return QueryTokenStream.ofFunction(ctx.EXTRACT(), nested);
	}

	@Override
	public QueryTokenStream visitCoalesce_expression(EqlParser.Coalesce_expressionContext ctx) {

		return QueryTokenStream.ofFunction(ctx.COALESCE(),
				QueryTokenStream.concat(ctx.scalar_expression(), this::visit, TOKEN_COMMA));
	}

	@Override
	public QueryTokenStream visitNullif_expression(EqlParser.Nullif_expressionContext ctx) {

		return QueryTokenStream.ofFunction(ctx.NULLIF(),
				QueryTokenStream.concat(ctx.scalar_expression(), this::visit, TOKEN_COMMA));
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
	public QueryTokenStream visitEntity_name(EqlParser.Entity_nameContext ctx) {
		return QueryTokenStream.concat(ctx.reserved_word(), this::visit, TOKEN_DOT);
	}

	@Override
	public QueryTokenStream visitChildren(RuleNode node) {

		int childCount = node.getChildCount();

		if (childCount == 1 && node.getChild(0) instanceof RuleContext t) {
			return visit(t);
		}

		if (childCount == 1 && node.getChild(0) instanceof TerminalNode t) {
			return QueryTokens.token(t);
		}

		return QueryTokenStream.concatExpressions(node, this::visit);
	}

}
