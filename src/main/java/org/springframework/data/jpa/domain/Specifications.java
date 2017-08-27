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

import java.io.Serializable;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import static org.springframework.data.jpa.domain.Specification.CompositionType.*;

/**
 * Helper class to easily combine {@link Specification} instances.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Sebastian Staudt
 */
public class Specifications<T> implements Specification<T>, Serializable {

	private static final long serialVersionUID = 1L;

	private final Specification<T> spec;

	/**
	 * Creates a new {@link Specifications} wrapper for the given {@link Specification}.
	 *
	 * @param spec can be {@literal null}.
	 */
	Specifications(Specification<T> spec) {
		this.spec = spec;
	}

	/**
	 * Simple static factory method to add some syntactic sugar around a {@link Specification}.
	 *
	 * @deprecated Use {@link Specification#where} instead
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
	 * @deprecated Use {@link Specification#and} instead
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
	 * @deprecated Use {@link Specification#or} instead
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
	 * @deprecated Use {@link Specification#not} instead
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

}
