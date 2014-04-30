/*
 * Copyright 2008-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.domain;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 * Helper class to easily combine {@link Specification} instances.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class Specifications<T> implements Specification<T> {

	private static final long serialVersionUID = 1L;

	private final Specification<T> spec;

	/**
	 * Creates a new {@link Specifications} wrapper for the given {@link Specification}.
	 * 
	 * @param spec can be {@literal null}.
	 */
	private Specifications(Specification<T> spec) {
		this.spec = spec;
	}

	/**
	 * Simple static factory method to add some syntactic sugar around a {@link Specification}.
	 * 
	 * @param <T>
	 * @param spec can be {@literal null}.
	 * @return
	 */
	public static <T> Specifications<T> where(Specification<T> spec) {
		return new Specifications<T>(spec);
	}

	/**
	 * ANDs the given {@link Specification} to the current one.
	 * 
	 * @param <T>
	 * @param other can be {@literal null}.
	 * @return
	 */
	public Specifications<T> and(final Specification<T> other) {

		return new Specifications<T>(new Specification<T>() {
			public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {

				Predicate otherPredicate = other == null ? null : other.toPredicate(root, query, builder);
				Predicate thisPredicate = spec == null ? null : spec.toPredicate(root, query, builder);

				return thisPredicate == null ? otherPredicate : otherPredicate == null ? thisPredicate : builder.and(
						thisPredicate, otherPredicate);
			}
		});
	}

	/**
	 * ORs the given specification to the current one.
	 * 
	 * @param <T>
	 * @param other can be {@literal null}.
	 * @return
	 */
	public Specifications<T> or(final Specification<T> other) {

		return new Specifications<T>(new Specification<T>() {
			public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {

				Predicate otherPredicate = other == null ? null : other.toPredicate(root, query, builder);
				Predicate thisPredicate = spec == null ? null : spec.toPredicate(root, query, builder);

				return thisPredicate == null ? otherPredicate : otherPredicate == null ? thisPredicate : builder.or(
						thisPredicate, otherPredicate);
			}
		});
	}

	/**
	 * Negates the given {@link Specification}.
	 * 
	 * @param <T>
	 * @param spec can be {@literal null}.
	 * @return
	 */
	public static <T> Specifications<T> not(final Specification<T> spec) {
		return new Specifications<T>(new Specification<T>() {
			public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
				return spec == null ? null : builder.not(spec.toPredicate(root, query, builder));
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.domain.Specification#toPredicate(javax.persistence.criteria.Root, javax.persistence.criteria.CriteriaQuery, javax.persistence.criteria.CriteriaBuilder)
	 */
	public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		return spec == null ? null : spec.toPredicate(root, query, builder);
	}
}
