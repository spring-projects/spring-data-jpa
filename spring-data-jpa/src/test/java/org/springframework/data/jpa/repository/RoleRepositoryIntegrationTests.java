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
package org.springframework.data.jpa.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.repository.sample.RoleRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link RoleRepository}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Jens Schauder
 * @author Krzysztof Krason
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:application-context.xml" })
@Transactional
class RoleRepositoryIntegrationTests {

	@Autowired RoleRepository repository;

	@Test
	void createsRole() {

		Role reference = new Role("ADMIN");
		Role result = repository.save(reference);
		assertThat(result).isEqualTo(reference);
	}

	@Test
	void updatesRole() {

		Role reference = new Role("ADMIN");
		Role result = repository.save(reference);
		assertThat(result).isEqualTo(reference);

		// Change role name
		ReflectionTestUtils.setField(reference, "name", "USER");
		repository.save(reference);

		assertThat(repository.findById(result.getId())).contains(reference);
	}

	@Test // DATAJPA-509
	void shouldUseExplicitlyConfiguredEntityNameInOrmXmlInCountQueries() {

		Role reference = new Role("ADMIN");
		repository.save(reference);

		assertThat(repository.count()).isOne();
	}

	@Test // DATAJPA-509
	void shouldUseExplicitlyConfiguredEntityNameInOrmXmlInExistsQueries() {

		Role reference = new Role("ADMIN");
		reference = repository.save(reference);

		assertThat(repository.existsById(reference.getId())).isTrue();
	}

	@Test // DATAJPA-509
	void shouldUseExplicitlyConfiguredEntityNameInDerivedCountQueries() {

		Role reference = new Role("ADMIN");
		reference = repository.save(reference);

		assertThat(repository.countByName(reference.getName())).isOne();
	}
}
