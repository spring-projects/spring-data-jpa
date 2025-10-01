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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManagerFactory;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.JpaEntityMetadata;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.data.jpa.repository.query.QueryEnhancerSelector;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.query.ReturnedType;

/**
 * Unit tests for {@link QueriesFactory}.
 *
 * @author Christoph Strobl
 */
class QueriesFactoryUnitTests {

	QueriesFactory factory;

	@BeforeEach
	void setUp() {

		RepositoryConfigurationSource configSource = Mockito.mock(RepositoryConfigurationSource.class);
		EntityManagerFactory entityManagerFactory = Mockito.mock(EntityManagerFactory.class);

		factory = new QueriesFactory(configSource, entityManagerFactory, this.getClass().getClassLoader());
	}

	@Test // GH-4029
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void stringQueryShouldResolveEntityNameFromJakartaAnnotationIfPresent() {

		RepositoryInformation repositoryInformation = Mockito.mock(RepositoryInformation.class);
		JpaEntityMetadata<?> entityMetadata = Mockito.mock(JpaEntityInformation.class);
		when(entityMetadata.getEntityName()).thenReturn("CustomNamed");

		MergedAnnotation<Query> queryAnnotation = Mockito.mock(MergedAnnotation.class);
		when(queryAnnotation.isPresent()).thenReturn(true);
		when(queryAnnotation.getString(eq("value"))).thenReturn("select t from #{#entityName} t");
		when(queryAnnotation.getBoolean(eq("nativeQuery"))).thenReturn(false);
		when(queryAnnotation.getString("countQuery")).thenReturn("select count(t) from #{#entityName} t");

		JpaQueryMethod queryMethod = Mockito.mock(JpaQueryMethod.class);
		when(queryMethod.getEntityInformation()).thenReturn((JpaEntityMetadata) entityMetadata);

		AotQueries generatedQueries = factory.createQueries(repositoryInformation,
				ReturnedType.of(Object.class, Object.class, new SpelAwareProxyProjectionFactory()),
				QueryEnhancerSelector.DEFAULT_SELECTOR, queryAnnotation, queryMethod);

		assertThat(generatedQueries.result()).asInstanceOf(InstanceOfAssertFactories.type(StringAotQuery.class))
				.extracting(StringAotQuery::getQueryString).isEqualTo("select t from CustomNamed t");
		assertThat(generatedQueries.count()).asInstanceOf(InstanceOfAssertFactories.type(StringAotQuery.class))
				.extracting(StringAotQuery::getQueryString).isEqualTo("select count(t) from CustomNamed t");
	}
}
