/*
 * Copyright 2012-2023 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.history.RevisionMetadata.RevisionType.DELETE;
import static org.springframework.data.history.RevisionMetadata.RevisionType.INSERT;
import static org.springframework.data.history.RevisionMetadata.RevisionType.UPDATE;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.envers.Config;
import org.springframework.data.envers.sample.Country;
import org.springframework.data.envers.sample.CountryRepository;
import org.springframework.data.envers.sample.License;
import org.springframework.data.envers.sample.LicenseRepository;
import org.springframework.data.history.Revision;
import org.springframework.data.history.RevisionSort;
import org.springframework.data.history.Revisions;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Integration tests for repositories.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Krzysztof Krason
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Config.class)
class RepositoryIntegrationTests {

	@Autowired LicenseRepository licenseRepository;
	@Autowired CountryRepository countryRepository;

	@BeforeEach
	void setUp() {

		licenseRepository.deleteAll();
		countryRepository.deleteAll();
	}

	@AfterEach
	void tearDown() {

		licenseRepository.deleteAll();
		countryRepository.deleteAll();
	}

	@Test
	void testLifeCycle() {

		License license = new License();
		license.name = "Schnitzel";

		licenseRepository.save(license);

		Country de = new Country();
		de.code = "de";
		de.name = "Deutschland";

		countryRepository.save(de);

		Country se = new Country();
		se.code = "se";
		se.name = "Schweden";

		countryRepository.save(se);

		license.laender = new HashSet<>();
		license.laender.addAll(Arrays.asList(de, se));

		licenseRepository.save(license);

		de.name = "Daenemark";

		countryRepository.save(de);

		Optional<Revision<Integer, License>> revision = licenseRepository.findLastChangeRevision(license.id);

		assertThat(revision).hasValueSatisfying(it -> {

			Page<Revision<Integer, License>> page = licenseRepository.findRevisions(license.id, PageRequest.of(0, 10));
			Revisions<Integer, License> revisions = Revisions.of(page.getContent());
			Revision<Integer, License> latestRevision = revisions.getLatestRevision();

			assertThat(latestRevision.getRequiredRevisionNumber()).isEqualTo(it.getRequiredRevisionNumber());
			assertThat(latestRevision.getEntity()).isEqualTo(it.getEntity());
		});
	}

	@Test // #1
	void returnsEmptyLastRevisionForUnrevisionedEntity() {
		assertThat(countryRepository.findLastChangeRevision(100L)).isEmpty();
	}

	@Test // #47
	void returnsEmptyRevisionsForUnrevisionedEntity() {
		assertThat(countryRepository.findRevisions(100L)).isEmpty();
	}

	@Test // #47
	void returnsEmptyRevisionForUnrevisionedEntity() {
		assertThat(countryRepository.findRevision(100L, 23)).isEmpty();
	}

	@Test // #31
	void returnsParticularRevisionForAnEntity() {

		Country de = new Country();
		de.code = "de";
		de.name = "Deutschland";

		countryRepository.save(de);

		de.name = "Germany";

		countryRepository.save(de);

		Revisions<Integer, Country> revisions = countryRepository.findRevisions(de.id);

		assertThat(revisions).hasSize(2);

		Iterator<Revision<Integer, Country>> iterator = revisions.iterator();
		Revision<Integer, Country> first = iterator.next();
		Revision<Integer, Country> second = iterator.next();

		assertThat(countryRepository.findRevision(de.id, first.getRequiredRevisionNumber()))
				.hasValueSatisfying(it -> assertThat(it.getEntity().name).isEqualTo("Deutschland"));

		assertThat(countryRepository.findRevision(de.id, second.getRequiredRevisionNumber()))
				.hasValueSatisfying(it -> assertThat(it.getEntity().name).isEqualTo("Germany"));
	}

	@Test // #55
	void considersRevisionNumberSortOrder() {

		Country de = new Country();
		de.code = "de";
		de.name = "Deutschland";

		countryRepository.save(de);

		de.name = "Germany";

		countryRepository.save(de);

		Page<Revision<Integer, Country>> page = countryRepository.findRevisions(de.id,
				PageRequest.of(0, 10, RevisionSort.desc()));

		assertThat(page).hasSize(2);
		assertThat(page.getContent().get(0).getRequiredRevisionNumber())
				.isGreaterThan(page.getContent().get(1).getRequiredRevisionNumber());
	}

	@Test // #21
	void findsDeletedRevisions() {

		Country de = new Country();
		de.code = "de";
		de.name = "Deutschland";

		countryRepository.save(de);

		countryRepository.delete(de);

		Revisions<Integer, Country> revisions = countryRepository.findRevisions(de.id);

		assertThat(revisions).hasSize(2);
		assertThat(revisions.getLatestRevision().getEntity()) //
				.extracting(c -> c.name, c -> c.code) //
				.containsExactly(null, null);
	}

	@Test // #47
	void includesCorrectRevisionType() {

		Country de = new Country();
		de.code = "de";
		de.name = "Deutschland";

		countryRepository.save(de);

		de.name = "Bundes Republik Deutschland";

		countryRepository.save(de);

		countryRepository.delete(de);

		Revisions<Integer, Country> revisions = countryRepository.findRevisions(de.id);

		assertThat(revisions) //
				.extracting(r -> r.getMetadata().getRevisionType()) //
				.containsExactly( //
						INSERT, //
						UPDATE, //
						DELETE //
				);
	}

	@Test // #146
	void shortCircuitingWhenOffsetIsToLarge() {

		Country de = new Country();
		de.code = "de";
		de.name = "Deutschland";

		countryRepository.save(de);

		countryRepository.delete(de);

		check(de.id, 0, 1, 2);
		check(de.id, 1, 1, 2);
		check(de.id, 2, 0, 2);
	}

	@Test // #47
	void paginationWithEmptyResult() {

		check(23L, 0, 0, 0);
	}

	void check(Long id, int page, int expectedSize, int expectedTotalSize) {

		Page<Revision<Integer, Country>> revisions = countryRepository.findRevisions(id, PageRequest.of(page, 1));
		assertThat(revisions).hasSize(expectedSize);
		assertThat(revisions.getTotalElements()).isEqualTo(expectedTotalSize);
	}
}
