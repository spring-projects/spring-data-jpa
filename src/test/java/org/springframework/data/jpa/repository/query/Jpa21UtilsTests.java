/*
 * Copyright 2017-2019 the original author or authors.
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

import static org.junit.Assume.*;
import static org.springframework.data.jpa.support.EntityManagerTestUtils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.persistence.AttributeNode;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Subgraph;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Jens Schauder
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context.xml")
@Transactional
public class Jpa21UtilsTests {

	@Autowired EntityManager em;

	@Test // DATAJPA-1041, DATAJPA-1075
	public void shouldCreateGraphWithoutSubGraphCorrectly() {

		assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

		EntityGraph<User> graph = em.createEntityGraph(User.class);
		Jpa21Utils.configureFetchGraphFrom(
				new JpaEntityGraph("name", EntityGraphType.FETCH, new String[] { "roles", "colleagues" }), graph);

		AttributeNode<?> roles = findNode("roles", graph);
		assertThat(roles).terminatesGraph();

		AttributeNode<?> colleagues = findNode("colleagues", graph);
		assertThat(colleagues).terminatesGraph();
	}

	@Test // DATAJPA-1041, DATAJPA-1075
	public void shouldCreateGraphWithMultipleSubGraphCorrectly() {

		assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

		EntityGraph<User> graph = em.createEntityGraph(User.class);
		Jpa21Utils.configureFetchGraphFrom(new JpaEntityGraph("name", EntityGraphType.FETCH,
				new String[] { "roles", "colleagues.roles", "colleagues.colleagues" }), graph);

		AttributeNode<?> roles = findNode("roles", graph);
		assertThat(roles).terminatesGraph();

		AttributeNode<?> colleagues = findNode("colleagues", graph);
		assertThat(colleagues).terminatesGraphWith("roles", "colleagues");

	}

	@Test // DATAJPA-1041, DATAJPA-1075
	public void shouldCreateGraphWithDeepSubGraphCorrectly() {

		assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

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
	public void shouldIgnoreIntermedeateSubGraphNodesThatAreNotNeeded() {

		assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

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
	public void orderOfSubGraphsShouldNotMatter() {

		assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

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

	@Test(expected = Exception.class) // DATAJPA-1041, DATAJPA-1075
	public void errorsOnUnknownProperties() {

		assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

		Jpa21Utils.configureFetchGraphFrom(new JpaEntityGraph("name", EntityGraphType.FETCH, new String[] { "¯\\_(ツ)_/¯" }),
				em.createEntityGraph(User.class));
	}

	/**
	 * Lookup the {@link AttributeNode} with given {@literal nodeName} in the root of the given {@literal graph}.
	 *
	 * @param nodeName
	 * @param graph
	 * @return
	 */
	public static @Nullable AttributeNode<?> findNode(String nodeName, @Nullable EntityGraph<?> graph) {

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
	public static AttributeNode<?> findNode(String nodeName, List<AttributeNode<?>> nodes) {

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
	public static AttributeNode<?> findNode(String attributeName, AttributeNode<?> node) {

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
							String.format("Leaf properties %s could not be found. The node does not have any subgraphs.", nodes)) //
					.isNotNull() //
					.isNotEmpty();

			Subgraph<?> graph = attributeNode.getSubgraphs().values().iterator().next();

			SoftAssertions.assertSoftly(softly -> {
				for (String nodeName : nodes) {

					AttributeNode<?> node = findNode(nodeName, graph.getAttributeNodes());

					String notInSubgraph = String.format(
							"AttributeNode '%s' could not be found in subgraph for '%s'. Know nodes are: %s.", nodeName,
							attributeNode.getAttributeName(), extractExistingAttributeNames(graph));

					softly.assertThat(node).describedAs(notInSubgraph).isNotNull();

					String notLeaf = String.format(
							"AttributeNode %s of subgraph %s is not a leaf property but has %d SubGraph(s).", nodeName,
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
							String.format("Subgraphs %s could not be found. The node does not have any subgraphs.", subgraphs)) //
					.isNotNull() //
					.isNotEmpty();

			Subgraph<?> graph = attributeNode.getSubgraphs().values().iterator().next();

			SoftAssertions.assertSoftly(softly -> {

				for (String subgraphName : subgraphs) {

					AttributeNode<?> node = findNode(subgraphName, graph.getAttributeNodes());

					String notFound = String.format("Subgraph '%s' could not be found in SubGraph for '%s'. Known nodes are: %s.",
							subgraphName, attributeNode.getAttributeName(), extractExistingAttributeNames(graph));
					softly.assertThat(node) //
							.describedAs(notFound) //
							.isNotNull();

					String notSubGraph = String.format("'%s' of SubGraph '%s' is not a SubGraph.", subgraphName,
							attributeNode.getAttributeName());

					softly.assertThat(node.getSubgraphs()) //
							.describedAs(notSubGraph).isNotNull() //
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
