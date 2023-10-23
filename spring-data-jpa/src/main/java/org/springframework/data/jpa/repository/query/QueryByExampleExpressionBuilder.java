/*
 * Copyright 2023 the original author or authors.
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

import static org.springframework.data.jpa.repository.query.ExpressionContext.*;

import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.Attribute.PersistentAttributeType;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.SingularAttribute;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ExampleMatcher.PropertyValueTransformer;
import org.springframework.data.support.ExampleMatcherAccessor;
import org.springframework.data.util.DirectFieldAccessFallbackBeanWrapper;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Builds an {@link ExpressionContext}, representing the criteria and potential joins, based upon an {@link Example} and
 * its related {@link EntityType}.<br />
 * It includes any {@link SingularAttribute}s of the {@link Example#getProbe()}, applying {@link String} and
 * {@literal null} matching strategies configured in the {@link Example}. Ignored paths will be skipped entirely.<br />
 * 
 * @author Greg Turnquist
 */
class QueryByExampleExpressionBuilder {

	private static final Set<PersistentAttributeType> ASSOCIATION_TYPES = EnumSet.of( //
			PersistentAttributeType.MANY_TO_MANY, //
			PersistentAttributeType.MANY_TO_ONE, //
			PersistentAttributeType.ONE_TO_MANY, //
			PersistentAttributeType.ONE_TO_ONE);

	@Nullable
	public static <T> ExpressionContext getExpression(EntityType<?> model, Example<T> example) {
		return getExpression(new PathBasedExpressionContext(), model, example, EscapeCharacter.DEFAULT);
	}

	@Nullable
	private static <T> ExpressionContext getExpression(ExpressionContext root, EntityType<?> model, Example<T> example,
			EscapeCharacter escapeCharacter) {

		Assert.notNull(example, "Example must not be null");

		ExampleMatcher matcher = example.getMatcher();

		List<ExpressionContext> expressions = getExpressions("", root, model, example.getProbe(), example.getProbeType(),
				new ExampleMatcherAccessor(matcher), new PathNode("root", null, example.getProbe()), escapeCharacter);

		if (expressions.isEmpty()) {
			return null;
		}

		if (expressions.size() == 1) {
			return expressions.iterator().next();
		}

		return matcher.isAllMatching() ? and(expressions) : or(expressions);
	}

	/**
	 * Join a collection of {@link ExpressionContext}s into a single clause via an {@literal AND} operation.
	 *
	 * @param expressions
	 * @return
	 */
	private static ExpressionContext and(List<ExpressionContext> expressions) {
		return new AndExpressionContext(expressions);
	}

	/**
	 * Join a collection of {@link ExpressionContext}s into a single clause via an {@literal OR} operation.
	 *
	 * @param expressions
	 * @return
	 */
	private static ExpressionContext or(List<ExpressionContext> expressions) {
		return new OrExpressionContext(expressions);
	}

	/**
	 * Recursively walk through the path and extract a collection of {@link ExpressionContext}s to form a query.
	 * 
	 * @param path
	 * @param from
	 * @param entityType
	 * @param value
	 * @param probeType
	 * @param exampleAccessor
	 * @param currentNode
	 * @param escapeCharacter
	 * @return
	 */
	private static List<ExpressionContext> getExpressions(String path, ExpressionContext from, ManagedType<?> entityType,
			Object value, Class<?> probeType, ExampleMatcherAccessor exampleAccessor, PathNode currentNode,
			EscapeCharacter escapeCharacter) {

		List<ExpressionContext> expressions = new ArrayList<>();
		DirectFieldAccessFallbackBeanWrapper beanWrapper = new DirectFieldAccessFallbackBeanWrapper(value);

		for (SingularAttribute attribute : entityType.getSingularAttributes()) {

			String currentPath = !StringUtils.hasText(path) ? attribute.getName() : path + "." + attribute.getName();

			if (exampleAccessor.isIgnoredPath(currentPath)) {
				continue;
			}

			PropertyValueTransformer transformer = exampleAccessor.getValueTransformerForPath(currentPath);
			Optional<Object> optionalValue = transformer
					.apply(Optional.ofNullable(beanWrapper.getPropertyValue(attribute.getName())));

			if (optionalValue.isEmpty()) {

				if (exampleAccessor.getNullHandler().equals(ExampleMatcher.NullHandler.INCLUDE)) {
					expressions.add(isNull(from.get(currentPath, entityType, probeType, attribute)));
				}
				continue;
			}

			Object attributeValue = optionalValue.get();

			if (attributeValue == Optional.empty()) {
				continue;
			}

			if (attribute.getPersistentAttributeType().equals(PersistentAttributeType.EMBEDDED)
					|| (isAssociation(attribute) && !(from instanceof PathBasedExpressionContext))) {

				expressions.addAll(getExpressions(currentPath, from.get(currentPath, entityType, probeType, attribute),
						(ManagedType<?>) attribute.getType(), attributeValue, probeType, exampleAccessor, currentNode,
						escapeCharacter));

				continue;
			}

			if (isAssociation(attribute)) {

				PathNode node = currentNode.add(attribute.getName(), attributeValue);

				if (node.spansCycle()) {
					throw new InvalidDataAccessApiUsageException(
							String.format("Path '%s' from %s must not span a cyclic reference%n%s", currentNode,
									ClassUtils.getShortName(probeType), node));
				}

				expressions.addAll(getExpressions(currentPath, from.join(attribute.getName()),
						(EntityType<?>) attribute.getType(), attributeValue, probeType, exampleAccessor, node, escapeCharacter));

				continue;
			}

			if (attribute.getJavaType().equals(String.class)) {

				ExpressionContext expression = from.get(currentPath, entityType, probeType, attribute);

				if (exampleAccessor.isIgnoreCaseForPath(currentPath)) {

					expression = upper(expression);
					attributeValue = attributeValue.toString().toUpperCase();
				}

				switch (exampleAccessor.getStringMatcherForPath(currentPath)) {

					case DEFAULT:
					case EXACT:
						expressions.add(equal(expression, attributeValue));
						break;
					case CONTAINING:
						expressions.add(like( //
								expression, //
								"%" + escapeCharacter.escape(attributeValue.toString()) + "%" //
						));
						break;
					case STARTING:
						expressions.add(like( //
								expression, //
								escapeCharacter.escape(attributeValue.toString()) + "%" //
						));
						break;
					case ENDING:
						expressions.add(like( //
								expression, //
								"%" + escapeCharacter.escape(attributeValue.toString()) //
						));
						break;
					default:
						throw new IllegalArgumentException(
								"Unsupported StringMatcher " + exampleAccessor.getStringMatcherForPath(currentPath));
				}
			} else {
				expressions.add(equal(from.get(currentPath, entityType, probeType, attribute), attributeValue));
			}
		}

		return expressions;
	}

	private static boolean isAssociation(Attribute<?, ?> attribute) {
		return ASSOCIATION_TYPES.contains(attribute.getPersistentAttributeType());
	}

	/**
	 * {@link PathNode} is used to dynamically grow a directed graph structure that allows to detect cycles within its
	 * direct predecessor nodes by comparing parent node values using {@link System#identityHashCode(Object)}.
	 *
	 * @author Christoph Strobl
	 */
	private static class PathNode {

		String name;
		@Nullable PathNode parent;
		List<PathNode> siblings = new ArrayList<>();
		@Nullable Object value;

		PathNode(String edge, @Nullable PathNode parent, @Nullable Object value) {

			this.name = edge;
			this.parent = parent;
			this.value = value;
		}

		PathNode add(String attribute, @Nullable Object value) {

			PathNode node = new PathNode(attribute, this, value);
			siblings.add(node);
			return node;
		}

		boolean spansCycle() {

			if (value == null) {
				return false;
			}

			String identityHex = ObjectUtils.getIdentityHexString(value);
			PathNode current = parent;

			while (current != null) {

				if (current.value != null && ObjectUtils.getIdentityHexString(current.value).equals(identityHex)) {
					return true;
				}
				current = current.parent;
			}

			return false;
		}

		@Override
		public String toString() {

			StringBuilder sb = new StringBuilder();

			if (parent != null) {
				sb.append(parent);
				sb.append(" -");
				sb.append(name);
				sb.append("-> ");
			}

			sb.append("[{ ");
			sb.append(ObjectUtils.nullSafeToString(value));
			sb.append(" }]");

			return sb.toString();
		}
	}
}
