/*
 * Copyright 2011-2024 the original author or authors.
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

import static org.springframework.data.jpa.repository.query.ParameterBinding.*;

import jakarta.persistence.criteria.CriteriaBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.support.JpqlQueryTemplates;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.Part.IgnoreCaseType;
import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.expression.Expression;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * Helper class to allow easy creation of {@link ParameterMetadata}s.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Jens Schauder
 * @author Andrey Kovalev
 * @author Yuriy Tsarkov
 * @author Donghun Shin
 * @author Greg Turnquist
 */
class ParameterMetadataProvider {

	private final Iterator<? extends Parameter> parameters;
	private final List<ParameterBinding> bindings;
	private final @Nullable Iterator<Object> bindableParameterValues;
	private final EscapeCharacter escape;
	private final JpqlQueryTemplates templates;
	private final JpaParameters jpaParameters;
	private int position;

	/**
	 * Creates a new {@link ParameterMetadataProvider} from the given {@link CriteriaBuilder} and
	 * {@link ParametersParameterAccessor}.
	 *
	 * @param accessor must not be {@literal null}.
	 * @param escape must not be {@literal null}.
	 * @param templates must not be {@literal null}.
	 */
	public ParameterMetadataProvider(JpaParametersParameterAccessor accessor,
			EscapeCharacter escape, JpqlQueryTemplates templates) {
		this(accessor.iterator(), accessor.getParameters(), escape, templates);
	}

	/**
	 * Creates a new {@link ParameterMetadataProvider} from the given {@link CriteriaBuilder} and {@link Parameters} with
	 * support for parameter value customizations via {@link PersistenceProvider}.
	 *
	 * @param parameters must not be {@literal null}.
	 * @param escape must not be {@literal null}.
	 * @param templates must not be {@literal null}.
	 */
	public ParameterMetadataProvider(JpaParameters parameters, EscapeCharacter escape,
			JpqlQueryTemplates templates) {
		this(null, parameters, escape, templates);
	}

	/**
	 * Creates a new {@link ParameterMetadataProvider} from the given {@link CriteriaBuilder} an {@link Iterable} of all
	 * bindable parameter values, and {@link Parameters}.
	 *
	 * @param bindableParameterValues may be {@literal null}.
	 * @param parameters must not be {@literal null}.
	 * @param escape must not be {@literal null}.
	 * @param templates must not be {@literal null}.
	 */
	private ParameterMetadataProvider(@Nullable Iterator<Object> bindableParameterValues, JpaParameters parameters,
			EscapeCharacter escape, JpqlQueryTemplates templates) {

		Assert.notNull(parameters, "Parameters must not be null");
		Assert.notNull(escape, "EscapeCharacter must not be null");
		Assert.notNull(templates, "JpqlQueryTemplates must not be null");

		this.jpaParameters = parameters;
		this.parameters = parameters.getBindableParameters().iterator();
		this.bindings = new ArrayList<>();
		this.bindableParameterValues = bindableParameterValues;
		this.escape = escape;
		this.templates = templates;
	}

	/**
	 * Returns all {@link ParameterBinding}s built.
	 *
	 * @return the bindings.
	 */
	public List<ParameterBinding> getBindings() {
		return bindings;
	}

	/**
	 * Builds a new {@link PartTreeParameterBinding} for given {@link Part} and the next {@link Parameter}.
	 */
	@SuppressWarnings("unchecked")
	public <T> PartTreeParameterBinding next(Part part) {

		Assert.isTrue(parameters.hasNext(), () -> String.format("No parameter available for part %s", part));

		Parameter parameter = parameters.next();
		return next(part, parameter.getType(), parameter);
	}

	/**
	 * Builds a new {@link PartTreeParameterBinding} of the given {@link Part} and type. Forwards the underlying
	 * {@link Parameters} as well.
	 *
	 * @param <T> is the type parameter of the returned {@link ParameterMetadata}.
	 * @param type must not be {@literal null}.
	 * @return ParameterMetadata for the next parameter.
	 */
	@SuppressWarnings("unchecked")
	public <T> PartTreeParameterBinding next(Part part, Class<T> type) {

		Parameter parameter = parameters.next();
		Class<?> typeToUse = ClassUtils.isAssignable(type, parameter.getType()) ? parameter.getType() : type;
		return next(part, typeToUse, parameter);
	}

	/**
	 * Builds a new {@link PartTreeParameterBinding} for the given type and name.
	 *
	 * @param <T> type parameter for the returned {@link ParameterMetadata}.
	 * @param part must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 * @param parameter providing the name for the returned {@link ParameterMetadata}.
	 * @return a new {@link ParameterMetadata} for the given type and name.
	 */
	private <T> PartTreeParameterBinding next(Part part, Class<T> type, Parameter parameter) {

		Assert.notNull(type, "Type must not be null");

		/*
		 * We treat Expression types as Object vales since the real value to be bound as a parameter is determined at query time.
		 */
		@SuppressWarnings("unchecked")
		Class<T> reifiedType = Expression.class.equals(type) ? (Class<T>) Object.class : type;

		Object value = bindableParameterValues == null ? ParameterMetadata.PLACEHOLDER : bindableParameterValues.next();

		int currentPosition = ++position;

		BindingIdentifier bindingIdentifier = BindingIdentifier.of(currentPosition);

		/* identifier refers to bindable parameters, not _all_ parameters index */
		MethodInvocationArgument methodParameter = ParameterOrigin.ofParameter(bindingIdentifier);
		PartTreeParameterBinding binding = new PartTreeParameterBinding(bindingIdentifier, methodParameter, reifiedType,
				part, value, templates, escape);

		bindings.add(binding);

		return binding;
	}

	EscapeCharacter getEscape() {
		return escape;
	}

	/**
	 * Builds a new synthetic {@link ParameterBinding} for the given value.
	 *
	 * @param value
	 * @param source
	 * @return a new {@link ParameterBinding} for the given value and source.
	 */
	public ParameterBinding nextSynthetic(Object value, Object source) {

		int currentPosition = ++position;

		return new ParameterBinding(BindingIdentifier.of(currentPosition), ParameterOrigin.synthetic(value, source));
	}

	public JpaParameters getParameters() {
		return this.jpaParameters;
	}

	/**
	 * @author Oliver Gierke
	 * @author Thomas Darimont
	 * @author Andrey Kovalev
	 */
	public static class ParameterMetadata {

		static final Object PLACEHOLDER = new Object();

		private final Class<?> parameterType;
		private final Type type;
		private final int position;
		private final JpqlQueryTemplates templates;
		private final EscapeCharacter escape;
		private final boolean ignoreCase;
		private final boolean noWildcards;

		/**
		 * Creates a new {@link ParameterMetadata}.
		 */
		public ParameterMetadata(Class<?> parameterType, Part part, @Nullable Object value, EscapeCharacter escape,
				int position, JpqlQueryTemplates templates) {

			this.parameterType = parameterType;
			this.position = position;
			this.templates = templates;
			this.type = value == null && Type.SIMPLE_PROPERTY.equals(part.getType()) ? Type.IS_NULL : part.getType();
			this.ignoreCase = IgnoreCaseType.ALWAYS.equals(part.shouldIgnoreCase());
			this.noWildcards = part.getProperty().getLeafProperty().isCollection();
			this.escape = escape;
		}

		public int getPosition() {
			return position;
		}

		public Class<?> getParameterType() {
			return parameterType;
		}

		/**
		 * Returns whether the parameter shall be considered an {@literal IS NULL} parameter.
		 */
		public boolean isIsNullParameter() {
			return Type.IS_NULL.equals(type);
		}

		/**
		 * Prepares the object before it's actually bound to the {@link jakarta.persistence.Query;}.
		 *
		 * @param value can be {@literal null}.
		 */
		@Nullable
		public Object prepare(@Nullable Object value) {

			if (value == null || parameterType == null) {
				return value;
			}

			if (String.class.equals(parameterType) && !noWildcards) {

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

			return Collection.class.isAssignableFrom(parameterType) //
					? potentiallyIgnoreCase(ignoreCase, toCollection(value)) //
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

			if (value instanceof Collection<?> collection) {
				return collection.isEmpty() ? null : collection;
			}

			if (ObjectUtils.isArray(value)) {

				List<Object> collection = Arrays.asList(ObjectUtils.toObjectArray(value));
				return collection.isEmpty() ? null : collection;
			}

			return Collections.singleton(value);
		}

		@Nullable
		@SuppressWarnings("unchecked")
		private Collection<?> potentiallyIgnoreCase(boolean ignoreCase, @Nullable Collection<?> collection) {

			if (!ignoreCase || CollectionUtils.isEmpty(collection)) {
				return collection;
			}

			return ((Collection<String>) collection).stream() //
					.map(it -> it == null //
							? null //
							: templates.ignoreCase(it)) //
					.collect(Collectors.toList());
		}

	}
}
