/*
 * Copyright 2015 the original author or authors.
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

import static org.mockito.Mockito.*;

import javax.persistence.EntityGraph;
import javax.persistence.Subgraph;

import org.junit.Test;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;

/**
 * Tests for {@link Jpa21Utils}.
 * 
 * @author Thomas Darimont
 */
public class Jpa21UtilsTests {

	/**
	 * @see DATAJPA-696
	 */
	@Test
	public void shouldBuildCorrectSubgraphForJpaEntityGraph() throws Exception {

		EntityGraph<?> entityGraph = mock(EntityGraph.class);
		Subgraph<?> guguSubgraph = mock(Subgraph.class);
		Subgraph<?> blaSubgraph = mock(Subgraph.class);
		doReturn(guguSubgraph).when(entityGraph).addSubgraph("gugu");
		doReturn(blaSubgraph).when(entityGraph).addSubgraph("bla");

		JpaEntityGraph jpaEntityGraph = new JpaEntityGraph("foo", EntityGraphType.FETCH, new String[] { "foo", "gugu.gaga",
				"bar", "gugu.fugu", "bla.fasel" });

		Jpa21Utils.configureFetchGraphFrom(jpaEntityGraph, entityGraph);

		verify(entityGraph, times(1)).addAttributeNodes(new String[] { "foo", "bar" });
		verify(entityGraph, times(1)).addSubgraph("gugu");
		verify(guguSubgraph, times(1)).addAttributeNodes(new String[] { "gaga", "fugu" });
		verify(blaSubgraph, times(1)).addAttributeNodes("fasel");
	}
}
