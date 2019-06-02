/*
 * Copyright 2011-2019 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.ParameterExpression;

import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.expression.Expression;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Helper class to allow easy creation of {@link ParameterMetadata}s.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Jens Schauder
 */
class ParameterMetadataProvider {

	private final CriteriaBuilder builder;
	private final Iterator<? extends Parameter> parameters;
	private final List<ParameterMetadata<?>> expressions;
	private final @Nullable Iterator<Object> bindableParameterValues;
	private final PersistenceProvider persistenceProvider;
	private final EscapeCharacter escape;

	/**
	 * Creates a new {@link ParameterMetadataProvider} from the given {@link CriteriaBuilder} and
	 * {@link ParametersParameterAccessor} with support for parameter value customizations via {@link PersistenceProvider}
	 * .
	 *  @param builder must not be {@literal null}.
	 * @param accessor must not be {@literal null}.
	 * @param provider must not be {@literal null}.
	 * @param escape
	 */
	public ParameterMetadataProvider(CriteriaBuilder builder, ParametersParameterAccessor accessor,
									 PersistenceProvider provider, EscapeCharacter escape) {
		this(builder, accessor.iterator(), accessor.getParameters(), provider, escape);
	}

	/**
	 * Creates a new {@link ParameterMetadataProvider} from the given {@link CriteriaBuilder} and {@link Parameters} with
	 * support for parameter value customizations via {@link PersistenceProvider}.
	 *  @param builder must not be {@literal null}.
	 * @param parameters must not be {@literal null}.
	 * @param provider must not be {@literal null}.
	 * @param escape
	 */
	public ParameterMetadataProvider(CriteriaBuilder builder, Parameters<?, ?> parameters, PersistenceProvider provider, EscapeCharacter escape) {
		this(builder, null, parameters, provider, escape);
	}

	/**
	 * Creates a new {@link ParameterMetadataProvider} from the given {@link CriteriaBuilder} an {@link Iterable} of all
	 * bindable parameter values, and {@link Parameters} with support for parameter value customizations via
	 * {@link PersistenceProvider}.
	 *
	 * @param builder must not be {@literal null}.
	 * @param bindableParameterValues may be {@literal null}.
	 * @param parameters must not be {@literal null}.
	 * @param provider must not be {@literal null}.
	 * @param escape
	 */
	private ParameterMetadataProvider(CriteriaBuilder builder, @Nullable Iterator<Object> bindableParameterValues,
									  Parameters<?, ?> parameters, PersistenceProvider provider, EscapeCharacter escape) {

		Assert.notNull(builder, "CriteriaBuilder must not be null!");
		Assert.notNull(parameters, "Parameters must not be null!");
		Assert.notNull(provider, "PesistenceProvider must not be null!");

		this.builder = builder;
		this.parameters = parameters.getBindableParameters().iterator();
		this.expressions = new ArrayList<>();
		this.bindableParameterValues = bindableParameterValues;
		this.persistenceProvider = provider;
		this.escape = escape;
	}

	/**
	 * Returns all {@link ParameterMetadata}s built.
	 *
	 * @return the expressions
	 */
	public List<ParameterMetadata<?>> getExpressions() {
		return Collections.unmodifiableList(expressions);
	}

	/**
	 * Builds a new {@link ParameterMetadata} for given {@link Part} and the next {@link Parameter}.
	 */
	@SuppressWarnings("unchecked")
	public <T> ParameterMetadata<T> next(Part part) {

		Assert.isTrue(parameters.hasNext(), () -> String.format("No parameter available for part %s.", part));

		Parameter parameter = parameters.next();
		return (ParameterMetadata<T>) next(part, parameter.getType(), parameter);
	}

	/**
	 * Builds a new {@link ParameterMetadata} of the given {@link Part} and type. Forwards the underlying
	 * {@link Parameters} as well.
	 *
	 * @param <T> is the type parameter of the returend {@link ParameterMetadata}.
	 * @param type must not be {@literal null}.
	 * @return ParameterMetadata for the next parameter.
	 */
	@SuppressWarnings("unchecked")
	public <T> ParameterMetadata<? extends T> next(Part part, Class<T> type) {

		Parameter parameter = parameters.next();
		Class<?> typeToUse = ClassUtils.isAssignable(type, parameter.getType()) ? parameter.getType() : type;
		return (ParameterMetadata<? extends T>) next(part, typeToUse, parameter);
	}

	/**
	 * Builds a new {@link ParameterMetadata} for the given type and name.
	 *
	 * @param <T> type parameter for the returned {@link ParameterMetadata}.
	 * @param part must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 * @param parameter providing the name for the returned {@link ParameterMetadata}.
	 * @return a new {@link ParameterMetadata} for the given type and name.
	 */
	private <T> ParameterMetadata<T> next(Part part, Class<T> type, Parameter parameter) {

		Assert.notNull(type, "Type must not be null!");

		/*
		 * We treat Expression types as Object vales since the real value to be bound as a parameter is determined at query time.
		 */
		@SuppressWarnings("unchecked")
		Class<T> reifiedType = Expression.class.equals(type) ? (Class<T>) Object.class : type;

		Supplier<String> name = () -> parameter.getName()
				.orElseThrow(() -> new IllegalArgumentException("o_O Parameter needs to be named"));

		ParameterExpression<T> expression = parameter.isExplicitlyNamed() //
				? builder.parameter(reifiedType, name.get()) //
				: builder.parameter(reifiedType);

		Object value = bindableParameterValues == null ? ParameterMetadata.PLACEHOLDER : bindableParameterValues.next();

		ParameterMetadata<T> metadata = new ParameterMetadata<>(expression, part.getType(), value, persistenceProvider, escape);
		expressions.add(metadata);

		return metadata;
	}

	EscapeCharacter getEscape() {
		return escape;
	}

	/**
	 * @author Oliver Gierke
	 * @author Thomas Darimont
	 * @param <T>
	 */
	static class ParameterMetadata<T> {

		static final Object PLACEHOLDER = new Object();

		private final Type type;
		private final ParameterExpression<T> expression;
		private final PersistenceProvider persistenceProvider;
		private final EscapeCharacter escape;

		/**
		 * Creates a new {@link ParameterMetadata}.
		 */
		public ParameterMetadata(ParameterExpression<T> expression, Type type, @Nullable Object value,
								 PersistenceProvider provider, EscapeCharacter escape) {

			this.expression = expression;
			this.persistenceProvider = provider;
			this.type = value == null && Type.SIMPLE_PROPERTY.equals(type) ? Type.IS_NULL : type;
			this.escape = escape;
		}

		/**
		 * Returns the {@link ParameterExpression}.
		 *
		 * @return the expression
		 */
		public ParameterExpression<T> getExpression() {
			return expression;
		}

		/**
		 * Returns whether the parameter shall be considered an {@literal IS NULL} parameter.
		 */
		public boolean isIsNullParameter() {
			return Type.IS_NULL.equals(type);
		}

		/**
		 * Prepares the object before it's actually bound to the {@link javax.persistence.Query;}.
		 *
		 * @param value must not be {@literal null}.
		 */
		@Nullable
		public Object prepare(Object value) {

			Assert.notNull(value, "Value must not be null!");

			Class<? extends T> expressionType = expression.getJavaType();

			if (String.class.equals(expressionType)) {

				switch (type) {
					case STARTING_WITH:
						return String.format("%s%%", escape.escape(value.toString()));
					case ENDING_WITH:
						return String.format("%%%s", escape.escape(value.toString()));
					case CONTAINING:
					case NOT_CONTAINING:
						return String.format("%%%s%%", escape.escape(value.toString()));
					default:
						return value;
				}
			}

			return Collection.class.isAssignableFrom(expressionType) //
					? persistenceProvider.potentiallyConvertEmptyCollection(toCollection(value)) //
					: value;
		}

		/**
		 * Returns the given argument as {@link Collection} which means it will return it as is if it's a
		 * {@link Collections}, turn an array into an {@link ArrayList} or simply wrap any other value into a single element
		 * {@link Collections}.
		 *
		 * @param value the value to be converted to a {@link Collection}.
		 * @return the object itself as a {@link Collection} or a {@link Collection} constructed from the value.
		 */
		@Nullable
		private static Collection<?> toCollection(@Nullable Object value) {

			if (value == null) {
				return null;
			}

			if (value instanceof Collection) {
				return (Collection<?>) value;
			}

			if (ObjectUtils.isArray(value)) {
				return Arrays.asList(ObjectUtils.toObjectArray(value));
			}

			return Collections.singleton(value);
		}
	}
}
