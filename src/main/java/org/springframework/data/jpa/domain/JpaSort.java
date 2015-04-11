/*
 * Copyright 2013-2015 the original author or authors.
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
package org.springframework.data.jpa.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.PluralAttribute;

import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;

/**
 * Sort option for queries that wraps JPA meta-model {@link Attribute}s for sorting.
 * 
 * @author Thomas Darimont
 * @author Oliver Gierke
 */
public class JpaSort extends Sort {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new {@link JpaSort} for the given attributes with the default sort direction.
	 * 
	 * @param attributes must not be {@literal null} or empty.
	 */
	public JpaSort(Attribute<?, ?>... attributes) {
		this(DEFAULT_DIRECTION, attributes);
	}

	/**
	 * Creates a new {@link JpaSort} instance with the given {@link Path}s.
	 * 
	 * @param paths must not be {@literal null} or empty.
	 */
	public JpaSort(JpaSort.Path<?, ?>... paths) {
		this(DEFAULT_DIRECTION, paths);
	}

	/**
	 * Creates a new {@link JpaSort} for the given direction and attributes.
	 * 
	 * @param direction the sorting direction.
	 * @param attributes must not be {@literal null} or empty.
	 */
	public JpaSort(Direction direction, Attribute<?, ?>... attributes) {
		this(direction, paths(attributes));
	}

	/**
	 * Creates a new {@link JpaSort} for the given direction and {@link Path}s.
	 * 
	 * @param direction the sorting direction.
	 * @param paths must not be {@literal null} or empty.
	 */
	public JpaSort(Direction direction, Path<?, ?>... paths) {
		this(direction, Arrays.asList(paths));
	}

	private JpaSort(Direction direction, List<Path<?, ?>> paths) {
		this(Collections.<Order> emptyList(), direction, paths);
	}

	private JpaSort(List<Order> orders, Direction direction, List<Path<?, ?>> paths) {
		super(combine(orders, direction, paths));
	}

	/**
	 * Returns a new {@link JpaSort} with the given sorting criteria added to the current one.
	 * 
	 * @param direction can be {@literal null}.
	 * @param attributes must not be {@literal null}.
	 * @return
	 */
	public JpaSort and(Direction direction, Attribute<?, ?>... attributes) {

		Assert.notNull(attributes, "Attributes must not be null!");

		return and(direction, paths(attributes));
	}

	/**
	 * Returns a new {@link JpaSort} with the given sorting criteria added to the current one.
	 * 
	 * @param direction can be {@literal null}.
	 * @param paths must not be {@literal null}.
	 * @return
	 */
	public JpaSort and(Direction direction, Path<?, ?>... paths) {

		Assert.notNull(paths, "Paths must not be null!");

		List<Order> existing = new ArrayList<Order>();

		for (Order order : this) {
			existing.add(order);
		}

		return new JpaSort(existing, direction, Arrays.asList(paths));
	}

	/**
	 * Turns the given {@link Attribute}s into {@link Path}s.
	 * 
	 * @param attributes must not be {@literal null} or empty.
	 * @return
	 */
	private static Path<?, ?>[] paths(Attribute<?, ?>[] attributes) {

		Assert.notNull(attributes, "Attributes must not be null!");
		Assert.isTrue(attributes.length > 0, "Attributes must not be empty");

		Path<?, ?>[] paths = new Path[attributes.length];

		for (int i = 0; i < attributes.length; i++) {
			paths[i] = path(attributes[i]);
		}

		return paths;
	}

	private static List<Order> combine(List<Order> orders, Direction direction, List<Path<?, ?>> paths) {

		List<Order> result = new ArrayList<Sort.Order>(orders);

		for (Path<?, ?> path : paths) {
			result.add(new Order(direction, path.toString()));
		}

		return result;
	}

	/**
	 * Creates a new {@link Path} for the given {@link Attribute}.
	 * 
	 * @param attribute must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <A extends Attribute<T, S>, T, S> Path<T, S> path(A attribute) {

		Assert.notNull(attribute, "Attribute must not be null!");
		return new Path<T, S>(Arrays.asList(attribute));
	}

	/**
	 * Creates a new {@link Path} for the given {@link PluralAttribute}.
	 * 
	 * @param attribute must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <P extends PluralAttribute<T, ?, S>, T, S> Path<T, S> path(P attribute) {

		Assert.notNull(attribute, "Attribute must not be null!");
		return new Path<T, S>(Arrays.asList(attribute));
	}

	/**
	 * Value object to abstract a collection of {@link Attribute}s.
	 * 
	 * @author Oliver Gierke
	 */
	public static class Path<T, S> {

		private final Collection<Attribute<?, ?>> attributes;

		private Path(List<? extends Attribute<?, ?>> attributes) {
			this.attributes = Collections.unmodifiableList(attributes);
		}

		/**
		 * Collects the given {@link Attribute} and returning a new {@link Path} pointing to the attribute type.
		 * 
		 * @param attribute must not be {@literal null}.
		 * @return
		 */
		public <A extends Attribute<S, U>, U> Path<S, U> dot(A attribute) {
			return new Path<S, U>(add(attribute));
		}

		/**
		 * Collects the given {@link PluralAttribute} and returning a new {@link Path} pointing to the attribute type.
		 * 
		 * @param attribute must not be {@literal null}.
		 * @return
		 */
		public <P extends PluralAttribute<S, ?, U>, U> Path<S, U> dot(P attribute) {
			return new Path<S, U>(add(attribute));
		}

		private List<Attribute<?, ?>> add(Attribute<?, ?> attribute) {

			Assert.notNull(attribute, "Attribute must not be null!");

			List<Attribute<?, ?>> newAttributes = new ArrayList<Attribute<?, ?>>(attributes.size() + 1);
			newAttributes.addAll(attributes);
			newAttributes.add(attribute);
			return newAttributes;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {

			StringBuilder builder = new StringBuilder();

			for (Attribute<?, ?> attribute : attributes) {
				builder.append(attribute.getName()).append(".");
			}

			return builder.length() == 0 ? "" : builder.substring(0, builder.lastIndexOf("."));
		}
	}
}
