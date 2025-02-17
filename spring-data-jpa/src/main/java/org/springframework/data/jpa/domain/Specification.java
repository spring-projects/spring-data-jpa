/*
 * Copyright 2008-2025 the original author or authors.
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
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
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
 * {@link #and(Specification)}, {@link #or(Specification)} or factory methods such as {@link #allOf(Iterable)}.
 * Composition considers whether one or more specifications contribute to the overall predicate by returning a
 * {@link Predicate} or {@literal null}. Specifications returning {@literal null} are considered to not contribute to
 * the overall predicate and their result is not considered in the final predicate.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Krzysztof Rzymkowski
 * @author Sebastian Staudt
 * @author Mark Paluch
 * @author Jens Schauder
 * @author Daniel Shuy
 * @author Sergey Rukin
 */
@FunctionalInterface
public interface Specification<T> extends Serializable {

	/**
	 * Simple static factory method to create a specification matching all objects.
	 *
	 * @param <T> the type of the {@link Root} the resulting {@literal Specification} operates on.
	 * @return guaranteed to be not {@literal null}.
	 */
	static <T> Specification<T> unrestricted() {
		return (root, query, builder) -> null;
	}

	/**
	 * Simple static factory method to add some syntactic sugar translating {@link PredicateSpecification} to
	 * {@link Specification}.
	 *
	 * @param <T> the type of the {@link Root} the resulting {@literal Specification} operates on.
	 * @param spec the {@link PredicateSpecification} to wrap.
	 * @return guaranteed to be not {@literal null}.
	 */
	static <T> Specification<T> where(PredicateSpecification<T> spec) {

		Assert.notNull(spec, "PredicateSpecification must not be null");

		return (root, update, criteriaBuilder) -> spec.toPredicate(root, criteriaBuilder);
	}

	/**
	 * ANDs the given {@link Specification} to the current one.
	 *
	 * @param other the other {@link Specification}.
	 * @return the conjunction of the specifications.
	 * @since 2.0
	 */
	@Contract("_ -> new")
	@CheckReturnValue
	default Specification<T> and(Specification<T> other) {

		Assert.notNull(other, "Other specification must not be null");

		return SpecificationComposition.composed(this, other, CriteriaBuilder::and);
	}

	/**
	 * ANDs the given {@link Specification} to the current one.
	 *
	 * @param other the other {@link PredicateSpecification}.
	 * @return the conjunction of the specifications.
	 * @since 2.0
	 */
	@Contract("_ -> new")
	@CheckReturnValue
	default Specification<T> and(PredicateSpecification<T> other) {

		Assert.notNull(other, "Other specification must not be null");

		return SpecificationComposition.composed(this, where(other), CriteriaBuilder::and);
	}

	/**
	 * ORs the given specification to the current one.
	 *
	 * @param other the other {@link Specification}.
	 * @return the disjunction of the specifications
	 * @since 2.0
	 */
	@Contract("_ -> new")
	@CheckReturnValue
	default Specification<T> or(Specification<T> other) {

		Assert.notNull(other, "Other specification must not be null");

		return SpecificationComposition.composed(this, other, CriteriaBuilder::or);
	}

	/**
	 * ORs the given specification to the current one.
	 *
	 * @param other the other {@link PredicateSpecification}.
	 * @return the disjunction of the specifications
	 * @since 2.0
	 */
	@Contract("_ -> new")
	@CheckReturnValue
	default Specification<T> or(PredicateSpecification<T> other) {

		Assert.notNull(other, "Other specification must not be null");

		return SpecificationComposition.composed(this, where(other), CriteriaBuilder::or);
	}

	/**
	 * Negates the given {@link Specification}.
	 *
	 * @param <T> the type of the {@link Root} the resulting {@literal Specification} operates on.
	 * @param spec can be {@literal null}.
	 * @return guaranteed to be not {@literal null}.
	 * @since 2.0
	 */
	static <T> Specification<T> not(Specification<T> spec) {

		Assert.notNull(spec, "Specification must not be null");

		return (root, query, builder) -> {

			Predicate predicate = spec.toPredicate(root, query, builder);
			return predicate != null ? builder.not(predicate) : null;
		};
	}

	/**
	 * Applies an AND operation to all the given {@link Specification}s. If {@code specifications} is empty, the resulting
	 * {@link Specification} will be unrestricted applying to all objects.
	 *
	 * @param specifications the {@link Specification}s to compose.
	 * @return the conjunction of the specifications.
	 * @see #and(Specification)
	 * @see #allOf(Iterable)
	 * @since 3.0
	 */
	@SafeVarargs
	static <T> Specification<T> allOf(Specification<T>... specifications) {
		return allOf(Arrays.asList(specifications));
	}

	/**
	 * Applies an AND operation to all the given {@link Specification}s. If {@code specifications} is empty, the resulting
	 * {@link Specification} will be unrestricted applying to all objects.
	 *
	 * @param specifications the {@link Specification}s to compose.
	 * @return the conjunction of the specifications.
	 * @see #and(Specification)
	 * @see #allOf(Specification[])
	 * @since 3.0
	 */
	static <T> Specification<T> allOf(Iterable<Specification<T>> specifications) {

		return StreamSupport.stream(specifications.spliterator(), false) //
				.reduce(Specification.unrestricted(), Specification::and);
	}

	/**
	 * Applies an OR operation to all the given {@link Specification}s. If {@code specifications} is empty, the resulting
	 * {@link Specification} will be unrestricted applying to all objects.
	 *
	 * @param specifications the {@link Specification}s to compose.
	 * @return the disjunction of the specifications
	 * @see #or(Specification)
	 * @see #anyOf(Iterable)
	 * @since 3.0
	 */
	@SafeVarargs
	static <T> Specification<T> anyOf(Specification<T>... specifications) {
		return anyOf(Arrays.asList(specifications));
	}

	/**
	 * Applies an OR operation to all the given {@link Specification}s. If {@code specifications} is empty, the resulting
	 * {@link Specification} will be unrestricted applying to all objects.
	 *
	 * @param specifications the {@link Specification}s to compose.
	 * @return the disjunction of the specifications
	 * @see #or(Specification)
	 * @see #anyOf(Iterable)
	 * @since 3.0
	 */
	static <T> Specification<T> anyOf(Iterable<Specification<T>> specifications) {

		return StreamSupport.stream(specifications.spliterator(), false) //
				.reduce(Specification.unrestricted(), Specification::or);
	}

	/**
	 * Creates a WHERE clause for a query of the referenced entity in form of a {@link Predicate} for the given
	 * {@link Root} and {@link CriteriaUpdate}.
	 *
	 * @param root must not be {@literal null}.
	 * @param query the criteria query.
	 * @param criteriaBuilder must not be {@literal null}.
	 * @return a {@link Predicate}, may be {@literal null}.
	 */
	@Nullable
	Predicate toPredicate(Root<T> root, @Nullable CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder);

}
