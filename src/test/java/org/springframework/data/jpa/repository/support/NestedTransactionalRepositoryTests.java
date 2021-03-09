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

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.sample.Site;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.sample.SiteRepository;
import org.springframework.data.jpa.repository.support.TransactionalRepositoryTests.DelegatingTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for transactional behaviour with nested transactions.<br>
 * Based on the {@link TransactionalRepositoryTests}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Lachezar Dobrev
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration({ "classpath:config/namespace-autoconfig-context.xml", "classpath:tx-nesting-service.xml" })
public class NestedTransactionalRepositoryTests {

	@Autowired DelegatingTransactionManager transactionManager;

	@Autowired NestingService service;

	private Integer siteId;

	@BeforeEach
	void setUp() {
		siteId = service.init();
		// Reset counter
		transactionManager.resetCount();
	}

	@AfterEach
	void tearDown() {
		service.done();
	}

	public static class DelegatingTransactionManager implements PlatformTransactionManager {

		private PlatformTransactionManager txManager;
		private int transactionRequests;
		private int transactionsCreated;
		private TransactionDefinition definition;

		public DelegatingTransactionManager(PlatformTransactionManager txManager) {

			this.txManager = txManager;
		}

		@Override
		public void commit(TransactionStatus status) throws TransactionException {

			txManager.commit(status);
		}

		@Override
		public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {

			this.transactionRequests++;
			this.definition = definition;

			TransactionStatus status = txManager.getTransaction(definition);
			if (status.isNewTransaction())
				transactionsCreated++;
			return status;
		}

		int getTransactionRequests() {

			return transactionRequests;
		}

		int getTransactionsCreated() {
					
			return transactionsCreated;
		}
			
		public TransactionDefinition getDefinition() {

			return definition;
		}

		public void resetCount() {

			this.transactionRequests = 0;
			this.transactionsCreated = 0;
			this.definition = null;
		}

		@Override
		public void rollback(TransactionStatus status) throws TransactionException {

			txManager.rollback(status);
		}
	}

	public static interface NestingService {
		@Transactional
		public Integer init();
		@Transactional
		public void done();
		@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
		public int[] readEntityMultipleTimesSupports(Integer id);
		@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
		public int[] readEntityMultipleTimesRequired(Integer id);
	}

	@Test
	public void testRequired() {
		int[] ints = service.readEntityMultipleTimesRequired(siteId);
		// All Identity Hash Codes must be the same object
		assertThat(ints[1]).isEqualTo(ints[0]);
		assertThat(ints[2]).isEqualTo(ints[0]);
		assertThat(ints[3]).isEqualTo(ints[0]);
		// Transaction Manager should have been called 1 time for service + 2 times repository
		assertThat(transactionManager.getTransactionRequests()).isEqualTo(3);
		// Exactly one transaction must have been created for the service
		assertThat(transactionManager.getTransactionsCreated()).isEqualTo(1);
	}

	@Test
	public void testSupports() {
		int[] ints = service.readEntityMultipleTimesSupports(siteId);
		// All Identity Hash Codes must be the same object
		assertThat(ints[1]).isEqualTo(ints[0]);
		assertThat(ints[2]).isEqualTo(ints[0]);
		assertThat(ints[3]).isEqualTo(ints[0]);
		// Transaction Manager should have been called 1 time for service + 2 times repository
		assertThat(transactionManager.getTransactionRequests()).isEqualTo(3);
		// No transactions must have been created
		assertThat(transactionManager.getTransactionsCreated()).isEqualTo(0);
	}

	public static class NestingServiceImpl implements NestingService {
		@PersistenceContext
		private EntityManager entityManager;

		@Autowired
		private SiteRepository repository;

		@PostConstruct
		void create() {
			//repository = new JpaRepositoryFactory(entityManager).getRepository(SiteRepository.class);
		}

		@Override
		public Integer init() {
			return repository.saveAndFlush(new Site()).getId();
		}

		@Override
		public void done() {
			repository.deleteAll();
		}

		@Override
		public int[] readEntityMultipleTimesSupports(Integer id) {
			return readEntityMultipleTimes(id);
		}

		@Override
		public int[] readEntityMultipleTimesRequired(Integer id) {
			return readEntityMultipleTimes(id);
		}

		int[] readEntityMultipleTimes(Integer id) {
			int[] ints = new int[4];
			ints[0] = System.identityHashCode(entityManager.find(Site.class, id));
			ints[1] = System.identityHashCode(entityManager.find(Site.class, id));
			ints[2] = System.identityHashCode(repository.findById(id).get());
			ints[3] = System.identityHashCode(repository.findById(id).get());
			return ints;
		}

	}

}
