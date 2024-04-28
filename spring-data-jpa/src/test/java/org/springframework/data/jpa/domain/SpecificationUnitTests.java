/*
 * Copyright 2013-2025 the original author or authors.
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
import jakarta.persistence.criteria.CriteriaQuery;
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
 * Unit tests for {@link Specification}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Sebastian Staudt
 * @author Jens Schauder
 * @author Mark Paluch
 * @author Daniel Shuy
 */
@SuppressWarnings("removal")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SpecificationUnitTests {

	private Specification<Object> spec;
	@Mock(serializable = true) Root<Object> root;
	@Mock(serializable = true) CriteriaQuery<?> query;
	@Mock(serializable = true) CriteriaBuilder builder;

	@Mock(serializable = true) Predicate predicate;

	@BeforeEach
	void setUp() {

		spec = (root, query, cb) -> predicate;
	}

	@Test // GH-1943
	void emptyAllOfReturnsEmptySpecification() {

		Specification<Object> specification = Specification.allOf();

		assertThat(specification).isNotNull();
		assertThat(specification.toPredicate(root, query, builder)).isNull();
	}

	@Test // GH-1943
	void emptyAnyOfReturnsEmptySpecification() {

		Specification<Object> specification = Specification.anyOf();

		assertThat(specification).isNotNull();
		assertThat(specification.toPredicate(root, query, builder)).isNull();
	}

	@Test // DATAJPA-523
	void specificationsShouldBeSerializable() {

		Specification<Object> serializableSpec = new SerializableSpecification();
		Specification<Object> specification = serializableSpec.and(serializableSpec);

		assertThat(specification).isNotNull();

		@SuppressWarnings({ "unchecked", "deprecation"})
		Specification<Object> transferredSpecification = (Specification<Object>) deserialize(serialize(specification));

		assertThat(transferredSpecification).isNotNull();
	}

	@Test // DATAJPA-523
	void complexSpecificationsShouldBeSerializable() {

		SerializableSpecification serializableSpec = new SerializableSpecification();
		Specification<Object> specification = Specification
				.not(serializableSpec.and(serializableSpec).or(serializableSpec));

		assertThat(specification).isNotNull();

		@SuppressWarnings({ "unchecked", "deprecation"})
		Specification<Object> transferredSpecification = (Specification<Object>) deserialize(serialize(specification));

		assertThat(transferredSpecification).isNotNull();
	}

	@Test // #2146
	void andCombinesSpecificationsInOrder() {

		Predicate firstPredicate = mock(Predicate.class);
		Predicate secondPredicate = mock(Predicate.class);

		Specification<Object> first = ((root1, query1, criteriaBuilder) -> firstPredicate);

		Specification<Object> second = ((root1, query1, criteriaBuilder) -> secondPredicate);

		first.and(second).toPredicate(root, query, builder);

		verify(builder).and(firstPredicate, secondPredicate);
	}

	@Test // #2146
	void orCombinesSpecificationsInOrder() {

		Predicate firstPredicate = mock(Predicate.class);
		Predicate secondPredicate = mock(Predicate.class);

		Specification<Object> first = ((root1, query1, criteriaBuilder) -> firstPredicate);

		Specification<Object> second = ((root1, query1, criteriaBuilder) -> secondPredicate);

		first.or(second).toPredicate(root, query, builder);

		verify(builder).or(firstPredicate, secondPredicate);
	}

	static class SerializableSpecification implements Serializable, Specification<Object> {

		@Override
		public Predicate toPredicate(Root<Object> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
			return null;
		}
	}
}
