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
package org.springframework.data.jpa.domain.sample;

import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.JoinSpecification;
import org.springframework.data.jpa.domain.NestableSpecification;
import org.springframework.data.jpa.domain.Specification;

/**
 * Collection of {@link Specification}s for a {@link User}.
 *
 * @author Oliver Gierke
 * @author Diego Krupitza
 */
public class UserSpecifications {

	public static NestableSpecification<User> userHasFirstname(final String firstname) {

		return simplePropertySpec("firstname", firstname);
	}

	public static NestableSpecification<User> userHasLastname(final String lastname) {

		return simplePropertySpec("lastname", lastname);
	}

	public static NestableSpecification<User> userHasFirstnameLike(final String expression) {

		return (from, query, cb) -> cb.like(from.get("firstname").as(String.class), String.format("%%%s%%", expression));
	}

	public static NestableSpecification<User> userHasAgeLess(final Integer age) {

		return (from, query, cb) -> cb.lessThan(from.get("age").as(Integer.class), age);
	}

	public static NestableSpecification<User> userHasLastnameLikeWithSort(final String expression) {

		return (from, query, cb) -> {

			query.orderBy(cb.asc(from.get("firstname")));

			return cb.like(from.get("lastname").as(String.class), String.format("%%%s%%", expression));
		};
	}

	private static <T> NestableSpecification<T> simplePropertySpec(final String property, final Object value) {

		return (from, query, builder) -> builder.equal(from.get(property), value);
	}

	public static NestableSpecification<User> withManager(Specification<User> specification) {

		return new JoinSpecification<>(specification) {
			@Override
			protected Join<User, User> join(From<User, User> from) {
				return from.join("manager");
			}
		};
	}
}
