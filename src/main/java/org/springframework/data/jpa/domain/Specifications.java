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

import static org.springframework.data.jpa.domain.Specifications.CompositionType.*;

import java.io.Serializable;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.lang.Nullable;

/**
 * Helper class to easily combine {@link Specification} instances.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Sebastian Staudt
 * @author Mark Paluch
 * @deprecated since 2.0, use factory methods on {@link Specification} instead.
 */
@Deprecated
public class Specifications<T> implements Specification<T>, Serializable {

	private static final long serialVersionUID = 1L;

	private final @Nullable Specification<T> spec;

	/**
	 * Creates a new {@link Specifications} wrapper for the given {@link Specification}.
	 *
	 * @param spec can be {@literal null}.
	 */
	Specifications(@Nullable Specification<T> spec) {
		this.spec = spec;
	}

	/**
	 * Simple static factory method to add some syntactic sugar around a {@link Specification}.
	 *
	 * @deprecated since 2.0, use {@link Specification#where} instead
	 * @param <T> type parameter for the specification parameter.
	 * @param spec can be {@literal null}.
	 * @return a new Specifcations instance. Guaranteed to be not {@code null}.
	 */
	@Deprecated
	public static <T> Specifications<T> where(@Nullable Specification<T> spec) {
		return new Specifications<>(spec);
	}

	/**
	 * ANDs the given {@link Specification} to the current one.
	 *
	 * @deprecated since 2.0, use {@link Specification#and} instead
	 * @param other can be {@literal null}.
	 * @return a new Specifications instance combining this and the parameter instance. Guaranteed to be not {@code null}.
	 */
	@Deprecated
	public Specifications<T> and(@Nullable Specification<T> other) {
		return new Specifications<>(composed(spec, other, AND));
	}

	/**
	 * ORs the given specification to the current one.
	 *
	 * @deprecated since 2.0, use {@link Specification#or} instead
	 * @param other can be {@literal null}.
	 * @return a new Specifications instance combining this and the parameter instance. Guaranteed to be not {@code null}.
	 */
	@Deprecated
	public Specifications<T> or(@Nullable Specification<T> other) {
		return new Specifications<>(composed(spec, other, OR));
	}

	/**
	 * Negates the given {@link Specification}.
	 *
	 * @deprecated since 2.0, use {@link Specification#not} instead
	 * @param <T> type parameter for the specification parameter.
	 * @param spec can be {@literal null}.
	 * @return a new Specifications instance combining this and the parameter instance. Guaranteed to be not {@code null}.
	 */
	@Deprecated
	public static <T> Specifications<T> not(@Nullable Specification<T> spec) {
		return new Specifications<>(negated(spec));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.domain.Specification#toPredicate(javax.persistence.criteria.Root, javax.persistence.criteria.CriteriaQuery, javax.persistence.criteria.CriteriaBuilder)
	 */
	@Nullable
	public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		return spec == null ? null : spec.toPredicate(root, query, builder);
	}

	/**
	 * Enum for the composition types for {@link Predicate}s. Can not be turned into lambdas as we need to be
	 * serializable.
	 *
	 * @author Thomas Darimont
	 */
	enum CompositionType {

		AND {
			@Override
			public Predicate combine(CriteriaBuilder builder, Predicate lhs, Predicate rhs) {
				return builder.and(lhs, rhs);
			}
		},

		OR {
			@Override
			public Predicate combine(CriteriaBuilder builder, Predicate lhs, Predicate rhs) {
				return builder.or(lhs, rhs);
			}
		};

		abstract Predicate combine(CriteriaBuilder builder, Predicate lhs, Predicate rhs);
	}

	static <T> Specification<T> negated(@Nullable Specification<T> spec) {
		return (root, query, builder) -> spec == null ? null : builder.not(spec.toPredicate(root, query, builder));
	}

	static <T> Specification<T> composed(@Nullable Specification<T> lhs, @Nullable Specification<T> rhs, CompositionType compositionType) {

		return (root, query, builder) -> {

			Predicate otherPredicate = rhs == null ? null : rhs.toPredicate(root, query, builder);
			Predicate thisPredicate = lhs == null ? null : lhs.toPredicate(root, query, builder);

			return thisPredicate == null ? otherPredicate
					: otherPredicate == null ? thisPredicate : compositionType.combine(builder, thisPredicate, otherPredicate);
		};
	}
}
