/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.jpa.repository;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.sample.Ticket;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.sample.TicketRepository;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Thomas Darimont
 */
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class NestedEntityNavigationRepositoryTests {

	@Configuration
	@ImportResource({ "classpath:infrastructure.xml", "classpath:hibernate.xml" })
	@EnableJpaRepositories
	static class Config {}

	@Autowired UserRepository userRepository;
	@Autowired TicketRepository ticketRepository;

	// Test fixture
	User firstUser, secondUser, thirdUser, fourthUser;
	Integer id;

	@Before
	public void setUp() throws Exception {

		firstUser = new User("Oliver", "Gierke", "gierke@synyx.de");
		firstUser.setAge(28);
		secondUser = new User("Joachim", "Arrasz", "arrasz@synyx.de");
		secondUser.setAge(35);
		Thread.sleep(10);
		thirdUser = new User("Dave", "Matthews", "no@email.com");
		thirdUser.setAge(43);
		fourthUser = new User("kevin", "raymond", "no@gmail.com");
		fourthUser.setAge(31);
	}

	protected void flushTestUsers() {

		firstUser = userRepository.save(firstUser);
		secondUser = userRepository.save(secondUser);
		thirdUser = userRepository.save(thirdUser);
		fourthUser = userRepository.save(fourthUser);

		userRepository.flush();

		id = firstUser.getId();

		assertThat(id, is(notNullValue()));
		assertThat(secondUser.getId(), is(notNullValue()));
		assertThat(thirdUser.getId(), is(notNullValue()));
		assertThat(fourthUser.getId(), is(notNullValue()));

		assertThat(userRepository.exists(id), is(true));
		assertThat(userRepository.exists(secondUser.getId()), is(true));
		assertThat(userRepository.exists(thirdUser.getId()), is(true));
		assertThat(userRepository.exists(fourthUser.getId()), is(true));
	}

	/**
	 * @see DATAJPA-346
	 */
	@Test
	public void findAllWithPaginationOnNestedPropertyPath() {

		flushTestUsers();

		ticketRepository.save(new Ticket(0L, null)); // no user assigned to ticket
		ticketRepository.save(new Ticket(1L, firstUser));
		ticketRepository.save(new Ticket(2L, secondUser));
		ticketRepository.save(new Ticket(3L, firstUser));
		ticketRepository.save(new Ticket(4L, secondUser));
		ticketRepository.save(new Ticket(5L, thirdUser));

		List<Ticket> result = ticketRepository.findAll();
		assertThat(result.size(), is(6));

		Page<Ticket> pages = ticketRepository.findAll(new PageRequest(0, 3, new Sort(Sort.Direction.ASC,
				"assignedTo.firstname")));
		assertThat(pages.getSize(), is(3));
		assertThat(pages.getContent().get(0).getAssignedTo(), is(nullValue()));
		assertThat(pages.getContent().get(1).getAssignedTo().getFirstname(), is("Dave"));
		assertThat(pages.getContent().get(2).getAssignedTo().getFirstname(), is("Joachim"));
		assertThat(pages.getTotalElements(), is(6L));
	}
}
