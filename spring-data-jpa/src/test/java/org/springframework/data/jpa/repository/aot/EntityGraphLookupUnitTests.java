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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManagerFactory;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.config.AotRepositoryInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;

/**
 * Unit tests for {@link EntityGraphLookup}.
 *
 * @author LordKay-sudo
 */
class EntityGraphLookupUnitTests {

	@Test // GH-4166
	void resolvesNamedEntityGraphFromAnnotationMetadataWithoutEntityManagerFactoryGraphs() throws NoSuchMethodException {

		EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);
		when(entityManagerFactory.getNamedEntityGraphs(User.class)).thenReturn(Map.of());

		JpaAnnotationMetadata annotationMetadata = JpaAnnotationMetadata.from(List.of(User.class));
		EntityGraphLookup lookup = new EntityGraphLookup(entityManagerFactory, annotationMetadata);

		RepositoryInformation repositoryInformation = new AotRepositoryInformation(
				AbstractRepositoryMetadata.getMetadata(NamedEntityGraphRepository.class),
				NamedEntityGraphRepository.class, Collections.emptyList());

		Method method = NamedEntityGraphRepository.class.getMethod("findWithNamedEntityGraphByFirstname", String.class);
		JpaQueryMethod queryMethod = new JpaQueryMethod(method, repositoryInformation,
				new SpelAwareProxyProjectionFactory(), mock(QueryExtractor.class));

		AotEntityGraph entityGraph = lookup.findEntityGraph(MergedAnnotations.from(method).get(EntityGraph.class),
				repositoryInformation, queryMethod.getResultProcessor().getReturnedType(), queryMethod);

		assertThat(entityGraph).isNotNull();
		assertThat(entityGraph.name()).isEqualTo("User.detail");
		assertThat(entityGraph.type()).isEqualTo(EntityGraph.EntityGraphType.FETCH);
		assertThat(entityGraph.attributePaths()).isEmpty();
	}

	interface NamedEntityGraphRepository extends Repository<User, Long> {

		@EntityGraph(type = EntityGraph.EntityGraphType.FETCH, value = "User.detail")
		User findWithNamedEntityGraphByFirstname(String firstname);
	}

}
