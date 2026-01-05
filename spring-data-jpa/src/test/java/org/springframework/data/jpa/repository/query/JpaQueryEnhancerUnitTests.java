/*
 * Copyright 2025-present the original author or authors.
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

import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link JpaQueryEnhancer}.
 *
 * @author Mark Paluch
 */
class JpaQueryEnhancerUnitTests {

	@ParameterizedTest // GH-3997
	@MethodSource("queryEnhancers")
	void shouldRemoveCommentsFromJpql(Function<String, JpaQueryEnhancer<?>> enhancerFunction) {

		QueryEnhancer enhancer = enhancerFunction
				.apply("SELECT /* foo */ some_alias FROM /* some other */ table_name some_alias");

		assertThat(enhancer.getQuery().getQueryString())
				.isEqualToIgnoringCase("SELECT some_alias FROM table_name some_alias");

		enhancer = enhancerFunction.apply("""
				SELECT /* multi
				line
				comment
				*/ some_alias FROM /* some other */ table_name some_alias
				""");

		assertThat(enhancer.getQuery().getQueryString())
				.isEqualToIgnoringCase("SELECT some_alias FROM table_name some_alias");
	}

	@ParameterizedTest // GH-2856
	@MethodSource("queryEnhancers")
	@SuppressWarnings("NullableProblems")
	void detectsQueryType(Function<String, JpaQueryEnhancer<QueryInformation>> enhancerFunction) {

		JpaQueryEnhancer<QueryInformation> select = enhancerFunction.apply("SELECT some_alias FROM table_name some_alias");
		assertThat(select.getQueryInformation().getStatementType()).isEqualTo(QueryInformation.StatementType.SELECT);

		JpaQueryEnhancer<QueryInformation> from = enhancerFunction.apply("FROM table_name some_alias");
		assertThat(from.getQueryInformation().getStatementType()).isEqualTo(QueryInformation.StatementType.SELECT);

		JpaQueryEnhancer<QueryInformation> delete = enhancerFunction.apply("DELETE FROM table_name some_alias");
		assertThat(delete.getQueryInformation().getStatementType()).isEqualTo(QueryInformation.StatementType.DELETE);

		JpaQueryEnhancer<QueryInformation> update = enhancerFunction
				.apply("UPDATE table_name some_alias SET some_alias.property = ?1");
		assertThat(update.getQueryInformation().getStatementType()).isEqualTo(QueryInformation.StatementType.UPDATE);

		if (((Object) select) instanceof JpaQueryEnhancer.HqlQueryParser) {

			JpaQueryEnhancer<QueryInformation> insert = enhancerFunction.apply("INSERT Person(name) VALUES(?1)");
			assertThat(insert.getQueryInformation().getStatementType()).isEqualTo(QueryInformation.StatementType.INSERT);
		}
	}

	static Stream<Function<String, JpaQueryEnhancer<?>>> queryEnhancers() {
		return Stream.of(JpaQueryEnhancer::forHql, JpaQueryEnhancer::forEql, JpaQueryEnhancer::forJpql);
	}
}
