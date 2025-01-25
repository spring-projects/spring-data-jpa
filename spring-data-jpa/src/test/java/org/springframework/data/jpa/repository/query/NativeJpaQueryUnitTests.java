/*
 * Copyright 2024-2025 the original author or authors.
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
package org.springframework.data.jpa.repository.query;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.Metamodel;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.util.ReflectionUtils;

/**
 * Unit tests for {@link NativeJpaQuery}.
 *
 * @author Mark Paluch
 */
@MockitoSettings(strictness = Strictness.LENIENT)
class NativeJpaQueryUnitTests {

	@Mock EntityManager em;
	@Mock EntityManagerFactory emf;
	@Mock Metamodel metamodel;

	@BeforeEach
	void setUp() {

		when(em.getMetamodel()).thenReturn(metamodel);
		when(em.getEntityManagerFactory()).thenReturn(emf);
		when(em.getDelegate()).thenReturn(em);
	}

	@Test // GH-3546
	void shouldApplySorting() {

		Method respositoryMethod = ReflectionUtils.findMethod(TestRepo.class, "find", Sort.class);
		RepositoryMetadata repositoryMetadata = new DefaultRepositoryMetadata(TestRepo.class);
		SpelAwareProxyProjectionFactory projectionFactory = mock(SpelAwareProxyProjectionFactory.class);
		QueryExtractor queryExtractor = mock(QueryExtractor.class);
		JpaQueryMethod queryMethod = new JpaQueryMethod(respositoryMethod, repositoryMetadata, projectionFactory,
				queryExtractor);

		Query annotation = AnnotatedElementUtils.getMergedAnnotation(respositoryMethod, Query.class);

		NativeJpaQuery query = new NativeJpaQuery(queryMethod, em, annotation.value(), annotation.countQuery(),
				new JpaQueryConfiguration(QueryRewriterProvider.simple(), QueryEnhancerSelector.DEFAULT_SELECTOR,
						ValueExpressionDelegate.create(), EscapeCharacter.DEFAULT));
		String sql = query.getSortedQueryString(Sort.by("foo", "bar"), queryMethod.getResultProcessor().getReturnedType());

		assertThat(sql).isEqualTo("SELECT e FROM Employee e order by e.foo asc, e.bar asc");
	}

	interface TestRepo extends Repository<Object, Object> {

		@Query("SELECT e FROM Employee e")
		Object find(Sort sort);
	}

}
