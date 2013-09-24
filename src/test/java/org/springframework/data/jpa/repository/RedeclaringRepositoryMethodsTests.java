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

import static org.hamcrest.Matchers.*;
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
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.sample.RedeclaringRepositoryMethodsRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Thomas Darimont
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
public class RedeclaringRepositoryMethodsTests {

	@Configuration
	@ImportResource("classpath:infrastructure.xml")
	@EnableJpaRepositories
	static class Config {}

	@Autowired RedeclaringRepositoryMethodsRepository repository;

	User olli;
	User tom;

	@Before
	public void setup() {
		olli = new User("Oliver", "Gierke", "ogierke@gopivotal.com");
		tom = new User("Thomas", "Darimont", "tdarimont@gopivotal.com");
	}

	/**
	 * @see DATAJPA-398
	 */
	@Test
	public void adjustedWellKnownPagedFindAllMethodShouldReturnOnlyTheUserWithFirstnameOliver() {

		olli = repository.save(olli);
		tom = repository.save(tom);

		Page<User> page = repository.findAll(new PageRequest(0, 2));

		assertThat(page.getNumberOfElements(), is(1));
		assertThat(page.getContent().get(0).getFirstname(), is("Oliver"));
	}

	/**
	 * @see DATAJPA-398
	 */
	@Test
	public void adjustedWllKnownFindAllMethodShouldReturnAnEmptyList() {

		olli = repository.save(olli);
		tom = repository.save(tom);

		List<User> result = repository.findAll();

		assertThat(result.isEmpty(), is(true));
	}

}
