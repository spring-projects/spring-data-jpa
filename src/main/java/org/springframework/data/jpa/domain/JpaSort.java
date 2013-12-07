/*
 * Copyright 2013 the original author or authors.
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
		this(direction, paths(Arrays.asList(attributes)));
	}

	/**
	 * Creates a new {@link JpaSort} for the given direction and {@link Path}s.
	 * 
	 * @param direction the sorting direction.
	 * @param paths must not be {@literal null} or empty.
	 */
	public JpaSort(Direction direction, JpaSort.Path<?, ?>... paths) {
		this(direction, Arrays.asList(paths));
	}

	private JpaSort(Direction direction, List<JpaSort.Path<?, ?>> paths) {
		super(direction, toString(paths));
	}

	/**
	 * Turns the given {@link Attribute}s into {@link Path}s.
	 * 
	 * @param attributes must not be {@literal null} or empty.
	 * @return
	 */
	private static List<Path<?, ?>> paths(List<? extends Attribute<?, ?>> attributes) {

		Assert.notNull(attributes, "Attributes must not be null!");
		Assert.isTrue(!attributes.isEmpty(), "Attributes must not be empty");

		List<Path<?, ?>> paths = new ArrayList<Path<?, ?>>(attributes.size());

		for (Attribute<?, ?> attribute : attributes) {
			paths.add(path(attribute));
		}

		return paths;
	}

	/**
	 * Renders the given {@link Path}s into a {@link String} array.
	 * 
	 * @param paths must not be {@literal null} or empty.
	 * @return
	 */
	private static String[] toString(List<Path<?, ?>> paths) {

		List<String> strings = new ArrayList<String>(paths.size());

		for (Path<?, ?> path : paths) {
			strings.add(path.toString());
		}

		return strings.toArray(new String[strings.size()]);
	}

	/**
	 * Creates a new {@link Path} for the given {@link Attribute}.
	 * 
	 * @param attribute must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T, S> Path<T, S> path(Attribute<T, S> attribute) {

		Assert.notNull(attribute, "Attribute must not be null!");

		List<? extends Attribute<?, ?>> attributes = Arrays.asList(attribute);
		return new Path<T, S>(attributes);
	}

	/**
	 * Creates a new {@link Path} for the given {@link PluralAttribute}.
	 * 
	 * @param attribute must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T, S> Path<T, S> path(PluralAttribute<T, ?, S> attribute) {

		Assert.notNull(attribute, "Attribute must not be null!");

		List<? extends Attribute<?, ?>> attributes = Arrays.asList(attribute);
		return new Path<T, S>(attributes);
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
		public <U> Path<S, U> dot(Attribute<S, U> attribute) {
			return new Path<S, U>(add(attribute));
		}

		/**
		 * Collects the given {@link Attribute} and returning a new {@link Path} pointing to the attribute type.
		 * 
		 * @param attribute must not be {@literal null}.
		 * @return
		 */
		public <U> Path<S, U> dot(PluralAttribute<S, ?, U> attribute) {
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
