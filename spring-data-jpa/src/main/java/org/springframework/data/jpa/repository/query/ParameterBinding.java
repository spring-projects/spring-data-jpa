/*
 * Copyright 2023-2024 the original author or authors.
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

import static org.springframework.util.ObjectUtils.*;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.data.expression.ValueExpression;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * A generic parameter binding with name or position information.
 *
 * @author Thomas Darimont
 * @author Mark Paluch
 */
class ParameterBinding {

	private final BindingIdentifier identifier;
	private final ParameterOrigin origin;

	/**
	 * Creates a new {@link ParameterBinding} for the parameter with the given identifier and origin.
	 *
	 * @param identifier of the parameter, must not be {@literal null}.
	 * @param origin the origin of the parameter (expression or method argument)
	 */
	ParameterBinding(BindingIdentifier identifier, ParameterOrigin origin) {

		Assert.notNull(identifier, "BindingIdentifier must not be null");
		Assert.notNull(origin, "ParameterOrigin must not be null");

		this.identifier = identifier;
		this.origin = origin;
	}

	public BindingIdentifier getIdentifier() {
		return identifier;
	}

	public ParameterOrigin getOrigin() {
		return origin;
	}

	/**
	 * @return the name if available or {@literal null}.
	 */
	@Nullable
	public String getName() {
		return identifier.hasName() ? identifier.getName() : null;
	}

	/**
	 * @return the name
	 * @throws IllegalStateException if the name is not available.
	 * @since 2.0
	 */
	String getRequiredName() throws IllegalStateException {

		String name = getName();

		if (name != null) {
			return name;
		}

		throw new IllegalStateException(String.format("Required name for %s not available", this));
	}

	/**
	 * @return the position if available or {@literal null}.
	 */
	@Nullable
	Integer getPosition() {
		return identifier.hasPosition() ? identifier.getPosition() : null;
	}

	/**
	 * @return the position
	 * @throws IllegalStateException if the position is not available.
	 * @since 2.0
	 */
	int getRequiredPosition() throws IllegalStateException {

		Integer position = getPosition();

		if (position != null) {
			return position;
		}

		throw new IllegalStateException(String.format("Required position for %s not available", this));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		ParameterBinding that = (ParameterBinding) o;

		if (!nullSafeEquals(identifier, that.identifier)) {
			return false;
		}
		return nullSafeEquals(origin, that.origin);
	}

	@Override
	public int hashCode() {
		int result = nullSafeHashCode(identifier);
		result = 31 * result + nullSafeHashCode(origin);
		return result;
	}

	@Override
	public String toString() {
		return String.format("ParameterBinding [identifier: %s, origin: %s]", identifier, origin);
	}

	/**
	 * @param valueToBind value to prepare
	 */
	@Nullable
	public Object prepare(@Nullable Object valueToBind) {
		return valueToBind;
	}

	/**
	 * Check whether the {@code other} binding uses the same bind target.
	 *
	 * @param other must not be {@literal null}.
	 * @return {@code true} if the other binding uses the same parameter to bind to as this one.
	 */
	public boolean bindsTo(ParameterBinding other) {

		if (identifier.hasName() && other.identifier.hasName()) {
			if (identifier.getName().equals(other.identifier.getName())) {
				return true;
			}
		}

		if (identifier.hasPosition() && other.identifier.hasPosition()) {
			if (identifier.getPosition() == other.identifier.getPosition()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Check whether this binding can be bound as the {@code other} binding by checking its type and origin. Subclasses
	 * may override this method to include other properties for the compatibility check.
	 *
	 * @param other
	 * @return {@code true} if the other binding is compatible with this one.
	 */
	public boolean isCompatibleWith(ParameterBinding other) {
		return other.getClass() == getClass() && other.getOrigin().equals(getOrigin());
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
		InParameterBinding(BindingIdentifier identifier, ParameterOrigin origin) {
			super(identifier, origin);
		}

		@Override
		public Object prepare(@Nullable Object value) {

			if (!ObjectUtils.isArray(value)) {
				return value;
			}

			int length = Array.getLength(value);
			Collection<Object> result = new ArrayList<>(length);

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
		 * Creates a new {@link LikeParameterBinding} for the parameter with the given name and {@link Type} and parameter
		 * binding input.
		 *
		 * @param identifier must not be {@literal null} or empty.
		 * @param type must not be {@literal null}.
		 */
		LikeParameterBinding(BindingIdentifier identifier, ParameterOrigin origin, Type type) {

			super(identifier, origin);

			Assert.notNull(type, "Type must not be null");

			Assert.isTrue(SUPPORTED_TYPES.contains(type),
					String.format("Type must be one of %s", StringUtils.collectionToCommaDelimitedString(SUPPORTED_TYPES)));

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
		 * Extracts the raw value properly.
		 */
		@Nullable
		@Override
		public Object prepare(@Nullable Object value) {

			Object unwrapped = PersistenceProvider.unwrapTypedParameterValue(value);
			if (unwrapped == null) {
				return null;
			}

			return switch (type) {
				case STARTING_WITH -> String.format("%s%%", unwrapped);
				case ENDING_WITH -> String.format("%%%s", unwrapped);
				case CONTAINING -> String.format("%%%s%%", unwrapped);
				default -> unwrapped;
			};
		}

		@Override
		public boolean equals(Object obj) {

			if (!(obj instanceof LikeParameterBinding that)) {
				return false;
			}

			return super.equals(obj) && this.type.equals(that.type);
		}

		@Override
		public int hashCode() {

			int result = super.hashCode();

			result += nullSafeHashCode(this.type);

			return result;
		}

		@Override
		public String toString() {
			return String.format("LikeBinding [identifier: %s, origin: %s, type: %s]", getIdentifier(), getOrigin(),
					getType());
		}

		@Override
		public boolean isCompatibleWith(ParameterBinding binding) {

			if (super.isCompatibleWith(binding) && binding instanceof LikeParameterBinding other) {
				return getType() == other.getType();
			}

			return false;
		}

		/**
		 * Extracts the like {@link Type} from the given JPA like expression.
		 *
		 * @param expression must not be {@literal null} or empty.
		 */
		static Type getLikeTypeFrom(String expression) {

			Assert.hasText(expression, "Expression must not be null or empty");

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

	/**
	 * Identifies a binding parameter by name, position or both. Used to bind parameters to a query or to describe a
	 * {@link MethodInvocationArgument} origin.
	 *
	 * @author Mark Paluch
	 * @since 3.1.2
	 */
	sealed interface BindingIdentifier permits Named,Indexed,NamedAndIndexed {

		/**
		 * Creates an identifier for the given {@code name}.
		 *
		 * @param name
		 * @return
		 */
		static BindingIdentifier of(String name) {

			Assert.hasText(name, "Name must not be empty");

			return new Named(name);
		}

		/**
		 * Creates an identifier for the given {@code position}.
		 *
		 * @param position 1-based index.
		 * @return
		 */
		static BindingIdentifier of(int position) {

			Assert.isTrue(position > 0, "Index position must be greater zero");

			return new Indexed(position);
		}

		/**
		 * Creates an identifier for the given {@code name} and {@code position}.
		 *
		 * @param name
		 * @return
		 */
		static BindingIdentifier of(String name, int position) {

			Assert.hasText(name, "Name must not be empty");

			return new NamedAndIndexed(name, position);
		}

		/**
		 * @return {@code true} if the binding is associated with a name.
		 */
		default boolean hasName() {
			return false;
		}

		/**
		 * @return {@code true} if the binding is associated with a position index.
		 */
		default boolean hasPosition() {
			return false;
		}

		/**
		 * Returns the binding name {@link #hasName() if present} or throw {@link IllegalStateException} if no name
		 * associated.
		 *
		 * @return the binding name.
		 */
		default String getName() {
			throw new IllegalStateException("No name associated");
		}

		/**
		 * Returns the binding name {@link #hasPosition() if present} or throw {@link IllegalStateException} if no position
		 * associated.
		 *
		 * @return the binding position.
		 */
		default int getPosition() {
			throw new IllegalStateException("No position associated");
		}
	}

	private record Named(String name) implements BindingIdentifier {

		@Override
		public boolean hasName() {
			return true;
		}

		@Override
		public String getName() {
			return name();
		}

		@Override
		public String toString() {
			return name();
		}
	}

	private record Indexed(int position) implements BindingIdentifier {

		@Override
		public boolean hasPosition() {
			return true;
		}

		@Override
		public int getPosition() {
			return position();
		}

		@Override
		public String toString() {
			return "[" + position() + "]";
		}
	}

	private record NamedAndIndexed(String name, int position) implements BindingIdentifier {

		@Override
		public boolean hasName() {
			return true;
		}

		@Override
		public String getName() {
			return name();
		}

		@Override
		public boolean hasPosition() {
			return true;
		}

		@Override
		public int getPosition() {
			return position();
		}

		@Override
		public String toString() {
			return "[" + name() + ", " + position() + "]";
		}
	}

	/**
	 * Value type hierarchy to describe where a binding parameter comes from, either method call or an expression.
	 *
	 * @author Mark Paluch
	 * @since 3.1.2
	 */
	sealed interface ParameterOrigin permits Expression,MethodInvocationArgument {

		/**
		 * Creates a {@link Expression} for the given {@code expression}.
		 *
		 * @param expression must not be {@literal null}.
		 * @return {@link Expression} for the given {@code expression}.
		 */
		static Expression ofExpression(ValueExpression expression) {
			return new Expression(expression);
		}

		/**
		 * Creates a {@link MethodInvocationArgument} object for {@code name} and {@code position}. Either the name or the
		 * position must be given.
		 *
		 * @param name the parameter name from the method invocation, can be {@literal null}.
		 * @param position the parameter position (1-based) from the method invocation, can be {@literal null}.
		 * @return {@link MethodInvocationArgument} object for {@code name} and {@code position}.
		 */
		static MethodInvocationArgument ofParameter(@Nullable String name, @Nullable Integer position) {

			BindingIdentifier identifier;
			if (!ObjectUtils.isEmpty(name) && position != null) {
				identifier = BindingIdentifier.of(name, position);
			} else if (!ObjectUtils.isEmpty(name)) {
				identifier = BindingIdentifier.of(name);
			} else {
				identifier = BindingIdentifier.of(position);
			}

			return ofParameter(identifier);
		}

		/**
		 * Creates a {@link MethodInvocationArgument} object for {@code position}.
		 *
		 * @param position the parameter position (1-based) from the method invocation.
		 * @return {@link MethodInvocationArgument} object for {@code position}.
		 */
		static MethodInvocationArgument ofParameter(int position) {
			return ofParameter(BindingIdentifier.of(position));
		}

		/**
		 * Creates a {@link MethodInvocationArgument} using {@link BindingIdentifier}.
		 *
		 * @param identifier must not be {@literal null}.
		 * @return {@link MethodInvocationArgument} for {@link BindingIdentifier}.
		 */
		static MethodInvocationArgument ofParameter(BindingIdentifier identifier) {
			return new MethodInvocationArgument(identifier);
		}

		/**
		 * @return {@code true} if the origin is a method argument reference.
		 */
		boolean isMethodArgument();

		/**
		 * @return {@code true} if the origin is an expression.
		 */
		boolean isExpression();
	}

	/**
	 * Value object capturing the expression of which a binding parameter originates.
	 *
	 * @param expression
	 * @author Mark Paluch
	 * @since 3.1.2
	 */
	public record Expression(ValueExpression expression) implements ParameterOrigin {

		@Override
		public boolean isMethodArgument() {
			return false;
		}

		@Override
		public boolean isExpression() {
			return true;
		}
	}

	/**
	 * Value object capturing the method invocation parameter reference.
	 *
	 * @param identifier
	 * @author Mark Paluch
	 * @since 3.1.2
	 */
	public record MethodInvocationArgument(BindingIdentifier identifier) implements ParameterOrigin {

		@Override
		public boolean isMethodArgument() {
			return true;
		}

		@Override
		public boolean isExpression() {
			return false;
		}
	}
}
