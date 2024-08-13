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
import jakarta.persistence.criteria.CriteriaUpdate;
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
 * Unit tests for {@link UpdateSpecification}.
 *
 * @author Mark Paluch
 */
@SuppressWarnings("serial")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UpdateSpecificationUnitTests implements Serializable {

	private UpdateSpecification<Object> spec;
	@Mock(serializable = true) Root<Object> root;
	@Mock(serializable = true) CriteriaUpdate<Object> update;
	@Mock(serializable = true) CriteriaBuilder builder;
	@Mock(serializable = true) Predicate predicate;
	@Mock(serializable = true) Predicate another;

	@BeforeEach
	void setUp() {
		spec = (root, update, cb) -> predicate;
	}

	@Test // GH-3521
	void allReturnsEmptyPredicate() {

		UpdateSpecification<Object> specification = UpdateSpecification.all();

		assertThat(specification).isNotNull();
		assertThat(specification.toPredicate(root, update, builder)).isNull();
	}

	@Test // GH-3521
	void allOfCombinesPredicatesInOrder() {

		UpdateSpecification<Object> specification = UpdateSpecification.allOf(spec);

		assertThat(specification).isNotNull();
		assertThat(specification.toPredicate(root, update, builder)).isSameAs(predicate);
	}

	@Test // GH-3521
	void anyOfCombinesPredicatesInOrder() {

		UpdateSpecification<Object> specification = UpdateSpecification.allOf(spec);

		assertThat(specification).isNotNull();
		assertThat(specification.toPredicate(root, update, builder)).isSameAs(predicate);
	}

	@Test // GH-3521
	void emptyAllOfReturnsEmptySpecification() {

		UpdateSpecification<Object> specification = UpdateSpecification.allOf();

		assertThat(specification).isNotNull();
		assertThat(specification.toPredicate(root, update, builder)).isNull();
	}

	@Test // GH-3521
	void emptyAnyOfReturnsEmptySpecification() {

		UpdateSpecification<Object> specification = UpdateSpecification.anyOf();

		assertThat(specification).isNotNull();
		assertThat(specification.toPredicate(root, update, builder)).isNull();
	}

	@Test // GH-3521
	void specificationsShouldBeSerializable() {

		UpdateSpecification<Object> serializableSpec = new SerializableSpecification();
		UpdateSpecification<Object> specification = serializableSpec.and(serializableSpec);

		assertThat(specification).isNotNull();

		@SuppressWarnings("unchecked")
		UpdateSpecification<Object> transferredSpecification = (UpdateSpecification<Object>) deserialize(
				serialize(specification));

		assertThat(transferredSpecification).isNotNull();
	}

	@Test // GH-3521
	void complexSpecificationsShouldBeSerializable() {

		SerializableSpecification serializableSpec = new SerializableSpecification();
		UpdateSpecification<Object> specification = UpdateSpecification
				.not(serializableSpec.and(serializableSpec).or(serializableSpec));

		assertThat(specification).isNotNull();

		@SuppressWarnings("unchecked")
		UpdateSpecification<Object> transferredSpecification = (UpdateSpecification<Object>) deserialize(
				serialize(specification));

		assertThat(transferredSpecification).isNotNull();
	}

	@Test // GH-3521
	void andCombinesSpecificationsInOrder() {

		Predicate firstPredicate = mock(Predicate.class);
		Predicate secondPredicate = mock(Predicate.class);

		UpdateSpecification<Object> first = ((root1, update, criteriaBuilder) -> firstPredicate);
		UpdateSpecification<Object> second = ((root1, update, criteriaBuilder) -> secondPredicate);

		first.and(second).toPredicate(root, update, builder);

		verify(builder).and(firstPredicate, secondPredicate);
	}

	@Test // GH-3521
	void orCombinesSpecificationsInOrder() {

		Predicate firstPredicate = mock(Predicate.class);
		Predicate secondPredicate = mock(Predicate.class);

		UpdateSpecification<Object> first = ((root1, update, criteriaBuilder) -> firstPredicate);
		UpdateSpecification<Object> second = ((root1, update, criteriaBuilder) -> secondPredicate);

		first.or(second).toPredicate(root, update, builder);

		verify(builder).or(firstPredicate, secondPredicate);
	}

	static class SerializableSpecification implements Serializable, UpdateSpecification<Object> {

		@Override
		public Predicate toPredicate(Root<Object> root, CriteriaUpdate<Object> update, CriteriaBuilder cb) {
			return null;
		}
	}
}
