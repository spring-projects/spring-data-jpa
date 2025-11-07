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
package org.springframework.data.jpa.repository.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearSystemProperty;
import org.junitpioneer.jupiter.SetSystemProperty;

import org.springframework.aot.AotDetector;
import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.aot.AotContext;
import org.springframework.data.jpa.repository.aot.JpaRepositoryContributor;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.config.AotRepositoryContextSupport;
import org.springframework.data.repository.config.AotRepositoryInformation;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.javapoet.ClassName;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;

/**
 * Unit tests for {@link JpaRepositoryConfigExtension.JpaRepositoryRegistrationAotProcessor}.
 *
 * @author Christoph Strobl
 * @author Hyunsang Han
 * @author Mark Paluch
 */
class JpaRepositoryRegistrationAotProcessorUnitTests {

	@Test // GH-2628
	@Disabled("TODO: Superfluous contributeType in Commons")
	void aotProcessorMustNotRegisterDomainTypes() {

		GenerationContext ctx = createGenerationContext();
		GenericApplicationContext context = new GenericApplicationContext();

		new JpaRepositoryConfigExtension.JpaRepositoryRegistrationAotProcessor()
				.configureTypeContributions(new DummyAotRepositoryContext(context) {
					@Override
					public Set<Class<?>> getResolvedTypes() {
						return Collections.singleton(Person.class);
					}
				}, ctx);

		assertThat(RuntimeHintsPredicates.reflection().onType(Person.class)).rejects(ctx.getRuntimeHints());
	}

	@Test // GH-2628
	void aotProcessorMustNotRegisterAnnotations() {

		GenerationContext ctx = createGenerationContext();

		GenericApplicationContext context = new GenericApplicationContext();
		new JpaRepositoryConfigExtension.JpaRepositoryRegistrationAotProcessor()
				.configureTypeContributions(new DummyAotRepositoryContext(context) {

					@Override
					public Set<MergedAnnotation<Annotation>> getResolvedAnnotations() {

						MergedAnnotation mergedAnnotation = MergedAnnotation.of(Entity.class);
						return Set.of(mergedAnnotation);
					}
				}, ctx);

		assertThat(RuntimeHintsPredicates.reflection().onType(Entity.class)).rejects(ctx.getRuntimeHints());
	}

	@Test // GH-3838
	void repositoryProcessorShouldConsiderPersistenceManagedTypes() {

		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean(PersistenceManagedTypes.class, () -> {

			return new PersistenceManagedTypes() {
				@Override
				public List<String> getManagedClassNames() {
					return List.of(Person.class.getName());
				}

				@Override
				public List<String> getManagedPackages() {
					return List.of();
				}

				@Override
				public @Nullable URL getPersistenceUnitRootUrl() {
					return null;
				}
			};
		});

		JpaRepositoryContributor contributor = new JpaRepositoryConfigExtension.JpaRepositoryRegistrationAotProcessor()
				.contributeAotRepository(new DummyAotRepositoryContext(context));

		assertThat(contributor.getPersistenceUnit().getMetamodel().managedType(Person.class)).isNotNull();
	}

	@Test // GH-3899
	@SetSystemProperty(key = AotDetector.AOT_ENABLED, value = "true")
	void repositoryProcessorShouldEnableAotRepositoriesByDefaultWhenAotIsEnabled() {

		GenericApplicationContext context = new GenericApplicationContext();

		JpaRepositoryContributor contributor = createContributorWithPersonTypes(context);

		assertThat(contributor).isNotNull();
	}

	@Test // GH-3899
	@ClearSystemProperty(key = AotContext.GENERATED_REPOSITORIES_ENABLED)
	void shouldEnableAotRepositoriesByDefault() {

		GenericApplicationContext context = new GenericApplicationContext();

		JpaRepositoryContributor contributor = createContributorWithPersonTypes(context);

		assertThat(contributor).isNotNull();
	}

	@Test // GH-3899
	@SetSystemProperty(key = AotContext.GENERATED_REPOSITORIES_ENABLED, value = "false")
	void shouldDisableAotRepositoriesWhenGeneratedRepositoriesIsFalse() {

		GenericApplicationContext context = new GenericApplicationContext();

		JpaRepositoryContributor contributor = createContributorWithPersonTypes(context);

		assertThat(contributor).isNull();
	}

	@Test // GH-3899
	@SetSystemProperty(key = "spring.aot.jpa.repositories.enabled", value = "false")
	void shouldDisableAotRepositoriesWhenJpaGeneratedRepositoriesIsFalse() {

		GenericApplicationContext context = new GenericApplicationContext();

		JpaRepositoryContributor contributor = createContributorWithPersonTypes(context);

		assertThat(contributor).isNull();
	}

	private GenerationContext createGenerationContext() {
		return new DefaultGenerationContext(new ClassNameGenerator(ClassName.OBJECT),
				new InMemoryGeneratedFiles());
	}

	private JpaRepositoryContributor createContributorWithPersonTypes(GenericApplicationContext context) {

		return new JpaRepositoryConfigExtension.JpaRepositoryRegistrationAotProcessor()
				.contributeAotRepository(new DummyAotRepositoryContext(context) {
			@Override
			public Set<Class<?>> getResolvedTypes() {
				return Collections.singleton(Person.class);
			}
				});
	}

	@Entity
	static class Person {
		@Id Long id;
	}

	interface PersonRepository extends Repository<Person, Long> {}

	static class DummyAotRepositoryContext extends AotRepositoryContextSupport {

		private final AbstractApplicationContext applicationContext;

		DummyAotRepositoryContext(AbstractApplicationContext applicationContext) {
			super(AotContext.from(applicationContext, applicationContext.getEnvironment()));
			this.applicationContext = applicationContext;
		}

		@Override
		public String getModuleName() {
			return "JPA";
		}

		@Override
		public RepositoryConfigurationSource getConfigurationSource() {
			return mock(RepositoryConfigurationSource.class);
		}

		@Override
		public Set<Class<? extends Annotation>> getIdentifyingAnnotations() {
			return Collections.singleton(Entity.class);
		}

		@Override
		public RepositoryInformation getRepositoryInformation() {
			return new AotRepositoryInformation(AbstractRepositoryMetadata.getMetadata(PersonRepository.class),
					SimpleJpaRepository.class, List.of());
		}

		@Override
		public Set<MergedAnnotation<Annotation>> getResolvedAnnotations() {
			return Set.of();
		}

		@Override
		public Set<Class<?>> getResolvedTypes() {
			return Set.of();
		}

		@Override
		public ConfigurableListableBeanFactory getBeanFactory() {
			return applicationContext != null ? applicationContext.getBeanFactory() : null;
		}

		@Override
		public Environment getEnvironment() {
			return applicationContext == null ? new StandardEnvironment() : applicationContext.getEnvironment();
		}


	}

}
