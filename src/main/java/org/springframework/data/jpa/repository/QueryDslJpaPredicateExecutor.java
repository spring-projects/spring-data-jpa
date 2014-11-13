/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.jpa.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;

import com.mysema.query.types.FactoryExpression;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.Projections;

/**
 * Interface to allow execution of QueryDsl {@link Predicate} with support for JPA specific projections via
 * {@link FactoryExpression}s.
 * <p>
 * Projections can be defined via the querydsl {@link Projections} class:
 * {@code
 * QBean<User> projection = Projections.bean(User.class, user.firstname);
 * }
 * 
 * @author Thomas Darimont
 * @since 1.8
 */
public interface QueryDslJpaPredicateExecutor<T> extends QueryDslPredicateExecutor<T> {

	/**
	 * Returns all entities matching the given {@link Predicate} with the fields populated by the given
	 * {@link FactoryExpression}.
	 * 
	 * @param projection
	 * @param predicate
	 * @return all entities matching the given {@link Predicate}.
	 */
	List<T> findAll(FactoryExpression<T> projection, Predicate predicate);

	/**
	 * Returns all entities matching the given {@link Predicate} with the fields populated by the given
	 * {@link FactoryExpression} ordered by the given {@link OrderSpecifier}s.
	 * 
	 * @param projection
	 * @param predicate
	 * @param orders
	 * @return all entities matching the given {@link Predicate}.
	 */
	List<T> findAll(FactoryExpression<T> projection, Predicate predicate, OrderSpecifier<?>... orders);

	/**
	 * Returns a {@link Page} of entities matching the given {@link Predicate} with the fields populated by the given
	 * {@link FactoryExpression}. In case no match could be found, an empty {@link Page} is returned.
	 * 
	 * @param projection
	 * @param predicate
	 * @param pageable
	 * @return a {@link Page} of entities matching the given {@link Predicate}.
	 */
	Page<T> findAll(FactoryExpression<T> projection, Predicate predicate, Pageable pageable);
}
