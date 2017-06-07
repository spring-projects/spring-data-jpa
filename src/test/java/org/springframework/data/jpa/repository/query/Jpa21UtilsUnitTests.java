/*
 * Copyright 2015-2017 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Map;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Subgraph;

import org.junit.Test;

import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;

/**
 * Unit tests for {@link Jpa21Utils}.
 * 
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author MD Sayem Ahmed
 */
public class Jpa21UtilsUnitTests {

	@Test // DATAJPA-696, DATAJPA-1093
	public void shouldBuildCorrectSubgraphForJpaEntityGraph() throws Exception {

		EntityGraph<?> entityGraph = mock(EntityGraph.class);
		Subgraph<?> subgraph = mock(Subgraph.class);
		doReturn(subgraph).when(entityGraph).addSubgraph(anyString());

		JpaEntityGraph jpaEntityGraph = new JpaEntityGraph("foo", EntityGraphType.FETCH,
				new String[] { "foo", "gugu.gaga" });

		Jpa21Utils.configureFetchGraphFrom(jpaEntityGraph, entityGraph);

		verify(entityGraph, times(1)).addAttributeNodes("foo");
		verify(entityGraph, times(1)).addSubgraph("gugu");
		verify(subgraph, times(1)).addAttributeNodes("gaga");
	}

	@Test // DATAJPA-1093
	public void givenNullEntityGraphWhenTryGetFetchGraphHintsThenReturnsEmptyMap() {
		Map<String, Object> graphHints = Jpa21Utils.tryGetFetchGraphHints(mock(EntityManager.class), null, Object.class);

		assertThat(graphHints).isEmpty();
	}

	@Test // DATAJPA-1093
	public void givenCreatingDynamicGraphFailsWhenTryGetFetchGraphHintsThenReturnsEmptyMap() {
		EntityManager entityManager = mock(EntityManager.class);
		Class<Object> entityType = Object.class;
		when(entityManager.createEntityGraph(entityType)).thenReturn(null);

		Map<String, Object> graphHints = Jpa21Utils.tryGetFetchGraphHints(entityManager, mock(JpaEntityGraph.class),
				entityType);

		assertThat(graphHints).isEmpty();
	}

	@Test // DATAJPA-1093
	public void givenGetEntityGraphReturnsNullWhenTryGetFetchGraphHintsThenReturnsEmptyMap() {
		EntityManager entityManager = mock(EntityManager.class);
		Class<Object> entityType = Object.class;
		String graphName = "sample graph name";
		JpaEntityGraph jpaEntityGraph = mock(JpaEntityGraph.class);
		when(jpaEntityGraph.getName()).thenReturn(graphName);
		when(entityManager.getEntityGraph(graphName)).thenReturn(null);

		Map<String, Object> graphHints = Jpa21Utils.tryGetFetchGraphHints(entityManager, jpaEntityGraph, entityType);

		assertThat(graphHints).isEmpty();
	}

	@Test // DATAJPA-1093
	public void givenGetEntityGraphThrowsExceptionWhenTryGetFetchGraphHintsThenCreatesEntityGraph() {
		EntityManager entityManager = mock(EntityManager.class);
		Class<Object> entityType = Object.class;
		JpaEntityGraph jpaEntityGraph = mock(JpaEntityGraph.class);
		EntityGraphType entityGraphType = EntityGraphType.FETCH;
		EntityGraph entityGraph = mock(EntityGraph.class);
		when(jpaEntityGraph.getType()).thenReturn(entityGraphType);
		when(jpaEntityGraph.isAdHocEntityGraph()).thenReturn(true);
		when(entityManager.getEntityGraph(any())).thenThrow(Exception.class);
		when(entityManager.createEntityGraph(entityType)).thenReturn(entityGraph);

		Map<String, Object> graphHints = Jpa21Utils.tryGetFetchGraphHints(entityManager, jpaEntityGraph, entityType);

		assertThat(graphHints).containsOnlyKeys(entityGraphType.getKey());
		assertThat(graphHints).containsValue(entityGraph);
	}
}
