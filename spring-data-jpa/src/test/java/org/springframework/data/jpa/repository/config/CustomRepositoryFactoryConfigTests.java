/*
 * Copyright 2008-2025 the original author or authors.
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
package org.springframework.data.jpa.repository.config;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.custom.UserCustomExtendedRepository;
import org.springframework.data.jpa.repository.support.TransactionalRepositoryTests.DelegatingTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Annotation to exclude repository interfaces from being picked up and thus in consequence getting an instance being
 * created.
 * <p>
 * This will typically be used when providing an extended base interface for all repositories in combination with a
 * custom repository base class to implement methods declared in that intermediate interface. In this case you typically
 * derive your concrete repository interfaces from the intermediate one but don't want to create a Spring bean for the
 * intermediate interface.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Jens Schauder
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:config/namespace-customfactory-context.xml")
class CustomRepositoryFactoryConfigTests {

	@Autowired(required = false) UserCustomExtendedRepository userRepository;

	@Autowired DelegatingTransactionManager transactionManager;

	@BeforeEach
	void setup() {

		transactionManager.resetCount();
	}

	@Test
	void testCustomFactoryUsed() {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> userRepository.customMethod(1));
	}

	@Test
	void reconfiguresTransactionalMethodWithoutGenericParameter() {

		userRepository.findAll();

		assertThat(transactionManager.getDefinition().isReadOnly()).isFalse();
		assertThat(transactionManager.getDefinition().getTimeout()).isEqualTo(10);
	}

	@Test
	void reconfiguresTransactionalMethodWithGenericParameter() {

		userRepository.findById(1);

		assertThat(transactionManager.getDefinition().isReadOnly()).isFalse();
		assertThat(transactionManager.getDefinition().getTimeout()).isEqualTo(10);
	}
}
