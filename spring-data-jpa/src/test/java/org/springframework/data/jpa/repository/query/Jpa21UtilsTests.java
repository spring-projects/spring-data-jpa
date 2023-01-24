/*
 * Copyright 2017-2023 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.springframework.data.jpa.support.EntityManagerTestUtils.currentEntityManagerIsAJpa21EntityManager;

import jakarta.persistence.AttributeNode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Subgraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Jens Schauder
 * @author Krzysztof Krason
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:application-context.xml")
@Transactional
class Jpa21UtilsTests {

	@Autowired EntityManager em;

	@Test // DATAJPA-1041, DATAJPA-1075
	void shouldCreateGraphWithoutSubGraphCorrectly() {

		assumeThat(currentEntityManagerIsAJpa21EntityManager(em)).isTrue();

		EntityGraph<User> graph = em.createEntityGraph(User.class);
		Jpa21Utils.configureFetchGraphFrom(
				new JpaEntityGraph("name", EntityGraphType.FETCH, new String[] { "roles", "colleagues" }), graph);

		AttributeNode<?> roles = findNode("roles", graph);
		assertThat(roles).terminatesGraph();

		AttributeNode<?> colleagues = findNode("colleagues", graph);
		assertThat(colleagues).terminatesGraph();
	}

	@Test // DATAJPA-1041, DATAJPA-1075
	void shouldCreateGraphWithMultipleSubGraphCorrectly() {

		assumeThat(currentEntityManagerIsAJpa21EntityManager(em)).isTrue();

		EntityGraph<User> graph = em.createEntityGraph(User.class);
		Jpa21Utils.configureFetchGraphFrom(new JpaEntityGraph("name", EntityGraphType.FETCH,
				new String[] { "roles", "colleagues.roles", "colleagues.colleagues" }), graph);

		AttributeNode<?> roles = findNode("roles", graph);
		assertThat(roles).terminatesGraph();

		AttributeNode<?> colleagues = findNode("colleagues", graph);
		assertThat(colleagues).terminatesGraphWith("roles", "colleagues");

	}

	@Test // DATAJPA-1041, DATAJPA-1075
	void shouldCreateGraphWithDeepSubGraphCorrectly() {

		assumeThat(currentEntityManagerIsAJpa21EntityManager(em)).isTrue();

		EntityGraph<User> graph = em.createEntityGraph(User.class);
		Jpa21Utils.configureFetchGraphFrom(new JpaEntityGraph("name", EntityGraphType.FETCH,
				new String[] { "roles", "colleagues.roles", "colleagues.colleagues.roles" }), graph);

		AttributeNode<?> roles = findNode("roles", graph);
		assertThat(roles).terminatesGraph();

		AttributeNode<?> colleagues = findNode("colleagues", graph);
		assertThat(colleagues) //
				.terminatesGraphWith("roles") //
				.hasSubgraphs("colleagues");

		AttributeNode<?> colleaguesOfColleagues = findNode("colleagues", colleagues);
		assertThat(colleaguesOfColleagues).terminatesGraphWith("roles");

	}

	@Test // DATAJPA-1041, DATAJPA-1075
	void shouldIgnoreIntermedeateSubGraphNodesThatAreNotNeeded() {

		assumeThat(currentEntityManagerIsAJpa21EntityManager(em)).isTrue();

		EntityGraph<User> graph = em.createEntityGraph(User.class);
		Jpa21Utils.configureFetchGraphFrom(new JpaEntityGraph("name", EntityGraphType.FETCH, new String[] { "roles",
				"colleagues", "colleagues.roles", "colleagues.colleagues", "colleagues.colleagues.roles" }), graph);

		AttributeNode<?> roles = findNode("roles", graph);
		assertThat(roles).terminatesGraph();

		AttributeNode<?> colleagues = findNode("colleagues", graph);
		assertThat(colleagues).terminatesGraphWith("roles");

		assertThat(colleagues).hasSubgraphs("colleagues");

		AttributeNode<?> colleaguesOfColleagues = findNode("colleagues", colleagues);
		assertThat(colleaguesOfColleagues).terminatesGraphWith("roles");

	}

	@Test // DATAJPA-1041, DATAJPA-1075
	void orderOfSubGraphsShouldNotMatter() {

		assumeThat(currentEntityManagerIsAJpa21EntityManager(em)).isTrue();

		EntityGraph<User> graph = em.createEntityGraph(User.class);
		Jpa21Utils.configureFetchGraphFrom(new JpaEntityGraph("name", EntityGraphType.FETCH, new String[] {
				"colleagues.colleagues.roles", "roles", "colleagues.colleagues", "colleagues", "colleagues.roles" }), graph);

		AttributeNode<?> roles = findNode("roles", graph);
		assertThat(roles).terminatesGraph();

		AttributeNode<?> colleagues = findNode("colleagues", graph);
		assertThat(colleagues) //
				.terminatesGraphWith("roles") //
				.hasSubgraphs("colleagues");

		AttributeNode<?> colleaguesOfColleagues = findNode("colleagues", colleagues);
		assertThat(colleaguesOfColleagues).terminatesGraphWith("roles");
	}

	@Test // DATAJPA-1041, DATAJPA-1075
	void errorsOnUnknownProperties() {

		assumeThat(currentEntityManagerIsAJpa21EntityManager(em)).isTrue();

		assertThatExceptionOfType(Exception.class).isThrownBy(() -> Jpa21Utils.configureFetchGraphFrom(
				new JpaEntityGraph("name", EntityGraphType.FETCH, new String[] { "¯\\_(ツ)_/¯" }),
				em.createEntityGraph(User.class)));
	}

	/**
	 * Lookup the {@link AttributeNode} with given {@literal nodeName} in the root of the given {@literal graph}.
	 *
	 * @param nodeName
	 * @param graph
	 * @return
	 */
	static @Nullable AttributeNode<?> findNode(String nodeName, @Nullable EntityGraph<?> graph) {

		if (graph == null) {
			return null;
		}

		return findNode(nodeName, graph.getAttributeNodes());
	}

	/**
	 * Lookup the {@link AttributeNode} with given {@literal nodeName} in the {@link List} of given {@literal nodes}.
	 *
	 * @param nodeName
	 * @param nodes
	 * @return
	 */
	@Nullable
	static AttributeNode<?> findNode(String nodeName, List<AttributeNode<?>> nodes) {

		if (CollectionUtils.isEmpty(nodes)) {
			return null;
		}

		for (AttributeNode<?> node : nodes) {
			if (ObjectUtils.nullSafeEquals(node.getAttributeName(), nodeName)) {
				return node;
			}
		}

		return null;
	}

	/**
	 * Lookup the {@link AttributeNode} with given {@literal nodeName} in the first {@link Subgraph} of the given
	 * {@literal node}.
	 *
	 * @param attributeName
	 * @param node
	 * @return
	 */
	@Nullable
	static AttributeNode<?> findNode(String attributeName, AttributeNode<?> node) {

		if (CollectionUtils.isEmpty(node.getSubgraphs())) {
			return null;
		}

		Subgraph<?> subgraph = node.getSubgraphs().values().iterator().next();
		return findNode(attributeName, subgraph.getAttributeNodes());
	}

	static <T> AttributeNodeAssert<T> assertThat(AttributeNode<T> actual) {
		return new AttributeNodeAssert<>(actual);
	}

	static class AttributeNodeAssert<T> extends AbstractAssert<AttributeNodeAssert<T>, AttributeNode<T>> {

		private final AttributeNode<T> attributeNode;

		AttributeNodeAssert(AttributeNode<T> attributeNode) {

			super(attributeNode, AttributeNodeAssert.class);

			this.attributeNode = attributeNode;
		}

		AttributeNodeAssert<T> terminatesGraph() {

			Assertions.assertThat(CollectionUtils.isEmpty(attributeNode.getSubgraphs()))
					.describedAs(String.format("'%s' was expected to be a terminating node but has subgraphs %s.",
							attributeNode.getAttributeName(), extractSubgraphsAttributeNames()))
					.isTrue();

			return this;
		}

		AttributeNodeAssert<T> terminatesGraphWith(String... nodeNames) {

			List<String> nodes = Arrays.asList(nodeNames);

			Assertions.assertThat(attributeNode.getSubgraphs()) //
					.describedAs(
							String.format("Leaf properties %s could not be found; The node does not have any subgraphs", nodes)) //
					.isNotEmpty();

			Subgraph<?> graph = attributeNode.getSubgraphs().values().iterator().next();

			SoftAssertions.assertSoftly(softly -> {
				for (String nodeName : nodes) {

					AttributeNode<?> node = findNode(nodeName, graph.getAttributeNodes());

					String notInSubgraph = String.format(
							"AttributeNode '%s' could not be found in subgraph for '%s'; Know nodes are: %s", nodeName,
							attributeNode.getAttributeName(), extractExistingAttributeNames(graph));

					softly.assertThat(node).describedAs(notInSubgraph).isNotNull();

					String notLeaf = String.format(
							"AttributeNode %s of subgraph %s is not a leaf property but has %d SubGraph(s)", nodeName,
							attributeNode.getAttributeName(), node.getSubgraphs().size());

					softly.assertThat(node.getSubgraphs()) //
							.describedAs(notLeaf) //
							.isEmpty();
				}
			});

			return this;
		}

		AttributeNodeAssert<T> hasSubgraphs(String... subgraphNames) {

			List<String> subgraphs = Arrays.asList(subgraphNames);

			Assertions.assertThat(attributeNode.getSubgraphs()) //
					.describedAs(
							String.format("Subgraphs %s could not be found; The node does not have any subgraphs", subgraphs)) //
					.isNotEmpty();

			Subgraph<?> graph = attributeNode.getSubgraphs().values().iterator().next();

			SoftAssertions.assertSoftly(softly -> {

				for (String subgraphName : subgraphs) {

					AttributeNode<?> node = findNode(subgraphName, graph.getAttributeNodes());

					String notFound = String.format("Subgraph '%s' could not be found in SubGraph for '%s'; Known nodes are: %s",
							subgraphName, attributeNode.getAttributeName(), extractExistingAttributeNames(graph));
					softly.assertThat(node) //
							.describedAs(notFound) //
							.isNotNull();

					String notSubGraph = String.format("'%s' of SubGraph '%s' is not a SubGraph", subgraphName,
							attributeNode.getAttributeName());

					softly.assertThat(node.getSubgraphs()) //
							.describedAs(notSubGraph) //
							.isNotEmpty();
				}
			});

			return this;
		}

		private List<String> extractSubgraphsAttributeNames() {

			Iterator<Subgraph> iterator = attributeNode.getSubgraphs().values().iterator();

			if (!iterator.hasNext()) {
				return Collections.emptyList();
			}

			return extractExistingAttributeNames(iterator.next());
		}

		private static List<String> extractExistingAttributeNames(Subgraph<?> graph) {

			List<String> result = new ArrayList<>(graph.getAttributeNodes().size());
			for (AttributeNode<?> node : graph.getAttributeNodes()) {
				result.add(node.getAttributeName());
			}
			return result;
		}
	}

}
