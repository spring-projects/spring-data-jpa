/*
 * Copyright 2015-present the original author or authors.
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.data.domain.Range;
import org.springframework.data.domain.Score;
import org.springframework.data.domain.Similarity;
import org.springframework.data.domain.Vector;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.support.JpqlQueryTemplates;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersSource;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Integration tests for {@link ParameterMetadataProvider}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Mark Paluch
 * @soundtrack Elephants Crossing - We are (Irrelephant)
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:hibernate-infrastructure.xml")
class ParameterMetadataProviderIntegrationTests {

	@PersistenceContext EntityManager em;

	@Test // DATAJPA-758
	void usesNamedParametersForExplicitlyNamedParameters() throws Exception {

		ParameterMetadataProvider provider = createProvider(Sample.class.getMethod("findByFirstname", String.class));
		ParameterBinding.PartTreeParameterBinding metadata = provider.next(new Part("firstname", User.class));

		assertThat(metadata.getName()).isEqualTo("name");
		assertThat(metadata.getPosition()).isEqualTo(1);
	}

	@Test // DATAJPA-758
	void usesNamedParameters() throws Exception {

		ParameterMetadataProvider provider = createProvider(Sample.class.getMethod("findByLastname", String.class));
		ParameterBinding.PartTreeParameterBinding metadata = provider.next(new Part("lastname", User.class));

		assertThat(metadata.getName()).isEqualTo("lastname");
		assertThat(metadata.getPosition()).isEqualTo(1);
	}

	@Test // DATAJPA-772
	void doesNotApplyLikeExpansionOnNonStringProperties() throws Exception {

		ParameterMetadataProvider provider = createProvider(Sample.class.getMethod("findByAgeContaining", Integer.class));
		ParameterBinding.PartTreeParameterBinding binding = provider.next(new Part("ageContaining", User.class));

		assertThat(binding.prepare(1)).isEqualTo(1);
	}

	@Test // GH-
	void appliesScoreValuePreparation() throws Exception {

		ParameterMetadataProvider provider = createProvider(
				Sample.class.getMethod("findByVectorWithin", Vector.class, Score.class));
		ParameterBinding.PartTreeParameterBinding vector = provider.next(new Part("VectorWithin", WithVector.class));
		ParameterBinding.PartTreeParameterBinding score = provider.next(new Part("VectorWithin", WithVector.class));
		ParameterMetadataProvider.ScoreParameterBinding binding = provider.normalize(score, SimilarityNormalizer.EUCLIDEAN);

		assertThat(binding.prepare(Score.of(1))).isEqualTo(0.0);
		assertThat(binding.prepare(Score.of(0.5))).isEqualTo(1.0);
		assertThat(provider.getBindings()).hasSize(2).contains(binding).doesNotContain(score);
	}

	@Test // GH-
	void appliesLowerRangeValuePreparation() throws Exception {

		ParameterMetadataProvider provider = createProvider(
				Sample.class.getMethod("findByVectorWithin", Vector.class, Range.class));
		ParameterBinding.PartTreeParameterBinding vector = provider.next(new Part("VectorWithin", WithVector.class));
		ParameterBinding.PartTreeParameterBinding score = provider.next(new Part("VectorWithin", WithVector.class));
		ParameterMetadataProvider.ScoreParameterBinding lower = provider.lower(score, SimilarityNormalizer.EUCLIDEAN);

		Range<Similarity> range = Similarity.between(0.5, 1);

		assertThat(lower.prepare(range)).isEqualTo(1.0);
		assertThat(provider.getBindings()).hasSize(2).contains(lower).doesNotContain(score);
	}

	@Test // GH-
	void appliesRangeValuePreparation() throws Exception {

		ParameterMetadataProvider provider = createProvider(
				Sample.class.getMethod("findByVectorWithin", Vector.class, Range.class));
		ParameterBinding.PartTreeParameterBinding vector = provider.next(new Part("VectorWithin", WithVector.class));
		ParameterBinding.PartTreeParameterBinding score = provider.next(new Part("VectorWithin", WithVector.class));
		ParameterMetadataProvider.ScoreParameterBinding lower = provider.lower(score, SimilarityNormalizer.EUCLIDEAN);
		ParameterMetadataProvider.ScoreParameterBinding upper = provider.upper(score, SimilarityNormalizer.EUCLIDEAN);

		Range<Similarity> range = Similarity.between(0.5, 1);

		assertThat(lower.prepare(range)).isEqualTo(1.0);
		assertThat(upper.prepare(range)).isEqualTo(0.0);
		assertThat(provider.getBindings()).hasSize(3).contains(lower, upper).doesNotContain(score);
	}

	private ParameterMetadataProvider createProvider(Method method) {

		JpaParameters parameters = new JpaParameters(ParametersSource.of(method));
		simulateDiscoveredParametername(parameters);

		return new ParameterMetadataProvider(parameters, EscapeCharacter.DEFAULT,
				JpqlQueryTemplates.UPPER);
	}

	@SuppressWarnings({ "unchecked", "ConstantConditions" })
	private static void simulateDiscoveredParametername(Parameters<?, ?> parameters) {

		List<Object> list = (List<Object>) ReflectionTestUtils.getField(parameters, "parameters");
		Object parameter = ReflectionTestUtils.getField(list.get(0), "parameter");
		ReflectionTestUtils.setField(parameter, "parameterName", "name");
	}

	interface Sample {

		User findByFirstname(@Param("name") String firstname);

		User findByLastname(String lastname);

		User findByAgeContaining(@Param("age") Integer age);

		User findByVectorWithin(Vector vector, Score score);

		User findByVectorWithin(Vector vector, Range<Score> score);
	}

	static class WithVector {
		Vector vector;
	}
}
