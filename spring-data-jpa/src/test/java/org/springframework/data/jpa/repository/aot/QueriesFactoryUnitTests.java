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
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
 * @author Greg Taube
 */
class QueriesFactoryUnitTests {

	QueriesFactory factory;

	@BeforeEach
	void setUp() {

		RepositoryConfigurationSource configSource = mock(RepositoryConfigurationSource.class);
		EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);
		Metamodel metamodel = mock(Metamodel.class);

		when(entityManagerFactory.getMetamodel()).thenReturn(metamodel);
		when(metamodel.entity(MyEntity.class)).thenReturn(mock(EntityType.class));

		factory = new QueriesFactory(configSource, entityManagerFactory, this.getClass().getClassLoader());
	}

	@Test // GH-4029
	void stringQueryShouldResolveEntityNameFromJakartaAnnotationIfPresent() throws NoSuchMethodException {

		RepositoryInformation repositoryInformation = new AotRepositoryInformation(
				AbstractRepositoryMetadata.getMetadata(MyRepository.class), MyRepository.class, Collections.emptyList());

		Method method = MyRepository.class.getMethod("someFind", Pageable.class);
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

	@Test // GH-4338
	void doesNotCreateCountQueryForNonPageStringQuery() throws NoSuchMethodException {
		assertThat(createQueries("someCollectionFind").count()).isNotInstanceOf(StringAotQuery.class);
	}

	@Test // GH-4338
	void doesNotCreateCountQueryForNonPageNamedQuery() throws NoSuchMethodException {
		assertThat(createQueries("someNamedFind").count()).isNotInstanceOf(StringAotQuery.class);
	}

	@Test // GH-4338
	void doesNotCreateCountQueryForNonPagePartTreeQuery() throws NoSuchMethodException {
		assertThat(createQueries("findAllBy").count()).isNotInstanceOf(StringAotQuery.class);
	}

	private AotQueries createQueries(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {

		RepositoryInformation repositoryInformation = new AotRepositoryInformation(
				AbstractRepositoryMetadata.getMetadata(MyRepository.class), MyRepository.class, Collections.emptyList());

		Method method = MyRepository.class.getMethod(methodName, parameterTypes);
		JpaQueryMethod queryMethod = new JpaQueryMethod(method, repositoryInformation,
				new SpelAwareProxyProjectionFactory(), mock(QueryExtractor.class));

		return factory.createQueries(repositoryInformation, queryMethod.getResultProcessor().getReturnedType(),
				QueryEnhancerSelector.DEFAULT_SELECTOR, MergedAnnotations.from(method).get(Query.class), queryMethod);
	}

	interface MyRepository extends Repository<MyEntity, Long> {

		@Query("select t from #{#entityName} t")
		Page<MyEntity> someFind(Pageable pageable);

		@Query(value = "select t from #{#entityName} t", countQuery = "not a valid query")
		Collection<MyEntity> someCollectionFind();

		@Query(name = "User.findBySpringDataNamedQuery", countQuery = "not a valid query")
		Collection<MyEntity> someNamedFind();

		Collection<MyEntity> findAllBy();
	}

	@Entity(name = "CustomNamed")
	static class MyEntity {

		@Id Long id;

	}
}
