/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.data.jpa.domain;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.util.SerializationUtils.*;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.io.Serializable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for {@link PredicateSpecification}.
 *
 * @author Mark Paluch
 */
@SuppressWarnings("serial")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PredicateSpecificationUnitTests implements Serializable {

	private PredicateSpecification<Object> spec;
	@Mock(serializable = true) Root<Object> root;
	@Mock(serializable = true) CriteriaBuilder builder;
	@Mock(serializable = true) Predicate predicate;
	@Mock(serializable = true) Predicate another;

	@BeforeEach
	void setUp() {
		spec = (root, cb) -> predicate;
	}

	@Test // GH-3521
	void allReturnsEmptyPredicate() {

		PredicateSpecification<Object> specification = PredicateSpecification.all();

		assertThat(specification).isNotNull();
		assertThat(specification.toPredicate(root, builder)).isNull();
	}

	@Test // GH-3521
	void allOfCombinesPredicatesInOrder() {

		PredicateSpecification<Object> specification = PredicateSpecification.allOf(spec);

		assertThat(specification).isNotNull();
		assertThat(specification.toPredicate(root, builder)).isSameAs(predicate);
	}

	@Test // GH-3521
	void anyOfCombinesPredicatesInOrder() {

		PredicateSpecification<Object> specification = PredicateSpecification.allOf(spec);

		assertThat(specification).isNotNull();
		assertThat(specification.toPredicate(root, builder)).isSameAs(predicate);
	}

	@Test // GH-3521
	void emptyAllOfReturnsEmptySpecification() {

		PredicateSpecification<Object> specification = PredicateSpecification.allOf();

		assertThat(specification).isNotNull();
		assertThat(specification.toPredicate(root, builder)).isNull();
	}

	@Test // GH-3521
	void emptyAnyOfReturnsEmptySpecification() {

		PredicateSpecification<Object> specification = PredicateSpecification.anyOf();

		assertThat(specification).isNotNull();
		assertThat(specification.toPredicate(root, builder)).isNull();
	}

	@Test // GH-3521
	void specificationsShouldBeSerializable() {

		PredicateSpecification<Object> serializableSpec = new SerializableSpecification();
		PredicateSpecification<Object> specification = serializableSpec.and(serializableSpec);

		assertThat(specification).isNotNull();

		@SuppressWarnings("unchecked")
		PredicateSpecification<Object> transferredSpecification = (PredicateSpecification<Object>) deserialize(
				serialize(specification));

		assertThat(transferredSpecification).isNotNull();
	}

	@Test // GH-3521
	void complexSpecificationsShouldBeSerializable() {

		SerializableSpecification serializableSpec = new SerializableSpecification();
		PredicateSpecification<Object> specification = PredicateSpecification
				.not(serializableSpec.and(serializableSpec).or(serializableSpec));

		assertThat(specification).isNotNull();

		@SuppressWarnings("unchecked")
		PredicateSpecification<Object> transferredSpecification = (PredicateSpecification<Object>) deserialize(
				serialize(specification));

		assertThat(transferredSpecification).isNotNull();
	}

	@Test // GH-3521
	void andCombinesSpecificationsInOrder() {

		Predicate firstPredicate = mock(Predicate.class);
		Predicate secondPredicate = mock(Predicate.class);

		PredicateSpecification<Object> first = ((root1, criteriaBuilder) -> firstPredicate);
		PredicateSpecification<Object> second = ((root1, criteriaBuilder) -> secondPredicate);

		first.and(second).toPredicate(root, builder);

		verify(builder).and(firstPredicate, secondPredicate);
	}

	@Test // GH-3521
	void orCombinesSpecificationsInOrder() {

		Predicate firstPredicate = mock(Predicate.class);
		Predicate secondPredicate = mock(Predicate.class);

		PredicateSpecification<Object> first = ((root1, criteriaBuilder) -> firstPredicate);
		PredicateSpecification<Object> second = ((root1, criteriaBuilder) -> secondPredicate);

		first.or(second).toPredicate(root, builder);

		verify(builder).or(firstPredicate, secondPredicate);
	}

	static class SerializableSpecification implements Serializable, PredicateSpecification<Object> {

		@Override
		public Predicate toPredicate(Root<Object> root, CriteriaBuilder cb) {
			return null;
		}
	}
}
