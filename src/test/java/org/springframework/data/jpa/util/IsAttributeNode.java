/*
 * Copyright 2017-2018 the original author or authors.
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
package org.springframework.data.jpa.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.AttributeNode;
import javax.persistence.EntityGraph;
import javax.persistence.Subgraph;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * @author Christoph Strobl
 * @author Mark Paluch
 */
public class IsAttributeNode<T> extends TypeSafeMatcher<AttributeNode<T>> {

	private boolean terminatingNodeCheck = false;
	private List<String> nodes;
	private List<String> subgraphs;
	private final List<String> errors = new ArrayList<>();

	@Override
	protected boolean matchesSafely(AttributeNode<T> item) {

		if (item == null) {

			errors.add("AttributeNode was null!");
			return false;
		}

		if (terminatingNodeCheck) {

			if (!CollectionUtils.isEmpty(item.getSubgraphs())) {

				errors.add(String.format("'%s' was expected to be a terminating node but has subgraphs %s.",
						item.getAttributeName(), extractExistingAttributeNames(item.getSubgraphs().values().iterator().next())));
				return false;
			}
			return true;
		}

		if (CollectionUtils.isEmpty(item.getSubgraphs())) {

			if (!CollectionUtils.isEmpty(nodes)) {

				errors
						.add(String.format("Leaf properties %s could not be found. The node does not have any subgraphs.", nodes));
			}
			if (!CollectionUtils.isEmpty(subgraphs)) {
				errors.add(String.format("Subgraphs %s could not be found. The node does not have any subgraphs.", subgraphs));
			}
			return false;
		}

		Subgraph<?> graph = item.getSubgraphs().values().iterator().next();

		if (!CollectionUtils.isEmpty(nodes)) {
			for (String nodeName : nodes) {

				AttributeNode<?> node = findNode(nodeName, graph.getAttributeNodes());
				if (node == null) {

					errors.add(String.format("AttributeNode '%s' could not be found in subgraph for '%s'. Know nodes are: %s.",
							nodeName, item.getAttributeName(), extractExistingAttributeNames(graph)));
					return false;
				}

				if (!CollectionUtils.isEmpty(node.getSubgraphs())) {

					errors.add(String.format("AttributeNode %s of subgraph %s is not a leaf property but has % SubGraph(s).",
							nodeName, item.getAttributeName(), node.getSubgraphs().size()));
					return false;
				}
			}
		}

		if (!CollectionUtils.isEmpty(subgraphs)) {
			for (String subgraphName : subgraphs) {

				AttributeNode<?> node = findNode(subgraphName, graph.getAttributeNodes());
				if (node == null) {

					errors.add(String.format("Subgraph '%s' could not be found in SubGraph for '%s'. Know nodes are: %s.",
							subgraphName, item.getAttributeName(), extractExistingAttributeNames(graph)));
					return false;
				}

				if (CollectionUtils.isEmpty(node.getSubgraphs())) {

					errors.add(String.format("'%s' of SubGraph '%s' is not a SubGraph.", subgraphName, item.getAttributeName()));
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public void describeTo(Description description) {

		for (String error : errors) {
			description.appendText(error);
		}
	}

	/**
	 * Lookup the {@link AttributeNode} with given {@literal nodeName} in the root of the given {@literal graph}.
	 *
	 * @param nodeName
	 * @param graph
	 * @return
	 */
	public static AttributeNode<?> findNode(String nodeName, @Nullable EntityGraph<?> graph) {

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

	private List<String> extractExistingAttributeNames(Subgraph<?> graph) {

		List<String> result = new ArrayList<>(graph.getAttributeNodes().size());
		for (AttributeNode<?> node : graph.getAttributeNodes()) {
			result.add(node.getAttributeName());
		}
		return result;
	}

	/**
	 * Asserts that the fetch graph terminates with {@link AttributeNode}s having the given {@literal nodeNames}.
	 *
	 * @param nodeNames
	 * @return
	 */
	public static <T> IsAttributeNode<T> terminatesGraphWith(String... nodeNames) {

		IsAttributeNode<T> matcher = new IsAttributeNode<>();
		matcher.nodes = Arrays.asList(nodeNames);
		return matcher;
	}

	/**
	 * Asserts that the fetch graph continues with {@link AttributeNode}s having {@link AttributeNode#getSubgraphs()} with
	 * given {@literal subgraphNames}.
	 *
	 * @return
	 */
	public static <T> IsAttributeNode<T> hasSubgraphs(String... subgraphNames) {

		IsAttributeNode<T> matcher = new IsAttributeNode<>();
		matcher.subgraphs = Arrays.asList(subgraphNames);
		return matcher;
	}

	/**
	 * Asserts that the fetch graph terminates with the given {@link AttributeNode} by checking
	 * {@link AttributeNode#getSubgraphs()} is empty.
	 *
	 * @return
	 */
	public static <T> IsAttributeNode<T> terminatesGraph() {

		IsAttributeNode<T> matcher = new IsAttributeNode<>();
		matcher.terminatingNodeCheck = true;
		return matcher;
	}
}
