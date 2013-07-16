/*
 * Copyright 2013 the original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.data.jpa.domain.sample.User;

/**
 * Unit tests for {@link ExpressionBasedStringQuery}.
 * 
 * @author Thomas Darimont
 */
public class ExpressionBasedStringQueryUnitTests {

	/**
	 * @see DATAJPA-170
	 */
	@Test
	public void shouldReturnQueryWithDomainTypeExpressionReplacedWithSimpleDomainTypeName() {

		String source = "select from #{#domainType} u where u.firstname like :firstname";
		StringQuery query = new ExpressionBasedStringQuery(source, User.class);
		String result = query.getQuery();

		assertThat(result, is("select from User u where u.firstname like :firstname"));
	}
}
