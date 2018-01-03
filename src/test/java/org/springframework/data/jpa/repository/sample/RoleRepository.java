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
package org.springframework.data.jpa.repository.sample;

import javax.persistence.LockModeType;
import javax.persistence.QueryHint;

import java.util.Optional;

import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;

import com.querydsl.core.types.Predicate;

/**
 * Typing interface for {@code Role}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public interface RoleRepository extends CrudRepository<Role, Integer>, QuerydslPredicateExecutor<Role> {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.CrudRepository#findAll()
	 */
	@Lock(LockModeType.READ)
	@QueryHints(@QueryHint(name = "foo", value = "bar"))
	Iterable<Role> findAll();

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.CrudRepository#findOne(java.io.Serializable)
	 */
	@Lock(LockModeType.READ)
	@QueryHints(@QueryHint(name = "foo", value = "bar"))
	Optional<Role> findById(Integer id);

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#findOne(com.mysema.query.types.Predicate)
	 */
	@Override
	@Lock(LockModeType.READ)
	@QueryHints(@QueryHint(name = "foo", value = "bar"))
	Optional<Role> findOne(Predicate predicate);

	// DATAJPA-509
	long countByName(String name);
}
