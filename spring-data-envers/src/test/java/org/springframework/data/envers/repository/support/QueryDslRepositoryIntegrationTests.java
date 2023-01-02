/*
 * Copyright 2018-2023 the original author or authors.
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
package org.springframework.data.envers.repository.support;

import static org.assertj.core.api.Assertions.*;

import java.util.Iterator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.envers.Config;
import org.springframework.data.envers.sample.Country;
import org.springframework.data.envers.sample.CountryQueryDslRepository;
import org.springframework.data.envers.sample.QCountry;
import org.springframework.data.history.Revision;
import org.springframework.data.history.Revisions;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Integration tests for repositories with Querydsl support. They make sure that methods provided by both
 * {@link org.springframework.data.repository.history.RevisionRepository} and {@link org.springframework.data.querydsl.QuerydslPredicateExecutor} are working.
 *
 * @author Dmytro Iaroslavskyi
 * @author Jens Schauder
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Config.class)
class QueryDslRepositoryIntegrationTests {

	@Autowired
	CountryQueryDslRepository countryRepository;

	@BeforeEach
	void setUp() {
		countryRepository.deleteAll();
	}

	@Test
	void testWithQueryDsl() {

		Country de = new Country();
		de.code = "de";
		de.name = "Deutschland";

		countryRepository.save(de);

		Country found = countryRepository.findOne(QCountry.country.name.eq("Deutschland")).get();

		assertThat(found).isNotNull();
		assertThat(found.id).isEqualTo(de.id);
	}

	@Test
	void testWithRevisions() {

		Country de = new Country();
		de.code = "de";
		de.name = "Deutschland";

		countryRepository.save(de);

		de.name = "Germany";

		countryRepository.save(de);

		Revisions<Integer, Country> revisions = countryRepository.findRevisions(de.id);

		assertThat(revisions).hasSize(2);

		Iterator<Revision<Integer, Country>> iterator = revisions.iterator();

		Integer firstRevisionNumber = iterator.next().getRevisionNumber().get();
		Integer secondRevisionNumber = iterator.next().getRevisionNumber().get();

		assertThat(countryRepository.findRevision(de.id, firstRevisionNumber).get().getEntity().name)
				.isEqualTo("Deutschland");
		assertThat(countryRepository.findRevision(de.id, secondRevisionNumber).get().getEntity().name).isEqualTo("Germany");
	}

}
