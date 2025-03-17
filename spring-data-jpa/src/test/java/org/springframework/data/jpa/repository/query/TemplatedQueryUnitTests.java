/*
 * Copyright 2013-2025 the original author or authors.
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

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.data.jpa.repository.query.ParameterBinding.LikeParameterBinding;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.data.repository.query.parser.Part.Type;

/**
 * Unit tests for {@link TemplatedQuery}.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Mark Paluch
 * @author Michael J. Simons
 * @author Diego Krupitza
 * @author Greg Turnquist
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TemplatedQueryUnitTests {

	private static final JpaQueryConfiguration CONFIG = new JpaQueryConfiguration(QueryRewriterProvider.simple(),
			QueryEnhancerSelector.DEFAULT_SELECTOR, ValueExpressionDelegate.create(), EscapeCharacter.DEFAULT);

	@Mock JpaEntityMetadata<?> metadata;

	@BeforeEach
	void setUp() {
		when(metadata.getEntityName()).thenReturn("User");
	}

	@Test // DATAJPA-170
	void shouldReturnQueryWithDomainTypeExpressionReplacedWithSimpleDomainTypeName() {

		String source = "select u from #{#entityName} u where u.firstname like :firstname";
		EntityQuery query = jpqlEntityQuery(source);
		assertThat(query.getQueryString()).isEqualTo("select u from User u where u.firstname like :firstname");
	}

	@Test // DATAJPA-424
	void renderAliasInExpressionQueryCorrectly() {

		DefaultEntityQuery query = jpqlEntityQuery("select u from #{#entityName} u");
		assertThat(query.getAlias()).isEqualTo("u");
		assertThat(query.getQueryString()).isEqualTo("select u from User u");
	}

	@Test // DATAJPA-1695
	void shouldDetectBindParameterCountCorrectly() {

		EntityQuery query = jpqlEntityQuery(
				"select n from #{#entityName} n where (LOWER(n.name) LIKE LOWER(:#{#networkRequest.name})) OR :#{#networkRequest.name} IS NULL "
						+ "AND (LOWER(n.server) LIKE LOWER(:#{#networkRequest.server})) OR :#{#networkRequest.server} IS NULL "
						+ "AND (n.createdAt >= :#{#networkRequest.createdTime.startDateTime}) AND (n.createdAt <=:#{#networkRequest.createdTime.endDateTime}) "
						+ "AND (n.updatedAt >= :#{#networkRequest.updatedTime.startDateTime}) AND (n.updatedAt <=:#{#networkRequest.updatedTime.endDateTime})");

		assertThat(query.getParameterBindings()).hasSize(8);
	}

	@Test // GH-2228
	void shouldDetectBindParameterCountCorrectlyWithJDBCStyleParameters() {

		EntityQuery query = jpqlEntityQuery(
				"select n from #{#entityName} n where (LOWER(n.name) LIKE LOWER(NULLIF(text(concat('%',?#{#networkRequest.name},'%')), '')) OR ?#{#networkRequest.name} IS NULL )"
						+ "AND (LOWER(n.server) LIKE LOWER(NULLIF(text(concat('%',?#{#networkRequest.server},'%')), '')) OR ?#{#networkRequest.server} IS NULL)"
						+ "AND (n.createdAt >= ?#{#networkRequest.createdTime.startDateTime}) AND (n.createdAt <=?#{#networkRequest.createdTime.endDateTime})"
						+ "AND (n.updatedAt >= ?#{#networkRequest.updatedTime.startDateTime}) AND (n.updatedAt <=?#{#networkRequest.updatedTime.endDateTime})");

		assertThat(query.getParameterBindings()).hasSize(8);
	}

	@Test
	void shouldDetectComplexNativeQueriesWithSpelAsNonNative() {

		DefaultEntityQuery query = jpqlEntityQuery(
				"select n from #{#entityName} n where (LOWER(n.name) LIKE LOWER(NULLIF(text(concat('%',?#{#networkRequest.name},'%')), '')) OR ?#{#networkRequest.name} IS NULL )"
						+ "AND (LOWER(n.server) LIKE LOWER(NULLIF(text(concat('%',?#{#networkRequest.server},'%')), '')) OR ?#{#networkRequest.server} IS NULL)"
						+ "AND (n.createdAt >= ?#{#networkRequest.createdTime.startDateTime}) AND (n.createdAt <=?#{#networkRequest.createdTime.endDateTime})"
						+ "AND (n.updatedAt >= ?#{#networkRequest.updatedTime.startDateTime}) AND (n.updatedAt <=?#{#networkRequest.updatedTime.endDateTime})");

		assertThat(query.isNative()).isFalse();
	}

	@Test
	void shouldDetectSimpleNativeQueriesWithoutSpelAsNative() {

		DefaultEntityQuery query = nativeEntityQuery("select u from User u");

		assertThat(query.isNative()).isTrue();
	}

	@Test // GH-3041
	void namedExpressionsShouldCreateLikeBindings() {

		EntityQuery query = jpqlEntityQuery(
				"select u from User u where u.firstname like %:#{foo} or u.firstname like :#{foo}%");

		assertThat(query.hasParameterBindings()).isTrue();
		assertThat(query.getQueryString()).isEqualTo(
				"select u from User u where u.firstname like :__$synthetic$__1 or u.firstname like :__$synthetic$__2");

		List<ParameterBinding> bindings = query.getParameterBindings();
		assertThat(bindings).hasSize(2);

		LikeParameterBinding binding = (LikeParameterBinding) bindings.get(0);
		assertThat(binding).isNotNull();
		assertThat(binding.getName()).isEqualTo("__$synthetic$__1");
		assertThat(binding.getType()).isEqualTo(Type.ENDING_WITH);

		binding = (LikeParameterBinding) bindings.get(1);
		assertThat(binding).isNotNull();
		assertThat(binding.getName()).isEqualTo("__$synthetic$__2");
		assertThat(binding.getType()).isEqualTo(Type.STARTING_WITH);
	}

	@Test // GH-3041
	void indexedExpressionsShouldCreateLikeBindings() {

		EntityQuery query = jpqlEntityQuery(
				"select u from User u where u.firstname like %?#{foo} or u.firstname like ?#{foo}%");

		assertThat(query.hasParameterBindings()).isTrue();
		assertThat(query.getQueryString())
				.isEqualTo("select u from User u where u.firstname like ?1 or u.firstname like ?2");

		List<ParameterBinding> bindings = query.getParameterBindings();
		assertThat(bindings).hasSize(2);

		LikeParameterBinding binding = (LikeParameterBinding) bindings.get(0);
		assertThat(binding).isNotNull();
		assertThat(binding.getPosition()).isEqualTo(1);
		assertThat(binding.getType()).isEqualTo(Type.ENDING_WITH);

		binding = (LikeParameterBinding) bindings.get(1);
		assertThat(binding).isNotNull();
		assertThat(binding.getPosition()).isEqualTo(2);
		assertThat(binding.getType()).isEqualTo(Type.STARTING_WITH);
	}

	@Test
	void doesTemplatingWhenEntityNameSpelIsPresent() {

		EntityQuery query = jpqlEntityQuery("select #{#entityName + 'Hallo'} from #{#entityName} u");

		assertThat(query.getQueryString()).isEqualTo("select UserHallo from User u");
	}

	@Test
	void doesNoTemplatingWhenEntityNameSpelIsNotPresent() {

		EntityQuery query = jpqlEntityQuery("select #{#entityName + 'Hallo'} from User u");

		assertThat(query.getQueryString()).isEqualTo("select UserHallo from User u");
	}

	@Test
	void doesTemplatingWhenEntityNameSpelIsPresentForBindParameter() {

		EntityQuery query = jpqlEntityQuery("select u from #{#entityName} u where name = :#{#something}");

		assertThat(query.getQueryString()).isEqualTo("select u from User u where name = :__$synthetic$__1");
	}

	private DefaultEntityQuery nativeEntityQuery(String source) {
		return (DefaultEntityQuery) TemplatedQuery.create(DeclaredQuery.nativeQuery(source), metadata, CONFIG);
	}

	private DefaultEntityQuery jpqlEntityQuery(String source) {
		return (DefaultEntityQuery) TemplatedQuery.create(DeclaredQuery.jpqlQuery(source), metadata, CONFIG);
	}
}
