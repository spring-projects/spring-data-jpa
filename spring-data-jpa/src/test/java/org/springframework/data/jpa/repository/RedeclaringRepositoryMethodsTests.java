/*
 * Copyright 2013-2023 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.sample.RedeclaringRepositoryMethodsRepository;
import org.springframework.data.jpa.repository.sample.SampleConfig;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Thomas Darimont
 * @author Jens Schauder
 * @author Krzysztof Krason
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SampleConfig.class)
@Transactional
class RedeclaringRepositoryMethodsTests {

	@Autowired RedeclaringRepositoryMethodsRepository repository;

	private User ollie;
	private User tom;

	@BeforeEach
	void setup() {

		ollie = new User("Oliver", "Gierke", "ogierke@gopivotal.com");
		tom = new User("Thomas", "Darimont", "tdarimont@gopivotal.com");
	}

	@Test // DATAJPA-398
	void adjustedWellKnownPagedFindAllMethodShouldReturnOnlyTheUserWithFirstnameOliver() {

		ollie = repository.save(ollie);
		tom = repository.save(tom);

		Page<User> page = repository.findAll(PageRequest.of(0, 2));

		assertThat(page.getNumberOfElements()).isOne();
		assertThat(page.getContent().get(0).getFirstname()).isEqualTo("Oliver");
	}

	@Test // DATAJPA-398
	void adjustedWllKnownFindAllMethodShouldReturnAnEmptyList() {

		ollie = repository.save(ollie);
		tom = repository.save(tom);

		List<User> result = repository.findAll();

		assertThat(result).isEmpty();
	}
}
