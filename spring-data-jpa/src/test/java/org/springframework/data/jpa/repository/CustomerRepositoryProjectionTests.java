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
package org.springframework.data.jpa.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.sample.Country;
import org.springframework.data.jpa.domain.sample.Customer;
import org.springframework.data.jpa.repository.sample.CustomerRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for executing projecting query methods.
 *
 * @author Mark Paluch
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(
		locations = { "classpath:config/namespace-application-context.xml", "classpath:hibernate-h2-infrastructure.xml" })
@Transactional
class CustomerRepositoryProjectionTests {

	@Autowired CustomerRepository repository;

	@AfterEach
	void clearUp() {
		repository.deleteAll();
	}

	@Test
	void returnsCountries() {

		Customer customer = new Customer();
		customer.setId(42L);
		customer.setCountry(Country.of("DE"));
		customer.setName("someone");

		repository.saveAndFlush(customer);

		List<Country> countries = repository.findCountries();

		assertThat(countries).hasSize(1);
	}

}
