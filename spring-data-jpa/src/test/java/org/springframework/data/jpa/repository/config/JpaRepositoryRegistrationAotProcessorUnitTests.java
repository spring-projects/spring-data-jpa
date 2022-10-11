/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.data.jpa.repository.config;

import static org.assertj.core.api.Assertions.*;

import jakarta.persistence.Entity;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.data.repository.aot.AotRepositoryContext;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.javapoet.ClassName;

/**
 * @author Christoph Strobl
 */
class JpaRepositoryRegistrationAotProcessorUnitTests {

	@Test // GH-2628
	void aotProcessorMustNotRegisterDomainTypes() {

		GenerationContext ctx = new DefaultGenerationContext(new ClassNameGenerator(ClassName.OBJECT),
				new InMemoryGeneratedFiles());

		new JpaRepositoryConfigExtension.JpaRepositoryRegistrationAotProcessor()
				.contribute(new DummyAotRepositoryContext() {
					@Override
					public Set<Class<?>> getResolvedTypes() {
						return Collections.singleton(Person.class);
					}
				}, ctx);

		assertThat(RuntimeHintsPredicates.reflection().onType(Person.class)).rejects(ctx.getRuntimeHints());
	}

	@Test // GH-2628
	void aotProcessorMustNotRegisterAnnotations() {

		GenerationContext ctx = new DefaultGenerationContext(new ClassNameGenerator(ClassName.OBJECT),
				new InMemoryGeneratedFiles());

		new JpaRepositoryConfigExtension.JpaRepositoryRegistrationAotProcessor()
				.contribute(new DummyAotRepositoryContext() {

					@Override
					public Set<MergedAnnotation<Annotation>> getResolvedAnnotations() {

						MergedAnnotation mergedAnnotation = MergedAnnotation.of(Entity.class);
						return Set.of(mergedAnnotation);
					}
				}, ctx);

		assertThat(RuntimeHintsPredicates.reflection().onType(Entity.class)).rejects(ctx.getRuntimeHints());
	}

	static class Person {}

	static class DummyAotRepositoryContext implements AotRepositoryContext {

		@Override
		public String getBeanName() {
			return "jpaRepository";
		}

		@Override
		public Set<String> getBasePackages() {
			return Collections.singleton(this.getClass().getPackageName());
		}

		@Override
		public Set<Class<? extends Annotation>> getIdentifyingAnnotations() {
			return Collections.singleton(Entity.class);
		}

		@Override
		public RepositoryInformation getRepositoryInformation() {
			return null;
		}

		@Override
		public Set<MergedAnnotation<Annotation>> getResolvedAnnotations() {
			return null;
		}

		@Override
		public Set<Class<?>> getResolvedTypes() {
			return null;
		}

		@Override
		public ConfigurableListableBeanFactory getBeanFactory() {
			return null;
		}

		@Override
		public TypeIntrospector introspectType(String typeName) {
			return null;
		}

		@Override
		public IntrospectedBeanDefinition introspectBeanDefinition(String beanName) {
			return null;
		}
	}
}
