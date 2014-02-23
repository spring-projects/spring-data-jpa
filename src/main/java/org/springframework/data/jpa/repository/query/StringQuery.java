/*
 * Copyright 2013-2014 the original author or authors.
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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Encapsulation of a String JPA query.
 * 
 * @author Oliver Gierke
 */
class StringQuery {

	private final String query;
	private final List<ParameterBinding> bindings;
	private final String alias;

	/**
	 * Creates a new {@link StringQuery} from the given JPQL query.
	 * 
	 * @param query must not be {@literal null} or empty.
	 */
	public StringQuery(String query) {

		Assert.hasText(query, "Query must not be null or empty!");

		this.bindings = new ArrayList<StringQuery.ParameterBinding>();
		this.query = ParameterBindingParser.INSTANCE.parseParameterBindingsOfQueryIntoBindingsAndReturnCleanedQuery(query,
				this.bindings);
		this.alias = QueryUtils.detectAlias(query);
	}

	/**
	 * Returns whether we have found some like bindings.
	 * 
	 * @return
	 */
	public boolean hasParameterBindings() {
		return !bindings.isEmpty();
	}

	/**
	 * Returns the {@link ParameterBinding}s registered.
	 * 
	 * @return
	 */
	List<ParameterBinding> getParameterBindings() {
		return bindings;
	}

	/**
	 * Returns the JPQL query.
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
	 * Returns the {@link ParameterBinding} for the given name.
	 * 
	 * @param name must not be {@literal null} or empty.
	 * @return
	 */
	public ParameterBinding getBindingFor(String name) {

		Assert.hasText(name, "Name must not be null or empty!");

		for (ParameterBinding binding : bindings) {
			if (binding.hasName(name)) {
				return binding;
			}
		}

		throw new IllegalArgumentException(String.format("No parameter binding found for name %s!", name));
	}

	/**
	 * Returns the {@link ParameterBinding} for the given position.
	 * 
	 * @param position
	 * @return
	 */
	public ParameterBinding getBindingFor(int position) {

		for (ParameterBinding binding : bindings) {
			if (binding.hasPosition(position)) {
				return binding;
			}
		}

		throw new IllegalArgumentException(String.format("No parameter binding found for position %s!", position));
	}

	/**
	 * A parser that extracts the parameter bindings from a given query string.
	 * 
	 * @author Thomas Darimont
	 */
	private static enum ParameterBindingParser {

		INSTANCE;

		private static final Pattern PARAMETER_BINDING_PATTERN;
		private static final String MESSAGE = "Already found parameter binding with same index / parameter name but differing binding type! "
				+ "Already have: %s, found %s! If you bind a parameter multiple times make sure they use the same binding.";

		static {

			List<String> keywords = new ArrayList<String>();

			for (ParameterBindingType type : ParameterBindingType.values()) {
				if (type.getKeyword() != null) {
					keywords.add(type.getKeyword());
				}
			}

			StringBuilder builder = new StringBuilder();
			builder.append("(");
			builder.append(StringUtils.collectionToDelimitedString(keywords, "|")); // keywords
			builder.append(")?");
			builder.append("(?: )?"); // some whitespace
			builder.append("(");
			builder.append("%?(\\?(\\d+))%?"); // position parameter
			builder.append("|"); // or
			builder.append("%?(:(\\w+))%?"); // named parameter;
			builder.append(")");

			PARAMETER_BINDING_PATTERN = Pattern.compile(builder.toString(), CASE_INSENSITIVE);
		}

		/**
		 * Parses {@link ParameterBinding} instances from the given query and adds them to the registered bindings. Returns
		 * the cleaned up query.
		 * 
		 * @param query
		 * @return
		 */
		private final String parseParameterBindingsOfQueryIntoBindingsAndReturnCleanedQuery(String query,
				List<ParameterBinding> bindings) {

			Matcher matcher = PARAMETER_BINDING_PATTERN.matcher(query);
			String result = query;

			while (matcher.find()) {

				String parameterIndexString = matcher.group(4);
				String parameterName = parameterIndexString != null ? null : matcher.group(6);
				Integer parameterIndex = parameterIndexString == null ? null : Integer.valueOf(parameterIndexString);
				String typeSource = matcher.group(1);

				switch (ParameterBindingType.of(typeSource)) {

					case LIKE:

						Type likeType = LikeParameterBinding.getLikeTypeFrom(matcher.group(2));
						String replacement = matcher.group(3);

						if (parameterIndex != null) {
							checkAndRegister(new LikeParameterBinding(parameterIndex, likeType), bindings);
						} else {
							checkAndRegister(new LikeParameterBinding(parameterName, likeType), bindings);
							replacement = matcher.group(5);
						}

						result = StringUtils.replace(result, matcher.group(2), replacement);
						break;

					case IN:

						if (parameterIndex != null) {
							checkAndRegister(new InParameterBinding(parameterIndex), bindings);
						} else {
							checkAndRegister(new InParameterBinding(parameterName), bindings);
						}

						result = query;
						break;

					case AS_IS: // fall-through we don't need a special parameter binding for the given parameter.
					default:

						bindings.add(parameterIndex != null ? new ParameterBinding(parameterIndex) : new ParameterBinding(
								parameterName));
				}
			}

			return result;
		}

		private static void checkAndRegister(ParameterBinding binding, List<ParameterBinding> bindings) {

			for (ParameterBinding existing : bindings) {
				if (existing.hasName(binding.getName()) || existing.hasPosition(binding.getPosition())) {
					Assert.isTrue(existing.equals(binding), String.format(MESSAGE, existing, binding));
				}
			}

			if (!bindings.contains(binding)) {
				bindings.add(binding);
			}
		}

		/**
		 * An enum for the different types of bindings.
		 * 
		 * @author Thomas Darimont
		 * @author Oliver Gierke
		 */
		private static enum ParameterBindingType {

			// Trailing whitespace is intentional to reflect that the keywords must be used with at least one whitespace
			// character, while = does not.
			LIKE("like "), IN("in "), AS_IS(null);

			private final String keyword;

			private ParameterBindingType(String keyword) {
				this.keyword = keyword;
			}

			/**
			 * Returns the keyword that will tirgger the binding type or {@literal null} if the type is not triggered by a
			 * keyword.
			 * 
			 * @return the keyword
			 */
			public String getKeyword() {
				return keyword;
			}

			/**
			 * Return the appropriate {@link ParameterBindingType} for the given {@link String}. Returns {@keyword
			 * #AS_IS} in case no other {@link ParameterBindingType} could be found.
			 * 
			 * @param typeSource
			 * @return
			 */
			static ParameterBindingType of(String typeSource) {

				if (!StringUtils.hasText(typeSource)) {
					return AS_IS;
				}

				for (ParameterBindingType type : values()) {
					if (type.name().equalsIgnoreCase(typeSource.trim())) {
						return type;
					}
				}

				throw new IllegalArgumentException(String.format("Unsupported parameter binding type %s!", typeSource));
			}
		}
	}

	/**
	 * A generic parameter binding with name or position information.
	 * 
	 * @author Thomas Darimont
	 */
	static class ParameterBinding {

		private final String name;
		private final Integer position;

		/**
		 * Creates a new {@link ParameterBinding} for the parameter with the given name.
		 * 
		 * @param name must not be {@literal null}.
		 */
		public ParameterBinding(String name) {

			Assert.notNull(name, "Name must not be null!");

			this.name = name;
			this.position = null;
		}

		/**
		 * Creates a new {@link ParameterBinding} for the parameter with the given position.
		 * 
		 * @param position must not be {@literal null}.
		 */
		public ParameterBinding(Integer position) {

			Assert.notNull(position, "Position must not be null!");

			this.name = null;
			this.position = position;
		}

		/**
		 * Returns whether the binding has the given name. Will always be {@literal false} in case the
		 * {@link ParameterBinding} has been set up from a position.
		 * 
		 * @param name
		 * @return
		 */
		public boolean hasName(String name) {
			return this.position == null && this.name != null && this.name.equals(name);
		}

		/**
		 * Returns whether the binding has the given position. Will always be {@literal false} in case the
		 * {@link ParameterBinding} has been set up from a name.
		 * 
		 * @param position
		 * @return
		 */
		public boolean hasPosition(Integer position) {
			return position != null && this.name == null && this.position == position;
		}

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @return the position
		 */
		public Integer getPosition() {
			return position;
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

			return result;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {

			if (!(obj instanceof ParameterBinding)) {
				return false;
			}

			ParameterBinding that = (ParameterBinding) obj;

			return nullSafeEquals(this.name, that.name) && nullSafeEquals(this.position, that.position);
		}

		/* 
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return String.format("ParameterBinding [name: %s, position: %d]", getName(), getPosition());
		}

		/**
		 * @param valueToBind
		 * @return
		 */
		public Object prepare(Object valueToBind) {
			return valueToBind;
		}
	}

	/**
	 * Represents a {@link ParameterBinding} in a JPQL query augmented with instructions of how to apply a parameter as an
	 * {@code IN} parameter.
	 * 
	 * @author Thomas Darimont
	 */
	static class InParameterBinding extends ParameterBinding {

		/**
		 * Creates a new {@link InParameterBinding} for the parameter with the given name.
		 * 
		 * @param name
		 */
		public InParameterBinding(String name) {
			super(name);
		}

		/**
		 * Creates a new {@link InParameterBinding} for the parameter with the given position.
		 * 
		 * @param position
		 */
		public InParameterBinding(int position) {
			super(position);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.query.StringQuery.ParameterBinding#prepare(java.lang.Object)
		 */
		@Override
		public Object prepare(Object value) {

			if (!ObjectUtils.isArray(value)) {
				return value;
			}

			int length = Array.getLength(value);
			Collection<Object> result = new ArrayList<Object>(length);

			for (int i = 0; i < length; i++) {
				result.add(Array.get(value, i));
			}

			return result;
		}
	}

	/**
	 * Represents a parameter binding in a JPQL query augmented with instructions of how to apply a parameter as LIKE
	 * parameter. This allows expressions like {@code …like %?1} in the JPQL query, which is not allowed by plain JPA.
	 * 
	 * @author Oliver Gierke
	 * @author Thomas Darimont
	 */
	static class LikeParameterBinding extends ParameterBinding {

		private static final List<Type> SUPPORTED_TYPES = Arrays.asList(Type.CONTAINING, Type.STARTING_WITH,
				Type.ENDING_WITH, Type.LIKE);

		private final Type type;

		/**
		 * Creates a new {@link LikeParameterBinding} for the parameter with the given name and {@link Type}.
		 * 
		 * @param name must not be {@literal null} or empty.
		 * @param type must not be {@literal null}.
		 */
		public LikeParameterBinding(String name, Type type) {

			super(name);

			Assert.hasText(name, "Name must not be null or empty!");
			Assert.notNull(type, "Type must not be null!");

			Assert.isTrue(SUPPORTED_TYPES.contains(type),
					String.format("Type must be one of %s!", StringUtils.collectionToCommaDelimitedString(SUPPORTED_TYPES)));

			this.type = type;
		}

		/**
		 * Creates a new {@link LikeParameterBinding} for the parameter with the given position and {@link Type}.
		 * 
		 * @param position
		 * @param type must not be {@literal null}.
		 */
		public LikeParameterBinding(int position, Type type) {

			super(position);

			Assert.isTrue(position > 0, "Position must be greater than zero!");
			Assert.notNull(type, "Type must not be null!");

			Assert.isTrue(SUPPORTED_TYPES.contains(type),
					String.format("Type must be one of %s!", StringUtils.collectionToCommaDelimitedString(SUPPORTED_TYPES)));

			this.type = type;
		}

		/**
		 * Returns the {@link Type} of the binding.
		 * 
		 * @return the type
		 */
		public Type getType() {
			return type;
		}

		/**
		 * Prepares the given raw keyword according to the like type.
		 * 
		 * @param keyword
		 */
		@Override
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

			if (!(obj instanceof LikeParameterBinding)) {
				return false;
			}

			LikeParameterBinding that = (LikeParameterBinding) obj;

			return super.equals(obj) && this.type.equals(that.type);
		}

		/* 
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {

			int result = super.hashCode();

			result += nullSafeHashCode(this.type);

			return result;
		}

		/* 
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return String.format("LikeBinding [name: %s, position: %d, type: %s]", getName(), getPosition(), type);
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
	}
}
