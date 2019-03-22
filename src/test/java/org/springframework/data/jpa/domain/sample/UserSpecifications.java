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
package org.springframework.data.jpa.domain.sample;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

/**
 * Collection of {@link Specification}s for a {@link User}.
 *
 * @author Oliver Gierke
 */
public class UserSpecifications {

	/**
	 * A {@link Specification} to match on a {@link User}'s firstname.
	 *
	 * @param firstname
	 * @return
	 */
	public static Specification<User> userHasFirstname(final String firstname) {

		return simplePropertySpec("firstname", firstname);
	}

	/**
	 * A {@link Specification} to match on a {@link User}'s lastname.
	 *
	 * @param firstname
	 * @return
	 */
	public static Specification<User> userHasLastname(final String lastname) {

		return simplePropertySpec("lastname", lastname);
	}

	/**
	 * A {@link Specification} to do a like-match on a {@link User}'s firstname.
	 *
	 * @param firstname
	 * @return
	 */
	public static Specification<User> userHasFirstnameLike(final String expression) {

		return new Specification<User>() {

			public Predicate toPredicate(Root<User> root, CriteriaQuery<?> query, CriteriaBuilder cb) {

				return cb.like(root.get("firstname").as(String.class), String.format("%%%s%%", expression));
			}
		};
	}

	/**
	 * A {@link Specification} to do a like-match on a {@link User}'s lastname but also adding a sort order on the
	 * firstname.
	 *
	 * @param firstname
	 * @return
	 */
	public static Specification<User> userHasLastnameLikeWithSort(final String expression) {

		return new Specification<User>() {

			public Predicate toPredicate(Root<User> root, CriteriaQuery<?> query, CriteriaBuilder cb) {

				query.orderBy(cb.asc(root.get("firstname")));

				return cb.like(root.get("lastname").as(String.class), String.format("%%%s%%", expression));
			}
		};
	}

	private static <T> Specification<T> simplePropertySpec(final String property, final Object value) {

		return new Specification<T>() {

			public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {

				return builder.equal(root.get(property), value);
			}
		};
	}
}
