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
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.StreamSupport;

import org.springframework.lang.Nullable;

/**
 * Specification in the sense of Domain Driven Design.
 * <p>
 * Specifications can be composed into higher order functions from other specifications using
 * {@link #and(Specification)}, {@link #or(Specification)} or factory methods such as {@link #allOf(Iterable)}.
 * <p>
 * Composition considers whether one or more specifications contribute to the overall predicate by returning a
 * {@link Predicate} or {@literal null}. Specifications returning {@literal null}, such as {@link #unrestricted()}, are
 * considered to not contribute to the overall predicate, and their result is not considered in the final predicate.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Krzysztof Rzymkowski
 * @author Sebastian Staudt
 * @author Mark Paluch
 * @author Jens Schauder
 * @author Daniel Shuy
 * @author Sergey Rukin
 * @author Peter Aisher
 */
@FunctionalInterface
public interface Specification<T> extends Serializable {

	@Serial long serialVersionUID = 1L;

	/**
	 * Negates the given {@link Specification}.
	 *
	 * @apiNote with 4.0, this method will no longer accept {@literal null} specifications.
	 * @param <T> the type of the {@link Root} the resulting {@literal Specification} operates on.
	 * @param spec can be {@literal null}.
	 * @return guaranteed to be not {@literal null}.
	 * @since 2.0
	 */
	static <T> Specification<T> not(@Nullable Specification<T> spec) {

		return spec == null //
				? (root, query, builder) -> null //
				: (root, query, builder) -> {

					Predicate predicate = spec.toPredicate(root, query, builder);
					return predicate != null ? builder.not(predicate) : null;
				};
	}

	/**
	 * Simple static factory method to create a specification which does not participate in matching. The specification
	 * returned is {@code null}-like, and is elided in all operations.
	 *
	 * <pre class="code">
	 * unrestricted().and(other) // consider only `other`
	 * unrestricted().or(other) // consider only `other`
	 * not(unrestricted()) // equivalent to `unrestricted()`
	 * </pre>
	 *
	 * @param <T> the type of the {@link Root} the resulting {@literal Specification} operates on.
	 * @return guaranteed to be not {@literal null}.
	 * @since 3.5.2
	 */
	static <T> Specification<T> unrestricted() {
		return (root, query, builder) -> null;
	}

	/**
	 * Simple static factory method to add some syntactic sugar around a {@link Specification}.
	 *
	 * @apiNote with 4.0, this method will no longer accept {@literal null} specifications.
	 * @param <T> the type of the {@link Root} the resulting {@literal Specification} operates on.
	 * @param spec can be {@literal null}.
	 * @return guaranteed to be not {@literal null}.
	 * @since 2.0
	 * @deprecated since 3.5, to be removed with 4.0 as we no longer want to support {@literal null} specifications. Use
	 *             {@link #unrestricted()} instead.
	 */
	@Deprecated(since = "3.5.0", forRemoval = true)
	static <T> Specification<T> where(@Nullable Specification<T> spec) {
		return spec == null ? unrestricted() : spec;
	}

	/**
	 * ANDs the given {@link Specification} to the current one.
	 *
	 * @apiNote with 4.0, this method will no longer accept {@literal null} specifications.
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
	 * @apiNote with 4.0, this method will no longer accept {@literal null} specifications.
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
	 * @param query can be {@literal null} to allow overrides that accept
	 *          {@link jakarta.persistence.criteria.CriteriaDelete} which is an
	 *          {@link jakarta.persistence.criteria.AbstractQuery} but no {@link CriteriaQuery}.
	 * @param criteriaBuilder must not be {@literal null}.
	 * @return a {@link Predicate}, may be {@literal null}.
	 */
	@Nullable
	Predicate toPredicate(Root<T> root, @Nullable CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder);

	/**
	 * Applies an AND operation to all the given {@link Specification}s.
	 *
	 * @param specifications The {@link Specification}s to compose. Can contain {@code null}s.
	 * @return The conjunction of the specifications
	 * @see #and(Specification)
	 * @since 3.0
	 */
	static <T> Specification<T> allOf(Iterable<Specification<T>> specifications) {

		return StreamSupport.stream(specifications.spliterator(), false) //
				.reduce(Specification.where(null), Specification::and);
	}

	/**
	 * @see #allOf(Iterable)
	 * @since 3.0
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
	 * @since 3.0
	 */
	static <T> Specification<T> anyOf(Iterable<Specification<T>> specifications) {

		return StreamSupport.stream(specifications.spliterator(), false) //
				.reduce(Specification.where(null), Specification::or);
	}

	/**
	 * @see #anyOf(Iterable)
	 * @since 3.0
	 */
	@SafeVarargs
	static <T> Specification<T> anyOf(Specification<T>... specifications) {
		return anyOf(Arrays.asList(specifications));
	}
}
