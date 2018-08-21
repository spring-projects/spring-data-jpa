/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository.query;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.Test;

/**
 * Unit tests for the QlParser;
 *
 * @author Jens Schauder
 */
public class QlParserUnitTests {

	Parser<String, List<Object>> tokenizer = QlParser.tokenizer;

	@Test // DATAJPA-1406
	public void quotedLiteral() {

		assertThat(QlParser.quotedLiteral.parse("'blah' something'")) //
				.isInstanceOf(Parser.Success.class) //
				.extracting(Parser.ParseResult::getRemainder) //
				.isEqualTo(" something'");
	}

	@Test // DATAJPA-1406
	public void doubleQuotedIdentifier() {

		assertThat(QlParser.doubleQuotedIdentifier.parse("\"blah\" something\"")) //
				.isInstanceOf(Parser.Success.class) //
				.extracting(Parser.ParseResult::getRemainder) //
				.isEqualTo(" something\"");
	}

	@Test // DATAJPA-1406
	public void blockComment() {

		assertThat(QlParser.blockComment.parse("/* something */ else*/")) //
				.isInstanceOf(Parser.Success.class) //
				.extracting(Parser.ParseResult::getRemainder) //
				.isEqualTo(" else*/");
	}

	@Test // DATAJPA-1406
	public void trivialSelect() {
		check("Select one from table", "Select", "one", "from", "table");
	}

	@Test // DATAJPA-1406
	public void lineBreak() {
		check("Select one \n from table", "Select", "one", "from", "table");
	}

	@Test // DATAJPA-1406
	public void extraWhiteSpace() {
		check("Select one,   two from table", "Select", "one", ",", "two", "from", "table");
	}

	@Test // DATAJPA-1406
	public void blockComments() {
		check("Select one /*,   two*/ from table", "Select", "one", "from", "table");
	}

	@Test // DATAJPA-1406
	public void inLineComment() {
		check("Select one --,   two \n from table", "Select", "one", "from", "table");
	}

	@Test // DATAJPA-1406
	public void numbers() {
		check("select 1 as one from dual", "select", "1", "as", "one", "from", "dual");
	}

	@Test // DATAJPA-1406
	public void stringLiterals() {
		check("select 'one' from dual", "select", "'one'", "from", "dual");
	}

	@Test // DATAJPA-1406
	public void bindVariables() {
		check("select :name from dual", "select", ":name", "from", "dual");
	}

	@Test // DATAJPA-1406
	public void subselect() {

		check("Select one, (select 1 from dual), three from table", //
				"Select", //
				"one", //
				",", //
				asList( //
						"(", //
						asList( //
								"select", //
								"1", //
								"from", //
								"dual" //
						), //
						")" //
				), //
				",", //
				"three", //
				"from", //
				"table" //
		);
	}

	@Test // DATAJPA-1406
	public void functionCall() {

		check("Select one('something'), two from table", //
				"Select", //
				"one", //
				asList( //
						"(", //
						singletonList("'something'"), //
						")" //
				), //
				",", //
				"two", //
				"from", //
				"table" //
		);
	}

	private void check(String toParse, Object... tokens) {

		List<Object> expectedTokens = asList(tokens);
		Parser.ParseResult<String, List<Object>> parseResult = tokenizer.parse(toParse);

		assertThat(parseResult).isEqualTo(new Parser.Success<>(expectedTokens, ""));
	}

}
