/*
 * Copyright 2018-2023 the original author or authors.
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

import java.io.Serializable;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.lang.Nullable;

/**
 * Helper class to support specification compositions.
 *
 * @author Sebastian Staudt
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Mark Paluch
 * @see Specification
 * @since 2.2
 */
class SpecificationComposition {

	interface Combiner extends Serializable {
		Predicate combine(CriteriaBuilder builder, @Nullable Predicate lhs, @Nullable Predicate rhs);
	}

	static <T> Specification<T> composed(@Nullable Specification<T> lhs, @Nullable Specification<T> rhs,
			Combiner combiner) {

		return (root, query, builder) -> {

			Predicate thisPredicate = toPredicate(lhs, root, query, builder);
			Predicate otherPredicate = toPredicate(rhs, root, query, builder);

			if (thisPredicate == null) {
				return otherPredicate;
			}

			return otherPredicate == null ? thisPredicate : combiner.combine(builder, thisPredicate, otherPredicate);
		};
	}

	@Nullable
	private static <T> Predicate toPredicate(@Nullable Specification<T> specification, Root<T> root, CriteriaQuery<?> query,
			CriteriaBuilder builder) {
		return specification == null ? null : specification.toPredicate(root, query, builder);
	}
}
