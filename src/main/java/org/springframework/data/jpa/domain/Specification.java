/*
 * Copyright 2008-2018 the original author or authors.
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
@SuppressWarnings("deprecation")
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
	static <T> Specification<T> not(Specification<T> spec) {
		return Specifications.negated(spec);
	}

	/**
	 * Simple static factory method to add some syntactic sugar around a {@link Specification}.
	 *
	 * @param <T>
	 * @param spec can be {@literal null}.
	 * @return
	 * @since 2.0
	 */
	static <T> Specification<T> where(Specification<T> spec) {
		return Specifications.where(spec);
	}

	/**
	 * ANDs the given {@link Specification} to the current one.
	 *
	 * @param other can be {@literal null}.
	 * @return The conjunction of the specifications
	 * @since 2.0
	 */
	default Specification<T> and(Specification<T> other) {
		return Specifications.composed(this, other, AND);
	}

	/**
	 * ORs the given specification to the current one.
	 *
	 * @param other can be {@literal null}.
	 * @return The disjunction of the specifications
	 * @since 2.0
	 */
	default Specification<T> or(Specification<T> other) {
		return Specifications.composed(this, other, OR);
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
