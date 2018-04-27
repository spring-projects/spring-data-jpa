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

import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.domain.sample.EmbeddedIdExampleEmployee;
import org.springframework.data.jpa.domain.sample.EmbeddedIdExampleEmployeePK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;

/**
 * Demonstrates the support for composite primary keys with {@code @EmbeddedId}.
 *
 * @author Thomas Darimont
 * @author Mark Paluch
 */
@Lazy
public interface EmployeeRepositoryWithEmbeddedId
		extends JpaRepository<EmbeddedIdExampleEmployee, EmbeddedIdExampleEmployeePK>,
		QuerydslPredicateExecutor<EmbeddedIdExampleEmployee> {

	@Override
	List<EmbeddedIdExampleEmployee> findAll(Predicate predicate, OrderSpecifier<?>... orders);

	// DATAJPA-920
	boolean existsByName(String name);
}
