/*
 * Copyright 2013-2023 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * Unit tests for {@link ExpressionBasedStringQuery}.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Mark Paluch
 * @author Michael J. Simons
 * @author Diego Krupitza
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExpressionBasedStringQueryUnitTests {

	private static final SpelExpressionParser SPEL_PARSER = new SpelExpressionParser();
	@Mock JpaEntityMetadata<?> metadata;

	@Test // DATAJPA-170
	void shouldReturnQueryWithDomainTypeExpressionReplacedWithSimpleDomainTypeName() {

		when(metadata.getEntityName()).thenReturn("User");

		String source = "select from #{#entityName} u where u.firstname like :firstname";
		StringQuery query = new ExpressionBasedStringQuery(source, metadata, SPEL_PARSER, false);
		assertThat(query.getQueryString()).isEqualTo("select from User u where u.firstname like :firstname");
	}

	@Test // DATAJPA-424
	void renderAliasInExpressionQueryCorrectly() {

		when(metadata.getEntityName()).thenReturn("User");

		StringQuery query = new ExpressionBasedStringQuery("select u from #{#entityName} u", metadata, SPEL_PARSER, true);
		assertThat(query.getAlias()).isEqualTo("u");
		assertThat(query.getQueryString()).isEqualTo("select u from User u");
	}

	@Test // DATAJPA-1695
	void shouldDetectBindParameterCountCorrectly() {

		StringQuery query = new ExpressionBasedStringQuery(
				"select n from #{#entityName} n where (LOWER(n.name) LIKE LOWER(NULLIF(text(concat('%',:#{#networkRequest.name},'%')), '')) OR :#{#networkRequest.name} IS NULL )\"\n"
						+ "+ \"AND (LOWER(n.server) LIKE LOWER(NULLIF(text(concat('%',:#{#networkRequest.server},'%')), '')) OR :#{#networkRequest.server} IS NULL)\"\n"
						+ "+ \"AND (n.createdAt >= :#{#networkRequest.createdTime.startDateTime}) AND (n.createdAt <=:#{#networkRequest.createdTime.endDateTime})\"\n"
						+ "+ \"AND (n.updatedAt >= :#{#networkRequest.updatedTime.startDateTime}) AND (n.updatedAt <=:#{#networkRequest.updatedTime.endDateTime})",
				metadata, SPEL_PARSER, false);

		assertThat(query.getParameterBindings()).hasSize(8);
	}

	@Test // GH-2228
	void shouldDetectBindParameterCountCorrectlyWithJDBCStyleParameters() {

		StringQuery query = new ExpressionBasedStringQuery(
				"select n from #{#entityName} n where (LOWER(n.name) LIKE LOWER(NULLIF(text(concat('%',?#{#networkRequest.name},'%')), '')) OR ?#{#networkRequest.name} IS NULL )\"\n"
						+ "+ \"AND (LOWER(n.server) LIKE LOWER(NULLIF(text(concat('%',?#{#networkRequest.server},'%')), '')) OR ?#{#networkRequest.server} IS NULL)\"\n"
						+ "+ \"AND (n.createdAt >= ?#{#networkRequest.createdTime.startDateTime}) AND (n.createdAt <=?#{#networkRequest.createdTime.endDateTime})\"\n"
						+ "+ \"AND (n.updatedAt >= ?#{#networkRequest.updatedTime.startDateTime}) AND (n.updatedAt <=?#{#networkRequest.updatedTime.endDateTime})",
				metadata, SPEL_PARSER, false);

		assertThat(query.getParameterBindings()).hasSize(8);
	}

	@Test
	void shouldDetectComplexNativeQueriesWithSpelAsNonNative() {

		StringQuery query = new ExpressionBasedStringQuery(
				"select n from #{#entityName} n where (LOWER(n.name) LIKE LOWER(NULLIF(text(concat('%',?#{#networkRequest.name},'%')), '')) OR ?#{#networkRequest.name} IS NULL )\"\n"
						+ "+ \"AND (LOWER(n.server) LIKE LOWER(NULLIF(text(concat('%',?#{#networkRequest.server},'%')), '')) OR ?#{#networkRequest.server} IS NULL)\"\n"
						+ "+ \"AND (n.createdAt >= ?#{#networkRequest.createdTime.startDateTime}) AND (n.createdAt <=?#{#networkRequest.createdTime.endDateTime})\"\n"
						+ "+ \"AND (n.updatedAt >= ?#{#networkRequest.updatedTime.startDateTime}) AND (n.updatedAt <=?#{#networkRequest.updatedTime.endDateTime})",
				metadata, SPEL_PARSER, true);

		assertThat(query.isNativeQuery()).isFalse();
	}

	@Test
	void shouldDetectSimpleNativeQueriesWithSpelAsNonNative() {

		StringQuery query = new ExpressionBasedStringQuery("select n from #{#entityName} n", metadata, SPEL_PARSER, true);

		assertThat(query.isNativeQuery()).isFalse();
	}

	@Test
	void shouldDetectSimpleNativeQueriesWithoutSpelAsNative() {

		StringQuery query = new ExpressionBasedStringQuery("select u from User u", metadata, SPEL_PARSER, true);

		assertThat(query.isNativeQuery()).isTrue();
	}
}
