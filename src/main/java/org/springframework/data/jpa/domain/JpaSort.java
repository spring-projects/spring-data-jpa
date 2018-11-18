/*
 * Copyright 2013-2019 the original author or authors.
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
package org.springframework.data.jpa.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.PluralAttribute;

import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Sort option for queries that wraps JPA meta-model {@link Attribute}s for sorting.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author David Madden
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

	@SuppressWarnings("deprecation")
	private JpaSort(List<Order> orders, @Nullable Direction direction, List<Path<?, ?>> paths) {
		super(combine(orders, direction, paths));
	}

	@SuppressWarnings("deprecation")
	private JpaSort(List<Order> orders) {
		super(orders);
	}

	/**
	 * Returns a new {@link JpaSort} with the given sorting criteria added to the current one.
	 *
	 * @param direction can be {@literal null}.
	 * @param attributes must not be {@literal null}.
	 * @return
	 */
	public JpaSort and(@Nullable Direction direction, Attribute<?, ?>... attributes) {

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
	public JpaSort and(@Nullable Direction direction, Path<?, ?>... paths) {

		Assert.notNull(paths, "Paths must not be null!");

		List<Order> existing = new ArrayList<Order>();

		for (Order order : this) {
			existing.add(order);
		}

		return new JpaSort(existing, direction, Arrays.asList(paths));
	}

	/**
	 * Returns a new {@link JpaSort} with the given sorting criteria added to the current one.
	 *
	 * @param direction can be {@literal null}.
	 * @param properties must not be {@literal null} or empty.
	 * @return
	 */
	public JpaSort andUnsafe(@Nullable Direction direction, String... properties) {

		Assert.notEmpty(properties, "Properties must not be empty!");

		List<Order> orders = new ArrayList<Order>();

		for (Order order : this) {
			orders.add(order);
		}

		for (String property : properties) {
			orders.add(new JpaOrder(direction, property));
		}

		return new JpaSort(orders, direction, Collections.<Path<?, ?>> emptyList());
	}

	/**
	 * Turns the given {@link Attribute}s into {@link Path}s.
	 *
	 * @param attributes must not be {@literal null} or empty.
	 * @return
	 */
	private static Path<?, ?>[] paths(Attribute<?, ?>[] attributes) {

		Assert.notNull(attributes, "Attributes must not be null!");
		Assert.notEmpty(attributes, "Attributes must not be empty!");

		Path<?, ?>[] paths = new Path[attributes.length];

		for (int i = 0; i < attributes.length; i++) {
			paths[i] = path(attributes[i]);
		}

		return paths;
	}

	private static List<Order> combine(List<Order> orders, @Nullable Direction direction, List<Path<?, ?>> paths) {

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
	public static <A extends Attribute<T, S>, T, S> Path<T, S> path(A attribute) {

		Assert.notNull(attribute, "Attribute must not be null!");
		return new Path<>(Collections.singletonList(attribute));
	}

	/**
	 * Creates a new {@link Path} for the given {@link PluralAttribute}.
	 *
	 * @param attribute must not be {@literal null}.
	 * @return
	 */
	public static <P extends PluralAttribute<T, ?, S>, T, S> Path<T, S> path(P attribute) {

		Assert.notNull(attribute, "Attribute must not be null!");
		return new Path<>(Collections.singletonList(attribute));
	}

	/**
	 * Creates new unsafe {@link JpaSort} based on given properties.
	 *
	 * @param properties must not be {@literal null} or empty.
	 * @return
	 */
	public static JpaSort unsafe(String... properties) {
		return unsafe(Sort.DEFAULT_DIRECTION, properties);
	}

	/**
	 * Creates new unsafe {@link JpaSort} based on given {@link Direction} and properties.
	 *
	 * @param direction must not be {@literal null}.
	 * @param properties must not be {@literal null} or empty.
	 * @return
	 */
	public static JpaSort unsafe(Direction direction, String... properties) {

		Assert.notNull(direction, "Direction must not be null!");
		Assert.notEmpty(properties, "Properties must not be empty!");
		Assert.noNullElements(properties, "Properties must not contain null values!");

		return unsafe(direction, Arrays.asList(properties));
	}

	/**
	 * Creates new unsafe {@link JpaSort} based on given {@link Direction} and properties.
	 *
	 * @param direction must not be {@literal null}.
	 * @param properties must not be {@literal null} or empty.
	 * @return
	 */
	public static JpaSort unsafe(Direction direction, List<String> properties) {

		Assert.notEmpty(properties, "Properties must not be empty!");

		List<Order> orders = new ArrayList<>(properties.size());

		for (String property : properties) {
			orders.add(new JpaOrder(direction, property));
		}

		return new JpaSort(orders);
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

	/**
	 * Custom {@link Order} that keeps a flag to indicate unsafe property handling, i.e. the String provided is not
	 * necessarily a property but can be an arbitrary expression piped into the query execution. We also keep an
	 * additional {@code ignoreCase} flag around as the constructor of the superclass is private currently.
	 *
	 * @author Christoph Strobl
	 * @author Oliver Gierke
	 */
	public static class JpaOrder extends Order {

		private static final long serialVersionUID = 1L;

		private final boolean unsafe;
		private final boolean ignoreCase;

		/**
		 * Creates a new {@link JpaOrder} instance. if order is {@literal null} then order defaults to
		 * {@link Sort#DEFAULT_DIRECTION}
		 *
		 * @param direction can be {@literal null}, will default to {@link Sort#DEFAULT_DIRECTION}.
		 * @param property must not be {@literal null}.
		 */
		private JpaOrder(@Nullable Direction direction, String property) {
			this(direction, property, NullHandling.NATIVE);
		}

		/**
		 * Creates a new {@link Order} instance. if order is {@literal null} then order defaults to
		 * {@link Sort#DEFAULT_DIRECTION}.
		 *
		 * @param direction can be {@literal null}, will default to {@link Sort#DEFAULT_DIRECTION}.
		 * @param property must not be {@literal null}.
		 * @param nullHandlingHint can be {@literal null}, will default to {@link NullHandling#NATIVE}.
		 */
		private JpaOrder(@Nullable Direction direction, String property, NullHandling nullHandlingHint) {
			this(direction, property, nullHandlingHint, false, true);
		}

		private JpaOrder(@Nullable Direction direction, String property, NullHandling nullHandling, boolean ignoreCase,
				boolean unsafe) {

			super(direction, property, nullHandling);
			this.ignoreCase = ignoreCase;
			this.unsafe = unsafe;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.domain.Sort.Order#with(org.springframework.data.domain.Sort.Direction)
		 */
		@Override
		public JpaOrder with(Direction order) {
			return new JpaOrder(order, getProperty(), getNullHandling(), isIgnoreCase(), this.unsafe);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.domain.Sort.Order#with(org.springframework.data.domain.Sort.NullHandling)
		 */
		@Override
		public JpaOrder with(NullHandling nullHandling) {
			return new JpaOrder(getDirection(), getProperty(), nullHandling, isIgnoreCase(), this.unsafe);
		}

		/**
		 * Creates new {@link Sort} with potentially unsafe {@link Order} instances.
		 *
		 * @param properties must not be {@literal null}.
		 * @return
		 */
		public Sort withUnsafe(String... properties) {

			Assert.notEmpty(properties, "Properties must not be empty!");
			Assert.noNullElements(properties, "Properties must not contain null values!");

			List<Order> orders = new ArrayList<>(properties.length);

			for (String property : properties) {
				orders.add(new JpaOrder(getDirection(), property, getNullHandling(), isIgnoreCase(), this.unsafe));
			}

			return Sort.by(orders);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.domain.Sort.Order#ignoreCase()
		 */
		@Override
		public JpaOrder ignoreCase() {
			return new JpaOrder(getDirection(), getProperty(), getNullHandling(), true, this.unsafe);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.domain.Sort.Order#isIgnoreCase()
		 */
		@Override
		public boolean isIgnoreCase() {
			return super.isIgnoreCase() || ignoreCase;
		}

		/**
		 * @return true if {@link JpaOrder} created {@link #withUnsafe(String...)}.
		 */
		public boolean isUnsafe() {
			return unsafe;
		}
	}
}
