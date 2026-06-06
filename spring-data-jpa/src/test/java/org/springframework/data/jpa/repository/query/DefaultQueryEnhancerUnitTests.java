/*
 * Copyright 2023-present the original author or authors.
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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.data.domain.Sort;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.query.ReturnedType;

/**
 * TCK Tests for {@link DefaultQueryEnhancer}.
 *
 * @author Mark Paluch
 * @author Alim Naizabek
 * @author Jewoo Shin
 */
class DefaultQueryEnhancerUnitTests extends QueryEnhancerTckTests {

	@Override
	QueryEnhancer createQueryEnhancer(DeclaredQuery query) {
		return new DefaultQueryEnhancer(query);
	}

	@Override
	@Test // GH-2511, GH-2773
	@Disabled("Not properly supported by QueryUtils")
	void shouldDeriveNativeCountQueryWithVariable(String query, String expected) {}

	@Test // GH-3546
	void shouldApplySorting() {

		QueryEnhancer enhancer = createQueryEnhancer(DeclaredQuery.nativeQuery("SELECT e FROM Employee e"));

		String sql = enhancer.rewrite(new DefaultQueryRewriteInformation(Sort.by("foo", "bar"),
				ReturnedType.of(Object.class, Object.class, new SpelAwareProxyProjectionFactory())));

		assertThat(sql).isEqualTo("SELECT e FROM Employee e order by e.foo asc, e.bar asc");
	}

	@Test // GH-3811
	void shouldApplySortingWithNullHandling() {

		QueryEnhancer enhancer = createQueryEnhancer(DeclaredQuery.nativeQuery("SELECT e FROM Employee e"));

		String sql = enhancer.rewrite(new DefaultQueryRewriteInformation(
				Sort.by(Sort.Order.asc("foo").nullsFirst(), Sort.Order.asc("bar").nullsLast()),
				ReturnedType.of(Object.class, Object.class, new SpelAwareProxyProjectionFactory())));

		assertThat(sql).isEqualTo("SELECT e FROM Employee e order by e.foo asc nulls first, e.bar asc nulls last");
	}

	@Test // GH-2395
	void appliesSortingWithoutBreakingDerivedTableOrderByLimit() {

		QueryEnhancer enhancer = createQueryEnhancer(DeclaredQuery.nativeQuery(
				"SELECT n.id, n.name, n.status FROM n_entity n LEFT JOIN (SELECT s.status FROM entity_statuses s ORDER BY s.updated_at DESC LIMIT 1) st ON n.id = st.entity_id WHERE n.status = :status"));

		String sql = enhancer.rewrite(new DefaultQueryRewriteInformation(Sort.by("name"),
				ReturnedType.of(Object.class, Object.class, new SpelAwareProxyProjectionFactory())));

		assertThat(sql).isEqualTo(
				"SELECT n.id, n.name, n.status FROM n_entity n LEFT JOIN (SELECT s.status FROM entity_statuses s ORDER BY s.updated_at DESC LIMIT 1) st ON n.id = st.entity_id WHERE n.status = :status order by n.name asc");
	}
}
