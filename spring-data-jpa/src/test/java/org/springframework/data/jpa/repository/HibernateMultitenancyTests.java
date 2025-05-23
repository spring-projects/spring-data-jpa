/*
 * Copyright 2011-2025 the original author or authors.
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
import static org.assertj.core.api.Assumptions.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.sample.RoleRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

/**
 * Tests for repositories that use multi-tenancy. This tests verifies that repositories can be created an injected
 * despite not having a tenant available at creation time
 *
 * @author Ariel Morelli Andres (Atlassian US, Inc.)
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration()
class HibernateMultitenancyTests {

	@Autowired RoleRepository roleRepository;
	@Autowired EntityManager em;

	@AfterEach
	void tearDown() {
		HibernateCurrentTenantIdentifierResolver.removeTenantIdentifier();
	}

	@Test
	void testPersistenceProviderFromFactoryWithoutTenant() {
		PersistenceProvider provider = PersistenceProvider.fromEntityManagerFactory(em.getEntityManagerFactory());
		assumeThat(provider).isEqualTo(PersistenceProvider.HIBERNATE);
	}

	@Test
	void testRepositoryWithTenant() {
		HibernateCurrentTenantIdentifierResolver.setTenantIdentifier("tenant-id");
		assertThatNoException().isThrownBy(() -> roleRepository.findAll());
	}

	@Test
	void testRepositoryWithoutTenantFails() {
		assertThatThrownBy(() -> roleRepository.findAll()).isInstanceOf(RuntimeException.class);
	}

	@Transactional
	List<Role> insertAndQuery() {
		roleRepository.save(new Role("DRUMMER"));
		roleRepository.flush();
		return roleRepository.findAll();
	}

	@ImportResource({ "classpath:multitenancy-test.xml" })
	@Configuration
	@EnableJpaRepositories(basePackageClasses = HibernateRepositoryTests.class, considerNestedRepositories = true,
			includeFilters = @ComponentScan.Filter(classes = { RoleRepository.class }, type = FilterType.ASSIGNABLE_TYPE))
	static class TestConfig {}
}
