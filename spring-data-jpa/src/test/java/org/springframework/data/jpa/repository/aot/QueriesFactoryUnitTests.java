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

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.InstanceOfAssertFactories.*;
import static org.mockito.Mockito.*;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.data.jpa.repository.query.QueryEnhancerSelector;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.config.AotRepositoryInformation;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;

/**
 * Unit tests for {@link QueriesFactory}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 */
class QueriesFactoryUnitTests {

	QueriesFactory factory;

	@BeforeEach
	void setUp() {

		RepositoryConfigurationSource configSource = mock(RepositoryConfigurationSource.class);
		EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);

		factory = new QueriesFactory(configSource, entityManagerFactory, this.getClass().getClassLoader());
	}

	@Test // GH-4029
	void stringQueryShouldResolveEntityNameFromJakartaAnnotationIfPresent() throws NoSuchMethodException {

		RepositoryInformation repositoryInformation = new AotRepositoryInformation(
				AbstractRepositoryMetadata.getMetadata(MyRepository.class), MyRepository.class, Collections.emptyList());

		Method method = MyRepository.class.getMethod("someFind");
		JpaQueryMethod queryMethod = new JpaQueryMethod(method, repositoryInformation,
				new SpelAwareProxyProjectionFactory(), mock(QueryExtractor.class));

		AotQueries generatedQueries = factory.createQueries(repositoryInformation,
				queryMethod.getResultProcessor().getReturnedType(), QueryEnhancerSelector.DEFAULT_SELECTOR,
				MergedAnnotations.from(method).get(Query.class), queryMethod);

		assertThat(generatedQueries.result()).asInstanceOf(type(StringAotQuery.class))
				.extracting(StringAotQuery::getQueryString).isEqualTo("select t from CustomNamed t");
		assertThat(generatedQueries.count()).asInstanceOf(type(StringAotQuery.class))
				.extracting(StringAotQuery::getQueryString).isEqualTo("select count(t) from CustomNamed t");
	}

	interface MyRepository extends Repository<MyEntity, Long> {

		@Query("select t from #{#entityName} t")
		Collection<MyEntity> someFind();
	}

	@Entity(name = "CustomNamed")
	static class MyEntity {

		@Id Long id;

	}
}
