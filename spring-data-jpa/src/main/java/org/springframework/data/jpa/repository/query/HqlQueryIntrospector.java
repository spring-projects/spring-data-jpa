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

import org.springframework.data.jpa.repository.query.HqlParser.VariableContext;

/**
 * {@link ParsedQueryIntrospector} for HQL queries.
 *
 * @author Mark Paluch
 * @author Oscar Fanchin
 * @author Soomin Kim
 */
@SuppressWarnings({ "UnreachableCode" })
class HqlQueryIntrospector extends HqlBaseVisitor<Void> implements ParsedQueryIntrospector<HibernateQueryInformation> {

	private final HqlQueryRenderer renderer = new HqlQueryRenderer();
	private final QueryInformationHolder introspection = new QueryInformationHolder();

	private boolean hasCte = false;
	private boolean hasFromFunction = false;

	@Override
	public HibernateQueryInformation getParsedQueryInformation() {
		return new HibernateQueryInformation(introspection, hasCte, hasFromFunction);
	}

	@Override
	public Void visitSelectStatement(HqlParser.SelectStatementContext ctx) {

		introspection.setStatementType(QueryInformation.StatementType.SELECT);
		return super.visitSelectStatement(ctx);
	}

	@Override
	public Void visitFromQuery(HqlParser.FromQueryContext ctx) {

		introspection.setStatementType(QueryInformation.StatementType.SELECT);
		return super.visitFromQuery(ctx);
	}

	@Override
	public Void visitInsertStatement(HqlParser.InsertStatementContext ctx) {

		introspection.setStatementType(QueryInformation.StatementType.INSERT);
		return super.visitInsertStatement(ctx);
	}

	@Override
	public Void visitUpdateStatement(HqlParser.UpdateStatementContext ctx) {

		introspection.setStatementType(QueryInformation.StatementType.UPDATE);
		return super.visitUpdateStatement(ctx);
	}

	@Override
	public Void visitDeleteStatement(HqlParser.DeleteStatementContext ctx) {

		introspection.setStatementType(QueryInformation.StatementType.DELETE);
		return super.visitDeleteStatement(ctx);
	}

	@Override
	public Void visitSelectClause(HqlParser.SelectClauseContext ctx) {

		introspection.captureProjection(ctx.selectionList().selection(), renderer::visitSelection);

		return super.visitSelectClause(ctx);
	}

	@Override
	public Void visitCte(HqlParser.CteContext ctx) {
		this.hasCte = true;
		return super.visitCte(ctx);
	}

	@Override
	public Void visitRootEntity(HqlParser.RootEntityContext ctx) {

		if (ctx.variable() != null && !HqlQueryRenderer.isSubquery(ctx)
				&& !HqlQueryRenderer.isSetQuery(ctx)) {
			capturePrimaryAlias(ctx.variable());
		}

		return super.visitRootEntity(ctx);
	}

	@Override
	public Void visitRootSubquery(HqlParser.RootSubqueryContext ctx) {

		if (ctx.variable() != null && !HqlQueryRenderer.isSubquery(ctx)
				&& !HqlQueryRenderer.isSetQuery(ctx)) {
			capturePrimaryAlias(ctx.variable());
		}

		return super.visitRootSubquery(ctx);
	}

	@Override
	public Void visitRootFunction(HqlParser.RootFunctionContext ctx) {

		if (ctx.variable() != null && !HqlQueryRenderer.isSubquery(ctx)
				&& !HqlQueryRenderer.isSetQuery(ctx)) {
			capturePrimaryAlias(ctx.variable());
			this.hasFromFunction = true;
		}

		return super.visitRootFunction(ctx);
	}

	@Override
	public Void visitInstantiation(HqlParser.InstantiationContext ctx) {

		introspection.constructorExpressionPresent();
		return super.visitInstantiation(ctx);
	}

	private void capturePrimaryAlias(VariableContext ctx) {
		introspection
				.capturePrimaryAlias((ctx.nakedIdentifier() != null ? ctx.nakedIdentifier() : ctx.identifier()).getText());
	}

}
