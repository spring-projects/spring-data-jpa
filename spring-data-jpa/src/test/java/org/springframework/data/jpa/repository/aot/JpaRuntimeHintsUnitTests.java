/*
 * Copyright 2022-present the original author or authors.
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.aot.hint.predicate.RuntimeHintsPredicates.reflection;

import jakarta.persistence.EntityManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.aspectj.AnnotationBeanConfigurerAspect;
import org.springframework.data.jpa.domain.support.AuditingBeanFactoryPostProcessor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.util.HidingClassLoader;
import org.springframework.util.ClassUtils;

/**
 * Unit tests for {@link JpaRuntimeHints}.
 *
 * @author Christoph Strobl
 */
class JpaRuntimeHintsUnitTests {

	@Test // GH-2497
	void registersAuditing() {

		RuntimeHints hints = new RuntimeHints();

		JpaRuntimeHints registrar = new JpaRuntimeHints();
		registrar.registerHints(hints, null);

		assertThat(hints).matches(reflection().onType(AnnotationBeanConfigurerAspect.class))
				.matches(reflection().onType(AuditingEntityListener.class))
				.matches(reflection().onType(AuditingBeanFactoryPostProcessor.class));
	}

	@Test // GH-2497
	void skipsAuditingHintsIfAspectjNotPresent() {

		RuntimeHints hints = new RuntimeHints();

		JpaRuntimeHints registrar = new JpaRuntimeHints();
		registrar.registerHints(hints, HidingClassLoader.hidePackages("org.springframework.beans.factory.aspectj"));

		assertThat(hints).matches(reflection().onType(AnnotationBeanConfigurerAspect.class).negate())
				.matches(reflection().onType(AuditingEntityListener.class).negate())
				.matches(reflection().onType(AuditingBeanFactoryPostProcessor.class).negate());
	}

	@Test // GH-4197
	void skipsHibernateIfNotPresent() {

		RuntimeHints hints = new RuntimeHints();

		new JpaRuntimeHints().registerHints(hints, HidingClassLoader.hidePackages("org.hibernate"));

		assertThat(hints).matches(reflection().onType(TypeReference.of("org.hibernate.query.spi.SqmQuery")).negate());
	}

	@Test // GH-4197
	void registersMethodsUsedByTheJakartaPersistenceAdapter() throws NoSuchMethodException {

		RuntimeHints hints = new RuntimeHints();

		new JpaRuntimeHints().registerHints(hints, null);

		// the overloads JpaPortableQueries resolves reflectively
		assertThat(hints)
				.matches(reflection().onMethodInvocation(EntityManager.class.getMethod("createQuery", String.class)))
				.matches(reflection().onMethodInvocation(EntityManager.class.getMethod("createNamedQuery", String.class)))
				.matches(reflection().onMethodInvocation(EntityManager.class.getMethod("createNativeQuery", String.class)))
				.matches(reflection()
						.onMethodInvocation(EntityManager.class.getMethod("createNativeQuery", String.class, Class.class)))
				.matches(reflection()
						.onMethodInvocation(EntityManager.class.getMethod("createNativeQuery", String.class, String.class)));
	}

	@Test // GH-4197
	void registersContractsResolvedByTheHibernateAdapter/*Hibernate 7 vs 8 */() throws NoSuchMethodException {

		Map<String, List<String>> contracts = new LinkedHashMap<>();
		contracts.put("org.hibernate.query.spi.SqmQuery", List.of("getSqmStatement"));
		contracts.put("org.hibernate.query.sqm.spi.SqmStatementAccess", List.of("getSqmStatement"));
		contracts.put("org.hibernate.query.MutationOrSelectionQuery", List.of("isSelectionQuery", "asSelectionQuery"));
		contracts.put("org.hibernate.query.sqm.spi.NamedSqmQueryMemento", List.of("getHqlString", "getSqmStatement"));
		contracts.put("org.hibernate.query.named.NamedSqmQueryMemento", List.of("getHqlString", "getSqmStatement"));
		contracts.put("org.hibernate.query.named.spi.NamedSqmQueryMemento", List.of("getHqlString", "getSqmStatement"));
		contracts.put("org.hibernate.query.sql.spi.NamedNativeQueryMemento", List.of("getSqlString"));
		contracts.put("org.hibernate.query.named.NamedNativeQueryMemento", List.of("getSqlString"));
		contracts.put("org.hibernate.query.named.spi.NamedNativeQueryMemento", List.of("getSqlString"));
		contracts.put("org.hibernate.query.sqm.tree.SqmVisitableNode", List.of("toHqlString"));
		contracts.put("org.hibernate.query.sqm.tree.spi.SqmVisitableNode", List.of("toHqlString"));

		RuntimeHints hints = new RuntimeHints();

		new JpaRuntimeHints().registerHints(hints, null);

		boolean verified = false;

		// check if registered correctly relying on build profiles for hibernate 7 and 8
		for (Map.Entry<String, List<String>> contract : contracts.entrySet()) {

			if (!ClassUtils.isPresent(contract.getKey(), null)) {
				continue;
			}

			Class<?> type = ClassUtils.resolveClassName(contract.getKey(), null);

			for (String methodName : contract.getValue()) {

				assertThat(hints).matches(reflection().onMethodInvocation(type.getMethod(methodName)));
				verified = true;
			}
		}

		assertThat(verified).isTrue();
	}
}
