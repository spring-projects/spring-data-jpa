/*
 * Copyright 2011-2025 the original author or authors.
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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.data.domain.Range;
import org.springframework.data.domain.Score;
import org.springframework.data.domain.ScoringFunction;
import org.springframework.data.domain.Vector;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.support.JpqlQueryTemplates;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.expression.Expression;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Helper class to allow easy creation of {@link PartTreeParameterBinding}s.
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
public class ParameterMetadataProvider {

	static final Object PLACEHOLDER = new Object();

	private final Iterator<? extends Parameter> parameters;
	private final @Nullable JpaParametersParameterAccessor accessor;
	private final List<ParameterBinding> bindings;
	private final Set<String> syntheticParameterNames = new LinkedHashSet<>();
	private @Nullable ParameterBinding vector;
	private final @Nullable Iterator<Object> bindableParameterValues;
	private final EscapeCharacter escape;
	private final JpqlQueryTemplates templates;
	private final JpaParameters jpaParameters;
	private int position;
	private int bindMarker;

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
		this(accessor.iterator(), accessor, accessor.getParameters(), escape, templates);
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
		this(null, null, parameters, escape, templates);
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
	private ParameterMetadataProvider(@Nullable Iterator<Object> bindableParameterValues,
			@Nullable JpaParametersParameterAccessor accessor, JpaParameters parameters,
			EscapeCharacter escape, JpqlQueryTemplates templates) {
		Assert.notNull(parameters, "Parameters must not be null");
		Assert.notNull(escape, "EscapeCharacter must not be null");
		Assert.notNull(templates, "JpqlQueryTemplates must not be null");

		this.jpaParameters = parameters;
		this.accessor = accessor;
		this.parameters = parameters.getBindableParameters().iterator();
		this.bindings = new ArrayList<>();
		this.bindableParameterValues = bindableParameterValues;
		this.escape = escape;
		this.templates = templates;
	}

	public JpaParameters getParameters() {
		return this.jpaParameters;
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
	 * @return the {@link SimilarityNormalizer}.
	 */
	SimilarityNormalizer getSimilarityNormalizer() {

		if (accessor != null && accessor.normalizeSimilarity()) {
			return SimilarityNormalizer.get(accessor.getScoringFunction());
		}

		return SimilarityNormalizer.IDENTITY;
	}

	/**
	 * Builds a new {@link PartTreeParameterBinding} for given {@link Part} and the next {@link Parameter}.
	 */
	@SuppressWarnings("unchecked")
	PartTreeParameterBinding next(Part part) {

		Assert.isTrue(parameters.hasNext(), () -> String.format("No parameter available for part %s", part));

		Parameter parameter = parameters.next();
		return next(part, parameter.getType(), parameter);
	}

	/**
	 * Builds a new {@link PartTreeParameterBinding} of the given {@link Part} and type. Forwards the underlying
	 * {@link Parameters} as well.
	 *
	 * @param <T> is the type parameter of the returned {@link PartTreeParameterBinding}.
	 * @param type must not be {@literal null}.
	 * @return ParameterMetadata for the next parameter.
	 */
	<T> PartTreeParameterBinding next(Part part, Class<T> type) {

		Parameter parameter = parameters.next();
		Class<?> typeToUse = ClassUtils.isAssignable(type, parameter.getType()) ? parameter.getType() : type;
		return next(part, typeToUse, parameter);
	}

	/**
	 * Builds a new {@link PartTreeParameterBinding} for the given type and name.
	 *
	 * @param <T> type parameter for the returned {@link PartTreeParameterBinding}.
	 * @param part must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 * @param parameter providing the name for the returned {@link PartTreeParameterBinding}.
	 * @return a new {@link PartTreeParameterBinding} for the given type and name.
	 */
	private <T> PartTreeParameterBinding next(Part part, Class<T> type, Parameter parameter) {

		Assert.notNull(type, "Type must not be null");

		/*
		 * We treat Expression types as Object vales since the real value to be bound as a parameter is determined at query time.
		 */
		@SuppressWarnings("unchecked")
		Class<T> reifiedType = Expression.class.equals(type) ? (Class<T>) Object.class : type;

		Object value = bindableParameterValues == null ? PLACEHOLDER : bindableParameterValues.next();
		int currentPosition = ++position;
		int currentBindMarker = ++bindMarker;

		BindingIdentifier bindingIdentifier = parameter.getName().map(it -> BindingIdentifier.of(it, currentPosition))
				.orElseGet(() -> BindingIdentifier.of(currentPosition));

		/* identifier refers to bindable parameters, not _all_ parameters index */
		MethodInvocationArgument methodParameter = ParameterOrigin.ofParameter(BindingIdentifier.of(currentPosition));
		PartTreeParameterBinding binding = new PartTreeParameterBinding(BindingIdentifier.of(currentBindMarker),
				methodParameter, reifiedType,
				part, value, templates, escape);

		// PartTreeParameterBinding is more expressive than a potential ParameterBinding for Vector.
		bindings.add(binding);

		if (Vector.class.isAssignableFrom(parameter.getType())) {
			this.vector = binding;
		}

		return binding;
	}

	ScoringFunction getScoringFunction() {

		if (accessor != null) {
			return accessor.getScoringFunction();
		}

		return ScoringFunction.UNSPECIFIED;
	}

	ParameterBinding getVectorBinding() {

		if (!getParameters().hasVectorParameter()) {
			throw new IllegalStateException("Vector parameter not available");
		}

		if (this.vector != null) {
			return this.vector;
		}

		int vectorIndex = getParameters().getVectorIndex();

		BindingIdentifier bindingIdentifier = BindingIdentifier.of(vectorIndex + 1);

		/* identifier refers to bindable parameters, not _all_ parameters index */
		MethodInvocationArgument methodParameter = ParameterOrigin.ofParameter(bindingIdentifier);
		ParameterBinding parameterBinding = new ParameterBinding(bindingIdentifier, methodParameter);

		this.bindings.add(parameterBinding);

		return parameterBinding;
	}

	EscapeCharacter getEscape() {
		return escape;
	}

	/**
	 * Builds a new synthetic {@link ParameterBinding} for the given value.
	 *
	 * @param nameHint
	 * @param value
	 * @param source
	 * @return a new {@link ParameterBinding} for the given value and source.
	 */
	ParameterBinding nextSynthetic(String nameHint, Object value, Object source) {

		int currentPosition = ++bindMarker;
		String bindingName = nameHint;

		if (!syntheticParameterNames.add(bindingName)) {

			bindingName = bindingName + "_" + currentPosition;
			syntheticParameterNames.add(bindingName);
		}

		return new ParameterBinding(BindingIdentifier.of(bindingName, currentPosition),
				ParameterOrigin.synthetic(value, source));
	}

	RangeParameterBinding lower(PartTreeParameterBinding within, SimilarityNormalizer normalizer) {

		int bindMarker = within.getRequiredPosition();

		if (!bindings.remove(within)) {
			bindMarker = ++this.bindMarker;
		}

		BindingIdentifier identifier = within.getIdentifier();
		RangeParameterBinding rangeBinding = new RangeParameterBinding(
				identifier.mapName(name -> name + "_upper").withPosition(bindMarker), within.getOrigin(), true, normalizer);
		bindings.add(rangeBinding);

		return rangeBinding;
	}

	RangeParameterBinding upper(PartTreeParameterBinding within, SimilarityNormalizer normalizer) {

		int bindMarker = within.getRequiredPosition();

		if (!bindings.remove(within)) {
			bindMarker = ++this.bindMarker;
		}

		BindingIdentifier identifier = within.getIdentifier();
		RangeParameterBinding rangeBinding = new RangeParameterBinding(
				identifier.mapName(name -> name + "_upper").withPosition(bindMarker), within.getOrigin(), false, normalizer);
		bindings.add(rangeBinding);

		return rangeBinding;
	}

	ScoreParameterBinding normalize(PartTreeParameterBinding within, SimilarityNormalizer normalizer) {

		bindings.remove(within);

		ScoreParameterBinding rangeBinding = new ScoreParameterBinding(within.getIdentifier(), within.getOrigin(),
				normalizer);
		bindings.add(rangeBinding);

		return rangeBinding;
	}

	static class ScoreParameterBinding extends ParameterBinding {

		private final SimilarityNormalizer normalizer;

		/**
		 * Creates a new {@link ParameterBinding} for the parameter with the given identifier and origin.
		 *
		 * @param identifier of the parameter, must not be {@literal null}.
		 * @param origin the origin of the parameter (expression or method argument)
		 */
		ScoreParameterBinding(BindingIdentifier identifier, ParameterOrigin origin, SimilarityNormalizer normalizer) {
			super(identifier, origin);
			this.normalizer = normalizer;
		}

		@Override
		public @Nullable Object prepare(@Nullable Object valueToBind) {

			if (valueToBind instanceof Score score) {
				return normalizer.getScore(score.getValue());
			}

			return super.prepare(valueToBind);
		}

		@Override
		public boolean isCompatibleWith(ParameterBinding binding) {

			if (super.isCompatibleWith(binding) && binding instanceof ScoreParameterBinding other) {
				return normalizer == other.normalizer;
			}

			return false;
		}
	}

	static class RangeParameterBinding extends ScoreParameterBinding {

		private final boolean lower;

		/**
		 * Creates a new {@link ParameterBinding} for the parameter with the given identifier and origin.
		 *
		 * @param identifier of the parameter, must not be {@literal null}.
		 * @param origin the origin of the parameter (expression or method argument)
		 */
		RangeParameterBinding(BindingIdentifier identifier, ParameterOrigin origin, boolean lower,
				SimilarityNormalizer normalizer) {
			super(identifier, origin, normalizer);
			this.lower = lower;
		}

		@Override
		public @Nullable Object prepare(@Nullable Object valueToBind) {

			if (valueToBind instanceof Range<?> r) {
				if (lower) {
					return super.prepare(r.getLowerBound().getValue().orElse(null));
				} else {
					return super.prepare(r.getUpperBound().getValue().orElse(null));
				}
			}

			return super.prepare(valueToBind);
		}

		@Override
		public boolean isCompatibleWith(ParameterBinding binding) {

			if (super.isCompatibleWith(binding) && binding instanceof RangeParameterBinding other) {
				return lower == other.lower;
			}

			return false;
		}
	}

}
