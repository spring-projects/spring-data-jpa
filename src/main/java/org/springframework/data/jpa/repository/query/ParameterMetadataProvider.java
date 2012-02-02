package org.springframework.data.jpa.repository.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.ParameterExpression;

import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Helper class to allow easy creation of {@link ParameterMetadata}s.
 * 
 * @author Oliver Gierke
 */
class ParameterMetadataProvider {

	private final CriteriaBuilder builder;
	private final Iterator<Parameter> parameters;
	private final List<ParameterMetadata<?>> expressions;
	private Iterator<Object> accessor;

	/**
	 * Creates a new {@link ParameterMetadataProvider} from the given {@link CriteriaBuilder} and
	 * {@link ParametersParameterAccessor}.
	 * 
	 * @param builder must not be {@literal null}.
	 * @param parameters must not be {@literal null}.
	 */
	public ParameterMetadataProvider(CriteriaBuilder builder, ParametersParameterAccessor accessor) {

		this(builder, accessor.getParameters());
		Assert.notNull(accessor);
		this.accessor = accessor.iterator();
	}

	public ParameterMetadataProvider(CriteriaBuilder builder, Parameters parameters) {

		Assert.notNull(builder);

		this.builder = builder;
		this.parameters = parameters.getBindableParameters().iterator();
		this.expressions = new ArrayList<ParameterMetadata<?>>();
		this.accessor = null;
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
	 * 
	 * @param <T>
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> ParameterMetadata<T> next(Part part) {

		Parameter parameter = parameters.next();
		return (ParameterMetadata<T>) next(part, parameter.getType(), parameter.getName());
	}

	/**
	 * Builds a new {@link ParameterMetadata} of the given {@link Part} and type. Forwards the underlying
	 * {@link Parameters} as well.
	 * 
	 * @param <T>
	 * @param type must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> ParameterMetadata<? extends T> next(Part part, Class<T> type) {

		Parameter parameter = parameters.next();
		Class<?> typeToUse = ClassUtils.isAssignable(type, parameter.getType()) ? parameter.getType() : type;
		return (ParameterMetadata<? extends T>) next(part, typeToUse, null);
	}

	/**
	 * Builds a new {@link ParameterMetadata} for the given type and name.
	 * 
	 * @param <T>
	 * @param part must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 * @param name
	 * @return
	 */
	private <T> ParameterMetadata<T> next(Part part, Class<T> type, String name) {

		Assert.notNull(type);

		ParameterExpression<T> expression = name == null ? builder.parameter(type) : builder.parameter(type, name);
		ParameterMetadata<T> value = new ParameterMetadata<T>(expression, part.getType(),
				accessor == null ? ParameterMetadata.PLACEHOLDER : accessor.next());
		expressions.add(value);

		return value;
	}

	static class ParameterMetadata<T> {

		static final Object PLACEHOLDER = new Object();

		private final ParameterExpression<T> expression;
		private final Type type;

		public ParameterMetadata(ParameterExpression<T> expression, Type type, Object value) {

			this.expression = expression;
			this.type = value == null && Type.SIMPLE_PROPERTY.equals(type) ? Type.IS_NULL : type;
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
		 * 
		 * @return
		 */
		public boolean isIsNullParameter() {
			return Type.IS_NULL.equals(type);
		}
	}
}