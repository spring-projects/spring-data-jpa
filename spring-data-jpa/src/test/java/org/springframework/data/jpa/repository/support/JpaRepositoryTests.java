/*
 * Copyright 2008-2021 the original author or authors.
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
package org.springframework.data.jpa.repository.support;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.data.jpa.domain.sample.PersistableWithIdClass;
import org.springframework.data.jpa.domain.sample.PersistableWithIdClassPK;
import org.springframework.data.jpa.domain.sample.SampleEntity;
import org.springframework.data.jpa.domain.sample.SampleEntityPK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for {@link JpaRepository}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Jens Schauder
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration({ "classpath:infrastructure.xml" })
@Transactional
class JpaRepositoryTests {

	@PersistenceContext EntityManager em;

	private JpaRepository<SampleEntity, SampleEntityPK> repository;
	private CrudRepository<PersistableWithIdClass, PersistableWithIdClassPK> idClassRepository;

	@BeforeEach
	void setUp() {

		repository = new JpaRepositoryFactory(em).getRepository(SampleEntityRepository.class);
		idClassRepository = new JpaRepositoryFactory(em).getRepository(SampleWithIdClassRepository.class);
	}

	@Test
	void testCrudOperationsForCompoundKeyEntity() throws Exception {

		SampleEntity entity = new SampleEntity("foo", "bar");
		repository.saveAndFlush(entity);
		assertThat(repository.existsById(new SampleEntityPK("foo", "bar"))).isTrue();
		assertThat(repository.count()).isEqualTo(1L);
		assertThat(repository.findById(new SampleEntityPK("foo", "bar"))).isEqualTo(Optional.of(entity));

		repository.deleteAll(Arrays.asList(entity));
		repository.flush();
		assertThat(repository.count()).isEqualTo(0L);
	}

	@Test // DATAJPA-50
	void executesCrudOperationsForEntityWithIdClass() {

		PersistableWithIdClass entity = new PersistableWithIdClass(1L, 1L);
		idClassRepository.save(entity);

		assertThat(entity.getFirst()).isNotNull();
		assertThat(entity.getSecond()).isNotNull();

		PersistableWithIdClassPK id = new PersistableWithIdClassPK(entity.getFirst(), entity.getSecond());

		assertThat(idClassRepository.findById(id)).isEqualTo(Optional.of(entity));
	}

	@Test // DATAJPA-266
	void testExistsForDomainObjectsWithCompositeKeys() throws Exception {

		PersistableWithIdClass s1 = idClassRepository.save(new PersistableWithIdClass(1L, 1L));
		PersistableWithIdClass s2 = idClassRepository.save(new PersistableWithIdClass(2L, 2L));

		assertThat(idClassRepository.existsById(s1.getId())).isTrue();
		assertThat(idClassRepository.existsById(s2.getId())).isTrue();
		assertThat(idClassRepository.existsById(new PersistableWithIdClassPK(1L, 2L))).isFalse();
	}

	@Test // DATAJPA-527
	void executesExistsForEntityWithIdClass() {

		PersistableWithIdClass entity = new PersistableWithIdClass(1L, 1L);
		idClassRepository.save(entity);

		assertThat(entity.getFirst()).isNotNull();
		assertThat(entity.getSecond()).isNotNull();

		PersistableWithIdClassPK id = new PersistableWithIdClassPK(entity.getFirst(), entity.getSecond());

		assertThat(idClassRepository.existsById(id)).isTrue();
	}

	@Test // DATAJPA-1818
	void deleteAllByIdInBatch() {

		SampleEntity one = new SampleEntity("one", "eins");
		SampleEntity two = new SampleEntity("two", "zwei");
		SampleEntity three = new SampleEntity("three", "drei");
		repository.saveAll(Arrays.asList(one, two, three));
		repository.flush();

		repository.deleteAllByIdInBatch(Arrays.asList(new SampleEntityPK("one", "eins"),new SampleEntityPK("three", "drei")));
		assertThat(repository.findAll()).containsExactly(two);
	}

	private interface SampleEntityRepository extends JpaRepository<SampleEntity, SampleEntityPK> {

	}

	private interface SampleWithIdClassRepository
			extends CrudRepository<PersistableWithIdClass, PersistableWithIdClassPK> {

	}
}
