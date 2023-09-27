/*
 * Copyright 2023 the original author or authors.
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
package org.springframework.data.jpa.repository;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.jpa.domain.sample.UserSpecifications.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.sample.SampleEvaluationContextExtension.SampleSecurityContextHolder;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verify that {@link JpaSort#unsafe(String...)} works properly with Hibernate.
 *
 * @author Greg Turnquist
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:application-context.xml")
@Transactional
class JpaSortUnsafeHqlTests {

	@PersistenceContext EntityManager em;

	// CUT
	@Autowired UserRepository repository;

	// Test fixture
	private User firstUser;
	private User secondUser;
	private User thirdUser;
	private User fourthUser;
	private Integer id;
	private Role adminRole;

	@BeforeEach
	void setUp() throws Exception {

		firstUser = new User("Oliver", "Gierke", "gierke@synyx.de");
		firstUser.setAge(28);
		secondUser = new User("Joachim", "Arrasz", "arrasz@synyx.de");
		secondUser.setAge(35);
		Thread.sleep(10);
		thirdUser = new User("Dave", "Matthews", "no@email.com");
		thirdUser.setAge(43);
		fourthUser = new User("kevin", "raymond", "no@gmail.com");
		fourthUser.setAge(31);
		adminRole = new Role("admin");

		SampleSecurityContextHolder.clear();
	}

	void flushTestUsers() {

		em.persist(adminRole);

		firstUser = repository.save(firstUser);
		secondUser = repository.save(secondUser);
		thirdUser = repository.save(thirdUser);
		fourthUser = repository.save(fourthUser);

		repository.flush();

		id = firstUser.getId();

		assertThat(id).isNotNull();
		assertThat(secondUser.getId()).isNotNull();
		assertThat(thirdUser.getId()).isNotNull();
		assertThat(fourthUser.getId()).isNotNull();

		assertThat(repository.existsById(id)).isTrue();
		assertThat(repository.existsById(secondUser.getId())).isTrue();
		assertThat(repository.existsById(thirdUser.getId())).isTrue();
		assertThat(repository.existsById(fourthUser.getId())).isTrue();
	}

	@Test // GH-3172
	void unsafeFindAllWithPageRequest() {

		flushTestUsers();

		firstUser.setManager(firstUser);
		secondUser.setManager(firstUser);
		thirdUser.setManager(secondUser);
		fourthUser.setManager(secondUser);

		repository.saveAllAndFlush(List.of(firstUser, secondUser, thirdUser, fourthUser));

		// Generic function calls with one or more arguments
		assertThat(repository.findAll(PageRequest.of(0, 4, JpaSort.unsafe("LENGTH(firstname)")))).containsExactly(thirdUser,
				fourthUser, firstUser, secondUser);
		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("char_length(firstname)"))))
				.containsExactly(thirdUser, fourthUser, firstUser, secondUser);
		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("substring(emailAddress, 0, 3)"))))
				.containsExactly(secondUser, firstUser, thirdUser, fourthUser);
		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("repeat('a', 5)"))))
				.containsExactly(firstUser, secondUser, thirdUser, fourthUser);

		// Trim function call
		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("trim(leading '.' from lastname)"))))
				.containsExactly(secondUser, firstUser, thirdUser, fourthUser);

		// Grouped expression
		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("(firstname)"))))
				.containsExactly(thirdUser, secondUser, firstUser, fourthUser);

		// Tuple argument
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> {
			repository.findAll( //
					PageRequest.of(0, 4, JpaSort.unsafe("(firstname, lastname)")));
		});

		// Subquery
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> {
			repository.findAll( //
					PageRequest.of(0, 4, JpaSort.unsafe("(select e from Employee e)")));

		});

		// Literal expressions
		assertThatExceptionOfType(InvalidDataAccessResourceUsageException.class).isThrownBy(() -> {
			repository.findAll( //
					PageRequest.of(0, 4, JpaSort.unsafe("'a'")));
		});

		assertThatExceptionOfType(InvalidDataAccessResourceUsageException.class).isThrownBy(() -> {
			repository.findAll( //
					PageRequest.of(0, 4, JpaSort.unsafe("'abc'")));
		});

		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("5")))).containsExactly(firstUser, secondUser, thirdUser, fourthUser);

		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("length('a')"))))
				.containsExactly(firstUser, secondUser, thirdUser, fourthUser);

		// Parameters
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> {
			repository.findAll(PageRequest.of(0, 4, JpaSort.unsafe(":name")));
		});
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> {
			repository.findAll(PageRequest.of(0, 4, JpaSort.unsafe("?1")));
		});

		// Arithmetic calls
		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("LENGTH(firstname) + 5"))))
				.containsExactly(thirdUser, fourthUser, firstUser, secondUser);

		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("LENGTH(firstname) - 1"))))
				.containsExactly(thirdUser, fourthUser, firstUser, secondUser);

		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("LENGTH(firstname) * 5.0"))))
				.containsExactly(thirdUser, fourthUser, firstUser, secondUser);

		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("LENGTH(firstname) / 5.0"))))
				.containsExactly(thirdUser, firstUser, secondUser, fourthUser);

		// Concat operation
		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("firstname || lastname"))))
				.containsExactly(thirdUser, secondUser, firstUser, fourthUser);

		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("upper(firstname) || upper(lastname)"))))
				.containsExactly(thirdUser, secondUser, fourthUser, firstUser);

		// Path-based expression
		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("manager.firstname"))))
				.containsExactly(thirdUser, fourthUser, firstUser, secondUser);

		// Compound JpaOrder.unsafe operation
		assertThat(repository.findAll(PageRequest.of(0, 4, JpaSort.unsafe("LENGTH(lastname)") //
				.and(JpaSort.unsafe("LENGTH(firstname)").descending()))))
				.containsExactly(secondUser, firstUser, fourthUser, thirdUser);

		// Case-based expressions
		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("case firstname when 'Oliver' then 'A' else firstname end"))))
				.containsExactly(firstUser, thirdUser, secondUser, fourthUser);

		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("case firstname when 'Oliver' then 'z' else firstname end"))))
				.containsExactly(thirdUser, secondUser, fourthUser, firstUser);

		assertThat(repository.findAll( //
				PageRequest.of(0, 4,
						JpaSort.unsafe("case firstname when 'Oliver' then 'A' when 'Joachim' then 'z' else firstname end"))))
				.containsExactly(firstUser, thirdUser, fourthUser, secondUser);

		assertThat(repository.findAll( //
				PageRequest.of(0, 4,
						JpaSort.unsafe("case firstname when 'Oliver' then 'z' when 'Joachim' then 'A' else firstname end"))))
				.containsExactly(secondUser, thirdUser, fourthUser, firstUser);

		assertThat(repository.findAll( //
				PageRequest.of(0, 4,
						JpaSort.unsafe(
								"case when firstname = 'Oliver' then 'z' when firstname = 'Joachim' then 'A' else firstname end"))))
				.containsExactly(secondUser, thirdUser, fourthUser, firstUser);

		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("case when age < 31 then 'A' else firstname end"))))
				.containsExactly(firstUser, thirdUser, secondUser, fourthUser);

		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("case when age <= 31 then 'A' else firstname end"))))
				.containsExactly(firstUser, fourthUser, thirdUser, secondUser);

		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("case when age > 42 then 'A' else firstname end"))))
				.containsExactly(thirdUser, secondUser, firstUser, fourthUser);

		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("case when age >= 43 then 'A' else firstname end"))))
				.containsExactly(thirdUser, secondUser, firstUser, fourthUser);

		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("case when age <> 28 then 'A' else firstname end"))))
				.containsExactly(secondUser, thirdUser, fourthUser, firstUser);
		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("case when age != 28 then 'A' else firstname end"))))
				.containsExactly(secondUser, thirdUser, fourthUser, firstUser);
		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("case when age ^= 28 then 'A' else firstname end"))))
				.containsExactly(secondUser, thirdUser, fourthUser, firstUser);

		// Hibernate doesn't support using function calls inside case predicates.
		assertThatExceptionOfType(InvalidDataAccessResourceUsageException.class).isThrownBy(() -> {
			repository.findAll( //
					PageRequest.of(0, 4, JpaSort.unsafe("case when LENGTH(firstname) = 6 then 'A' else firstname end")));
		});

		// Case when IS NOT? NULL expressions
		firstUser.setManager(null);
		secondUser.setManager(firstUser);
		thirdUser.setManager(firstUser);
		fourthUser.setManager(firstUser);

		repository.saveAllAndFlush(List.of(firstUser, secondUser, thirdUser, fourthUser));

		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("case when manager is null then 'A' else firstname end"))))
				.containsExactly(firstUser, thirdUser, secondUser, fourthUser);

		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("case when manager is not null then 'A' else firstname end"))))
				.containsExactly(secondUser, thirdUser, fourthUser, firstUser);

		// Case IS NOT? DISTINCT FROM expression
		firstUser.setLastname(firstUser.getFirstname());
		repository.saveAndFlush(firstUser);

		assertThat(repository.findAll( //
				PageRequest.of(0, 4,
						JpaSort.unsafe("case when firstname is distinct from lastname then 'A' else firstname end"))))
				.containsExactly(secondUser, thirdUser, fourthUser, firstUser);

		assertThat(repository.findAll( //
				PageRequest.of(0, 4,
						JpaSort.unsafe("case when firstname is not distinct from lastname then 'A' else firstname end"))))
				.containsExactly(firstUser, thirdUser, secondUser, fourthUser);

		// Case NOT? IN
		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("case when firstname in ('Oliver', 'Dave') then 'A' else firstname end"))))
				.containsExactly(firstUser, thirdUser, secondUser, fourthUser);

		assertThat(repository.findAll( //
				PageRequest.of(0, 4,
						JpaSort.unsafe("case when firstname not in ('Oliver', 'Dave') then 'A' else firstname end"))))
				.containsExactly(secondUser, fourthUser, thirdUser, firstUser);

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> {
			repository.findAll( //
					PageRequest.of(0, 4,
							JpaSort.unsafe("case when firstname in ELEMENTS (manager.firstname) then 'A' else firstname end")));
		});

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> {
			repository.findAll( //
					PageRequest.of(0, 4,
							JpaSort.unsafe("case when firstname in (select u.firstname from User u) then 'A' else firstname end")));
		});

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> {
			repository.findAll( //
					PageRequest.of(0, 4, JpaSort.unsafe("case when firstname in :names then 'A' else firstname end")));
		});

		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("case when age between 25 and 30 then 'A' else firstname end"))))
				.containsExactly(firstUser, thirdUser, secondUser, fourthUser);

		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("case when age not between 25 and 30 then 'A' else firstname end"))))
				.containsExactly(secondUser, thirdUser, fourthUser, firstUser);

		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("case when firstname like 'O%' then 'A' else firstname end"))))
				.containsExactly(firstUser, thirdUser, secondUser, fourthUser);

		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("case when firstname not like 'O%' then 'A' else firstname end"))))
				.containsExactly(secondUser, thirdUser, fourthUser, firstUser);

		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("case when firstname ilike 'O%' then 'A' else firstname end"))))
				.containsExactly(firstUser, thirdUser, secondUser, fourthUser);

		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("case when firstname not ilike 'O%' then 'A' else firstname end"))))
				.containsExactly(secondUser, thirdUser, fourthUser, firstUser);

		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("case when firstname like 'O%' escape '^' then 'A' else firstname end"))))
				.containsExactly(firstUser, thirdUser, secondUser, fourthUser);

		assertThat(repository.findAll( //
				PageRequest.of(0, 4,
						JpaSort.unsafe("case when firstname not like 'O%' escape '^' then 'A' else firstname end"))))
				.containsExactly(secondUser, thirdUser, fourthUser, firstUser);

		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("case when firstname ilike 'O%' escape '^' then 'A' else firstname end"))))
				.containsExactly(firstUser, thirdUser, secondUser, fourthUser);

		assertThat(repository.findAll( //
				PageRequest.of(0, 4,
						JpaSort.unsafe("case when firstname not ilike 'O%' escape '^' then 'A' else firstname end"))))
				.containsExactly(secondUser, thirdUser, fourthUser, firstUser);

		//
		// Also works with custom Specification
		//

		Specification<User> spec = userHasFirstname("Oliver").or(userHasLastname("Matthews"));

		assertThat(repository.findAll(spec, //
				PageRequest.of(0, 4, JpaSort.unsafe("LENGTH(firstname)")))).containsExactly(thirdUser, firstUser);
	}
}
