/*
 * Copyright 2015-2019 the original author or authors.
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

import javax.persistence.TransactionRequiredException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.data.jpa.repository.support.TransactionalRepositoryTests.DelegatingTransactionManager;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration tests for disabling default transactions using JavaConfig.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @soundtrack The Intersphere - Live in Mannheim
 */
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class DefaultTransactionDisablingIntegrationTests {

	public @Rule ExpectedException exception = ExpectedException.none();

	@Autowired UserRepository repository;
	@Autowired DelegatingTransactionManager txManager;

	@Test // DATAJPA-685
	public void considersExplicitConfigurationOnRepositoryInterface() {

		repository.findById(1);

		assertThat(txManager.getDefinition().isReadOnly()).isFalse();
	}

	@Test // DATAJPA-685
	public void doesNotUseDefaultTransactionsOnNonRedeclaredMethod() {

		repository.findAll(PageRequest.of(0, 10));

		assertThat(txManager.getDefinition()).isNull();
	}

	@Test // DATAJPA-685
	public void persistingAnEntityShouldThrowExceptionDueToMissingTransaction() {

		assertThatThrownBy(() -> repository.saveAndFlush(new User())) //
				.isInstanceOf(InvalidDataAccessApiUsageException.class) //
				.hasCauseExactlyInstanceOf(TransactionRequiredException.class);
	}
}
