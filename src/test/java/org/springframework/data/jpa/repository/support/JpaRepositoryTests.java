/*
 * Copyright 2008-2018 the original author or authors.
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
package org.springframework.data.jpa.repository.support;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.jpa.domain.sample.SampleEntity;
import org.springframework.data.jpa.domain.sample.SampleEntityPK;
import org.springframework.data.jpa.domain.sample.PersistableWithIdClass;
import org.springframework.data.jpa.domain.sample.PersistableWithIdClassPK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for {@link JpaRepository}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "classpath:infrastructure.xml" })
@Transactional
public class JpaRepositoryTests {

	@PersistenceContext EntityManager em;

	JpaRepository<SampleEntity, SampleEntityPK> repository;
	CrudRepository<PersistableWithIdClass, PersistableWithIdClassPK> idClassRepository;

	@Before
	public void setUp() {

		repository = new JpaRepositoryFactory(em).getRepository(SampleEntityRepository.class);
		idClassRepository = new JpaRepositoryFactory(em).getRepository(SampleWithIdClassRepository.class);
	}

	@Test
	public void testCrudOperationsForCompoundKeyEntity() throws Exception {

		SampleEntity entity = new SampleEntity("foo", "bar");
		repository.saveAndFlush(entity);
		assertThat(repository.existsById(new SampleEntityPK("foo", "bar")), is(true));
		assertThat(repository.count(), is(1L));
		assertThat(repository.findById(new SampleEntityPK("foo", "bar")), is(Optional.of(entity)));

		repository.deleteAll(Arrays.asList(entity));
		repository.flush();
		assertThat(repository.count(), is(0L));
	}

	@Test // DATAJPA-50
	public void executesCrudOperationsForEntityWithIdClass() {

		PersistableWithIdClass entity = new PersistableWithIdClass(1L, 1L);
		idClassRepository.save(entity);

		assertThat(entity.getFirst(), is(notNullValue()));
		assertThat(entity.getSecond(), is(notNullValue()));

		PersistableWithIdClassPK id = new PersistableWithIdClassPK(entity.getFirst(), entity.getSecond());

		assertThat(idClassRepository.findById(id), is(Optional.of(entity)));
	}

	@Test // DATAJPA-266
	public void testExistsForDomainObjectsWithCompositeKeys() throws Exception {

		PersistableWithIdClass s1 = idClassRepository.save(new PersistableWithIdClass(1L, 1L));
		PersistableWithIdClass s2 = idClassRepository.save(new PersistableWithIdClass(2L, 2L));

		assertThat(idClassRepository.existsById(s1.getId()), is(true));
		assertThat(idClassRepository.existsById(s2.getId()), is(true));
		assertThat(idClassRepository.existsById(new PersistableWithIdClassPK(1L, 2L)), is(false));
	}

	@Test // DATAJPA-527
	public void executesExistsForEntityWithIdClass() {

		PersistableWithIdClass entity = new PersistableWithIdClass(1L, 1L);
		idClassRepository.save(entity);

		assertThat(entity.getFirst(), is(notNullValue()));
		assertThat(entity.getSecond(), is(notNullValue()));

		PersistableWithIdClassPK id = new PersistableWithIdClassPK(entity.getFirst(), entity.getSecond());

		assertThat(idClassRepository.existsById(id), is(true));
	}

	private static interface SampleEntityRepository extends JpaRepository<SampleEntity, SampleEntityPK> {

	}

	private static interface SampleWithIdClassRepository extends CrudRepository<PersistableWithIdClass, PersistableWithIdClassPK> {

	}
}
