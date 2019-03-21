/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.data.jpa.support;

import static org.assertj.core.api.Assertions.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicReference;

import javax.persistence.EntityManagerFactory;
import javax.transaction.Transactional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Integration tests for {@link JpaExecutor}.
 *
 * @author Mark Paluch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:config/namespace-application-context.xml")
public class DefaultJpaExecutorIntegrationTests {

	@Autowired EntityManagerFactory emf;

	@Autowired UserRepository repository;

	@Autowired PlatformTransactionManager transactionManager;

	JpaExecutor executor;

	private User firstUser;
	private User secondUser;

	@Before
	@Transactional
	public void before() {

		executor = new DefaultJpaExecutor(emf, transactionManager, Schedulers.newParallel("jpa", 2));

		firstUser = new User("Oliver", "Gierke", "gierke@synyx.de");
		firstUser.setAge(28);

		secondUser = new User("Joachim", "Arrasz", "arrasz@synyx.de");
		secondUser.setAge(35);

		repository.deleteAll();

		repository.saveAndFlush(firstUser);
		repository.saveAndFlush(secondUser);
	}

	@After
	@Transactional
	public void after() {
		repository.deleteAll();
	}

	@Test // DATAJPA-1350
	public void shouldCallAsync() {

		AtomicReference<Thread> callingThread = new AtomicReference<>();

		Flux<User> read = executor.readMany(em -> {

			callingThread.set(Thread.currentThread());
			return em.createQuery("SELECT u FROM User u").getResultList();
		});

		assertThat(callingThread).hasValue(null);

		read.as(StepVerifier::create).expectNextCount(2).verifyComplete();
		assertThat(callingThread).doesNotHaveValue(Thread.currentThread());
	}

	@Test // DATAJPA-1350
	public void shouldReturnEmptyResult() {

		Flux<User> read = executor.readMany(em -> {

			return em.createQuery("SELECT u FROM User u WHERE u.firstname = ?0").setParameter(0, "foo").getResultList();
		});

		read.as(StepVerifier::create).verifyComplete();
	}

	@Test // DATAJPA-1350
	public void shouldReadFromRepository() {

		Flux<User> read = executor.readMany(repository, JpaRepository::findAll);

		read.as(StepVerifier::create).expectNextCount(2).verifyComplete();
	}

	@Test // DATAJPA-1350
	public void shouldCommitTransaction() {

		Mono<Object> delete = executor.transactional().doInTransaction(repository, userRepository -> {
			userRepository.deleteAll();
			return null;
		});

		delete.as(StepVerifier::create).verifyComplete();

		assertThat(repository.findAll()).isEmpty();
	}

	@Test // DATAJPA-1350
	public void shouldRollbackTransaction() {

		Mono<Void> delete = executor.transactional().doInTransaction(repository, userRepository -> {
			userRepository.deleteAll();
			throw new IllegalStateException("e");
		});

		delete.as(StepVerifier::create).expectError(IllegalStateException.class).verify();

		assertThat(repository.findAll()).isNotEmpty();
	}

	@Test // DATAJPA-1350
	public void shouldCommitWithExceptionTransaction() {

		Mono<Void> delete = executor.transactional().withNoRollbackOn(IllegalStateException.class)
				.doInTransaction(repository, userRepository -> {
					userRepository.deleteAll();
					throw new IllegalStateException("e");
				});

		delete.as(StepVerifier::create).expectError(IllegalStateException.class).verify();

		assertThat(repository.findAll()).isEmpty();
	}

	@Test // DATAJPA-1350
	public void shouldRollbackWithExceptionTransaction() {

		Mono<Void> delete = executor.transactional() //
				.withNoRollbackOn(RuntimeException.class) //
				.withRollbackOn(IllegalStateException.class) //
				.doInTransaction(repository, userRepository -> {
					userRepository.deleteAll();
					throw new IllegalStateException("e");
				});

		delete.as(StepVerifier::create).expectError(IllegalStateException.class).verify();

		assertThat(repository.findAll()).isNotEmpty();
	}
}
