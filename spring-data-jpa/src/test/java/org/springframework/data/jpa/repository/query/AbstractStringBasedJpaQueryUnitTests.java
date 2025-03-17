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

import static org.mockito.Mockito.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Metamodel;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.assertj.core.api.Assertions;
import org.assertj.core.util.Arrays;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.ParametersSource;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;

/**
 * Unit tests for {@link AbstractStringBasedJpaQuery}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 */
class AbstractStringBasedJpaQueryUnitTests {

	private static final JpaQueryConfiguration CONFIG = new JpaQueryConfiguration(QueryRewriterProvider.simple(),
			QueryEnhancerSelector.DEFAULT_SELECTOR, ValueExpressionDelegate.create(), EscapeCharacter.DEFAULT);

	@Test // GH-3310
	void shouldNotAttemptToAppendSortIfNoSortArgumentPresent() {

		InvocationCapturingStringQueryStub stringQuery = forMethod(TestRepo.class, "find");
		stringQuery.createQueryWithArguments();

		stringQuery.neverCalled("applySorting");
	}

	@Test // GH-3310, GH-3076
	void shouldRunQueryRewriterOnce() {

		InvocationCapturingStringQueryStub stringQuery = forMethod(TestRepo.class, "find", Sort.class);
		stringQuery.createQueryWithArguments(Sort.unsorted());
		stringQuery.createQueryWithArguments(Sort.unsorted());

		stringQuery.called("applySorting").times(1);
	}

	@Test // GH-3310
	void shouldAppendSortIfSortPresent() {

		InvocationCapturingStringQueryStub stringQuery = forMethod(TestRepo.class, "find", Sort.class);
		stringQuery.createQueryWithArguments(Sort.by("name"));

		stringQuery.called("applySorting").times(1);
	}

	@Test // GH-3311
	void cachesInvocationBasedOnSortArgument() {

		InvocationCapturingStringQueryStub stringQuery = forMethod(TestRepo.class, "find", Sort.class);
		stringQuery.createQueryWithArguments(Sort.by("name"));
		stringQuery.called("applySorting").times(1);

		stringQuery.createQueryWithArguments(Sort.by("name"));
		stringQuery.called("applySorting").times(1);

		stringQuery.createQueryWithArguments(Sort.by("age"));
		stringQuery.called("applySorting").times(2);
	}

	interface TestRepo extends Repository<Object, Object> {

		@Query("SELECT e FROM Employee e")
		Object find();

		@Query("SELECT e FROM Employee e")
		Object find(Sort sort);
	}

	static InvocationCapturingStringQueryStub forMethod(Class<?> repository, String method, Class<?>... args) {

		Method respositoryMethod = ReflectionUtils.findMethod(repository, method, args);
		RepositoryMetadata repositoryMetadata = new DefaultRepositoryMetadata(repository);
		SpelAwareProxyProjectionFactory projectionFactory = Mockito.mock(SpelAwareProxyProjectionFactory.class);
		QueryExtractor queryExtractor = Mockito.mock(QueryExtractor.class);
		JpaQueryMethod queryMethod = new JpaQueryMethod(respositoryMethod, repositoryMetadata, projectionFactory,
				queryExtractor);

		Query query = AnnotatedElementUtils.getMergedAnnotation(respositoryMethod, Query.class);

		return new InvocationCapturingStringQueryStub(respositoryMethod, queryMethod, query.value(), query.countQuery(),
				CONFIG);
	}

	static class InvocationCapturingStringQueryStub extends AbstractStringBasedJpaQuery {

		private final Method targetMethod;
		private final MultiValueMap<String, Arguments> capturedArguments = new LinkedMultiValueMap<>(3);

		InvocationCapturingStringQueryStub(Method targetMethod, JpaQueryMethod queryMethod, String queryString,
				@Nullable String countQueryString, JpaQueryConfiguration queryConfiguration) {
			super(queryMethod, new Supplier<EntityManager>() {

				@Override
				public EntityManager get() {

					EntityManager em = Mockito.mock(EntityManager.class);

					Metamodel meta = mock(Metamodel.class);
					when(em.getMetamodel()).thenReturn(meta);
					when(em.getDelegate()).thenReturn(new Object()); // some generic jpa

					return em;
				}
			}.get(), queryString, countQueryString, queryConfiguration);

			this.targetMethod = targetMethod;
		}

		@Override
		protected QueryProvider applySorting(CachableQuery query) {

			captureInvocation("applySorting", query);

			return super.applySorting(query);
		}

		@Override
		protected jakarta.persistence.Query createJpaQuery(QueryProvider query, Sort sort,
				@Nullable Pageable pageable,
				ReturnedType returnedType) {

			captureInvocation("createJpaQuery", query, sort, pageable, returnedType);

			jakarta.persistence.Query jpaQuery = super.createJpaQuery(query, sort, pageable, returnedType);
			return jpaQuery == null ? Mockito.mock(jakarta.persistence.Query.class) : jpaQuery;
		}

		// --> convenience for tests

		JpaParameters getParameters() {
			return new JpaParameters(ParametersSource.of(targetMethod));
		}

		JpaParametersParameterAccessor getParameterAccessor(Object... args) {
			return new JpaParametersParameterAccessor(getParameters(), args);
		}

		jakarta.persistence.Query createQueryWithArguments(Object... args) {
			return doCreateQuery(getParameterAccessor(args));
		}

		// --> capturing methods

		private void captureInvocation(String key, Object... args) {
			capturedArguments.add(key, new Arguments(args));
		}

		// --> verification methdos

		int getInvocationCount(String method) {

			List<Arguments> invocations = capturedArguments.get(method);
			return invocations != null ? invocations.size() : 0;
		}

		public void neverCalled(String method) {
			called(method).never();
		}

		public Times called(String method) {

			return (invocationCount -> {

				int actualCount = getInvocationCount(method);
				Assertions.assertThat(actualCount)
						.withFailMessage(
								() -> "Expected %d invocations for %s, but recorded %d".formatted(invocationCount, method, actualCount))
						.isEqualTo(invocationCount);
			});
		}

		static class Arguments {

			List<Object> values = new ArrayList<>(3);

			public Arguments(Object... values) {
				this.values = Arrays.asList(values);
			}
		}

		interface Times {

			void times(int invocationCount);

			default void never() {
				times(0);
			}
		}

	}
}
