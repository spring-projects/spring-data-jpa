/*
 * Copyright 2008-2018 the original author or authors.
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
package org.springframework.data.jpa.repository.custom;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.domain.sample.User;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom Extended repository interface for a {@code User}. This relies on the custom intermediate repository interface
 * {@link CustomGenericRepository}.
 *
 * @author Oliver Gierke
 */
public interface UserCustomExtendedRepository extends CustomGenericRepository<User, Integer> {

	/**
	 * Sample method to test reconfiguring transactions on CRUD methods in combination with custom factory.
	 */
	@Transactional(readOnly = false, timeout = 10)
	List<User> findAll();

	@Transactional(readOnly = false, timeout = 10)
	Optional<User> findById(Integer id);
}
