/*
 * Copyright 2013-2019 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * Unit tests for {@link ExpressionBasedStringQuery}.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Jens Schauder
 */
@RunWith(MockitoJUnitRunner.class)
public class ExpressionBasedStringQueryUnitTests {

	static final SpelExpressionParser SPEL_PARSER = new SpelExpressionParser();
	@Mock JpaEntityMetadata<?> metadata;

	@Test // DATAJPA-170
	public void shouldReturnQueryWithDomainTypeExpressionReplacedWithSimpleDomainTypeName() {

		when(metadata.getEntityName()).thenReturn("User");

		String source = "select from #{#entityName} u where u.firstname like :firstname";
		StringQuery query = new ExpressionBasedStringQuery(source, metadata, SPEL_PARSER);
		assertThat(query.getQueryString()).isEqualTo("select from User u where u.firstname like :firstname");
	}

	@Test // DATAJPA-424
	public void renderAliasInExpressionQueryCorrectly() {

		when(metadata.getEntityName()).thenReturn("User");

		StringQuery query = new ExpressionBasedStringQuery("select u from #{#entityName} u", metadata, SPEL_PARSER);
		assertThat(query.getAlias()).isEqualTo("u");
		assertThat(query.getQueryString()).isEqualTo("select u from User u");
	}

}
