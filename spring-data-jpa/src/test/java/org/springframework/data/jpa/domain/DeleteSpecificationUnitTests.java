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
import jakarta.persistence.criteria.CriteriaDelete;
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
 * Unit tests for {@link DeleteSpecification}.
 *
 * @author Mark Paluch
 */
@SuppressWarnings("serial")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeleteSpecificationUnitTests implements Serializable {

	private DeleteSpecification<Object> spec;
	@Mock(serializable = true) Root<Object> root;
	@Mock(serializable = true) CriteriaDelete<Object> delete;
	@Mock(serializable = true) CriteriaBuilder builder;
	@Mock(serializable = true) Predicate predicate;
	@Mock(serializable = true) Predicate another;

	@BeforeEach
	void setUp() {
		spec = (root, delete, cb) -> predicate;
	}

	@Test // GH-3521
	void allReturnsEmptyPredicate() {

		DeleteSpecification<Object> specification = DeleteSpecification.all();

		assertThat(specification).isNotNull();
		assertThat(specification.toPredicate(root, delete, builder)).isNull();
	}

	@Test // GH-3521
	void allOfCombinesPredicatesInOrder() {

		DeleteSpecification<Object> specification = DeleteSpecification.allOf(spec);

		assertThat(specification).isNotNull();
		assertThat(specification.toPredicate(root, delete, builder)).isSameAs(predicate);
	}

	@Test // GH-3521
	void anyOfCombinesPredicatesInOrder() {

		DeleteSpecification<Object> specification = DeleteSpecification.allOf(spec);

		assertThat(specification).isNotNull();
		assertThat(specification.toPredicate(root, delete, builder)).isSameAs(predicate);
	}

	@Test // GH-3521
	void emptyAllOfReturnsEmptySpecification() {

		DeleteSpecification<Object> specification = DeleteSpecification.allOf();

		assertThat(specification).isNotNull();
		assertThat(specification.toPredicate(root, delete, builder)).isNull();
	}

	@Test // GH-3521
	void emptyAnyOfReturnsEmptySpecification() {

		DeleteSpecification<Object> specification = DeleteSpecification.anyOf();

		assertThat(specification).isNotNull();
		assertThat(specification.toPredicate(root, delete, builder)).isNull();
	}

	@Test // GH-3521
	void specificationsShouldBeSerializable() {

		DeleteSpecification<Object> serializableSpec = new SerializableSpecification();
		DeleteSpecification<Object> specification = serializableSpec.and(serializableSpec);

		assertThat(specification).isNotNull();

		@SuppressWarnings("unchecked")
		DeleteSpecification<Object> transferredSpecification = (DeleteSpecification<Object>) deserialize(
				serialize(specification));

		assertThat(transferredSpecification).isNotNull();
	}

	@Test // GH-3521
	void complexSpecificationsShouldBeSerializable() {

		SerializableSpecification serializableSpec = new SerializableSpecification();
		DeleteSpecification<Object> specification = DeleteSpecification
				.not(serializableSpec.and(serializableSpec).or(serializableSpec));

		assertThat(specification).isNotNull();

		@SuppressWarnings("unchecked")
		DeleteSpecification<Object> transferredSpecification = (DeleteSpecification<Object>) deserialize(
				serialize(specification));

		assertThat(transferredSpecification).isNotNull();
	}

	@Test // GH-3521
	void andCombinesSpecificationsInOrder() {

		Predicate firstPredicate = mock(Predicate.class);
		Predicate secondPredicate = mock(Predicate.class);

		DeleteSpecification<Object> first = ((root1, delete, criteriaBuilder) -> firstPredicate);
		DeleteSpecification<Object> second = ((root1, delete, criteriaBuilder) -> secondPredicate);

		first.and(second).toPredicate(root, delete, builder);

		verify(builder).and(firstPredicate, secondPredicate);
	}

	@Test // GH-3521
	void orCombinesSpecificationsInOrder() {

		Predicate firstPredicate = mock(Predicate.class);
		Predicate secondPredicate = mock(Predicate.class);

		DeleteSpecification<Object> first = ((root1, delete, criteriaBuilder) -> firstPredicate);
		DeleteSpecification<Object> second = ((root1, delete, criteriaBuilder) -> secondPredicate);

		first.or(second).toPredicate(root, delete, builder);

		verify(builder).or(firstPredicate, secondPredicate);
	}

	static class SerializableSpecification implements Serializable, DeleteSpecification<Object> {

		@Override
		public Predicate toPredicate(Root<Object> root, CriteriaDelete<Object> delete, CriteriaBuilder cb) {
			return null;
		}
	}
}
