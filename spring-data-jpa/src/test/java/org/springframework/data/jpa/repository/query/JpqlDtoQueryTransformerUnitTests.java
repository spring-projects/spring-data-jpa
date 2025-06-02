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

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.QueryMethod;

/**
 * Unit tests for {@link DtoProjectionTransformerDelegate}.
 *
 * @author Mark Paluch
 */
class JpqlDtoQueryTransformerUnitTests extends AbstractDtoQueryTransformerUnitTests<JpaQueryEnhancer.JpqlQueryParser> {

	@Override
	JpaQueryEnhancer.JpqlQueryParser parse(String query) {
		return JpaQueryEnhancer.JpqlQueryParser.parseQuery(query);
	}

	@Override
	ParseTreeVisitor<QueryTokenStream> getTransformer(JpaQueryEnhancer.JpqlQueryParser parser, QueryMethod method) {
		return new JpqlSortedQueryTransformer(Sort.unsorted(), parser.getQueryInformation(),
				method.getResultProcessor().getReturnedType());
	}

}
