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
package org.springframework.data.jpa.domain;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.util.Assert;

/**
 * A wrapper around a prototype object that can be used in <quote>Query by Example<quote> queries
 * 
 * @author Thomas Darimont
 * @param <T>
 */
public class Example<T> {

	private final T prototype;
	private final Set<String> ignoredAttributes;

	/**
	 * Creates a new {@link Example} with the given {@code prototype}.
	 * 
	 * @param prototype must not be {@literal null}
	 */
	public Example(T prototype) {
		this(prototype, Collections.<String> emptySet());
	}

	/**
	 * Creates a new {@link Example} with the given {@code prototype} ignoring the given attributes.
	 * 
	 * @param prototype prototype must not be {@literal null}
	 * @param attributeNames prototype must not be {@literal null}
	 */
	public Example(T prototype, Set<String> attributeNames) {

		Assert.notNull(prototype, "Prototype must not be null!");
		Assert.notNull(attributeNames, "attributeNames must not be null!");

		this.prototype = prototype;
		this.ignoredAttributes = attributeNames;
	}

	public T getPrototype() {
		return prototype;
	}

	public Set<String> getIgnoredAttributes() {
		return Collections.unmodifiableSet(ignoredAttributes);
	}

	public boolean isAttributeIgnored(String attributePath) {
		return ignoredAttributes.contains(attributePath);
	}

	public static <T> Example<T> exampleOf(T prototype) {
		return new Example<T>(prototype);
	}

	public static <T> Builder<T> newExample(T prototype) {
		return new Builder<T>(prototype);
	}

	/**
	 * A {@link Builder} for {@link Example}s.
	 * 
	 * @author Thomas Darimont
	 * @param <T>
	 */
	public static class Builder<T> {

		private final T prototype;
		private Set<String> ignoredAttributeNames;

		/**
		 * @param prototype
		 */
		public Builder(T prototype) {

			Assert.notNull(prototype, "Prototype must not be null!");

			this.prototype = prototype;
		}

		/**
		 * Allows to specify attribute names that should be ignored.
		 * 
		 * @param attributeNames
		 * @return
		 */
		public Builder<T> ignoring(String... attributeNames) {
			
			Assert.notNull(attributeNames, "attributeNames must not be null!");
			
			return ignoring(Arrays.asList(attributeNames));
		}

		/**
		 * Allows to specify attribute names that should be ignored.
		 * 
		 * @param attributeNames
		 * @return
		 */
		public Builder<T> ignoring(Collection<String> attributeNames) {

			Assert.notNull(attributeNames, "attributeNames must not be null!");

			this.ignoredAttributeNames = new HashSet<String>(attributeNames);
			return this;
		}

		/**
		 * Constructs the actual {@link Example} instance.
		 * 
		 * @return
		 */
		public Example<T> build() {
			return new Example<T>(prototype, ignoredAttributeNames);
		}
	}
}
