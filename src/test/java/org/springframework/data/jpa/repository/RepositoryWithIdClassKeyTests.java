/*
 * Copyright 2016-2019 the original author or authors.
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

import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.jpa.domain.sample.Item;
import org.springframework.data.jpa.domain.sample.ItemId;
import org.springframework.data.jpa.domain.sample.ItemSite;
import org.springframework.data.jpa.domain.sample.ItemSiteId;
import org.springframework.data.jpa.domain.sample.Site;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.sample.ItemRepository;
import org.springframework.data.jpa.repository.sample.ItemSiteRepository;
import org.springframework.data.jpa.repository.sample.SampleConfig;
import org.springframework.data.jpa.repository.sample.SiteRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for Repositories using {@link javax.persistence.IdClass} identifiers.
 *
 * @author Mark Paluch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RepositoryWithIdClassKeyTests.TestConfig.class)
@Transactional
public class RepositoryWithIdClassKeyTests {

	@Rule public ExpectedException expectedException = ExpectedException.none();

	@Autowired private SiteRepository siteRepository;

	@Autowired private ItemRepository itemRepository;

	@Autowired private ItemSiteRepository itemSiteRepository;

	@Configuration
	@EnableJpaRepositories(basePackageClasses = SampleConfig.class)
	static abstract class Config {

	}

	@ImportResource("classpath:infrastructure.xml")
	static class TestConfig extends Config {}

	/**
	 * @see <a href="download.oracle.com/otn-pub/jcp/persistence-2_1-fr-eval-spec/JavaPersistence.pdf">Final JPA 2.1
	 *      Specification 2.4.1.3 Derived Identities Example 2</a>
	 */
	@Test // DATAJPA-413
	public void shouldSaveAndLoadEntitiesWithDerivedIdentities() throws Exception {

		Site site = siteRepository.save(new Site());
		Item item = itemRepository.save(new Item(123, 456));

		itemSiteRepository.save(new ItemSite(item, site));

		Optional<ItemSite> loaded = itemSiteRepository
				.findById(new ItemSiteId(new ItemId(item.getId(), item.getManufacturerId()), site.getId()));

		assertThat(loaded, is(notNullValue()));
		assertThat(loaded.isPresent(), is(true));
	}
}
