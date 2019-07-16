/*
 * Copyright 2013-2019 the original author or authors.
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

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.sample.AbstractMappedType;
import org.springframework.data.jpa.domain.sample.ConcreteType1;
import org.springframework.data.jpa.domain.sample.ConcreteType2;
import org.springframework.data.jpa.repository.sample.ConcreteRepository1;
import org.springframework.data.jpa.repository.sample.ConcreteRepository2;
import org.springframework.data.jpa.repository.sample.MappedTypeRepository;
import org.springframework.data.jpa.repository.sample.SampleConfig;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link MappedTypeRepository}.
 *
 * @author Thomas Darimont
 * @author Jens Schauder
 */
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SampleConfig.class)
public class MappedTypeRepositoryIntegrationTests {

	@Autowired ConcreteRepository1 concreteRepository1;
	@Autowired ConcreteRepository2 concreteRepository2;

	@Autowired EntityManager entityManager;

	@Test // DATAJPA-170
	public void supportForExpressionBasedQueryMethods() {

		concreteRepository1.save(new ConcreteType1("foo"));
		concreteRepository2.save(new ConcreteType2("foo"));

		List<ConcreteType1> concretes1 = concreteRepository1.findAllByAttribute1("foo");
		List<ConcreteType2> concretes2 = concreteRepository2.findAllByAttribute1("foo");

		assertThat(concretes1.size()).isEqualTo(1);
		assertThat(concretes2.size()).isEqualTo(1);
	}

	@Test // DATAJPA-424
	public void supportForPaginationCustomQueryMethodsWithEntityExpression() {

		concreteRepository1.save(new ConcreteType1("foo"));
		concreteRepository2.save(new ConcreteType2("foo"));

		Page<ConcreteType2> page = concreteRepository2.findByAttribute1Custom("foo",
				PageRequest.of(0, 10, Sort.Direction.DESC, "attribute1"));

		assertThat(page.getNumberOfElements()).isEqualTo(1);
	}

	@Test // DATAJPA-1535
	@SuppressWarnings("unchecked")
	public void deletesConcreteInstancesForRepositoryBoundToMappedSuperclass() {

		JpaRepositoryFactory factory = new JpaRepositoryFactory(entityManager);
		CustomMappedTypeRepository<AbstractMappedType> repository = factory.getRepository(CustomMappedTypeRepository.class);

		ConcreteType1 entity = repository.save(new ConcreteType1());

		assertThatCode(() -> repository.delete(entity)).doesNotThrowAnyException();
		assertThat(concreteRepository1.findById(entity.getId())).isEmpty();
	}

	private interface CustomMappedTypeRepository<T extends AbstractMappedType> extends CrudRepository<T, Long> {}
}
