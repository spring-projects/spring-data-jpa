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
package org.springframework.data.jpa.repository;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.sample.RedeclaringRepositoryMethodsRepository;
import org.springframework.data.jpa.repository.sample.SampleConfig;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Thomas Darimont
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SampleConfig.class)
@Transactional
public class RedeclaringRepositoryMethodsTests {

	@Autowired RedeclaringRepositoryMethodsRepository repository;

	User ollie, tom;

	@Before
	public void setup() {

		ollie = new User("Oliver", "Gierke", "ogierke@gopivotal.com");
		tom = new User("Thomas", "Darimont", "tdarimont@gopivotal.com");
	}

	@Test // DATAJPA-398
	public void adjustedWellKnownPagedFindAllMethodShouldReturnOnlyTheUserWithFirstnameOliver() {

		ollie = repository.save(ollie);
		tom = repository.save(tom);

		Page<User> page = repository.findAll(PageRequest.of(0, 2));

		assertThat(page.getNumberOfElements(), is(1));
		assertThat(page.getContent().get(0).getFirstname(), is("Oliver"));
	}

	@Test // DATAJPA-398
	public void adjustedWllKnownFindAllMethodShouldReturnAnEmptyList() {

		ollie = repository.save(ollie);
		tom = repository.save(tom);

		List<User> result = repository.findAll();

		assertThat(result.isEmpty(), is(true));
	}
}
