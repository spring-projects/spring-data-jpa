/*
 * Copyright 2017-2025 the original author or authors.
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

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.data.domain.Range;
import org.springframework.data.domain.Score;
import org.springframework.data.domain.ScoringFunction;
import org.springframework.data.domain.Similarity;
import org.springframework.data.jpa.repository.query.JpaParameters.JpaParameter;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersParameterAccessor;

/**
 * {@link org.springframework.data.repository.query.ParameterAccessor} based on an {@link Parameters} instance. It also
 * offers access to all the values, not just the bindable ones based on a {@link JpaParameter} instance.
 *
 * @author Jens Schauder
 * @author Mark Paluch
 * @author Greg Turnquist
 */
public class JpaParametersParameterAccessor extends ParametersParameterAccessor {

	private final JpaParameters parameters;

	/**
	 * Creates a new {@link ParametersParameterAccessor}.
	 *
	 * @param parameters must not be {@literal null}.
	 * @param values must not be {@literal null}.
	 */
	public JpaParametersParameterAccessor(JpaParameters parameters, Object[] values) {
		super(parameters, values);
		this.parameters = parameters;
	}

	public JpaParameters getParameters() {
		return parameters;
	}

	public <T> @Nullable T getValue(Parameter parameter) {
		return super.getValue(parameter.getIndex());
	}

	@Override
	public Object[] getValues() {
		return super.getValues();
	}

	/**
	 * Apply potential unwrapping to {@code parameterValue}.
	 *
	 * @param parameterValue
	 * @since 3.0.4
	 */
	protected Object potentiallyUnwrap(Object parameterValue) {
		return parameterValue;
	}

	/**
	 * Returns the {@link ScoringFunction}.
	 *
	 * @return
	 */
	public ScoringFunction getScoringFunction() {
		return doWithScore(Score::getFunction, Score.class::isInstance, ScoringFunction::unspecified);
	}

	/**
	 * Returns whether to normalize similarities (i.e. translate the database-specific score into {@link Similarity}).
	 *
	 * @return
	 */
	public boolean normalizeSimilarity() {
		return doWithScore(it -> true, Similarity.class::isInstance, () -> false);
	}

	/**
	 * Returns the {@link ScoringFunction}.
	 *
	 * @return
	 */
	public <T> T doWithScore(Function<Score, T> function, Predicate<Score> scoreFilter, Supplier<T> defaultValue) {

		Score score = getScore();
		if (score != null && scoreFilter.test(score)) {
			return function.apply(score);
		}

		JpaParameters parameters = getParameters();
		if (parameters.hasScoreRangeParameter()) {

			Range<Score> range = getScoreRange();

			if (range != null && range.getLowerBound().isBounded()
					&& scoreFilter.test(range.getLowerBound().getValue().get())) {
				return function.apply(range.getUpperBound().getValue().get());
			}

			if (range != null && range.getUpperBound().isBounded()
					&& scoreFilter.test(range.getUpperBound().getValue().get())) {
				return function.apply(range.getUpperBound().getValue().get());
			}

		}

		return defaultValue.get();
	}

}
