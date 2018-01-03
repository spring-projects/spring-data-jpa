/*
 * Copyright 2013-2018 the original author or authors.
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

/**
 * Custom repository interface that adjusts the querys of well known repository interface methods via {@link Query}
 * annotation.
 *
 * @author Thomas Darimont
 */
public interface RedeclaringRepositoryMethodsRepository extends CrudRepository<User, Long> {

	/**
	 * Should not find any users at all.
	 */
	@Query("SELECT u FROM User u where u.id = -1")
	List<User> findAll();

	/**
	 * Should only find users with the firstname 'Oliver'.
	 *
	 * @param page
	 * @return
	 */
	@Query("SELECT u FROM User u where u.firstname = 'Oliver'")
	Page<User> findAll(Pageable page);
}
