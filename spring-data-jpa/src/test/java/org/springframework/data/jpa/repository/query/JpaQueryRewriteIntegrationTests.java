/*
 * Copyright 2022-2023 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryRewriter;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Unit tests for repository with {@link Query} and {@link QueryRewrite}.
 *
 * @author Greg Turnquist
 * @author Krzysztof Krason
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
class JpaQueryRewriteIntegrationTests {

	@Autowired private UserRepositoryWithRewriter repository;

	// Results
	static final String ORIGINAL_QUERY = "original query";
	static final String REWRITTEN_QUERY = "rewritten query";
	static final String SORT = "sort";
	static Map<String, String> results = new HashMap<>();

	@BeforeEach
	void setUp() {
		results.clear();
	}

	@Test
	void nativeQueryShouldHandleRewrites() {

		repository.findByNativeQuery("Matthews");

		assertThat(results).containsExactly( //
				entry(ORIGINAL_QUERY, "select original_user_alias.* from SD_USER original_user_alias"), //
				entry(REWRITTEN_QUERY, "select rewritten_user_alias.* from SD_USER rewritten_user_alias"), //
				entry(SORT, Sort.unsorted().toString()));
	}

	@Test
	void nonNativeQueryShouldHandleRewrites() {

		repository.findByNonNativeQuery("Matthews");

		assertThat(results).containsExactly( //
				entry(ORIGINAL_QUERY, "select original_user_alias from User original_user_alias"), //
				entry(REWRITTEN_QUERY, "select rewritten_user_alias from User rewritten_user_alias"), //
				entry(SORT, Sort.unsorted().toString()));
	}

	@Test
	void nonNativeQueryWithSortShouldHandleRewrites() {

		repository.findByNonNativeSortedQuery("Matthews", Sort.by("lastname"));

		assertThat(results).containsExactly( //
				entry(ORIGINAL_QUERY,
						"select original_user_alias from User original_user_alias order by original_user_alias.lastname asc"), //
				entry(REWRITTEN_QUERY,
						"select rewritten_user_alias from User rewritten_user_alias order by rewritten_user_alias.lastname asc"), //
				entry(SORT, Sort.by("lastname").ascending().toString()));

		repository.findByNonNativeSortedQuery("Matthews", Sort.by("firstname").descending());

		assertThat(results).containsExactly( //
				entry(ORIGINAL_QUERY,
						"select original_user_alias from User original_user_alias order by original_user_alias.firstname desc"), //
				entry(REWRITTEN_QUERY,
						"select rewritten_user_alias from User rewritten_user_alias order by rewritten_user_alias.firstname desc"), //
				entry(SORT, Sort.by("firstname").descending().toString()));
	}

	@Test
	void nonNativeQueryWithPageableShouldHandleRewrites() {

		repository.findByNonNativePagedQuery("Matthews", PageRequest.of(2, 1));

		assertThat(results).containsExactly( //
				entry(ORIGINAL_QUERY, "select original_user_alias from User original_user_alias"), //
				entry(REWRITTEN_QUERY, "select rewritten_user_alias from User rewritten_user_alias"), //
				entry(SORT, Sort.unsorted().toString()));
	}

	@Test
	void nativeQueryWithNoRewriteAnnotationShouldNotDoRewrites() {

		repository.findByNativeQueryWithNoRewrite("Matthews");

		assertThat(results).isEmpty();
	}

	@Test
	void nonNativeQueryWithNoRewriteAnnotationShouldNotDoRewrites() {

		repository.findByNonNativeQueryWithNoRewrite("Matthews");

		assertThat(results).isEmpty();
	}

	@Test
	void nativeQueryShouldHandleRewritesUsingRepositoryRewriter() {

		repository.findByNativeQueryUsingRepository("Matthews");

		assertThat(results).containsExactly( //
				entry(ORIGINAL_QUERY, "select original_user_alias.* from SD_USER original_user_alias"), //
				entry(REWRITTEN_QUERY, "select rewritten_user_alias.* from SD_USER rewritten_user_alias"), //
				entry(SORT, Sort.unsorted().toString()));
	}

	@Test // GH-1380
	void counting() {

		repository.saveAllAndFlush(List.of( //
				new User("Frodo", "Baggins", "ringdude@aol.com"), //
				new User("Bilbo", "Baggins", "riddler@hotmail.com"), //
				new User("Samwise", "Gamgee", "gardener@gmail.com")));

		assertThat(repository.count()).isEqualTo(3);
		assertThat(repository.countDistinctByLastname("Baggins")).isEqualTo(2);
		assertThat(repository.countDistinctByLastname("Gamgee")).isOne();
	}

	public interface UserRepositoryWithRewriter
			extends JpaRepository<User, Integer>, QueryRewriter, JpaSpecificationExecutor<User> {

		@Query(value = "select original_user_alias.* from SD_USER original_user_alias", nativeQuery = true,
				queryRewriter = TestQueryRewriter.class)
		List<User> findByNativeQuery(String param);

		@Query(value = "select original_user_alias from User original_user_alias", queryRewriter = TestQueryRewriter.class)
		List<User> findByNonNativeQuery(String param);

		@Query(value = "select original_user_alias from User original_user_alias", queryRewriter = TestQueryRewriter.class)
		List<User> findByNonNativeSortedQuery(String param, Sort sort);

		@Query(value = "select original_user_alias from User original_user_alias", queryRewriter = TestQueryRewriter.class)
		List<User> findByNonNativePagedQuery(String param, Pageable pageable);

		@Query(value = "select original_user_alias.* from SD_USER original_user_alias", nativeQuery = true)
		List<User> findByNativeQueryWithNoRewrite(String param);

		@Query(value = "select original_user_alias from User original_user_alias")
		List<User> findByNonNativeQueryWithNoRewrite(String param);

		@Query(value = "select original_user_alias.* from SD_USER original_user_alias", nativeQuery = true,
				queryRewriter = UserRepositoryWithRewriter.class)
		List<User> findByNativeQueryUsingRepository(String param);

		long countDistinctByLastname(String lastname);

		@Override
		default String rewrite(String query, Sort sort) {
			return replaceAlias(query, sort);
		}
	}

	static class TestQueryRewriter implements QueryRewriter {

		@Override
		public String rewrite(String query, Sort sort) {
			return replaceAlias(query, sort);
		}
	}

	/**
	 * One query rewriter function to rule them all!
	 *
	 * @param query
	 * @param sort
	 */
	private static String replaceAlias(String query, Sort sort) {

		String rewrittenQuery = query.replaceAll("original_user_alias", "rewritten_user_alias");

		// Capture results for testing.
		results.put(ORIGINAL_QUERY, query);
		results.put(REWRITTEN_QUERY, rewrittenQuery);
		results.put(SORT, sort.toString());

		return rewrittenQuery;
	}

	@Configuration
	@ImportResource("classpath:infrastructure.xml")
	@EnableJpaRepositories(considerNestedRepositories = true, basePackageClasses = UserRepositoryWithRewriter.class, //
			includeFilters = @ComponentScan.Filter(value = { UserRepositoryWithRewriter.class },
					type = FilterType.ASSIGNABLE_TYPE))
	static class JpaRepositoryConfig {

		@Bean
		QueryRewriter queryRewriter() {
			return new TestQueryRewriter();
		}

	}
}
