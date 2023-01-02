/*
 * Copyright 2008-2023 the original author or authors.
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
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.StreamSupport;

import org.springframework.lang.Nullable;

/**
 * Specification in the sense of Domain Driven Design.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Krzysztof Rzymkowski
 * @author Sebastian Staudt
 * @author Mark Paluch
 * @author Jens Schauder
 * @author Daniel Shuy
 */
public interface Specification<T> extends Serializable {

	long serialVersionUID = 1L;

	/**
	 * Negates the given {@link Specification}.
	 *
	 * @param <T> the type of the {@link Root} the resulting {@literal Specification} operates on.
	 * @param spec can be {@literal null}.
	 * @return guaranteed to be not {@literal null}.
	 * @since 2.0
	 */
	static <T> Specification<T> not(@Nullable Specification<T> spec) {

		return spec == null //
				? (root, query, builder) -> null //
				: (root, query, builder) -> builder.not(spec.toPredicate(root, query, builder));
	}

	/**
	 * Simple static factory method to add some syntactic sugar around a {@link Specification}.
	 *
	 * @param <T> the type of the {@link Root} the resulting {@literal Specification} operates on.
	 * @param spec can be {@literal null}.
	 * @return guaranteed to be not {@literal null}.
	 * @since 2.0
	 */
	static <T> Specification<T> where(@Nullable Specification<T> spec) {
		return spec == null ? (root, query, builder) -> null : spec;
	}

	/**
	 * ANDs the given {@link Specification} to the current one.
	 *
	 * @param other can be {@literal null}.
	 * @return The conjunction of the specifications
	 * @since 2.0
	 */
	default Specification<T> and(@Nullable Specification<T> other) {
		return SpecificationComposition.composed(this, other, CriteriaBuilder::and);
	}

	/**
	 * ORs the given specification to the current one.
	 *
	 * @param other can be {@literal null}.
	 * @return The disjunction of the specifications
	 * @since 2.0
	 */
	default Specification<T> or(@Nullable Specification<T> other) {
		return SpecificationComposition.composed(this, other, CriteriaBuilder::or);
	}

	/**
	 * Creates a WHERE clause for a query of the referenced entity in form of a {@link Predicate} for the given
	 * {@link Root} and {@link CriteriaQuery}.
	 *
	 * @param root must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param criteriaBuilder must not be {@literal null}.
	 * @return a {@link Predicate}, may be {@literal null}.
	 */
	@Nullable
	Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder);

	/**
	 * Applies an AND operation to all the given {@link Specification}s.
	 *
	 * @param specifications The {@link Specification}s to compose. Can contain {@code null}s.
	 * @return The conjunction of the specifications
	 * @see #and(Specification)
	 */
	static <T> Specification<T> allOf(Iterable<Specification<T>> specifications) {

		return StreamSupport.stream(specifications.spliterator(), false) //
				.reduce(Specification.where(null), Specification::and);
	}

	/**
	 * @see #allOf(Iterable)
	 */
	@SafeVarargs
	static <T> Specification<T> allOf(Specification<T>... specifications) {
		return allOf(Arrays.asList(specifications));
	}

	/**
	 * Applies an OR operation to all the given {@link Specification}s.
	 *
	 * @param specifications The {@link Specification}s to compose. Can contain {@code null}s.
	 * @return The disjunction of the specifications
	 * @see #or(Specification)
	 */
	static <T> Specification<T> anyOf(Iterable<Specification<T>> specifications) {

		return StreamSupport.stream(specifications.spliterator(), false) //
				.reduce(Specification.where(null), Specification::or);
	}

	/**
	 * @see #anyOf(Iterable)
	 */
	@SafeVarargs
	static <T> Specification<T> anyOf(Specification<T>... specifications) {
		return anyOf(Arrays.asList(specifications));
	}
}
