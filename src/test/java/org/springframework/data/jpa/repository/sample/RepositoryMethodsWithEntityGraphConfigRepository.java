/*
 * Copyright 2014-2019 the original author or authors.
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.Nullable;

import com.querydsl.core.types.Predicate;

/**
 * Custom repository interface that customizes the fetching behavior of querys of well known repository interface
 * methods via {@link EntityGraph} annotation.
 *
 * @author Thomas Darimont
 * @author Jocelyn Ntakpe
 * @author Christoph Strobl
 * @author Jens Schauder
 */
public interface RepositoryMethodsWithEntityGraphConfigRepository
		extends CrudRepository<User, Integer>, QuerydslPredicateExecutor<User>, JpaSpecificationExecutor {

	/**
	 * Should find all users.
	 */
	@EntityGraph(type = EntityGraphType.LOAD, value = "User.overview")
	List<User> findAll();

	/**
	 * Should fetch all user details
	 */
	@EntityGraph(type = EntityGraphType.FETCH, value = "User.detail")
	Optional<User> findById(Integer id);

	// DATAJPA-696
	@EntityGraph
	User getOneWithDefinedEntityGraphById(Integer id);

	// DATAJPA-696
	@EntityGraph(attributePaths = { "roles", "colleagues.roles" })
	User getOneWithAttributeNamesById(Integer id);

	// DATAJPA-790
	@EntityGraph("User.detail")
	Page<User> findAll(Predicate predicate, Pageable pageable);

	// DATAJPA-1207
	@Override
	@EntityGraph("User.detail")
	Page<User> findAll(@Nullable Specification spec, Pageable pageable);

	// DATAJPA-1041
	@EntityGraph(type = EntityGraphType.FETCH, value = "User.withSubGraph")
	User findOneWithMultipleSubGraphsUsingNamedEntityGraphById(Integer id);

	// DATAJPA-1041
	@EntityGraph(attributePaths = { "colleagues", "colleagues.roles", "colleagues.colleagues" })
	User findOneWithMultipleSubGraphsById(Integer id);

	// DATAJPA-1041, DATAJPA-1075
	@EntityGraph(attributePaths = { "colleagues", "colleagues.roles", "colleagues.colleagues.roles" })
	User findOneWithDeepGraphById(Integer id);
}
