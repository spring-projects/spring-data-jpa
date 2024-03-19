/*
 * Copyright 2008-2024 the original author or authors.
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.sample.PersistableWithIdClass;
import org.springframework.data.jpa.domain.sample.PersistableWithIdClassPK;
import org.springframework.data.jpa.domain.sample.SampleEntity;
import org.springframework.data.jpa.domain.sample.SampleEntityPK;
import org.springframework.data.jpa.domain.sample.VersionedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for {@link JpaRepository}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Jens Schauder
 * @author Greg Turnquist
 * @author Krzysztof Krason
 * @author Yanming Zhou
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration({ "classpath:infrastructure.xml" })
@Transactional
class JpaRepositoryTests {

	@Autowired DataSource dataSource;

	@PersistenceContext EntityManager em;

	private JpaRepository<SampleEntity, SampleEntityPK> repository;
	private CrudRepository<PersistableWithIdClass, PersistableWithIdClassPK> idClassRepository;
	private JpaRepository<VersionedUser, Long> versionedUserRepository;
	private NamedParameterJdbcOperations jdbcOperations;

	@BeforeEach
	void setUp() {
		repository = new JpaRepositoryFactory(em).getRepository(SampleEntityRepository.class);
		idClassRepository = new JpaRepositoryFactory(em).getRepository(SampleWithIdClassRepository.class);
		versionedUserRepository = new JpaRepositoryFactory(em).getRepository(VersionedUserRepository.class);
		jdbcOperations = new NamedParameterJdbcTemplate(dataSource);
	}

	@Test
	void testCrudOperationsForCompoundKeyEntity() {

		SampleEntity entity = new SampleEntity("foo", "bar");
		repository.saveAndFlush(entity);

		assertThat(repository.existsById(new SampleEntityPK("foo", "bar"))).isTrue();
		assertThat(repository.count()).isOne();
		assertThat(repository.findById(new SampleEntityPK("foo", "bar"))).contains(entity);

		repository.deleteAll(Arrays.asList(entity));
		repository.flush();

		assertThat(repository.count()).isZero();
	}

	@Test // DATAJPA-50
	void executesCrudOperationsForEntityWithIdClass() {

		PersistableWithIdClass entity = new PersistableWithIdClass(1L, 1L);
		idClassRepository.save(entity);

		assertThat(entity.getFirst()).isNotNull();
		assertThat(entity.getSecond()).isNotNull();

		PersistableWithIdClassPK id = new PersistableWithIdClassPK(entity.getFirst(), entity.getSecond());

		assertThat(idClassRepository.findById(id)).contains(entity);
	}

	@Test // DATAJPA-266
	void testExistsForDomainObjectsWithCompositeKeys() {

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

		repository
				.deleteAllByIdInBatch(Arrays.asList(new SampleEntityPK("one", "eins"), new SampleEntityPK("three", "drei")));

		assertThat(repository.findAll()).containsExactly(two);
	}

	@Test // GH-2242
	void deleteAllByIdInBatchShouldConvertAnIterableToACollection() {

		SampleEntity one = new SampleEntity("one", "eins");
		SampleEntity two = new SampleEntity("two", "zwei");
		SampleEntity three = new SampleEntity("three", "drei");
		repository.saveAll(Arrays.asList(one, two, three));
		repository.flush();

		/**
		 * Wrap a {@link List} inside an {@link Iterable} to verify that {@link SimpleJpaRepository} can properly convert a
		 * pure {@link Iterable} to a {@link Collection}.
		 */
		Iterable<SampleEntityPK> ids = new Iterable<SampleEntityPK>() {

			private List<SampleEntityPK> ids = Arrays.asList(new SampleEntityPK("one", "eins"),
					new SampleEntityPK("three", "drei"));

			@NotNull
			@Override
			public Iterator<SampleEntityPK> iterator() {
				return ids.iterator();
			}
		};

		repository.deleteAllByIdInBatch(ids);
		assertThat(repository.findAll()).containsExactly(two);
	}

	@Test
	void deleteDirtyDetachedVersionedEntityShouldRaiseOptimisticLockException() {

		VersionedUser entity = new VersionedUser();
		entity.setName("name");
		versionedUserRepository.save(entity);
		versionedUserRepository.flush();
		em.detach(entity);

		versionedUserRepository.findById(entity.getId()).ifPresent(u -> {
			u.setName("new name");
			versionedUserRepository.flush();
		});

		assertThatExceptionOfType(OptimisticLockException.class).isThrownBy(() -> {
			versionedUserRepository.delete(entity);
			versionedUserRepository.flush();
		});

		jdbcOperations.update("delete from VersionedUser", Map.of());
	}

	@Test
	void deleteDirtyManagedVersionedEntityShouldRaiseOptimisticLockException() {

		VersionedUser entity = new VersionedUser();
		entity.setName("name");
		versionedUserRepository.save(entity);
		versionedUserRepository.flush();


		assertThat(jdbcOperations.update("update VersionedUser set version=version+1 where id=:id",
				Map.of("id", entity.getId()))).isEqualTo(1);

		assertThatExceptionOfType(OptimisticLockException.class).isThrownBy(() -> {
			versionedUserRepository.delete(entity);
			versionedUserRepository.flush();
		});

		jdbcOperations.update("delete from VersionedUser", Map.of());
	}

	private interface SampleEntityRepository extends JpaRepository<SampleEntity, SampleEntityPK> {

	}

	private interface SampleWithIdClassRepository
			extends CrudRepository<PersistableWithIdClass, PersistableWithIdClassPK> {

	}

	private interface VersionedUserRepository extends JpaRepository<VersionedUser, Long> {

	}
}
