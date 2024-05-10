/*
 * Copyright 2008-2024 the original author or authors.
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

import org.springframework.data.jpa.domain.Specification;

/**
 * Collection of {@link Specification}s for a {@link User}.
 *
 * @author Oliver Gierke
 * @author Diego Krupitza
 */
public class UserSpecifications {

	public static Specification<User> userHasFirstname(final String firstname) {

		return simplePropertySpec("firstname", firstname);
	}

	public static Specification<User> userHasLastname(final String lastname) {

		return simplePropertySpec("lastname", lastname);
	}

	public static Specification<User> userHasFirstnameLike(final String expression) {

		return (root, query, cb) -> cb.like(root.get("firstname").as(String.class), String.format("%%%s%%", expression));
	}

	public static Specification<User> userHasAgeLess(final Integer age) {

		return (root, query, cb) -> cb.lessThan(root.get("age").as(Integer.class), age);
	}

	public static Specification<User> userHasLastnameLikeWithSort(final String expression) {

		return (root, query, cb) -> {

			query.orderBy(cb.asc(root.get("firstname")));

			return cb.like(root.get("lastname").as(String.class), String.format("%%%s%%", expression));
		};
	}

	private static <T> Specification<T> simplePropertySpec(final String property, final Object value) {

		return (root, query, builder) -> builder.equal(root.get(property), value);
	}
}
