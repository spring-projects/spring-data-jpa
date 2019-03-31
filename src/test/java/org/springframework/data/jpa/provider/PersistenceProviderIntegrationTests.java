/*
 * Copyright 2014-2019 the original author or authors.
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
package org.springframework.data.jpa.provider;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.jpa.domain.sample.Category;
import org.springframework.data.jpa.domain.sample.Product;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.provider.ProxyIdAccessor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.sample.CategoryRepository;
import org.springframework.data.jpa.repository.sample.ProductRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Integration tests for {@link PersistenceProvider}.
 *
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class PersistenceProviderIntegrationTests {

	@Configuration
	@ImportResource("classpath:infrastructure.xml")
	@EnableJpaRepositories(basePackageClasses = CategoryRepository.class,//
			includeFilters = @Filter(value = { CategoryRepository.class, ProductRepository.class },
					type = FilterType.ASSIGNABLE_TYPE))
	static class Config {

	}

	@Autowired CategoryRepository categories;
	@Autowired ProductRepository products;
	@Autowired PlatformTransactionManager transactionManager;
	@Autowired EntityManager em;

	Product product;
	Category category;

	@Before
	public void setUp() {
		this.product = products.save(new Product());
		this.category = categories.save(new Category(product));
	}

	@Test // DATAJPA-630
	public void testname() {

		new TransactionTemplate(transactionManager).execute(new TransactionCallback<Void>() {

			@Override
			public Void doInTransaction(TransactionStatus status) {

				Product product = categories.findById(category.getId()).get().getProduct();
				ProxyIdAccessor accessor = PersistenceProvider.fromEntityManager(em);

				assertThat(accessor.shouldUseAccessorFor(product), is(true));
				assertThat(accessor.getIdentifierFrom(product).toString(), is((Object) product.getId().toString()));

				return null;
			}
		});
	}
}
