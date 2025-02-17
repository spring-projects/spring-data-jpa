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
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.StreamSupport;

import org.springframework.lang.CheckReturnValue;

import org.jspecify.annotations.Nullable;
import org.springframework.lang.Contract;
import org.springframework.util.Assert;

/**
 * Specification in the sense of Domain Driven Design.
 * <p>
 * Specifications can be composed into higher order functions from other specifications using
 * {@link #and(PredicateSpecification)}, {@link #or(PredicateSpecification)} or factory methods such as
 * {@link #allOf(Iterable)}. Composition considers whether one or more specifications contribute to the overall
 * predicate by returning a {@link Predicate} or {@literal null}. Specifications returning {@literal null} are
 * considered to not contribute to the overall predicate and their result is not considered in the final predicate.
 *
 * @author Mark Paluch
 * @since 4.0
 */
@FunctionalInterface
public interface PredicateSpecification<T> extends Serializable {

	/**
	 * Simple static factory method to create a specification matching all objects.
	 *
	 * @param <T> the type of the {@link Root} the resulting {@literal PredicateSpecification} operates on.
	 * @return guaranteed to be not {@literal null}.
	 */
	static <T> PredicateSpecification<T> unrestricted() {
		return (root, builder) -> null;
	}

	/**
	 * Simple static factory method to add some syntactic sugar around a {@literal PredicateSpecification}.
	 *
	 * @param <T> the type of the {@link Root} the resulting {@literal PredicateSpecification} operates on.
	 * @param spec must not be {@literal null}.
	 * @return guaranteed to be not {@literal null}.
	 * @since 2.0
	 */
	static <T> PredicateSpecification<T> where(PredicateSpecification<T> spec) {

		Assert.notNull(spec, "PredicateSpecification must not be null");

		return spec;
	}

	/**
	 * ANDs the given {@literal PredicateSpecification} to the current one.
	 *
	 * @param other the other {@link PredicateSpecification}.
	 * @return the conjunction of the specifications.
	 */
	@Contract("_ -> new")
	@CheckReturnValue
	default PredicateSpecification<T> and(PredicateSpecification<T> other) {

		Assert.notNull(other, "Other specification must not be null");

		return SpecificationComposition.composed(this, other, CriteriaBuilder::and);
	}

	/**
	 * ORs the given specification to the current one.
	 *
	 * @param other the other {@link PredicateSpecification}.
	 * @return the disjunction of the specifications.
	 */
	@Contract("_ -> new")
	@CheckReturnValue
	default PredicateSpecification<T> or(PredicateSpecification<T> other) {

		Assert.notNull(other, "Other specification must not be null");

		return SpecificationComposition.composed(this, other, CriteriaBuilder::or);
	}

	/**
	 * Negates the given {@link PredicateSpecification}.
	 *
	 * @param <T> the type of the {@link Root} the resulting {@literal PredicateSpecification} operates on.
	 * @param spec can be {@literal null}.
	 * @return guaranteed to be not {@literal null}.
	 */
	static <T> PredicateSpecification<T> not(PredicateSpecification<T> spec) {

		Assert.notNull(spec, "Specification must not be null");

		return (root, builder) -> {

			Predicate predicate = spec.toPredicate(root, builder);
			return predicate != null ? builder.not(predicate) : null;
		};
	}

	/**
	 * Applies an AND operation to all the given {@link PredicateSpecification}s. If {@code specifications} is empty, the
	 * resulting {@link PredicateSpecification} will be unrestricted applying to all objects.
	 *
	 * @param specifications the {@link PredicateSpecification}s to compose.
	 * @return the conjunction of the specifications.
	 * @see #allOf(Iterable)
	 * @see #and(PredicateSpecification)
	 */
	@SafeVarargs
	static <T> PredicateSpecification<T> allOf(PredicateSpecification<T>... specifications) {
		return allOf(Arrays.asList(specifications));
	}

	/**
	 * Applies an AND operation to all the given {@link PredicateSpecification}s. If {@code specifications} is empty, the
	 * resulting {@link PredicateSpecification} will be unrestricted applying to all objects.
	 *
	 * @param specifications the {@link PredicateSpecification}s to compose.
	 * @return the conjunction of the specifications.
	 * @see #and(PredicateSpecification)
	 * @see #allOf(PredicateSpecification[])
	 */
	static <T> PredicateSpecification<T> allOf(Iterable<PredicateSpecification<T>> specifications) {

		return StreamSupport.stream(specifications.spliterator(), false) //
				.reduce(PredicateSpecification.unrestricted(), PredicateSpecification::and);
	}

	/**
	 * Applies an OR operation to all the given {@link PredicateSpecification}s. If {@code specifications} is empty, the
	 * resulting {@link PredicateSpecification} will be unrestricted applying to all objects.
	 *
	 * @param specifications the {@link PredicateSpecification}s to compose.
	 * @return the disjunction of the specifications.
	 * @see #or(PredicateSpecification)
	 * @see #anyOf(Iterable)
	 */
	@SafeVarargs
	static <T> PredicateSpecification<T> anyOf(PredicateSpecification<T>... specifications) {
		return anyOf(Arrays.asList(specifications));
	}

	/**
	 * Applies an OR operation to all the given {@link PredicateSpecification}s. If {@code specifications} is empty, the
	 * resulting {@link PredicateSpecification} will be unrestricted applying to all objects.
	 *
	 * @param specifications the {@link PredicateSpecification}s to compose.
	 * @return the disjunction of the specifications.
	 * @see #or(PredicateSpecification)
	 * @see #anyOf(PredicateSpecification[])
	 */
	static <T> PredicateSpecification<T> anyOf(Iterable<PredicateSpecification<T>> specifications) {

		return StreamSupport.stream(specifications.spliterator(), false) //
				.reduce(PredicateSpecification.unrestricted(), PredicateSpecification::or);
	}

	/**
	 * Creates a WHERE clause for a query of the referenced entity in form of a {@link Predicate} for the given
	 * {@link Root} and {@link CriteriaBuilder}.
	 *
	 * @param root must not be {@literal null}.
	 * @param criteriaBuilder must not be {@literal null}.
	 * @return a {@link Predicate}, may be {@literal null}.
	 */
	@Nullable
	Predicate toPredicate(Root<T> root, CriteriaBuilder criteriaBuilder);

}
