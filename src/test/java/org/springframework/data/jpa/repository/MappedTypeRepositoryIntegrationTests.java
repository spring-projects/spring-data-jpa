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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.sample.ConcreteType1;
import org.springframework.data.jpa.domain.sample.ConcreteType2;
import org.springframework.data.jpa.repository.sample.ConcreteRepository1;
import org.springframework.data.jpa.repository.sample.ConcreteRepository2;
import org.springframework.data.jpa.repository.sample.MappedTypeRepository;
import org.springframework.data.jpa.repository.sample.SampleConfig;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link MappedTypeRepository}.
 *
 * @author Thomas Darimont
 */
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SampleConfig.class)
public class MappedTypeRepositoryIntegrationTests {

	@Autowired ConcreteRepository1 concreteRepository1;
	@Autowired ConcreteRepository2 concreteRepository2;

	@Test // DATAJPA-170
	public void supportForExpressionBasedQueryMethods() {

		concreteRepository1.save(new ConcreteType1("foo"));
		concreteRepository2.save(new ConcreteType2("foo"));

		List<ConcreteType1> concretes1 = concreteRepository1.findAllByAttribute1("foo");
		List<ConcreteType2> concretes2 = concreteRepository2.findAllByAttribute1("foo");

		assertThat(concretes1.size(), is(1));
		assertThat(concretes2.size(), is(1));
	}

	@Test // DATAJPA-424
	public void supportForPaginationCustomQueryMethodsWithEntityExpression() {

		concreteRepository1.save(new ConcreteType1("foo"));
		concreteRepository2.save(new ConcreteType2("foo"));

		Page<ConcreteType2> page = concreteRepository2.findByAttribute1Custom("foo", PageRequest.of(0, 10,
				Sort.Direction.DESC, "attribute1"));

		assertThat(page.getNumberOfElements(), is(1));
	}
}
