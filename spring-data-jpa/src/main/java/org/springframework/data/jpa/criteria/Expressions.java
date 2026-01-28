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
package org.springframework.data.jpa.criteria;

import jakarta.persistence.criteria.CollectionJoin;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.criteria.SetJoin;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.core.PropertyReference;
import org.springframework.data.core.TypedPropertyPath;
import org.springframework.data.jpa.repository.query.QueryUtils;

/**
 * Utility methods to resolve JPA Criteria API objects using Spring Data's type-safe property references. These helper
 * methods obtain Criteria API objects using {@link TypedPropertyPath} and {@link PropertyReference} to resolve
 * {@link Expression property expressions} and {@link Join joins}.
 * <p>
 * The class is intended for concise, type-aware criteria construction through a type-safe DSL where property references
 * are preferred over string-based navigation.
 * <p>
 * Example:
 *
 * <pre class="code">
 * Root<User> root = criteriaQuery.from(User.class);
 *
 * Expression<User> expr = Expressions.get(root, User::getManager);
 *
 * Join<User, Address> join = Expressions.join(root, JoinType.INNER, j -&gt; j.join(User::getAddress));
 * </pre>
 *
 * @author Mark Paluch
 * @since 4.1
 * @see PropertyReference
 * @see TypedPropertyPath
 */
public abstract class Expressions {

	/**
	 * Create an {@link Expression} for the given property path.
	 * <p>
	 * The resulting expression can be used in predicates. Expression resolution navigates joins as necessary.
	 *
	 * @param from the root or join to start from.
	 * @param property property path to navigate.
	 * @return the expression.
	 */
	static <T, P> Expression<P> get(From<?, T> from, TypedPropertyPath<T, P> property) {
		return QueryUtils.toExpressionRecursively(from, property, false);
	}

	/**
	 * Create a {@link Selection} for the given property path.
	 * <p>
	 * The resulting object can be used in the selection, joined paths consider outer joins as needed.
	 *
	 * @param from the root or join to start from.
	 * @param property property path to navigate.
	 * @return the selection.
	 */
	static <T, P> Selection<P> select(From<?, T> from, TypedPropertyPath<T, P> property) {
		return QueryUtils.toExpressionRecursively(from, property, true);
	}

	/**
	 * Create a list of {@link Selection selection objects} for the given property paths.
	 * <p>
	 * The resulting objects can be used in the selection, joined paths consider outer joins as needed.
	 *
	 * @param from the root or join to start from.
	 * @param properties property path to navigate.
	 * @return the selection.
	 */
	@SafeVarargs
	static <T> List<Selection<?>> select(From<?, T> from, TypedPropertyPath<T, ?>... properties) {
		return Arrays.stream(properties).map(it -> get(from, it)).collect(Collectors.toUnmodifiableList());
	}

	/**
	 * Create a {@link Join} using the given {@link PropertyReference property}.
	 *
	 * @param from the root or join to start from.
	 * @param property property reference to navigate.
	 * @return the resolved join.
	 * @see From#join(String)
	 */
	static <T, P> Join<T, P> join(From<?, T> from, PropertyReference<T, P> property) {
		return from.join(property.getName());
	}

	/**
	 * Create a {@link Join} considering {@link JoinType} using the given joiner function allowing to express joins using
	 * property references.
	 *
	 * @param from the root or join to start from.
	 * @param joinType the join type.
	 * @param function joiner function.
	 * @return the resolved join.
	 * @see From#join(String, JoinType)
	 */
	static <T, J extends Join<?, ?>> J join(From<?, T> from, JoinType joinType, Function<Joiner<T>, J> function) {
		return function.apply(new TypedJoiner<>(from, joinType));
	}

	/**
	 * Create a {@link Fetch fetch join} using the given {@link PropertyReference property}.
	 *
	 * @param from the root or join to start from.
	 * @param property property reference to navigate.
	 * @return the resolved fetch.
	 * @see From#fetch(String)
	 */
	static <T, P> Fetch<T, P> fetch(From<?, T> from, PropertyReference<T, P> property) {
		return from.fetch(property.getName());
	}

	/**
	 * Create a {@link Fetch fetch join} considering {@link JoinType} using the given fetcher function allowing to express
	 * fetches using property references.
	 *
	 * @param from the root or join to start from.
	 * @param joinType the join type.
	 * @param function fetcher function.
	 * @return the resolved fetch.
	 * @see From#fetch(String, JoinType)
	 */
	static <T, F extends Fetch<?, ?>> F fetch(From<?, T> from, JoinType joinType, Function<Fetcher<T>, F> function) {
		return function.apply(new TypedFetcher<>(from, joinType));
	}

	private Expressions() {}

	/**
	 * Strategy interface used by {@link Expressions#join} to obtain joins using property references.
	 * <p>
	 * Implementations adapt a {@link jakarta.persistence.criteria.From} and expose typed join methods for singular and
	 * collection-valued attributes as well as map-valued attributes. The methods accept
	 * {@link org.springframework.data.core.PropertyReference} instances to avoid string-based attribute navigation.
	 */
	interface Joiner<T> {

		/**
		 * Create a join for the given property.
		 *
		 * @param property the property to join.
		 * @see From#join
		 */
		<P> Join<T, P> join(PropertyReference<T, P> property);

		/**
		 * Create a collection join for the given property.
		 *
		 * @param property the property to join.
		 * @see From#joinCollection
		 */
		<P> CollectionJoin<T, P> joinCollection(PropertyReference<T, P> property);

		/**
		 * Create a list join for the given property.
		 *
		 * @param property the property to join.
		 * @see From#joinList
		 */
		<P> ListJoin<T, P> joinList(PropertyReference<T, P> property);

		/**
		 * Create a set join for the given property.
		 *
		 * @param property the property to join.
		 * @see From#joinSet
		 */
		<P> SetJoin<T, P> joinSet(PropertyReference<T, P> property);

		/**
		 * Create a map join for the given property.
		 *
		 * @param property the property to join.
		 * @see From#joinMap
		 */
		<K, V, P extends Map<K, V>> MapJoin<T, K, V> joinMap(PropertyReference<T, P> property);

	}

	record TypedJoiner<T>(From<?, T> from, JoinType type) implements Joiner<T> {

		public <P> Join<T, P> join(PropertyReference<T, P> property) {
			return from.join(property.getName(), type);
		}

		public <P> CollectionJoin<T, P> joinCollection(PropertyReference<T, P> property) {
			return from.joinCollection(property.getName(), type);
		}

		public <P> ListJoin<T, P> joinList(PropertyReference<T, P> property) {
			return from.joinList(property.getName(), type);
		}

		public <P> SetJoin<T, P> joinSet(PropertyReference<T, P> property) {
			return from.joinSet(property.getName(), type);
		}

		public <K, V, P extends Map<K, V>> MapJoin<T, K, V> joinMap(PropertyReference<T, P> property) {
			return from.joinMap(property.getName(), type);
		}

	}

	/**
	 * Strategy interface used by {@link Expressions#fetch} to obtain fetch joins using property references.
	 * <p>
	 * Implementations adapt a {@link jakarta.persistence.criteria.From} and expose typed fetch methods accepting
	 * {@link org.springframework.data.core.PropertyReference} instances to avoid string-based attribute navigation.
	 */
	interface Fetcher<T> {

		/**
		 * Create a fetch join for the given property.
		 *
		 * @param property the property to join.
		 * @see From#fetch
		 */
		<P> Fetch<T, P> fetch(PropertyReference<T, P> property);

	}

	record TypedFetcher<T>(From<?, T> from, JoinType type) implements Fetcher<T> {

		@Override
		public <P> Fetch<T, P> fetch(PropertyReference<T, P> property) {
			return from.fetch(property.getName(), type);
		}

	}

}
