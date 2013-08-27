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
package org.springframework.data.jpa.repository.query;

import static java.util.regex.Pattern.*;
import static org.springframework.util.ObjectUtils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Encapsulation of a String JPA query.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
class StringQuery {

	private static final Pattern LIKE_PATTERN;
	private static final String MESSAGE = "Already found like binding with same index / parameter name but differing binding type! Already have: %s, found %s! If you bind a parameter multiple times make sure they use the same like binding.";

	static {

		StringBuilder builder = new StringBuilder();
		builder.append("(?<=like)"); // starts with like
		builder.append("(?: )+"); // some whitespace
		builder.append("(");
		builder.append("%?(\\?(\\d+))%?"); // position parameter with likes
		builder.append("|"); // or
		builder.append("%?(:(\\w+))%?"); // named parameter with likes;
		builder.append(")");
		LIKE_PATTERN = Pattern.compile(builder.toString(), CASE_INSENSITIVE);
	}

	private final String query;
	private final List<StringQuery.LikeBinding> bindings;
	private final String alias;

	/**
	 * Creates a new {@link StringQuery} from the given JPQL query.
	 * 
	 * @param query must not be {@literal null} or empty.
	 */
	public StringQuery(String query) {

		Assert.hasText(query, "Query must not be null or empty!");

		this.bindings = new ArrayList<StringQuery.LikeBinding>();
		this.query = parseLikeBindings(query);
		this.alias = QueryUtils.detectAlias(query);
	}

	/**
	 * Returns whether we have found some like bindings.
	 * 
	 * @return
	 */
	public boolean hasLikeBindings() {
		return !bindings.isEmpty();
	}

	/**
	 * Returns the {@link LikeBinding}s registered.
	 * 
	 * @return
	 */
	List<LikeBinding> getLikeBindings() {
		return bindings;
	}

	/**
	 * Returns the query string.
	 * 
	 * @return
	 */
	public String getQueryString() {
		return query;
	}

	/**
	 * Returns the main alias used in the query.
	 * 
	 * @return the alias
	 */
	public String getAlias() {
		return alias;
	}

	/**
	 * Returns the {@link LikeBinding} for the given name.
	 * 
	 * @param name
	 * @return
	 */
	public LikeBinding getBindingFor(String name) {

		for (StringQuery.LikeBinding binding : bindings) {
			if (binding.hasName(name)) {
				return binding;
			}
		}

		return null;
	}

	/**
	 * Returns the {@link LikeBinding} for the given position.
	 * 
	 * @param position
	 * @return
	 */
	public LikeBinding getBindingFor(int position) {
		for (LikeBinding binding : bindings) {
			if (binding.hasPosition(position)) {
				return binding;
			}
		}

		return null;
	}

	/**
	 * Parses {@link LikeBinding} instances from the given query and adds them to the registered bindings. Returns the
	 * cleaned up query.
	 * 
	 * @param query
	 * @return
	 */
	private final String parseLikeBindings(String query) {

		Matcher matcher = LIKE_PATTERN.matcher(query);
		String result = query;

		while (matcher.find()) {

			Type likeType = getLikeTypeFrom(matcher.group(1));
			String index = matcher.group(3);
			String replacement = matcher.group(2);

			if (index != null) {
				checkAndRegister(new LikeBinding(Integer.parseInt(index), likeType));
			} else {
				checkAndRegister(new LikeBinding(matcher.group(5), likeType));
				replacement = matcher.group(4);
			}

			result = StringUtils.replace(result, matcher.group(1), replacement);
		}

		return result;
	}

	private final void checkAndRegister(LikeBinding binding) {

		for (LikeBinding existing : bindings) {
			if (existing.hasName(binding.name) || existing.hasPosition(binding.position)) {
				Assert.isTrue(existing.equals(binding), String.format(MESSAGE, existing, binding));
			}
		}

		this.bindings.add(binding);
	}

	/**
	 * Extracts the like {@link Type} from the given JPA like expression.
	 * 
	 * @param expression must not be {@literal null} or empty.
	 * @return
	 */
	private static Type getLikeTypeFrom(String expression) {

		Assert.hasText(expression);

		if (expression.matches("%.*%")) {
			return Type.CONTAINING;
		}

		if (expression.startsWith("%")) {
			return Type.ENDING_WITH;
		}

		if (expression.endsWith("%")) {
			return Type.STARTING_WITH;
		}

		return Type.LIKE;
	}

	/**
	 * Represents a paramter binding in a JPQL query augmented with instructions of how to apply a parameter as LIKE
	 * parameter. This allows expressions like {@code …like %?1} in the JPQL query, which is not allowed by plain JPA.
	 * 
	 * @author Oliver Gierke
	 */
	static class LikeBinding {

		private static final List<Type> SUPPORTED_TYPES = Arrays.asList(Type.CONTAINING, Type.STARTING_WITH,
				Type.ENDING_WITH, Type.LIKE);

		private final String name;
		private final Integer position;
		private final Type type;

		/**
		 * Creates a new {@link LikeBinding} for the parameter with the given name and {@link Type}.
		 * 
		 * @param name must not be {@literal null} or empty.
		 * @param type must not be {@literal null}.
		 */
		public LikeBinding(String name, Type type) {

			Assert.hasText(name, "Name must not be null or empty!");
			Assert.notNull(type, "Type must not be null!");
			Assert.isTrue(SUPPORTED_TYPES.contains(type),
					String.format("Type must be one of %s!", StringUtils.collectionToCommaDelimitedString(SUPPORTED_TYPES)));

			this.name = name;
			this.type = type;
			this.position = null;
		}

		/**
		 * Creates a new {@link LikeBinding} for the parameter with the given position and {@link Type}.
		 * 
		 * @param position
		 * @param type must not be {@literal null}.
		 */
		public LikeBinding(int position, Type type) {

			Assert.isTrue(position > 0, "Position must be greater than zero!");
			Assert.notNull(type, "Type must not be null!");

			this.position = position;
			this.type = type;
			this.name = null;
		}

		/**
		 * Returns whether the binding has the given name. Will always be {@literal false} in case the {@link LikeBinding}
		 * has been set up from a position.
		 * 
		 * @param name
		 * @return
		 */
		public boolean hasName(String name) {
			return this.position == null && this.name != null && this.name.equals(name);
		}

		/**
		 * Returns whether the binding has the given position. Will always be {@literal false} in case the
		 * {@link LikeBinding} has been set up from a name.
		 * 
		 * @param position
		 * @return
		 */
		public boolean hasPosition(Integer position) {
			return position != null && this.name == null && this.position == position;
		}

		/**
		 * Returns the type of the {@link LikeBinding}.
		 * 
		 * @return
		 */
		public Type getType() {
			return type;
		}

		/**
		 * Prepares the given raw value according to the like type.
		 * 
		 * @param value
		 */
		public Object prepare(Object value) {

			if (value == null) {
				return value;
			}

			switch (type) {
				case STARTING_WITH:
					return String.format("%s%%", value.toString());
				case ENDING_WITH:
					return String.format("%%%s", value.toString());
				case CONTAINING:
					return String.format("%%%s%%", value.toString());
				case LIKE:
				default:
					return value;
			}
		}

		/* 
		 * (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {

			if (!(obj instanceof LikeBinding)) {
				return false;
			}

			LikeBinding that = (LikeBinding) obj;

			return nullSafeEquals(this.name, that.name) && nullSafeEquals(this.position, that.position)
					&& this.type.equals(that.type);
		}

		/* 
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {

			int result = 17;

			result += nullSafeHashCode(this.name);
			result += nullSafeHashCode(this.position);
			result += nullSafeHashCode(this.type);

			return result;
		}

		/* 
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return String.format("LikeBinding [name: %s, position: %d, type: %s]", name, position, type);
		}
	}
}
