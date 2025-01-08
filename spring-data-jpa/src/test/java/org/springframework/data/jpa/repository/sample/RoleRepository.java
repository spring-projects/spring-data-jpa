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
package org.springframework.data.jpa.repository.sample;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import com.querydsl.core.types.Predicate;

/**
 * Typing interface for {@code Role}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Yanming Zhou
 */
public interface RoleRepository extends JpaRepository<Role, Integer>, QuerydslPredicateExecutor<Role> {

	@Override
	@Lock(LockModeType.READ)
	@QueryHints(@QueryHint(name = "foo", value = "bar"))
	List<Role> findAll();

	@Override
	@Lock(LockModeType.READ)
	@QueryHints(@QueryHint(name = "foo", value = "bar"))
	Optional<Role> findById(Integer id);

	@Override
	@Lock(LockModeType.READ)
	@QueryHints(@QueryHint(name = "foo", value = "bar"))
	Optional<Role> findOne(Predicate predicate);

	// DATAJPA-509
	long countByName(String name);
}
