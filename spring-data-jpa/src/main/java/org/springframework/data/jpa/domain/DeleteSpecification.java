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

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.StreamSupport;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Specification in the sense of Domain Driven Design to handle Criteria Deletes.
 *
 * @author Mark Paluch
 * @since xxx
 */
@FunctionalInterface
public interface DeleteSpecification<T> extends Serializable {

	@Serial long serialVersionUID = 1L;

	/**
	 * Simple static factory method to create a specification deleting all objects.
	 *
	 * @param <T> the type of the {@link Root} the resulting {@literal DeleteSpecification} operates on.
	 * @return guaranteed to be not {@literal null}.
	 */
	static <T> DeleteSpecification<T> all() {
		return (root, query, builder) -> null;
	}

	/**
	 * Simple static factory method to add some syntactic sugar around a {@literal DeleteSpecification}.
	 *
	 * @param <T> the type of the {@link Root} the resulting {@literal DeleteSpecification} operates on.
	 * @param spec must not be {@literal null}.
	 * @return guaranteed to be not {@literal null}.
	 */
	static <T> DeleteSpecification<T> where(DeleteSpecification<T> spec) {

		Assert.notNull(spec, "DeleteSpecification must not be null");

		return spec;
	}

	/**
	 * Simple static factory method to add some syntactic sugar translating {@link PredicateSpecification} to
	 * {@link DeleteSpecification}.
	 *
	 * @param <T> the type of the {@link Root} the resulting {@literal DeleteSpecification} operates on.
	 * @param spec the {@link PredicateSpecification} to wrap.
	 * @return guaranteed to be not {@literal null}.
	 */
	static <T> DeleteSpecification<T> where(PredicateSpecification<T> spec) {

		Assert.notNull(spec, "PredicateSpecification must not be null");

		return where((root, delete, criteriaBuilder) -> spec.toPredicate(root, criteriaBuilder));
	}

	/**
	 * ANDs the given {@link DeleteSpecification} to the current one.
	 *
	 * @param other the other {@link DeleteSpecification}.
	 * @return the conjunction of the specifications.
	 */
	default DeleteSpecification<T> and(DeleteSpecification<T> other) {

		Assert.notNull(other, "Other specification must not be null");

		return SpecificationComposition.composed(this, other, CriteriaBuilder::and);
	}

	/**
	 * ANDs the given {@link DeleteSpecification} to the current one.
	 *
	 * @param other the other {@link PredicateSpecification}.
	 * @return the conjunction of the specifications.
	 */
	default DeleteSpecification<T> and(PredicateSpecification<T> other) {

		Assert.notNull(other, "Other specification must not be null");

		return SpecificationComposition.composed(this, where(other), CriteriaBuilder::and);
	}

	/**
	 * ORs the given specification to the current one.
	 *
	 * @param other the other {@link DeleteSpecification}.
	 * @return the disjunction of the specifications.
	 */
	default DeleteSpecification<T> or(DeleteSpecification<T> other) {

		Assert.notNull(other, "Other specification must not be null");

		return SpecificationComposition.composed(this, other, CriteriaBuilder::or);
	}

	/**
	 * ORs the given specification to the current one.
	 *
	 * @param other the other {@link PredicateSpecification}.
	 * @return the disjunction of the specifications.
	 */
	default DeleteSpecification<T> or(PredicateSpecification<T> other) {

		Assert.notNull(other, "Other specification must not be null");

		return SpecificationComposition.composed(this, where(other), CriteriaBuilder::or);
	}

	/**
	 * Negates the given {@link DeleteSpecification}.
	 *
	 * @param <T> the type of the {@link Root} the resulting {@literal DeleteSpecification} operates on.
	 * @param spec can be {@literal null}.
	 * @return guaranteed to be not {@literal null}.
	 */
	static <T> DeleteSpecification<T> not(DeleteSpecification<T> spec) {

		Assert.notNull(spec, "Specification must not be null");

		return (root, delete, builder) -> {

			Predicate not = spec.toPredicate(root, delete, builder);
			return not != null ? builder.not(not) : null;
		};
	}

	/**
	 * Applies an AND operation to all the given {@link DeleteSpecification}s.
	 *
	 * @param specifications the {@link DeleteSpecification}s to compose.
	 * @return the conjunction of the specifications.
	 * @see #and(DeleteSpecification)
	 * @see #allOf(Iterable)
	 */
	@SafeVarargs
	static <T> DeleteSpecification<T> allOf(DeleteSpecification<T>... specifications) {
		return allOf(Arrays.asList(specifications));
	}

	/**
	 * Applies an AND operation to all the given {@link DeleteSpecification}s.
	 *
	 * @param specifications the {@link DeleteSpecification}s to compose.
	 * @return the conjunction of the specifications.
	 * @see #and(DeleteSpecification)
	 * @see #allOf(DeleteSpecification[])
	 */
	static <T> DeleteSpecification<T> allOf(Iterable<DeleteSpecification<T>> specifications) {

		return StreamSupport.stream(specifications.spliterator(), false) //
				.reduce(DeleteSpecification.all(), DeleteSpecification::and);
	}

	/**
	 * Applies an OR operation to all the given {@link DeleteSpecification}s.
	 *
	 * @param specifications the {@link DeleteSpecification}s to compose.
	 * @return the disjunction of the specifications.
	 * @see #or(DeleteSpecification)
	 * @see #anyOf(Iterable)
	 */
	@SafeVarargs
	static <T> DeleteSpecification<T> anyOf(DeleteSpecification<T>... specifications) {
		return anyOf(Arrays.asList(specifications));
	}

	/**
	 * Applies an OR operation to all the given {@link DeleteSpecification}s.
	 *
	 * @param specifications the {@link DeleteSpecification}s to compose.
	 * @return the disjunction of the specifications.
	 * @see #or(DeleteSpecification)
	 * @see #anyOf(Iterable)
	 */
	static <T> DeleteSpecification<T> anyOf(Iterable<DeleteSpecification<T>> specifications) {

		return StreamSupport.stream(specifications.spliterator(), false) //
				.reduce(DeleteSpecification.all(), DeleteSpecification::or);
	}

	/**
	 * Creates a WHERE clause for a query of the referenced entity in form of a {@link Predicate} for the given
	 * {@link Root} and {@link CriteriaDelete}.
	 *
	 * @param root must not be {@literal null}.
	 * @param delete the delete criteria.
	 * @param criteriaBuilder must not be {@literal null}.
	 * @return a {@link Predicate}, may be {@literal null}.
	 */
	@Nullable
	Predicate toPredicate(Root<T> root, CriteriaDelete<T> delete, CriteriaBuilder criteriaBuilder);

}
