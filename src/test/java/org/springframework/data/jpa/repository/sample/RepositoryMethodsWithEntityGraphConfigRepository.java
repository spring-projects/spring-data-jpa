/*
 * Copyright 2014-2015 the original author or authors.
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
package org.springframework.data.jpa.repository.sample;

import java.util.List;

import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

/**
 * Custom repository interface that customizes the fetching behavior of querys of well known repository interface
 * methods via {@link EntityGraph} annotation.
 * 
 * @author Thomas Darimont
 */
public interface RepositoryMethodsWithEntityGraphConfigRepository extends CrudRepository<User, Integer> {

	/**
	 * Should find all users.
	 */
	@EntityGraph(type = EntityGraphType.LOAD, value = "User.overview")
	List<User> findAll();

	/**
	 * Should fetch all user details
	 */
	@EntityGraph(type = EntityGraphType.FETCH, value = "User.detail")
	User findOne(Integer id);

	/**
	 * @see DATAJPA-696
	 */
	@EntityGraph
	User getOneWithDefinedEntityGraphById(Integer id);
	
	/**
	 * @see DATAJPA-696
	 */
	@EntityGraph(attributePaths = { "roles", "colleagues.roles" })
	User getOneWithAttributeNamesById(Integer id);
}
