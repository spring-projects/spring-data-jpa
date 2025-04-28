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

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SimilarityNormalizer}.
 *
 * @author Mark Paluch
 */
class SimilarityNormalizerUnitTests {

	@Test
	void normalizesEuclidean() {

		assertThat(SimilarityNormalizer.EUCLIDEAN.getSimilarity(0)).isCloseTo(1.0, offset(0.01));
		assertThat(SimilarityNormalizer.EUCLIDEAN.getSimilarity(0.223606791085977)).isCloseTo(0.9523810148239136,
				offset(0.01));
		assertThat(SimilarityNormalizer.EUCLIDEAN.getSimilarity(1.1618950141221271)).isCloseTo(0.42553189396858215,
				offset(0.01));

		assertThat(SimilarityNormalizer.EUCLIDEAN.getScore(1.0)).isCloseTo(0.0, offset(0.01));
		assertThat(SimilarityNormalizer.EUCLIDEAN.getScore(0.9523810148239136)).isCloseTo(0.223606791085977, offset(0.01));
		assertThat(SimilarityNormalizer.EUCLIDEAN.getScore(0.42553189396858215)).isCloseTo(1.1618950141221271,
				offset(0.01));
	}

	@Test
	void normalizesCosine() {

		assertThat(SimilarityNormalizer.COSINE.getSimilarity(0)).isCloseTo(1.0, offset(0.01));
		assertThat(SimilarityNormalizer.COSINE.getSimilarity(0.004470301418728173)).isCloseTo(0.9977648258209229,
				offset(0.01));
		assertThat(SimilarityNormalizer.COSINE.getSimilarity(0.05568200370295473)).isCloseTo(0.9721590280532837,
				offset(0.01));

		assertThat(SimilarityNormalizer.COSINE.getScore(1.0)).isCloseTo(0.0, offset(0.01));
		assertThat(SimilarityNormalizer.COSINE.getScore(0.9977648258209229)).isCloseTo(0.004470301418728173, offset(0.01));
		assertThat(SimilarityNormalizer.COSINE.getScore(0.9721590280532837)).isCloseTo(0.05568200370295473, offset(0.01));
	}

	@Test
	void normalizesNegativeInnerProduct() {

		assertThat(SimilarityNormalizer.DOT.getSimilarity(-0.8465620279312134)).isCloseTo(0.9232810139656067, offset(0.01));
		assertThat(SimilarityNormalizer.DOT.getSimilarity(-1.0626180171966553)).isCloseTo(1.0313090085983276, offset(0.01));
		assertThat(SimilarityNormalizer.DOT.getSimilarity(-2.0293400287628174)).isCloseTo(1.5146700143814087, offset(0.01));

		assertThat(SimilarityNormalizer.DOT.getScore(0.9232810139656067)).isCloseTo(-0.8465620279312134, offset(0.01));
		assertThat(SimilarityNormalizer.DOT.getScore(1.0313090085983276)).isCloseTo(-1.0626180171966553, offset(0.01));
		assertThat(SimilarityNormalizer.DOT.getScore(1.5146700143814087)).isCloseTo(-2.0293400287628174, offset(0.01));
	}

}
