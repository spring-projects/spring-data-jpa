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

import java.util.List;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * @author Christoph Strobl
 */
public class JpqlComplianceTests extends SqlParserTests {

	@Override
	ParseTree parse(String query) {

		JpqlLexer lexer = new JpqlLexer(CharStreams.fromString(query));
		JpqlParser parser = new JpqlParser(new CommonTokenStream(lexer));

		parser.addErrorListener(new BadJpqlGrammarErrorListener(query));

		return parser.start();
	}

	@Override
	<T extends ParseTree> List<JpaQueryParsingToken> analyze(T parseTree) {
		return new JpqlQueryRenderer().visit(parseTree);
	}

}
