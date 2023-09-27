/*
 * Copyright 2023 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.jpa.provider.PersistenceProviderUtils.*;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Test cases to verify {@link PersistenceProviderUtils}.
 * 
 * @author Greg Turnquist
 */
class PersistenceProviderUtilsTests {

	@Test
	void hideHibernateWorks() {

		assertThat(PersistenceProvider.HIBERNATE.isPresent()).isTrue();

		doWithHibernateHidden(() -> {
			assertThat(PersistenceProvider.HIBERNATE.isPresent()).isFalse();
		});

		assertThat(PersistenceProvider.HIBERNATE.isPresent()).isTrue();
	}

	@Test
	void hideEclipseLinkWorks() {

		assertThat(PersistenceProvider.ECLIPSELINK.isPresent()).isTrue();

		doWithEclipseLinkHidden(() -> {
			assertThat(PersistenceProvider.ECLIPSELINK.isPresent()).isFalse();
		});

		assertThat(PersistenceProvider.ECLIPSELINK.isPresent()).isTrue();
	}

	@Test
	void hideHibernateAndEclipseLinkWorks() {

		assertThat(PersistenceProvider.HIBERNATE.isPresent()).isTrue();
		assertThat(PersistenceProvider.ECLIPSELINK.isPresent()).isTrue();

		doWithPersistenceProvidersHidden(List.of(PersistenceProvider.HIBERNATE, PersistenceProvider.ECLIPSELINK), () -> {

			assertThat(PersistenceProvider.HIBERNATE.isPresent()).isFalse();
			assertThat(PersistenceProvider.ECLIPSELINK.isPresent()).isFalse();
		});

		assertThat(PersistenceProvider.HIBERNATE.isPresent()).isTrue();
		assertThat(PersistenceProvider.ECLIPSELINK.isPresent()).isTrue();
	}

	@Test
	@HidePersistenceProviders(PersistenceProvider.HIBERNATE)
	void hideHibernateGenericallyShouldWork() {

		assertThat(PersistenceProvider.HIBERNATE.isPresent()).isFalse();
		assertThat(PersistenceProvider.ECLIPSELINK.isPresent()).isTrue();
	}

	@Test
	@HidePersistenceProviders(PersistenceProvider.ECLIPSELINK)
	void hideEclipseLinkGenericallyShouldWork() {

		assertThat(PersistenceProvider.HIBERNATE.isPresent()).isTrue();
		assertThat(PersistenceProvider.ECLIPSELINK.isPresent()).isFalse();
	}

	@Test
	@HidePersistenceProviders({ PersistenceProvider.HIBERNATE, PersistenceProvider.ECLIPSELINK })
	void hideHibernateAndEclipseLinkGenericallyShouldWork() {

		assertThat(PersistenceProvider.HIBERNATE.isPresent()).isFalse();
		assertThat(PersistenceProvider.ECLIPSELINK.isPresent()).isFalse();
	}

	@Test
	@HideHibernate
	void hideHibernateViaHibernateSpecificAnnotationWorks() {

		assertThat(PersistenceProvider.HIBERNATE.isPresent()).isFalse();
		assertThat(PersistenceProvider.ECLIPSELINK.isPresent()).isTrue();
	}

	@Test
	@HideEclipseLink
	void hideEclipseLinkViaEclipseLinkSpecificAnnotationWorks() {

		assertThat(PersistenceProvider.HIBERNATE.isPresent()).isTrue();
		assertThat(PersistenceProvider.ECLIPSELINK.isPresent()).isFalse();
	}
}
