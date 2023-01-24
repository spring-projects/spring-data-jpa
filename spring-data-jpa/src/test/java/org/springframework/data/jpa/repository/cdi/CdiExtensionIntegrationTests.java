/*
 * Copyright 2011-2023 the original author or authors.
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
package org.springframework.data.jpa.repository.cdi;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import jakarta.enterprise.inject.spi.Bean;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Spring Data JPA CDI extension.
 *
 * @author Dirk Mahler
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Jens Schauder
 * @author Krzysztof Krason
 */
class CdiExtensionIntegrationTests {

	private static SeContainer container;
	private static Log LOGGER = LogFactory.getLog(CdiExtensionIntegrationTests.class);

	@BeforeAll
	static void setUpCdi() {

		container = SeContainerInitializer.newInstance() //
				.disableDiscovery() //
				.addPackages(PersonRepository.class) //
				.initialize();

		LOGGER.debug("CDI container bootstrapped");
	}

	@AfterAll
	static void tearDownCdi() {
		container.close();
	}

	@Test // DATAJPA-319, DATAJPA-1180
	@SuppressWarnings("rawtypes")
	void foo() {

		Set<Bean<?>> beans = container.getBeanManager().getBeans(PersonRepository.class);

		assertThat(beans).hasSize(1);
		assertThat(beans.iterator().next().getScope()).isEqualTo((Class) ApplicationScoped.class);
	}

	@Test // DATAJPA-136, DATAJPA-1180
	void saveAndFindAll() {

		RepositoryConsumer repositoryConsumer = container.select(RepositoryConsumer.class).get();

		Person person = new Person();
		repositoryConsumer.save(person);
		repositoryConsumer.findAll();
	}

	@Test // DATAJPA-584, DATAJPA-1180
	void returnOneFromCustomImpl() {

		RepositoryConsumer repositoryConsumer = container.select(RepositoryConsumer.class).get();
		assertThat(repositoryConsumer.returnOne()).isOne();
	}

	@Test // DATAJPA-584, DATAJPA-1180
	void useQualifiedCustomizedUserRepo() {

		RepositoryConsumer repositoryConsumer = container.select(RepositoryConsumer.class).get();
		repositoryConsumer.doSomethingOnUserDB();
	}

	@Test // DATAJPA-1287
	void useQualifiedFragmentUserRepo() {

		RepositoryConsumer repositoryConsumer = container.select(RepositoryConsumer.class).get();
		assertThat(repositoryConsumer.returnOneUserDB()).isOne();
	}
}
