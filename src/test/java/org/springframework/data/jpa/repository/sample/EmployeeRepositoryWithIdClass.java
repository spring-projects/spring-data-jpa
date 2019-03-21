/*
 * Copyright 2013-2019 the original author or authors.
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

import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.domain.sample.IdClassExampleEmployee;
import org.springframework.data.jpa.domain.sample.IdClassExampleEmployeePK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;

/**
 * Demonstrates the support for composite primary keys with {@code @IdClass}.
 *
 * @author Thomas Darimont
 * @author Mark Paluch
 */
@Lazy
public interface EmployeeRepositoryWithIdClass extends JpaRepository<IdClassExampleEmployee, IdClassExampleEmployeePK>,
		QuerydslPredicateExecutor<IdClassExampleEmployee> {

	List<IdClassExampleEmployee> findAll(Predicate predicate, OrderSpecifier<?>... orders);

	// DATAJPA-920
	boolean existsByName(String name);
}
