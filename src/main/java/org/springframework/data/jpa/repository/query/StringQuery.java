/*
 * Copyright 2013-2017 the original author or authors.
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
 * @author Thomas Darimont
 * @author Oliver Wehrens
 * @author Mark Paluch
 * @author Jens Schauder
 */
class StringQuery {

	private static final String PARAMETER_NAME_MISSING = "Name for parameter binding must not be null or empty! On JDKs < 8, you need to use @Param for named parameters, on JDK 8 or better, be sure to compile with -parameters.";

	private final String query;
	private final List<ParameterBinding> bindings;
	private final String alias;
	private final boolean hasConstructorExpression;

	/**
	 * Creates a new {@link StringQuery} from the given JPQL query.
	 * 
	 * @param query must not be {@literal null} or empty.
	 */
	StringQuery(String query) {

		Assert.hasText(query, "Query must not be null or empty!");

		this.bindings = new ArrayList<StringQuery.ParameterBinding>();
		this.query = ParameterBindingParser.INSTANCE.parseParameterBindingsOfQueryIntoBindingsAndReturnCleanedQuery(query,
				this.bindings);
		this.alias = QueryUtils.detectAlias(query);
		this.hasConstructorExpression = QueryUtils.hasConstructorExpression(query);
	}

	/**
	 * Returns whether we have found some like bindings.
	 */
	boolean hasParameterBindings() {
		return !bindings.isEmpty();
	}

	/**
	 * Returns the {@link ParameterBinding}s registered.
	 */
	List<ParameterBinding> getParameterBindings() {
		return bindings;
	}

	/**
	 * Returns the query string.
	 */
	String getQueryString() {
		return query;
	}

	/**
	 * Returns the main alias used in the query.
	 * 
	 * @return the alias
	 */
	String getAlias() {
		return alias;
	}

	/**
	 * Returns the {@link ParameterBinding} for the given name.
	 * 
	 * @param name must not be {@literal null} or empty.
	 */
	ParameterBinding getBindingFor(String name) {

		Assert.hasText(name, PARAMETER_NAME_MISSING);

		for (ParameterBinding binding : bindings) {
			if (binding.hasName(name)) {
				return binding;
			}
		}

		throw new IllegalArgumentException(String.format("No parameter binding found for name %s!", name));
	}

	/**
	 * Returns the {@link ParameterBinding} for the given position.
	 */
	ParameterBinding getBindingFor(int position) {

		for (ParameterBinding binding : bindings) {
			if (binding.hasPosition(position)) {
				return binding;
			}
		}

		throw new IllegalArgumentException(String.format("No parameter binding found for position %s!", position));
	}

	/**
	 * Returns whether the query is using a constructor expression.
	 * @since 1.10
	 */
	boolean hasConstructorExpression() {
		return hasConstructorExpression;
	}

	/**
	 * Returns whether the query uses the default projection, i.e. returns the main alias defined for the query.
	 */
	boolean isDefaultProjection() {
		return QueryUtils.getProjection(query).equals(alias);
	}

	/**
	 * A parser that extracts the parameter bindings from a given query string.
	 * 
	 * @author Thomas Darimont
	 */
	public enum ParameterBindingParser {

		INSTANCE;

		static final String EXPRESSION_PARAMETER_PREFIX = "__$synthetic$__";
		private static final Pattern PARAMETER_BINDING_BY_INDEX = Pattern.compile("\\?(\\d+)");
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
			builder.append("\\(?"); // optional braces around parameters
			builder.append("(");
			builder.append("%?(\\?(\\d+))%?"); // position parameter and parameter index
			builder.append("|"); // or
			builder.append("%?(:([\\p{L}\\w]+))%?"); // named parameter and the parameter name
			builder.append("|"); // or
			builder.append("%?((:|\\?)#\\{([^}]+)\\})%?"); // expression parameter and expression
			builder.append(")");
			builder.append("\\)?"); // optional braces around parameters

			PARAMETER_BINDING_PATTERN = Pattern.compile(builder.toString(), CASE_INSENSITIVE);
		}

		/**
		 * Parses {@link ParameterBinding} instances from the given query and adds them to the registered bindings. Returns
		 * the cleaned up query.
		 */
		private String parseParameterBindingsOfQueryIntoBindingsAndReturnCleanedQuery(String query,
																					  List<ParameterBinding> bindings) {

			String result = query;
			Matcher matcher = PARAMETER_BINDING_PATTERN.matcher(query);

			int greatestParameterIndex = tryFindGreatestParameterIndexIn(query);

			boolean parametersShouldBeAccessedByIndex = greatestParameterIndex != -1;

			/*
			 * Prefer indexed access over named parameters if only SpEL Expression parameters are present.
			 */
			if (!parametersShouldBeAccessedByIndex && query.contains("?#{")) {
				parametersShouldBeAccessedByIndex = true;
				greatestParameterIndex = 0;
			}

			/*
			 * If parameters need to be bound by index, we bind the synthetic expression parameters starting from position of the greatest discovered index parameter in order to
			 * not mix-up with the actual parameter indices.  
			 */
			int expressionParameterIndex = parametersShouldBeAccessedByIndex ? greatestParameterIndex : 0;

			while (matcher.find()) {

				String parameterIndexString = matcher.group(4);
				String parameterName = parameterIndexString != null ? null : matcher.group(6);
				Integer parameterIndex = parameterIndexString == null ? null : Integer.valueOf(parameterIndexString);
				String typeSource = matcher.group(1);
				String expression = null;
				String replacement = null;

				if (parameterName == null && parameterIndex == null) {
					expressionParameterIndex++;

					if (parametersShouldBeAccessedByIndex) {
						parameterIndex = expressionParameterIndex;
						replacement = "?" + parameterIndex;
					} else {
						parameterName = EXPRESSION_PARAMETER_PREFIX + expressionParameterIndex;
						replacement = ":" + parameterName;
					}

					expression = matcher.group(9);
				}

				switch (ParameterBindingType.of(typeSource)) {

					case LIKE:

						Type likeType = LikeParameterBinding.getLikeTypeFrom(matcher.group(2));
						replacement = replacement != null ? replacement : matcher.group(3);

						if (parameterIndex != null) {
							checkAndRegister(new LikeParameterBinding(parameterIndex, likeType, expression), bindings);
						} else {
							checkAndRegister(new LikeParameterBinding(parameterName, likeType, expression), bindings);

							replacement = expression != null ? ":" + parameterName : matcher.group(5);
						}

						break;

					case IN:

						if (parameterIndex != null) {
							checkAndRegister(new InParameterBinding(parameterIndex, expression), bindings);
						} else {
							checkAndRegister(new InParameterBinding(parameterName, expression), bindings);
						}

						break;

					case AS_IS: // fall-through we don't need a special parameter binding for the given parameter.
					default:

						bindings.add(parameterIndex != null ? new ParameterBinding(null, parameterIndex, expression)
								: new ParameterBinding(parameterName, null, expression));
				}

				if (replacement != null) {
					result = replaceFirst(result, matcher.group(2), replacement);
				}

			}

			return result;
		}

		private static String replaceFirst(String text, String substring, String replacement) {

			int index = text.indexOf(substring);
			if (index < 0) {
				return text;
			}

			return text.substring(0, index) + replacement + text.substring(index + substring.length());
		}

		private int tryFindGreatestParameterIndexIn(String query) {

			Matcher parameterIndexMatcher = PARAMETER_BINDING_BY_INDEX.matcher(query);

			int greatestParameterIndex = -1;
			while (parameterIndexMatcher.find()) {
				String parameterIndexString = parameterIndexMatcher.group(1);
				greatestParameterIndex = Math.max(greatestParameterIndex, Integer.parseInt(parameterIndexString));
			}

			return greatestParameterIndex;
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
		private enum ParameterBindingType {

			// Trailing whitespace is intentional to reflect that the keywords must be used with at least one whitespace
			// character, while = does not.
			LIKE("like "), IN("in "), AS_IS(null);

			private final String keyword;

			ParameterBindingType(String keyword) {
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
			 * Return the appropriate {@link ParameterBindingType} for the given {@link String}. Returns {@keyword #AS_IS} in
			 * case no other {@link ParameterBindingType} could be found.
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
		private final String expression;
		private final Integer position;

		/**
		 * Creates a new {@link ParameterBinding} for the parameter with the given position.
		 * 
		 * @param position must not be {@literal null}.
		 */
		ParameterBinding(Integer position) {
			this(null, position, null);
		}

		/**
		 * Creates a new {@link ParameterBinding} for the parameter with the given name, position and expression
		 * information.
		 */
		ParameterBinding(String name, Integer position, String expression) {

			if (name == null) {
				Assert.notNull(position, "Position must not be null!");
			}

			if (position == null) {
				Assert.notNull(name, "Name must not be null!");
			}

			this.name = name;
			this.position = position;
			this.expression = expression;
		}

		/**
		 * Returns whether the binding has the given name. Will always be {@literal false} in case the
		 * {@link ParameterBinding} has been set up from a position.
		 */
		boolean hasName(String name) {
			return this.position == null && this.name != null && this.name.equals(name);
		}

		/**
		 * Returns whether the binding has the given position. Will always be {@literal false} in case the
		 * {@link ParameterBinding} has been set up from a name.
		 */
		boolean hasPosition(Integer position) {
			return position != null && this.name == null && position.equals(this.position);
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
		Integer getPosition() {
			return position;
		}

		/**
		 * @return {@literal true} if this parameter binding is a synthetic SpEL expression.
		 */
		boolean isExpression() {
			return this.expression != null;
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
			result += nullSafeHashCode(this.expression);

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

			return nullSafeEquals(this.name, that.name) && nullSafeEquals(this.position, that.position)
					&& nullSafeEquals(this.expression, that.expression);
		}

		/* 
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return String.format("ParameterBinding [name: %s, position: %d, expression: %s]", getName(), getPosition(),
					getExpression());
		}

		Object prepare(Object valueToBind) {
			return valueToBind;
		}

		String getExpression() {
			return expression;
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
		 */
		InParameterBinding(String name, String expression) {
			super(name, null, expression);
		}

		/**
		 * Creates a new {@link InParameterBinding} for the parameter with the given position.
		 */
		InParameterBinding(int position, String expression) {
			super(null, position, expression);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.query.StringQuery.ParameterBinding#prepare(java.lang.Object)
		 */
		@Override
		Object prepare(Object value) {

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
		LikeParameterBinding(String name, Type type) {
			this(name, type, null);
		}

		/**
		 * Creates a new {@link LikeParameterBinding} for the parameter with the given name and {@link Type} and parameter
		 * binding input.
		 * 
		 * @param name must not be {@literal null} or empty.
		 * @param type must not be {@literal null}.
		 * @param expression may be {@literal null}.
		 */
		LikeParameterBinding(String name, Type type, String expression) {

			super(name, null, expression);

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
		LikeParameterBinding(int position, Type type) {
			this(position, type, null);
		}

		/**
		 * Creates a new {@link LikeParameterBinding} for the parameter with the given position and {@link Type}.
		 * 
		 * @param position
		 * @param type must not be {@literal null}.
		 * @param expression may be {@literal null}.
		 */
		LikeParameterBinding(int position, Type type, String expression) {

			super(null, position, expression);

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
		Type getType() {
			return type;
		}

		/**
		 * Prepares the given raw keyword according to the like type.
		 */
		@Override
		Object prepare(Object value) {

			if (value == null) {
				return null;
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
		 */
		private static Type getLikeTypeFrom(String expression) {

			Assert.hasText(expression, "Expression must not be null or empty!");

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
