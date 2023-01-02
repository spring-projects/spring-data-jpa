/*
 * Copyright 2013-2023 the original author or authors.
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
import static org.springframework.data.jpa.domain.Specification.*;
import static org.springframework.data.jpa.domain.Specification.not;
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
@SuppressWarnings("serial")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SpecificationUnitTests implements Serializable {

	private Specification<Object> spec;
	@Mock(serializable = true) Root<Object> root;
	@Mock(serializable = true) CriteriaQuery<?> query;
	@Mock(serializable = true) CriteriaBuilder builder;

	@Mock(serializable = true) Predicate predicate;

	@BeforeEach
	void setUp() {

		spec = (root, query, cb) -> predicate;
	}

	@Test // DATAJPA-300, DATAJPA-1170
	void createsSpecificationsFromNull() {

		Specification<Object> specification = where(null);
		assertThat(specification).isNotNull();
		assertThat(specification.toPredicate(root, query, builder)).isNull();
	}

	@Test // DATAJPA-300, DATAJPA-1170
	void negatesNullSpecToNull() {

		Specification<Object> specification = not(null);

		assertThat(specification).isNotNull();
		assertThat(specification.toPredicate(root, query, builder)).isNull();
	}

	@Test // DATAJPA-300, DATAJPA-1170
	void andConcatenatesSpecToNullSpec() {

		Specification<Object> specification = where(null);
		specification = specification.and(spec);

		assertThat(specification).isNotNull();
		assertThat(specification.toPredicate(root, query, builder)).isEqualTo(predicate);
	}

	@Test // DATAJPA-300, DATAJPA-1170
	void andConcatenatesNullSpecToSpec() {

		Specification<Object> specification = spec.and(null);

		assertThat(specification).isNotNull();
		assertThat(specification.toPredicate(root, query, builder)).isEqualTo(predicate);
	}

	@Test // DATAJPA-300, DATAJPA-1170
	void orConcatenatesSpecToNullSpec() {

		Specification<Object> specification = where(null);
		specification = specification.or(spec);

		assertThat(specification).isNotNull();
		assertThat(specification.toPredicate(root, query, builder)).isEqualTo(predicate);
	}

	@Test // DATAJPA-300, DATAJPA-1170
	void orConcatenatesNullSpecToSpec() {

		Specification<Object> specification = spec.or(null);

		assertThat(specification).isNotNull();
		assertThat(specification.toPredicate(root, query, builder)).isEqualTo(predicate);
	}

	@Test // GH-1943
	public void allOfConcatenatesNull() {

		Specification<Object> specification = Specification.allOf(null, spec, null);

		assertThat(specification).isNotNull();
		assertThat(specification.toPredicate(root, query, builder)).isEqualTo(predicate);
	}

	@Test // GH-1943
	public void anyOfConcatenatesNull() {

		Specification<Object> specification = Specification.anyOf(null, spec, null);

		assertThat(specification).isNotNull();
		assertThat(specification.toPredicate(root, query, builder)).isEqualTo(predicate);
	}

	@Test // GH-1943
	public void emptyAllOfReturnsEmptySpecification() {

		Specification<Object> specification = Specification.allOf();

		assertThat(specification).isNotNull();
		assertThat(specification.toPredicate(root, query, builder)).isNull();
	}

	@Test // GH-1943
	public void emptyAnyOfReturnsEmptySpecification() {

		Specification<Object> specification = Specification.anyOf();

		assertThat(specification).isNotNull();
		assertThat(specification.toPredicate(root, query, builder)).isNull();
	}

	@Test // DATAJPA-523
	void specificationsShouldBeSerializable() {

		Specification<Object> serializableSpec = new SerializableSpecification();
		Specification<Object> specification = serializableSpec.and(serializableSpec);

		assertThat(specification).isNotNull();

		@SuppressWarnings("unchecked")
		Specification<Object> transferredSpecification = (Specification<Object>) deserialize(serialize(specification));

		assertThat(transferredSpecification).isNotNull();
	}

	@Test // DATAJPA-523
	void complexSpecificationsShouldBeSerializable() {

		SerializableSpecification serializableSpec = new SerializableSpecification();
		Specification<Object> specification = Specification
				.not(serializableSpec.and(serializableSpec).or(serializableSpec));

		assertThat(specification).isNotNull();

		@SuppressWarnings("unchecked")
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
