/*
 * Copyright 2026-present the original author or authors.
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
import static org.springframework.data.jpa.repository.aot.StringAotQuery.*;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.aot.JpaCodeBlocks.QueryBlockBuilder;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.aot.generate.AotQueryMethodGenerationContext;
import org.springframework.data.repository.config.AotRepositoryInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.javapoet.CodeBlock;

/**
 * Unit tests for {@link JpaCodeBlocks}.
 *
 * @author Christoph Strobl
 */
class JpaCodeBlocksUnitTests {

	@Test // GH-4152
	void appliesLockModeIfPresent() throws NoSuchMethodException {

		CodeBlock codeBlock = initQueryBuilder()
				.filter(new AotQueries(jpqlQuery("SELECT d FROM DummyEntity d", List.of(), Limit.unlimited(), false, false))) //
				.lockMode(LockModeType.PESSIMISTIC_READ).build();

		assertThat(codeBlock.toString()).containsSubsequence(".setLockMode(", "LockModeType.PESSIMISTIC_READ)");
	}

	QueryBlockBuilder initQueryBuilder() throws NoSuchMethodException {

		RepositoryInformation repositoryInformation = new AotRepositoryInformation(
				AbstractRepositoryMetadata.getMetadata(MyRepository.class), MyRepository.class, Collections.emptyList());

		Method method = MyRepository.class.getMethod("findByIdEquals", Long.class);

		JpaQueryMethod queryMethod = new JpaQueryMethod(method, repositoryInformation,
				new SpelAwareProxyProjectionFactory(), PersistenceProvider.GENERIC_JPA);
		AotQueryMethodGenerationContext ctx = new DummyJpaAotQueryMethodGenerationContext(repositoryInformation, method,
				queryMethod);

		return JpaCodeBlocks.queryBuilder(ctx, queryMethod);
	}

	static class DummyJpaAotQueryMethodGenerationContext extends AotQueryMethodGenerationContext {

		protected DummyJpaAotQueryMethodGenerationContext(RepositoryInformation repositoryInformation, Method method,
				QueryMethod queryMethod) {
			super(repositoryInformation, method, queryMethod);
		}

		@Override
		public @Nullable String fieldNameOf(Class<?> type) {
			if (type.equals(EntityManager.class)) {
				return "em";
			}
			return super.fieldNameOf(type);
		}
	}

	interface MyRepository extends Repository<DummyEntity, Long> {
		DummyEntity findByIdEquals(Long id);
	}

	@Entity
	static class DummyEntity {
		@Id Long id;
	}

}
