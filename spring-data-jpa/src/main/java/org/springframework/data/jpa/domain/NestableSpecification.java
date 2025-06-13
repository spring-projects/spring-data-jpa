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

import jakarta.persistence.criteria.*;
import org.springframework.lang.Nullable;

import java.io.Serializable;

/**
 * Specification in the sense of Domain Driven Design.
 * <p>
 * A {@link Specification} working with {@link From} instead of {@link Root}.
 *
 * @author Sven Meier
 */
@FunctionalInterface
public interface NestableSpecification<T> extends Specification<T> {

	@Nullable
	default Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
		return toPredicate((From)root, query, criteriaBuilder);
	}

	/**
	 * Creates a WHERE clause for a query of the referenced entity in form of a {@link Predicate} for the given
	 * {@link From} and {@link CriteriaBuilder}.
	 *
	 * @param from must not be {@literal null}.
	 * @param query the criteria query.
	 * @param criteriaBuilder must not be {@literal null}.
	 * @return a {@link Predicate}, may be {@literal null}.
	 */
	@Nullable
	Predicate toPredicate(From<T, T> from, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder);

}
