/*
 * Copyright 2008-present the original author or authors.
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
package org.springframework.data.jpa.domain.sample;

import org.springframework.data.jpa.domain.PredicateSpecification;
import org.springframework.data.jpa.domain.Specification;

/**
 * Collection of {@link Specification}s for a {@link User}.
 *
 * @author Oliver Gierke
 * @author Diego Krupitza
 */
public class UserSpecifications {

	public static PredicateSpecification<User> userHasFirstname(String firstname) {

		return simplePropertySpec("firstname", firstname);
	}

	public static PredicateSpecification<User> userHasLastname(String lastname) {

		return simplePropertySpec("lastname", lastname);
	}

	public static PredicateSpecification<User> userHasFirstnameLike(String expression) {

		return (root, cb) -> cb.like(root.get("firstname").as(String.class), String.format("%%%s%%", expression));
	}

	public static PredicateSpecification<User> userHasAgeLess(Integer age) {

		return (root, cb) -> cb.lessThan(root.get("age").as(Integer.class), age);
	}

	public static Specification<User> userHasLastnameLikeWithSort(String expression) {

		return (root, query, cb) -> {

			query.orderBy(cb.asc(root.get("firstname")));

			return cb.like(root.get("lastname").as(String.class), String.format("%%%s%%", expression));
		};
	}

	private static <T> PredicateSpecification<T> simplePropertySpec(String property, Object value) {

		return (from, builder) -> builder.equal(from.get(property), value);
	}
}
