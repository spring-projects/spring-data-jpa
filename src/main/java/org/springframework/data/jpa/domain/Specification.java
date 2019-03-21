/*
 * Copyright 2008-2019 the original author or authors.
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

import static org.springframework.data.jpa.domain.SpecificationComposition.*;

import java.io.Serializable;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.lang.Nullable;

/**
 * Specification in the sense of Domain Driven Design.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Krzysztof Rzymkowski
 * @author Sebastian Staudt
 * @author Mark Paluch
 */
public interface Specification<T> extends Serializable {

	long serialVersionUID = 1L;

	/**
	 * Negates the given {@link Specification}.
	 *
	 * @param <T>
	 * @param spec can be {@literal null}.
	 * @return
	 * @since 2.0
	 */
	static <T> Specification<T> not(@Nullable Specification<T> spec) {

		return spec == null //
				? (root, query, builder) -> null//
				: (root, query, builder) -> builder.not(spec.toPredicate(root, query, builder));
	}

	/**
	 * Simple static factory method to add some syntactic sugar around a {@link Specification}.
	 *
	 * @param <T>
	 * @param spec can be {@literal null}.
	 * @return
	 * @since 2.0
	 */
	@Nullable
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
	@Nullable
	default Specification<T> and(@Nullable Specification<T> other) {
		return composed(this, other, (builder, left, rhs) -> builder.and(left, rhs));
	}

	/**
	 * ORs the given specification to the current one.
	 *
	 * @param other can be {@literal null}.
	 * @return The disjunction of the specifications
	 * @since 2.0
	 */
	@Nullable
	default Specification<T> or(@Nullable Specification<T> other) {
		return composed(this, other, (builder, left, rhs) -> builder.or(left, rhs));
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
}
