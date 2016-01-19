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
package org.springframework.data.domain;

import org.springframework.data.domain.Example.StringMatcher;
import org.springframework.util.Assert;

/**
 * Define specific property handling for a Dot-Path.
 * 
 * @author Christoph Strobl
 */
public class PropertySpecifier {

	private final String path;

	private StringMatcher stringMatcher;
	private Boolean ignoreCase;

	private PropertyValueTransformer valueTransformer;

	/**
	 * Creates new {@link PropertySpecifier} for given path.
	 * 
	 * @param path Dot-Path to the property. Must not be {@literal null}.
	 */
	PropertySpecifier(String path) {

		Assert.hasText(path, "Path must not be null/empty!");
		this.path = path;
	}

	/**
	 * Get the properties Dot-Path.
	 * 
	 * @return never {@literal null}.
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Get the {@link StringMatcher}.
	 * 
	 * @return can be {@literal null}.
	 */
	public StringMatcher getStringMatcher() {
		return stringMatcher;
	}

	/**
	 * @return {literal true} in case {@link StringMatcher} defined.
	 */
	public boolean hasStringMatcher() {
		return this.stringMatcher != null;
	}

	/**
	 * @return {@literal null} if not set.
	 */
	public Boolean getIgnoreCase() {
		return ignoreCase;
	}

	/**
	 * Get the property transformer to be applied.
	 * 
	 * @return never {@literal null}.
	 */
	public PropertyValueTransformer getPropertyValueTransformer() {
		return valueTransformer == null ? NoOpPropertyValueTransformer.INSTANCE : valueTransformer;
	}

	/**
	 * Transforms a given source using the {@link PropertyValueTransformer}.
	 * 
	 * @param source
	 * @return
	 */
	public Object transformValue(Object source) {
		return getPropertyValueTransformer().convert(source);
	}

	/**
	 * Creates new case ignoring {@link PropertySpecifier} for given path.
	 * 
	 * @param propertyPath must not be {@literal null}.
	 * @return
	 */
	public static PropertySpecifier ignoreCase(String propertyPath) {
		return new Builder(propertyPath).ignoreCase().get();
	}

	/**
	 * Create new {@link Builder} for specifying {@link PropertySpecifier}.
	 * 
	 * @param propertyPath must not be {@literal null}.
	 * @return
	 */
	public static Builder newPropertySpecifier(String propertyPath) {
		return new Builder(propertyPath);
	}

	/**
	 * Builder for specifying desired behavior of {@link PropertySpecifier}.
	 * 
	 * @author Christoph Strobl
	 */
	public static class Builder {

		private PropertySpecifier specifier;

		Builder(String path) {
			specifier = new PropertySpecifier(path);
		}

		/**
		 * Sets the {@link StringMatcher} used for {@link PropertySpecifier}.
		 * 
		 * @param stringMatcher
		 * @return
		 * @see Builder#stringMatcher(StringMatcher)
		 */
		public Builder with(StringMatcher stringMatcher) {
			return stringMatcher(stringMatcher);
		}

		/**
		 * Sets the {@link PropertyValueTransformer} used for {@link PropertySpecifier}.
		 * 
		 * @param valueTransformer
		 * @return
		 * @see Builder#valueTransformer(PropertyValueTransformer)
		 */
		public Builder with(PropertyValueTransformer valueTransformer) {
			return valueTransformer(valueTransformer);
		}

		/**
		 * Sets the {@link StringMatcher} used for {@link PropertySpecifier}.
		 * 
		 * @param stringMatcher
		 * @return
		 */
		public Builder stringMatcher(StringMatcher stringMatcher) {
			return stringMatcher(stringMatcher, specifier.ignoreCase);
		}

		/**
		 * Sets the {@link StringMatcher} used for {@link PropertySpecifier}.
		 * 
		 * @param stringMatcher
		 * @param ignoreCase
		 * @return
		 */
		public Builder stringMatcher(StringMatcher stringMatcher, Boolean ignoreCase) {

			specifier.stringMatcher = stringMatcher;
			specifier.ignoreCase = ignoreCase;
			return this;
		}

		/**
		 * @return
		 */
		public Builder ignoreCase() {
			specifier.ignoreCase = Boolean.TRUE;
			return this;
		}

		/**
		 * Sets the {@link PropertyValueTransformer} used for {@link PropertySpecifier}.
		 * 
		 * @param valueTransformer
		 * @return
		 */
		public Builder valueTransformer(PropertyValueTransformer valueTransformer) {
			specifier.valueTransformer = valueTransformer;
			return this;
		}

		/**
		 * @return {@link PropertySpecifier} as defined.
		 */
		public PropertySpecifier get() {
			return this.specifier;
		}
	}

	/**
	 * @author Christoph Strobl
	 */
	static enum NoOpPropertyValueTransformer implements PropertyValueTransformer {

		INSTANCE;

		@Override
		public Object convert(Object source) {
			return source;
		}

	}
}
