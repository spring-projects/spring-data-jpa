/*
 * Copyright 2024-2025 the original author or authors.
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

/**
 * {@link ParsedQueryIntrospector} for EQL queries.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Soomin Kim
 */
@SuppressWarnings("UnreachableCode")
class EqlQueryIntrospector extends EqlBaseVisitor<Void> implements ParsedQueryIntrospector<QueryInformation> {

	private final EqlQueryRenderer renderer = new EqlQueryRenderer();
	private final QueryInformationHolder introspection = new QueryInformationHolder();

	@Override
	public QueryInformation getParsedQueryInformation() {
		return new QueryInformation(introspection);
	}

	@Override
	public Void visitSelectQuery(EqlParser.SelectQueryContext ctx) {

		introspection.setStatementType(QueryInformation.StatementType.SELECT);
		return super.visitSelectQuery(ctx);
	}

	@Override
	public Void visitFromQuery(EqlParser.FromQueryContext ctx) {

		introspection.setStatementType(QueryInformation.StatementType.SELECT);
		return super.visitFromQuery(ctx);
	}

	@Override
	public Void visitUpdate_statement(EqlParser.Update_statementContext ctx) {

		introspection.setStatementType(QueryInformation.StatementType.UPDATE);
		return super.visitUpdate_statement(ctx);
	}

	@Override
	public Void visitDelete_statement(EqlParser.Delete_statementContext ctx) {

		introspection.setStatementType(QueryInformation.StatementType.DELETE);
		return super.visitDelete_statement(ctx);
	}

	@Override
	public Void visitSelect_clause(EqlParser.Select_clauseContext ctx) {

		introspection.captureProjection(ctx.select_item(), renderer::visitSelect_item);
		return super.visitSelect_clause(ctx);
	}

	@Override
	public Void visitRange_variable_declaration(EqlParser.Range_variable_declarationContext ctx) {

		if (ctx.identification_variable() != null && !EqlQueryRenderer.isSubquery(ctx)
				&& !EqlQueryRenderer.isSetQuery(ctx)) {
			introspection.capturePrimaryAlias(ctx.identification_variable().getText());
		}

		return super.visitRange_variable_declaration(ctx);
	}

	@Override
	public Void visitConstructor_expression(EqlParser.Constructor_expressionContext ctx) {

		introspection.constructorExpressionPresent();
		return super.visitConstructor_expression(ctx);
	}

}
