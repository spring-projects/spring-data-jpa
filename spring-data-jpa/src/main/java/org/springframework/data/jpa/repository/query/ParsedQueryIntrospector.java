/*
 * Copyright 2024-present the original author or authors.
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

import org.antlr.v4.runtime.tree.ParseTree;

/**
 * Interface defining an introspector for String-queries providing details about the primary table alias, the primary
 * selection projection and whether the query makes use of constructor expressions.
 *
 * @author Mark Paluch
 * @since 3.5
 */
interface ParsedQueryIntrospector<I extends QueryInformation> {

	/**
	 * Visit the parsed tree to introspect the AST tree.
	 *
	 * @param tree
	 * @return
	 */
	Void visit(ParseTree tree);

	/**
	 * Returns the parsed query information. The information is available after the {@link #visit(ParseTree)
	 * introspection} has been completed.
	 *
	 * @return parsed query information.
	 */
	I getParsedQueryInformation();
}
