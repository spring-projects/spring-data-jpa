package org.springframework.data.jpa.repository.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

class ParameterMetadataContextProvider {

	private final Iterator<? extends Parameter> parameters;
	private final List<ParameterMetadata<?>> expressions;
	private final @Nullable Iterator<Object> bindableParameterValues;
	private final EscapeCharacter escape;

	private final AtomicInteger positionalCounter = new AtomicInteger(1);

	public ParameterMetadataContextProvider(JpaParametersParameterAccessor accessor, EscapeCharacter escape) {
		this(accessor.iterator(), accessor.getParameters(), escape);

	}

	public ParameterMetadataContextProvider(JpaParameters parameters, EscapeCharacter escape) {
		this(null, parameters, escape);
	}

	private ParameterMetadataContextProvider(@Nullable Iterator<Object> bindableParameterValues,
			Parameters<?, ?> parameters, EscapeCharacter escape) {

		Assert.notNull(parameters, "Parameters must not be null");
		Assert.notNull(escape, "EscapeCharacter must not be null");

		this.parameters = parameters.getBindableParameters().iterator();
		this.expressions = new ArrayList<>();
		this.bindableParameterValues = bindableParameterValues;
		this.escape = escape;
	}

	public List<ParameterMetadata<?>> getExpressions() {
		return expressions;
	}

	public <T> ParameterMetadata<T> next(Part part) {

		Assert.isTrue(parameters.hasNext(), () -> String.format("No parameter available for part %s", part));

		Parameter parameter = parameters.next();
		return (ParameterMetadata<T>) next(part, parameter.getType(), parameter);
	}

	public <T> ParameterMetadata<? extends T> next(Part part, Class<T> type) {

		Parameter parameter = parameters.next();
		Class<?> typeToUse = ClassUtils.isAssignable(type, parameter.getType()) ? parameter.getType() : type;
		return (ParameterMetadata<? extends T>) next(part, typeToUse, parameter);
	}

	public <T> ParameterMetadata<T> next(Part part, Class<T> type, Parameter parameter) {

		Assert.notNull(type, "Type must not be null");

		Supplier<String> name = () -> parameter.getName()
				.orElseThrow(() -> new IllegalArgumentException("o_O Parameter needs to be named"));

		ParameterImpl<T> expression;

		if (parameter.isExplicitlyNamed()) {

			String paramName = name.get();
			expression = new ParameterImpl(type, paramName);
		} else {

			String paramName = parameter.getPlaceholder().substring(1);
			expression = new ParameterImpl(type, paramName);
		}

		// ParameterImpl<T> expression = parameter.isExplicitlyNamed() //
		// ? new ParameterImpl(type, name.get()) //
		// : new ParameterImpl(type, parameter.getPlaceholder().substring(1));

		Object value = bindableParameterValues == null //
				? ParameterMetadata.PLACEHOLDER //
				: bindableParameterValues.next();

		ParameterMetadata<T> metadata = new ParameterMetadata<>(expression, parameter, part, value, escape);
		expressions.add(metadata);

		return metadata;

	}

	public EscapeCharacter getEscape() {
		return escape;
	}

	record ParameterImpl<T>(Class<T> type, String name) implements jakarta.persistence.Parameter<T> {

		public String getValue() {
			return ":" + name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public Integer getPosition() {
			return null;
		}

		@Override
		public Class<T> getParameterType() {
			return type;
		}
	}

	static class ParameterMetadata<T> {
		static final Object PLACEHOLDER = new Object();

		private final ParameterImpl<T> expression;
		private final Type type;
		private final Parameter parameter;
		private final Object value;
		private final EscapeCharacter escape;
		private final boolean ignoreCase;
		private final boolean noWildcards;

		public ParameterMetadata(ParameterImpl<T> expression, Parameter parameter, Part part, @Nullable Object value,
				EscapeCharacter escape) {

			this.expression = expression;
			this.parameter = parameter;
			this.value = value;
			this.type = value == null && Type.SIMPLE_PROPERTY.equals(part.getType()) ? Type.IS_NULL : part.getType();
			this.ignoreCase = Part.IgnoreCaseType.ALWAYS.equals(part.shouldIgnoreCase());
			this.noWildcards = part.getProperty().getLeafProperty().isCollection();
			this.escape = escape;
		}

		public ParameterImpl<T> getExpression() {
			return expression;
		}

		public String getValue() {
			return expression.getValue();
		}

		public boolean isIsNullParameter() {
			return Type.IS_NULL.equals(type);
		}

		@Nullable
		public Object prepare(@Nullable Object value) {

			if (value == null) {
				return value;
			}

			if (String.class.equals(parameter.getType()) && !noWildcards) {

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

			return parameter.getType().isArray() || Collection.class.isAssignableFrom(parameter.getType()) //
					? upperIfIgnoreCase(ignoreCase, toCollection(value)) //
					: value;
		}

		@Nullable
		private Object upperIfIgnoreCase(boolean ignoreCase, @Nullable Collection<?> collection) {

			if (!ignoreCase || CollectionUtils.isEmpty(collection)) {
				return collection;
			}

			return ((Collection<String>) collection).stream() //
					.map(it -> it == null //
							? null //
							: it.toUpperCase()) //
					.collect(Collectors.toList());
		}

		@Nullable
		private Collection<?> toCollection(@Nullable Object value) {

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

	}
}
