/*
 * Copyright 2014-2025 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.sample.CustomAbstractPersistable;
import org.springframework.data.jpa.repository.sample.CustomAbstractPersistableRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Jens Schauder
 */
@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:config/namespace-autoconfig-context.xml" })
class CustomAbstractPersistableIntegrationTests {

	@Autowired CustomAbstractPersistableRepository repository;

	@Test // DATAJPA-622
	void shouldBeAbleToSaveAndLoadCustomPersistableWithUuidId() {

		CustomAbstractPersistable entity = new CustomAbstractPersistable();
		CustomAbstractPersistable saved = repository.save(entity);
		CustomAbstractPersistable found = repository.findById(saved.getId()).get();

		assertThat(found).isEqualTo(saved);
	}
}
