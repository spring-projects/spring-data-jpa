/*
 * Copyright 2014-2024 the original author or authors.
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
package org.springframework.data.jpa.repository.generics;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.sample.Book;
import org.springframework.data.jpa.domain.sample.Owner;
import org.springframework.data.jpa.repository.sample.BookRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Yanming Zhou
 */
@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:config/namespace-autoconfig-context.xml" })
class GenericsIntegrationTests {

	@Autowired
	BookRepository repository;

	@Autowired
	EntityManager entityManager;

	@BeforeEach
	void setUp() {
		Owner owner = new Owner();
		owner.setName("owner");
		entityManager.persist(owner);
		Book book = new Book();
		book.setOwner(owner);
		entityManager.persist(book);
	}

	@Test
	void findAllByGenericAssociationProperty() {
		assertThat(repository.findAllByOwnerName("owner")).hasSize(1);
	}

}
