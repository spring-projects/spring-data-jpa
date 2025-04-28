/*
 * Copyright 2025 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

import org.springframework.data.domain.ScoringFunction;
import org.springframework.data.domain.VectorScoringFunctions;

/**
 * Normalizes the score returned by a database to a similarity value and vice versa.
 *
 * @author Mark Paluch
 * @since 4.0
 * @see org.springframework.data.domain.Similarity
 */
public class SimilarityNormalizer {

	/**
	 * Identity normalizer for {@link ScoringFunction#UNSPECIFIED} scoring function without altering the score.
	 */
	public static final SimilarityNormalizer IDENTITY = new SimilarityNormalizer(ScoringFunction.UNSPECIFIED,
			DoubleUnaryOperator.identity(), DoubleUnaryOperator.identity());

	/**
	 * Normalizer for Euclidean scores using {@code euclidean_distance(…)} as the scoring function.
	 */
	public static final SimilarityNormalizer EUCLIDEAN = new SimilarityNormalizer(VectorScoringFunctions.EUCLIDEAN,
			it -> 1 / (1.0 + Math.pow(it, 2)), it -> it == 0 ? Float.MAX_VALUE : Math.sqrt((1 / it) - 1));

	/**
	 * Normalizer for Cosine scores using {@code cosine_distance(…)} as the scoring function.
	 */
	public static final SimilarityNormalizer COSINE = new SimilarityNormalizer(VectorScoringFunctions.COSINE,
			it -> (1.0 + (1 - it)) / 2.0, it -> 1 - ((it * 2) - 1));

	/**
	 * Normalizer for Negative Inner Product (Dot) scores using {@code negative_inner_product(…)} as the scoring function.
	 */
	public static final SimilarityNormalizer DOT = new SimilarityNormalizer(VectorScoringFunctions.DOT,
			it -> (1 - it) / 2, it -> 1 - (it * 2));

	private static final Map<ScoringFunction, SimilarityNormalizer> NORMALIZERS = new HashMap<>();

	static {
		NORMALIZERS.put(EUCLIDEAN.scoringFunction, EUCLIDEAN);
		NORMALIZERS.put(COSINE.scoringFunction, COSINE);
		NORMALIZERS.put(DOT.scoringFunction, DOT);
		NORMALIZERS.put(VectorScoringFunctions.INNER_PRODUCT, DOT);
	}

	private final ScoringFunction scoringFunction;
	private final DoubleUnaryOperator similarity;
	private final DoubleUnaryOperator score;

	/**
	 * Constructor for {@link SimilarityNormalizer} using the given {@link DoubleUnaryOperator} for similarity and score
	 * computation.
	 *
	 * @param similarity compute the similarity from the underlying score returned by a database result.
	 * @param score compute the score value from a given {@link org.springframework.data.domain.Similarity} to compare
	 *          against database results.
	 */
	SimilarityNormalizer(ScoringFunction scoringFunction, DoubleUnaryOperator similarity, DoubleUnaryOperator score) {
		this.scoringFunction = scoringFunction;
		this.score = score;
		this.similarity = similarity;
	}

	/**
	 * Lookup a {@link SimilarityNormalizer} for a given {@link ScoringFunction}.
	 *
	 * @param scoringFunction the scoring function to translate.
	 * @return the {@link SimilarityNormalizer} for the given {@link ScoringFunction}.
	 * @throws IllegalArgumentException if the {@link ScoringFunction} is not associated with a
	 *           {@link SimilarityNormalizer}.
	 */
	public static SimilarityNormalizer get(ScoringFunction scoringFunction) {

		SimilarityNormalizer normalizer = NORMALIZERS.get(scoringFunction);

		if (normalizer == null) {
			throw new IllegalArgumentException("No SimilarityNormalizer found for " + scoringFunction.getName());
		}

		return normalizer;
	}

	/**
	 * @param score score value as returned by the database.
	 * @return the {@link org.springframework.data.domain.Similarity} value.
	 */
	public double getSimilarity(double score) {
		return similarity.applyAsDouble(score);
	}

	/**
	 * @param similarity similarity value as requested by the query mechanism.
	 * @return database score value.
	 */
	public double getScore(double similarity) {
		return score.applyAsDouble(similarity);
	}

	@Override
	public String toString() {
		return "%s Normalizer: Similarity[0 to 1] -> Score[%f to %f]".formatted(scoringFunction.getName(), getScore(0),
				getScore(1));
	}

}
