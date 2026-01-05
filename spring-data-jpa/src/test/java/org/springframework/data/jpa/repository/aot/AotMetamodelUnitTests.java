/*
 * Copyright 2025-present the original author or authors.
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
package org.springframework.data.jpa.repository.aot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.aot.AotMetamodel.NoOpConnectionProvider;
import org.springframework.data.jpa.repository.aot.AotMetamodel.SpringDataJpaAotDialect;

/**
 * Unit tests for {@link AotMetamodel}.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 */
class AotMetamodelUnitTests {

	@Test // GH-4103
	void dialectSupportsSequences() {
		assertThat(AotMetamodel.SpringDataJpaAotDialect.INSTANCE.getSequenceSupport().supportsSequences()).isTrue();
		assertThat(AotMetamodel.SpringDataJpaAotDialect.INSTANCE.getSequenceSupport().supportsPooledSequences()).isTrue();
	}

	@Test // GH-4092
	void intializesPropertiesWithDefaults() {

		assertThat(AotMetamodel.initProperties(Map.of())) //
				.containsEntry("hibernate.dialect", SpringDataJpaAotDialect.INSTANCE) //
				.containsEntry("hibernate.boot.allow_jdbc_metadata_access", false) //
				.containsEntry("hibernate.connection.provider_class", NoOpConnectionProvider.INSTANCE) //
				.containsEntry("hibernate.jpa_callbacks.enabled", false) //
				.containsEntry("hibernate.query.startup_check", false);
	}

	@Test // GH-4092
	void allowsDialectOverridesViaProperties() {

		assertThat(AotMetamodel.initProperties(Map.of("hibernate.dialect", "H2Dialect", "jpa.bla.bla", "42")))
				.containsEntry("hibernate.dialect", "H2Dialect") //
				.containsEntry("jpa.bla.bla", "42");
	}

	@Test // GH-4092
	void preventsPropertyOverrides/* for cases we know cause trouble */() {

		assertThat(AotMetamodel.initProperties(Map.of(//
				"hibernate.boot.allow_jdbc_metadata_access", "true", //
				"hibernate.connection.provider_class", "DatasourceConnectionProviderImpl", //
				"hibernate.jpa_callbacks.enabled", "true", //
				"hibernate.query.startup_check", "true"))).containsEntry("hibernate.boot.allow_jdbc_metadata_access", false) //
				.containsEntry("hibernate.connection.provider_class", NoOpConnectionProvider.INSTANCE) //
				.containsEntry("hibernate.jpa_callbacks.enabled", false) //
				.containsEntry("hibernate.query.startup_check", false);
	}
}
