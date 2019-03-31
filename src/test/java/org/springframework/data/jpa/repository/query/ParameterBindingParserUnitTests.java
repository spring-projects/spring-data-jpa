/*
 * Copyright 2017-2019 the original author or authors.
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

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.springframework.data.jpa.repository.query.StringQuery.ParameterBindingParser;

/**
 * Unit tests for the {@link ParameterBindingParser}.
 *
 * @author Jens Schauder
 */
public class ParameterBindingParserUnitTests {

	@Test // DATAJPA-1200
	public void identificationOfParameters() {

		SoftAssertions softly = new SoftAssertions();

		checkHasParameter(softly, "select something from x where id = :id", true, "named parameter");
		checkHasParameter(softly, "in the :id middle", true, "middle");
		checkHasParameter(softly, ":id start", true, "beginning");
		checkHasParameter(softly, ":id", true, "alone");
		checkHasParameter(softly, "select something from x where id = :id", true, "named parameter");
		checkHasParameter(softly, ":UPPERCASE", true, "uppercase");
		checkHasParameter(softly, ":lowercase", true, "lowercase");
		checkHasParameter(softly, ":2something", true, "beginning digit");
		checkHasParameter(softly, ":2", true, "only digit");
		checkHasParameter(softly, ":.something", true, "dot"); // <--
		checkHasParameter(softly, ":_something", true, "underscore");
		checkHasParameter(softly, ":$something", true, "dollar"); // <--
		checkHasParameter(softly, ":\uFE0F", true, "non basic latin emoji"); // <--
		checkHasParameter(softly, ":\u4E01", true, "chinese japanese korean");
		checkHasParameter(softly, "select something from x where id = ?1", true, "indexed parameter");

		// <-- should we accept hash as named parameter start?
		checkHasParameter(softly, "select something from x where id = #something", false, "hash");

		checkHasParameter(softly, "no bind variable", false, "no bind variable");
		checkHasParameter(softly, ":\u2004whitespace", false, "non basic latin whitespace"); // <--
		checkHasParameter(softly, "::", false, "double colon");
		checkHasParameter(softly, ":", false, "end of query");
		checkHasParameter(softly, ":\u0003", false, "non-printable");
		checkHasParameter(softly, ":\u002A", false, "basic latin emoji");
		checkHasParameter(softly, "\\:", false, "escaped colon");
		checkHasParameter(softly, "::id", false, "double colon with identifier");
		checkHasParameter(softly, "\\:id", false, "escaped colon with identifier");

		softly.assertAll();
	}

	private void checkHasParameter(SoftAssertions softly, String query, boolean containsParameter, String label) {

		StringQuery stringQuery = new StringQuery(query);

		softly.assertThat(stringQuery.getParameterBindings().size()) //
				.describedAs(String.format("<%s> (%s)", query, label)) //
				.isEqualTo(containsParameter ? 1 : 0);
	}
}
