/*
 * Copyright 2008-2023 the original author or authors.
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
package org.springframework.data.jpa.repository.sample;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Meta;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.FluentQuery;

import com.querydsl.core.types.Predicate;

/**
 * Typed repository for {@link Role} but with {@link Meta} annotations applied.
 *
 * @author Greg Turnquist
 * @since 3.0
 */
public interface RoleRepositoryWithMeta extends JpaRepository<Role, Integer>, QuerydslPredicateExecutor<Role> {

	// Finders

	@Override
	@Meta(comment = "foobar")
	List<Role> findAll();

	@Override
	@Meta(comment = "foobar")
	Optional<Role> findById(Integer id);

	@Override
	@Meta(comment = "foobar")
	Optional<Role> findOne(Predicate predicate);

	@Meta(comment = "foobar")
	List<Role> findByName(String name);

	@Override
	@Meta(comment = "foobar")
	<S extends Role> Optional<S> findOne(Example<S> example);

	@Override
	@Meta(comment = "foobar")
	<S extends Role> List<S> findAll(Example<S> example);

	@Override
	@Meta(comment = "foobar")
	<S extends Role> List<S> findAll(Example<S> example, Sort sort);

	@Override
	@Meta(comment = "foobar")
	<S extends Role, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction);

	// counters

	@Override
	@Meta(comment = "foobar")
	long count();

	@Meta(comment = "foobar")
	long countByName(String name);

	// exists

	@Override
	@Meta(comment = "foobar")
	boolean existsById(Integer integer);

	@Override
	@Meta(comment = "foobar")
	<S extends Role> boolean exists(Example<S> example);

	// delete

	@Override
	@Meta(comment = "foobar")
	void deleteAllInBatch();

	@Override
	@Meta(comment = "foobar")
	void deleteAllByIdInBatch(Iterable<Integer> integers);
}
