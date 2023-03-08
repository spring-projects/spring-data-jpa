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

import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.springframework.data.domain.Sort;

/**
 * Implements the various parsing operations using {@link JpqlUtils} and {@link JpqlQueryTransformer}.
 *
 * @author Greg Turnquist
 * @since 3.1
 */
class JpqlQueryParser implements QueryParser {

	private final DeclaredQuery query;

	JpqlQueryParser(DeclaredQuery query) {
		this.query = query;
	}

	JpqlQueryParser(String query) {
		this(DeclaredQuery.of(query, false));
	}

	@Override
	public DeclaredQuery getDeclaredQuery() {
		return query;
	}

	@Override
	public ParserRuleContext parse() {
		return JpqlUtils.parseWithFastFailure(getQuery());
	}

	@Override
	public List<QueryParsingToken> doCreateQuery(ParserRuleContext parsedQuery, Sort sort) {
		return new JpqlQueryTransformer(sort).visit(parsedQuery);
	}

	@Override
	public List<QueryParsingToken> doCreateCountQuery(ParserRuleContext parsedQuery) {
		return new JpqlQueryTransformer(true).visit(parsedQuery);
	}

	@Override
	public String findAlias(ParserRuleContext parsedQuery) {

		JpqlQueryTransformer transformVisitor = new JpqlQueryTransformer();
		transformVisitor.visit(parsedQuery);
		return transformVisitor.getAlias();
	}

	@Override
	public List<QueryParsingToken> doFindProjection(ParserRuleContext parsedQuery) {

		JpqlQueryTransformer transformVisitor = new JpqlQueryTransformer();
		transformVisitor.visit(parsedQuery);
		return transformVisitor.getProjection();
	}

	@Override
	public boolean hasConstructor(ParserRuleContext parsedQuery) {

		JpqlQueryTransformer transformVisitor = new JpqlQueryTransformer();
		transformVisitor.visit(parsedQuery);
		return transformVisitor.hasConstructorExpression();
	}
}
