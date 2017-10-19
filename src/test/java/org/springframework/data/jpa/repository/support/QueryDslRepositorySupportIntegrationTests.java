/*
 * Copyright 2011-2017 the original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.config.InfrastructureConfig;
import org.springframework.data.jpa.repository.support.QueryDslRepositorySupportTests.UserRepository;
import org.springframework.data.jpa.repository.support.QueryDslRepositorySupportTests.UserRepositoryImpl;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for the setup of beans extending {@link QueryDslRepositorySupport}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 */
@Transactional
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class QueryDslRepositorySupportIntegrationTests {

	@Configuration
	@EnableTransactionManagement
	static class Config extends InfrastructureConfig {
		@Bean
		public UserRepositoryImpl userRepositoryImpl() {
			return new UserRepositoryImpl() {
				@Override
				@PersistenceContext(unitName = "querydsl")
				public void setEntityManager(EntityManager entityManager) {
					super.setEntityManager(entityManager);
				}
			};
		}

		@Bean
		static EntityManagerBeanDefinitionRegistrarPostProcessor entityManagerBeanDefinitionRegistrarPostProcessor() {
			return new EntityManagerBeanDefinitionRegistrarPostProcessor();
		}

		@Bean
		public ReconfiguringUserRepositoryImpl reconfiguringUserRepositoryImpl() {
			return new ReconfiguringUserRepositoryImpl();
		}

		@Bean
		public CustomRepoUsingQueryDsl customRepo() {
			return new CustomRepoUsingQueryDsl();
		}

		@Bean
		public EntityManagerContainer entityManagerContainer() {
			return new EntityManagerContainer();
		}

		@Override
		@Bean
		public LocalContainerEntityManagerFactoryBean entityManagerFactory() {

			LocalContainerEntityManagerFactoryBean emf = super.entityManagerFactory();
			emf.setPersistenceUnitName("querydsl");
			return emf;
		}
	}

	@Autowired UserRepository repository;
	@Autowired CustomRepoUsingQueryDsl querydslCustom;
	@Autowired ReconfiguringUserRepositoryImpl reconfiguredRepo;

	@PersistenceContext(unitName = "querydsl") EntityManager em;

	@Test
	public void createsRepoCorrectly() {
		assertThat(repository, is(notNullValue()));
	}

	@Test // DATAJPA-135
	public void createsReconfiguredRepoAccordingly() {

		assertThat(reconfiguredRepo, is(notNullValue()));
		assertThat(reconfiguredRepo.getEntityManager().getEntityManagerFactory(), is(em.getEntityManagerFactory()));
	}

	@Test // DATAJPA-1205
	public void createsRepositoryWithCustomImplementationUsingQueryDsl() {

		assertThat(querydslCustom, is(notNullValue()));
		assertThat(querydslCustom.getEntityManager().getEntityManagerFactory(), is(em.getEntityManagerFactory()));
	}

	static class ReconfiguringUserRepositoryImpl extends QueryDslRepositorySupport {

		public ReconfiguringUserRepositoryImpl() {
			super(User.class);
		}

		@Override
		@PersistenceContext(unitName = "querydsl")
		public void setEntityManager(EntityManager entityManager) {
			super.setEntityManager(entityManager);
		}
	}

	static class EntityManagerContainer {

		@PersistenceContext(unitName = "querydsl") EntityManager em;
	}

	static class CustomRepoUsingQueryDsl extends QueryDslRepositorySupport {

		public CustomRepoUsingQueryDsl() {
			super(User.class);
		}
	}
}
