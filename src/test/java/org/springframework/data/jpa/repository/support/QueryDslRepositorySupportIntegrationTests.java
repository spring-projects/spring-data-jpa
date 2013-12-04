/*
 * Copyright 2011-2013 the original author or authors.
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

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.sample.MailMessage;
import org.springframework.data.jpa.domain.sample.MailSender;
import org.springframework.data.jpa.domain.sample.QMailMessage;
import org.springframework.data.jpa.domain.sample.QMailSender;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.InfrastructureConfig;
import org.springframework.data.jpa.repository.sample.MailMessageRepository;
import org.springframework.data.jpa.repository.support.QueryDslRepositorySupportTests.UserRepository;
import org.springframework.data.jpa.repository.support.QueryDslRepositorySupportTests.UserRepositoryImpl;
import org.springframework.data.querydsl.QPageRequest;
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
 */
@Transactional
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class QueryDslRepositorySupportIntegrationTests {

	@Configuration
	@EnableJpaRepositories(basePackageClasses = MailMessageRepository.class, includeFilters = @Filter(
			type = FilterType.ASSIGNABLE_TYPE, value = { MailMessageRepository.class }))
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
		public ReconfiguringUserRepositoryImpl reconfiguringUserRepositoryImpl() {
			return new ReconfiguringUserRepositoryImpl();
		}

		@Bean
		public EntityManagerContainer entityManagerContainer() {
			return new EntityManagerContainer();
		}

		@Bean
		public LocalContainerEntityManagerFactoryBean entityManagerFactory() {

			LocalContainerEntityManagerFactoryBean emf = super.entityManagerFactory();
			emf.setPersistenceUnitName("querydsl");
			return emf;
		}
	}

	@Autowired UserRepository repository;

	@Autowired ReconfiguringUserRepositoryImpl reconfiguredRepo;

	@Autowired MailMessageRepository mailMessageRepository;

	@PersistenceContext(unitName = "querydsl") EntityManager em;

	static final QMailMessage qmail = QMailMessage.mailMessage;
	static final QMailSender qsender = QMailSender.mailSender;

	@Test
	public void createsRepoCorrectly() {
		assertThat(repository, is(notNullValue()));
	}

	/**
	 * @see DATAJPA-135
	 */
	@Test
	public void createsReconfiguredRepoAccordingly() {

		assertThat(reconfiguredRepo, is(notNullValue()));
		assertThat(reconfiguredRepo.getEntityManager().getEntityManagerFactory(), is(em.getEntityManagerFactory()));
	}

	/**
	 * @see DATAJPA-12
	 */
	@Test
	public void shouldSortMailWithQueryDslRepositoryAndQPageRequestDslSortCriteriaNullsFirst() {

		MailMessage message1 = new MailMessage();
		message1.setContent("abc");
		MailSender sender1 = new MailSender("foo");
		message1.setMailSender(sender1);

		MailMessage message2 = new MailMessage();
		message2.setContent("abc");

		mailMessageRepository.save(message1);
		mailMessageRepository.save(message2);

		Page<MailMessage> results = mailMessageRepository.findAll(qmail.content.eq("abc"), new QPageRequest(0, 20,
				qsender.name.asc()));
		List<MailMessage> messages = results.getContent();

		assertThat(messages, hasSize(2));
		assertThat(messages.get(0).getMailSender(), is(nullValue()));
		assertThat(messages.get(1).getMailSender(), is(sender1));
	}

	/**
	 * @see DATAJPA-12
	 */
	@Test
	public void shouldSortMailWithQueryDslRepositoryAndDslSortCriteriaNullsFirst() {

		MailMessage message1 = new MailMessage();
		message1.setContent("abc");
		MailSender sender1 = new MailSender("foo");
		message1.setMailSender(sender1);

		MailMessage message2 = new MailMessage();
		message2.setContent("abc");

		mailMessageRepository.save(message1);
		mailMessageRepository.save(message2);

		List<MailMessage> messages = mailMessageRepository.findAll(qmail.content.eq("abc"), qmail.mailSender.name.asc());

		assertThat(messages, hasSize(2));
		assertThat(messages.get(0).getMailSender(), is(nullValue()));
		assertThat(messages.get(1).getMailSender(), is(sender1));
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
}
