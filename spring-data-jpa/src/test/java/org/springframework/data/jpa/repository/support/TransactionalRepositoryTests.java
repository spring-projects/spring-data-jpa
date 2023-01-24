/*
 * Copyright 2008-2023 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

/**
 * Integration test for transactional behaviour of predicateExecutor operations.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Krzysztof Krason
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration({ "classpath:config/namespace-autoconfig-context.xml", "classpath:tx-manager.xml" })
public class TransactionalRepositoryTests {

	@Autowired UserRepository repository;
	@Autowired DelegatingTransactionManager transactionManager;

	@BeforeEach
	void setUp() {
		transactionManager.resetCount();
	}

	@AfterEach
	void tearDown() {
		repository.deleteAll();
	}

	@Test
	void simpleManipulatingOperation() {

		repository.saveAndFlush(new User("foo", "bar", "foo@bar.de"));
		assertThat(transactionManager.getTransactionRequests()).isOne();
	}

	@Test
	void unannotatedFinder() {

		repository.findByEmailAddress("foo@bar.de");
		assertThat(transactionManager.getTransactionRequests()).isZero();
	}

	@Test
	void invokeTransactionalFinder() {

		repository.findByAnnotatedQuery("foo@bar.de");
		assertThat(transactionManager.getTransactionRequests()).isOne();
	}

	@Test
	void invokeRedeclaredMethod() {

		repository.findById(1);
		assertThat(transactionManager.getDefinition().isReadOnly()).isFalse();
	}

	@Test // DATACMNS-649
	void invokeRedeclaredDeleteMethodWithoutTransactionDeclaration() {

		User user = repository.saveAndFlush(new User("foo", "bar", "foo@bar.de"));
		repository.deleteById(user.getId());

		assertThat(transactionManager.getDefinition().isReadOnly()).isFalse();
	}

	public static class DelegatingTransactionManager implements PlatformTransactionManager {

		private PlatformTransactionManager txManager;
		private int transactionRequests;
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

			return txManager.getTransaction(definition);
		}

		int getTransactionRequests() {
			return transactionRequests;
		}

		public TransactionDefinition getDefinition() {
			return definition;
		}

		public void resetCount() {

			this.transactionRequests = 0;
			this.definition = null;
		}

		@Override
		public void rollback(TransactionStatus status) throws TransactionException {
			txManager.rollback(status);
		}
	}
}
