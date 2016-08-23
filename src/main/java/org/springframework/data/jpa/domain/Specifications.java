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

import static org.springframework.data.jpa.domain.Specifications.CompositionType.*;

import java.io.Serializable;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.util.Assert;

/**
 * Helper class to easily combine {@link Specification} instances.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class Specifications<T> implements Specification<T>, Serializable {

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
	public Specifications<T> and(Specification<T> other) {
		return new Specifications<T>(new ComposedSpecification<T>(spec, other, AND));
	}

	/**
	 * ORs the given specification to the current one.
	 * 
	 * @param <T>
	 * @param other can be {@literal null}.
	 * @return
	 */
	public Specifications<T> or(Specification<T> other) {
		return new Specifications<T>(new ComposedSpecification<T>(spec, other, OR));
	}

	/**
	 * Negates the given {@link Specification}.
	 * 
	 * @param <T>
	 * @param spec can be {@literal null}.
	 * @return
	 */
	public static <T> Specifications<T> not(Specification<T> spec) {
		return new Specifications<T>(new NegatedSpecification<T>(spec));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.domain.Specification#toPredicate(javax.persistence.criteria.Root, javax.persistence.criteria.CriteriaQuery, javax.persistence.criteria.CriteriaBuilder)
	 */
	public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		return spec == null ? null : spec.toPredicate(root, query, builder);
	}

	/**
	 * Enum for the composition types for {@link Predicate}s.
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

	/**
	 * A {@link Specification} that negates a given {@code Specification}.
	 * 
	 * @author Thomas Darimont
	 * @since 1.6
	 */
	private static class NegatedSpecification<T> implements Specification<T>, Serializable {

		private static final long serialVersionUID = 1L;

		private final Specification<T> spec;

		/**
		 * Creates a new {@link NegatedSpecification} from the given {@link Specification}
		 * 
		 * @param spec may be {@iteral null}
		 */
		public NegatedSpecification(Specification<T> spec) {
			this.spec = spec;
		}

		public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
			return spec == null ? null : builder.not(spec.toPredicate(root, query, builder));
		}
	}

	/**
	 * A {@link Specification} that combines two given {@code Specification}s via a given {@link CompositionType}.
	 * 
	 * @author Thomas Darimont
	 * @since 1.6
	 */
	private static class ComposedSpecification<T> implements Specification<T>, Serializable {

		private static final long serialVersionUID = 1L;

		private final Specification<T> lhs;
		private final Specification<T> rhs;
		private final CompositionType compositionType;

		/**
		 * Creates a new {@link ComposedSpecification} from the given {@link Specification} for the left-hand-side and the
		 * right-hand-side with the given {@link CompositionType}.
		 * 
		 * @param lhs may be {@literal null}
		 * @param rhs may be {@literal null}
		 * @param compositionType must not be {@literal null}
		 */
		private ComposedSpecification(Specification<T> lhs, Specification<T> rhs, CompositionType compositionType) {

			Assert.notNull(compositionType, "CompositionType must not be null!");

			this.lhs = lhs;
			this.rhs = rhs;
			this.compositionType = compositionType;
		}

		/**
		 * Returns {@link Predicate} for the given {@link Root} and {@link CriteriaQuery} that is constructed via the given
		 * {@link CriteriaBuilder}.
		 */
		public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {

			Predicate otherPredicate = rhs == null ? null : rhs.toPredicate(root, query, builder);
			Predicate thisPredicate = lhs == null ? null : lhs.toPredicate(root, query, builder);

			return thisPredicate == null ? otherPredicate : otherPredicate == null ? thisPredicate : this.compositionType
					.combine(builder, thisPredicate, otherPredicate);
		}
	}
}
