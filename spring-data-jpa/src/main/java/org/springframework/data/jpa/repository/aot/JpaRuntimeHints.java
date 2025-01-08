/*
 * Copyright 2022-2025 the original author or authors.
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

import jakarta.persistence.NamedEntityGraph;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.data.jpa.domain.AbstractAuditable;
import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.data.jpa.domain.support.AuditingBeanFactoryPostProcessor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.support.QuerydslJpaPredicateExecutor;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.QuerydslUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * Runtime hints for JPA AOT processing.
 *
 * @author Christoph Strobl
 * @since 3.0
 */
class JpaRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {

		hints.proxies().registerJdkProxy(org.springframework.data.jpa.repository.support.CrudMethodMetadata.class, //
				org.springframework.aop.SpringProxy.class, //
				org.springframework.aop.framework.Advised.class, //
				org.springframework.core.DecoratingProxy.class);

		if (ClassUtils.isPresent("org.springframework.beans.factory.aspectj.ConfigurableObject", classLoader)) {

			hints.reflection().registerType(
					TypeReference.of("org.springframework.beans.factory.aspectj.AnnotationBeanConfigurerAspect"), hint -> hint
							.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_DECLARED_METHODS));

			hints.reflection().registerTypes(Arrays.asList( //
					TypeReference.of(AuditingBeanFactoryPostProcessor.class), //
					TypeReference.of(AuditingEntityListener.class)),
					hint -> hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
							MemberCategory.INVOKE_DECLARED_METHODS));
		}

		hints.reflection().registerType(TypeReference.of(SimpleJpaRepository.class),
				hint -> hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS));

		// needs to present for evaluating default attribute values in JpaQueryMethod
		hints.reflection().registerType(Query.class, hint -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS));

		// make sure annotations on the fields are visible and allow reflection on protected methods
		hints.reflection().registerTypes(
				List.of(TypeReference.of(AbstractPersistable.class), TypeReference.of(AbstractAuditable.class)),
				hint -> hint.withMembers(MemberCategory.DECLARED_FIELDS, MemberCategory.INVOKE_DECLARED_METHODS));

		if (QuerydslUtils.QUERY_DSL_PRESENT) {

			hints.reflection().registerType(QuerydslJpaPredicateExecutor.class,
					hint -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS)
							.onReachableType(QuerydslPredicateExecutor.class));
		}

		// streaming results requires reflective access to jakarta.persistence.Query#getResultAsStream
		hints.reflection().registerType(jakarta.persistence.Query.class, MemberCategory.INTROSPECT_PUBLIC_METHODS);
		hints.reflection().registerType(jakarta.persistence.Query.class, hint ->
				hint.withMethod("getResultStream", Collections.emptyList(), ExecutableMode.INVOKE));

		hints.reflection().registerType(NamedEntityGraph.class,
				hint -> hint.onReachableType(EntityGraph.class).withMembers(MemberCategory.INVOKE_PUBLIC_METHODS));

		if (ClassUtils.isPresent("org.hibernate.Hibernate", classLoader)) {

			/*
			   Fetching a single results causes: 
			       java.lang.IllegalArgumentException: Class org.hibernate.query.sqm.tree.select.SqmQueryPart[] is instantiated reflectively but was never registered.Register the class by adding "unsafeAllocated" for the class in reflect-config.json.
			       at org.graalvm.nativeimage.builder/com.oracle.svm.core.graal.snippets.SubstrateAllocationSnippets.arrayHubErrorStub(SubstrateAllocationSnippets.java:345)
			       at org.hibernate.internal.util.collections.StandardStack.push(StandardStack.java:48)
			       at org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter.visitQuerySpec(BaseSqmToSqlAstConverter.java:2073)
			   
			   both formats:
			   - org.hibernate.query.sqm.tree.select.SqmQueryPart[]
			   - [Lorg.hibernate.query.sqm.tree.select.SqmQueryPart;
			   seem to be supported via reflect-config. However TypeReference does not support [L...
			 */
			hints.reflection().registerType(TypeReference.of("org.hibernate.query.sqm.tree.select.SqmQueryPart[]"),
					MemberCategory.UNSAFE_ALLOCATED);
		}
	}
}
