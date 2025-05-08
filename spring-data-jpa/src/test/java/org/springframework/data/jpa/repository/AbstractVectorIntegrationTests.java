/*
 * Copyright 2015-2025 the original author or authors.
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
package org.springframework.data.jpa.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Range;
import org.springframework.data.domain.Score;
import org.springframework.data.domain.ScoringFunction;
import org.springframework.data.domain.SearchResult;
import org.springframework.data.domain.SearchResults;
import org.springframework.data.domain.Similarity;
import org.springframework.data.domain.Vector;
import org.springframework.data.domain.VectorScoringFunctions;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testcase to verify Vector Search work with Hibernate.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 */
@Transactional
@Rollback(value = false)
abstract class AbstractVectorIntegrationTests {

	Vector VECTOR = Vector.of(0.2001f, 0.32345f, 0.43456f, 0.54567f, 0.65678f);

	@Autowired VectorSearchRepository repository;

	@BeforeEach
	void setUp() {

		WithVector w1 = new WithVector("de", "one", "d1", new float[] { 0.1001f, 0.22345f, 0.33456f, 0.44567f, 0.55678f });
		WithVector w2 = new WithVector("de", "two", "d2", new float[] { 0.2001f, 0.32345f, 0.43456f, 0.54567f, 0.65678f });
		WithVector w3 = new WithVector("en", "three", "d3",
				new float[] { 0.9001f, 0.82345f, 0.73456f, 0.64567f, 0.55678f });
		WithVector w4 = new WithVector("de", "four", "d4", new float[] { 0.9001f, 0.92345f, 0.93456f, 0.94567f, 0.95678f });

		repository.deleteAllInBatch();
		repository.saveAllAndFlush(Arrays.asList(w1, w2, w3, w4));
	}

	@ParameterizedTest
	@MethodSource("scoringFunctions")
	void shouldApplyVectorSearchWithDistance(VectorScoringFunctions functions) {

		SearchResults<WithVector> results = repository.searchTop5ByCountryAndEmbeddingWithin("de", VECTOR,
				Similarity.of(0, functions));

		assertThat(results).hasSize(3).extracting(SearchResult::getContent).extracting(WithVector::getCountry)
				.containsOnly("de", "de");

		assertThat(results).extracting(SearchResult::getContent).extracting(WithVector::getDescription)
				.containsExactlyInAnyOrder("two", "one", "four");
	}

	static Set<VectorScoringFunctions> scoringFunctions() {
		return EnumSet.of(VectorScoringFunctions.COSINE, VectorScoringFunctions.DOT_PRODUCT,
				VectorScoringFunctions.EUCLIDEAN);
	}

	@Test // GH-3868
	void shouldNormalizeEuclideanSimilarity() {

		SearchResults<WithVector> results = repository.searchTop5ByCountryAndEmbeddingWithin("de", VECTOR,
				Similarity.of(0.99, VectorScoringFunctions.EUCLIDEAN));

		assertThat(results).hasSize(1);

		SearchResult<WithVector> two = results.getContent().get(0);

		assertThat(two.getContent().getDescription()).isEqualTo("two");
		assertThat(two.getScore()).isInstanceOf(Similarity.class);
		assertThat(two.getScore().getValue()).isGreaterThan(0.99);
	}

	@Test // GH-3868
	void orderTargetsProperty() {

		SearchResults<WithVector> results = repository.searchTop5ByCountryAndEmbeddingWithinOrderByDistance("de", VECTOR,
				Similarity.of(0, VectorScoringFunctions.EUCLIDEAN));

		assertThat(results.getContent()).extracting(it -> it.getContent().getDistance()).containsExactly("d1", "d2", "d4");
	}

	@Test// GH-3868
	void shouldNormalizeCosineSimilarity() {

		SearchResults<WithVector> results = repository.searchTop5ByCountryAndEmbeddingWithin("de", VECTOR,
				Similarity.of(0.999, VectorScoringFunctions.COSINE));

		assertThat(results).hasSize(1);

		SearchResult<WithVector> two = results.getContent().get(0);

		assertThat(two.getContent().getDescription()).isEqualTo("two");
		assertThat(two.getScore()).isInstanceOf(Similarity.class);
		assertThat(two.getScore().getValue()).isGreaterThan(0.99);
	}

	@Test // GH-3868
	void shouldRunStringQuery() {

		List<WithVector> results = repository.findAnnotatedByCountryAndEmbeddingWithin("de", VECTOR,
				Score.of(2, VectorScoringFunctions.COSINE));

		assertThat(results).hasSize(3).extracting(WithVector::getCountry).containsOnly("de", "de", "de");
		assertThat(results).extracting(WithVector::getDescription).containsSequence("two", "one", "four");
	}

	@Test // GH-3868
	void shouldRunStringQueryWithDistance() {

		SearchResults<WithVector> results = repository.searchAnnotatedByCountryAndEmbeddingWithin("de", VECTOR,
				Score.of(2, VectorScoringFunctions.COSINE));

		assertThat(results).hasSize(3).extracting(SearchResult::getContent).extracting(WithVector::getCountry)
				.containsOnly("de", "de", "de");
		assertThat(results).extracting(SearchResult::getContent).extracting(WithVector::getDescription)
				.containsSequence("two", "one", "four");

		SearchResult<WithVector> result = results.getContent().get(0);
		assertThat(result.getScore().getValue()).isGreaterThanOrEqualTo(0);
		assertThat(result.getScore().getFunction()).isEqualTo(VectorScoringFunctions.COSINE);
	}

	@Test // GH-3868
	void shouldRunStringQueryWithFloatDistance() {

		SearchResults<WithVector> results = repository.searchAnnotatedByCountryAndEmbeddingWithin("de", VECTOR, 2);

		assertThat(results).hasSize(3).extracting(SearchResult::getContent).extracting(WithVector::getCountry)
				.containsOnly("de", "de", "de");
		assertThat(results).extracting(SearchResult::getContent).extracting(WithVector::getDescription)
				.containsSequence("two", "one", "four");

		SearchResult<WithVector> result = results.getContent().get(0);
		assertThat(result.getScore().getValue()).isGreaterThanOrEqualTo(0);
		assertThat(result.getScore().getFunction()).isEqualTo(ScoringFunction.unspecified());
	}

	@Test // GH-3868
	void shouldApplyVectorSearchWithRange() {

		SearchResults<WithVector> results = repository.searchAllByCountryAndEmbeddingWithin("de", VECTOR,
				Similarity.between(0, 1, VectorScoringFunctions.COSINE));

		assertThat(results).hasSize(3).extracting(SearchResult::getContent).extracting(WithVector::getCountry)
				.containsOnly("de", "de", "de");
		assertThat(results).extracting(SearchResult::getContent).extracting(WithVector::getDescription)
				.containsSequence("two", "one", "four");
	}

	@Test // GH-3868
	void shouldApplyVectorSearchAndReturnList() {

		List<WithVector> results = repository.findAllByCountryAndEmbeddingWithin("de", VECTOR,
				Score.of(10, VectorScoringFunctions.COSINE));

		assertThat(results).hasSize(3).extracting(WithVector::getCountry).containsOnly("de", "de", "de");
		assertThat(results).extracting(WithVector::getDescription).containsSequence("one", "two", "four");
	}

	@Test // GH-3868
	void shouldProjectVectorSearchAsInterface() {

		SearchResults<WithDescription> results = repository.searchInterfaceProjectionByCountryAndEmbeddingWithin("de",
				VECTOR, Score.of(10, VectorScoringFunctions.COSINE));

		assertThat(results).hasSize(3).extracting(SearchResult::getContent).extracting(WithDescription::getDescription)
				.containsSequence("two", "one", "four");
	}

	@Test // GH-3868
	void shouldProjectVectorSearchAsDto() {

		SearchResults<DescriptionDto> results = repository.searchDtoByCountryAndEmbeddingWithin("de", VECTOR,
				Score.of(10, VectorScoringFunctions.COSINE));

		assertThat(results).hasSize(3).extracting(SearchResult::getContent).extracting(DescriptionDto::getDescription)
				.containsSequence("two", "one", "four");
	}

	@Test // GH-3868
	void shouldProjectVectorSearchDynamically() {

		SearchResults<DescriptionDto> dtos = repository.searchDynamicByCountryAndEmbeddingWithin("de", VECTOR,
				Score.of(10, VectorScoringFunctions.COSINE), DescriptionDto.class);

		assertThat(dtos).hasSize(3).extracting(SearchResult::getContent).extracting(DescriptionDto::getDescription)
				.containsSequence("two", "one", "four");

		SearchResults<WithDescription> proxies = repository.searchDynamicByCountryAndEmbeddingWithin("de", VECTOR,
				Score.of(10, VectorScoringFunctions.COSINE), WithDescription.class);

		assertThat(proxies).hasSize(3).extracting(SearchResult::getContent).extracting(WithDescription::getDescription)
				.containsSequence("two", "one", "four");
	}

	@Entity
	@Table(name = "with_vector")
	public static class WithVector {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY) //
		private Integer id;

		private String country;
		private String description;

		private String distance;

		@Column(name = "the_embedding")
		@JdbcTypeCode(SqlTypes.VECTOR)
		@Array(length = 5) private float[] embedding;

		public WithVector() {}

		public WithVector(String country, String description, String distance, float[] embedding) {
			this.country = country;
			this.description = description;
			this.embedding = embedding;
			this.distance = distance;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getCountry() {
			return country;
		}

		public void setCountry(String country) {
			this.country = country;
		}

		public String getDescription() {
			return description;
		}

		public float[] getEmbedding() {
			return embedding;
		}

		public void setEmbedding(float[] embedding) {
			this.embedding = embedding;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getDistance() {
			return distance;
		}

		public void setDistance(String distance) {
			this.distance = distance;
		}

		@Override
		public String toString() {
			return "WithVector{" + "id=" + id + ", country='" + country + '\'' + ", description='" + description + '\''
					+ ", distance='" + distance + '\'' + ", embedding=" + Arrays.toString(embedding) + '}';
		}
	}

	interface WithDescription {
		String getDescription();
	}

	static class DescriptionDto {

		private final String description;

		public DescriptionDto(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}

	interface VectorSearchRepository extends JpaRepository<WithVector, Integer> {

		List<WithVector> findAllByCountryAndEmbeddingWithin(String country, Vector embedding, Score distance);

		@Query("""
				SELECT w FROM org.springframework.data.jpa.repository.AbstractVectorIntegrationTests$WithVector w
				WHERE w.country = ?1
					AND cosine_distance(w.embedding, :embedding) <= :distance
				ORDER BY cosine_distance(w.embedding, :embedding) asc""")
		List<WithVector> findAnnotatedByCountryAndEmbeddingWithin(String country, Vector embedding, Score distance);

		@Query("""
				SELECT w, cosine_distance(w.embedding, :embedding) as distance FROM org.springframework.data.jpa.repository.AbstractVectorIntegrationTests$WithVector w
				WHERE w.country = ?1
					AND cosine_distance(w.embedding, :embedding) <= :distance
				ORDER BY distance asc""")
		SearchResults<WithVector> searchAnnotatedByCountryAndEmbeddingWithin(String country, Vector embedding,
				Score distance);

		@Query("""
				SELECT w, cosine_distance(w.embedding, :embedding) as distance FROM org.springframework.data.jpa.repository.AbstractVectorIntegrationTests$WithVector w
				WHERE w.country = ?1
					AND cosine_distance(w.embedding, :embedding) <= :distance
				ORDER BY distance asc""")
		SearchResults<WithVector> searchAnnotatedByCountryAndEmbeddingWithin(String country, Vector embedding,
				float distance);

		SearchResults<WithVector> searchAllByCountryAndEmbeddingWithin(String country, Vector embedding,
				Range<Similarity> distance);

		SearchResults<WithVector> searchTop5ByCountryAndEmbeddingWithin(String country, Vector embedding, Score distance);

		SearchResults<WithVector> searchTop5ByCountryAndEmbeddingWithinOrderByDistance(String country, Vector embedding,
				Score distance);

		SearchResults<WithDescription> searchInterfaceProjectionByCountryAndEmbeddingWithin(String country,
				Vector embedding, Score distance);

		SearchResults<DescriptionDto> searchDtoByCountryAndEmbeddingWithin(String country, Vector embedding,
				Score distance);

		<T> SearchResults<T> searchDynamicByCountryAndEmbeddingWithin(String country, Vector embedding, Score distance,
				Class<T> projection);

	}

}
