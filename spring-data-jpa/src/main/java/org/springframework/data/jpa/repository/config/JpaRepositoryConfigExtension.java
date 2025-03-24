/*
 * Copyright 2012-2025 the original author or authors.
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

import static org.springframework.data.jpa.repository.config.BeanDefinitionNames.*;

import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.aot.AotContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.aot.generated.JpaRepositoryContributor;
import org.springframework.data.jpa.repository.support.DefaultJpaContext;
import org.springframework.data.jpa.repository.support.EntityManagerBeanDefinitionRegistrarPostProcessor;
import org.springframework.data.jpa.repository.support.JpaEvaluationContextExtension;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.aot.generate.RepositoryContributor;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.AotRepositoryContext;
import org.springframework.data.repository.config.ImplementationDetectionConfiguration;
import org.springframework.data.repository.config.ImplementationLookupConfiguration;
import org.springframework.data.repository.config.RepositoryConfiguration;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryRegistrationAotProcessor;
import org.springframework.data.repository.config.XmlRepositoryConfigurationSource;
import org.springframework.data.util.Streamable;
import org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * JPA specific configuration extension parsing custom attributes from the XML namespace and
 * {@link EnableJpaRepositories} annotation. Also, it registers bean definitions for a
 * {@link PersistenceAnnotationBeanPostProcessor} (to trigger injection into {@link PersistenceContext}/
 * {@link PersistenceUnit} annotated properties and methods) as well as
 * {@link PersistenceExceptionTranslationPostProcessor} to enable exception translation of persistence specific
 * exceptions into Spring's {@link DataAccessException} hierarchy.
 *
 * @author Oliver Gierke
 * @author Eberhard Wolff
 * @author Gil Markham
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @author Mark Paluch
 */
public class JpaRepositoryConfigExtension extends RepositoryConfigurationExtensionSupport {

	private static final Class<?> PAB_POST_PROCESSOR = PersistenceAnnotationBeanPostProcessor.class;
	private static final String DEFAULT_TRANSACTION_MANAGER_BEAN_NAME = "transactionManager";
	private static final String ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE = "enableDefaultTransactions";
	private static final String JPA_METAMODEL_CACHE_CLEANUP_CLASSNAME = "org.springframework.data.jpa.util.JpaMetamodelCacheCleanup";
	private static final String ESCAPE_CHARACTER_PROPERTY = "escapeCharacter";

	private final Map<Object, String> entityManagerRefs = new LinkedHashMap<>();

	@Override
	public String getModuleName() {
		return "JPA";
	}

	@Override
	public String getRepositoryFactoryBeanClassName() {
		return JpaRepositoryFactoryBean.class.getName();
	}

	@Override
	protected String getModulePrefix() {
		return getModuleName().toLowerCase(Locale.US);
	}

	@Override
	protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
		return Arrays.asList(Entity.class, MappedSuperclass.class);
	}

	@Override
	protected Collection<Class<?>> getIdentifyingTypes() {
		return Collections.<Class<?>> singleton(JpaRepository.class);
	}

	@Override
	public void postProcess(BeanDefinitionBuilder builder, RepositoryConfigurationSource source) {

		Optional<String> transactionManagerRef = source.getAttribute("transactionManagerRef");
		builder.addPropertyValue("transactionManager", transactionManagerRef.orElse(DEFAULT_TRANSACTION_MANAGER_BEAN_NAME));
		if (entityManagerRefs.containsKey(source)) {
			builder.addPropertyReference("entityManager", entityManagerRefs.get(source));
		}
		builder.addPropertyValue(ESCAPE_CHARACTER_PROPERTY, getEscapeCharacter(source).orElse('\\'));
		builder.addPropertyReference("mappingContext", JPA_MAPPING_CONTEXT_BEAN_NAME);

		if (source instanceof AnnotationRepositoryConfigurationSource) {
			builder.addPropertyValue("queryEnhancerSelector",
					source.getAttribute("queryEnhancerSelector", Class.class).orElse(null));
		}
	}

	@Override
	public Class<? extends BeanRegistrationAotProcessor> getRepositoryAotProcessor() {
		return JpaRepositoryRegistrationAotProcessor.class;
	}

	/**
	 * XML configurations do not support {@link Character} values. This method catches the exception thrown and returns an
	 * {@link Optional#empty()} instead.
	 */
	private static Optional<Character> getEscapeCharacter(RepositoryConfigurationSource source) {

		try {
			return source.getAttribute(ESCAPE_CHARACTER_PROPERTY, Character.class);
		} catch (IllegalArgumentException ___) {
			return Optional.empty();
		}
	}

	@Override
	public void postProcess(BeanDefinitionBuilder builder, AnnotationRepositoryConfigurationSource config) {

		AnnotationAttributes attributes = config.getAttributes();

		builder.addPropertyValue(ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE,
				attributes.getBoolean(ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE));
	}

	@Override
	public void postProcess(BeanDefinitionBuilder builder, XmlRepositoryConfigurationSource config) {

		Optional<String> enableDefaultTransactions = config.getAttribute(ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE);

		if (enableDefaultTransactions.isPresent() && StringUtils.hasText(enableDefaultTransactions.get())) {
			builder.addPropertyValue(ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE, enableDefaultTransactions.get());
		}
	}

	@Override
	public void registerBeansForRoot(BeanDefinitionRegistry registry, RepositoryConfigurationSource config) {

		super.registerBeansForRoot(registry, config);

		registerSharedEntityMangerIfNotAlreadyRegistered(registry, config);

		Object source = config.getSource();

		registerLazyIfNotAlreadyRegistered(
				() -> new RootBeanDefinition(EntityManagerBeanDefinitionRegistrarPostProcessor.class), registry,
				EM_BEAN_DEFINITION_REGISTRAR_POST_PROCESSOR_BEAN_NAME, source);

		registerLazyIfNotAlreadyRegistered(() -> new RootBeanDefinition(JpaMetamodelMappingContextFactoryBean.class),
				registry, JPA_MAPPING_CONTEXT_BEAN_NAME, source);

		registerLazyIfNotAlreadyRegistered(() -> new RootBeanDefinition(PAB_POST_PROCESSOR), registry,
				AnnotationConfigUtils.PERSISTENCE_ANNOTATION_PROCESSOR_BEAN_NAME, source);

		// Register bean definition for DefaultJpaContext

		registerLazyIfNotAlreadyRegistered(() -> {

			RootBeanDefinition contextDefinition = new RootBeanDefinition(DefaultJpaContext.class);
			contextDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);

			return contextDefinition;
		}, registry, JPA_CONTEXT_BEAN_NAME, source);

		registerIfNotAlreadyRegistered(() -> new RootBeanDefinition(JPA_METAMODEL_CACHE_CLEANUP_CLASSNAME), registry,
				JPA_METAMODEL_CACHE_CLEANUP_CLASSNAME, source);

		// EvaluationContextExtension for JPA specific SpEL functions

		registerIfNotAlreadyRegistered(() -> {

			Object value = config instanceof AnnotationRepositoryConfigurationSource //
					? config.getRequiredAttribute(ESCAPE_CHARACTER_PROPERTY, Character.class) //
					: config.getAttribute(ESCAPE_CHARACTER_PROPERTY).orElse("\\");

			BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(JpaEvaluationContextExtension.class);
			builder.addConstructorArgValue(value);

			return builder.getBeanDefinition();
		}, registry, JpaEvaluationContextExtension.class.getName(), source);
	}

	private String registerSharedEntityMangerIfNotAlreadyRegistered(BeanDefinitionRegistry registry,
			RepositoryConfigurationSource config) {

		String entityManagerBeanRef = getEntityManagerBeanRef(config);
		String entityManagerBeanName = "jpaSharedEM_" + entityManagerBeanRef;

		if (!registry.containsBeanDefinition(entityManagerBeanName)) {

			AbstractBeanDefinition entityManager = getEntityManagerBeanDefinitionFor(config, null);
			entityManager.setRole(BeanDefinition.ROLE_SUPPORT);
			entityManager.setPrimary(false);
			entityManager.setAutowireCandidate(false);

			registry.registerBeanDefinition(entityManagerBeanName, entityManager);
		}

		entityManagerRefs.put(config, entityManagerBeanName);
		return entityManagerBeanName;
	}

	@Override
	protected @Nullable ClassLoader getConfigurationInspectionClassLoader(ResourceLoader loader) {

		ClassLoader classLoader = loader.getClassLoader();

		return classLoader != null && LazyJvmAgent.isActive(loader.getClassLoader())
				? new InspectionClassLoader(classLoader)
				: classLoader;
	}

	/**
	 * Creates an anonymous factory to extract the actual {@link jakarta.persistence.EntityManager} from the
	 * {@link jakarta.persistence.EntityManagerFactory} bean name reference.
	 *
	 * @param config
	 * @param source
	 * @return
	 */
	private static AbstractBeanDefinition getEntityManagerBeanDefinitionFor(RepositoryConfigurationSource config,
			@Nullable Object source) {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.rootBeanDefinition("org.springframework.orm.jpa.SharedEntityManagerCreator");
		builder.setFactoryMethod("createSharedEntityManager");
		builder.addConstructorArgReference(getEntityManagerBeanRef(config));

		AbstractBeanDefinition bean = builder.getRawBeanDefinition();
		bean.setSource(source);

		return bean;
	}

	private static String getEntityManagerBeanRef(RepositoryConfigurationSource config) {

		Optional<String> entityManagerFactoryRef = config.getAttribute("entityManagerFactoryRef");
		return entityManagerFactoryRef.orElse("entityManagerFactory");
	}

	/**
	 * Utility to determine if a lazy Java agent is being used that might transform classes at a later time.
	 *
	 * @author Mark Paluch
	 * @since 2.1
	 */
	static class LazyJvmAgent {

		private static final Set<String> AGENT_CLASSES;

		static {

			Set<String> agentClasses = new LinkedHashSet<>();

			agentClasses.add("org.springframework.instrument.InstrumentationSavingAgent");
			agentClasses.add("org.eclipse.persistence.internal.jpa.deployment.JavaSECMPInitializerAgent");

			AGENT_CLASSES = Collections.unmodifiableSet(agentClasses);
		}

		private LazyJvmAgent() {}

		/**
		 * Determine if any agent is active.
		 *
		 * @return {@literal true} if an agent is active.
		 */
		static boolean isActive(@Nullable ClassLoader classLoader) {

			return AGENT_CLASSES.stream() //
					.anyMatch(agentClass -> ClassUtils.isPresent(agentClass, classLoader));
		}
	}

	/**
	 * A {@link RepositoryRegistrationAotProcessor} implementation that maintains aot repository setup but skips domain
	 * type inspection which is handled by the core framework support for
	 * {@link org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes}.
	 *
	 * @since 3.0
	 */
	public static class JpaRepositoryRegistrationAotProcessor extends RepositoryRegistrationAotProcessor {

		protected RepositoryContributor contribute(AotRepositoryContext repositoryContext, GenerationContext generationContext) {

			// don't register domain types nor annotations.

			if (!AotContext.aotGeneratedRepositoriesEnabled()) {
				return null;
			}

			return new JpaRepositoryContributor(repositoryContext);
		}

		@Nullable
		@Override
		protected RepositoryConfiguration<?> getRepositoryMetadata(RegisteredBean bean) {
			RepositoryConfiguration<?> configuration = super.getRepositoryMetadata(bean);
			if (!configuration.getRepositoryBaseClassName().isEmpty()) {
				return configuration;
			}
			return new Meh<>(configuration);
		}
	}

	/**
	 * I'm just a dirty hack so we can refine the {@link #getRepositoryBaseClassName()} method as we cannot instantiate
	 * the bean safely to extract it form the repository factory in data commons. So we either have a configurable
	 * {@link RepositoryConfiguration} return from
	 * {@link RepositoryRegistrationAotProcessor#getRepositoryMetadata(RegisteredBean)} or change the arrangement and
	 * maybe move the type out of the factoy.
	 *
	 * @param <T>
	 */
	static class Meh<T extends RepositoryConfigurationSource> implements RepositoryConfiguration<T> {

		private RepositoryConfiguration<?> configuration;

		public Meh(RepositoryConfiguration<?> configuration) {
			this.configuration = configuration;
		}

		@Nullable
		@Override
		public Object getSource() {
			return configuration.getSource();
		}

		@Override
		public T getConfigurationSource() {
			return (T) configuration.getConfigurationSource();
		}

		@Override
		public boolean isLazyInit() {
			return configuration.isLazyInit();
		}

		@Override
		public boolean isPrimary() {
			return configuration.isPrimary();
		}

		@Override
		public Streamable<String> getBasePackages() {
			return configuration.getBasePackages();
		}

		@Override
		public Streamable<String> getImplementationBasePackages() {
			return configuration.getImplementationBasePackages();
		}

		@Override
		public String getRepositoryInterface() {
			return configuration.getRepositoryInterface();
		}

		@Override
		public Optional<Object> getQueryLookupStrategyKey() {
			return Optional.ofNullable(configuration.getQueryLookupStrategyKey());
		}

		@Override
		public Optional<String> getNamedQueriesLocation() {
			return configuration.getNamedQueriesLocation();
		}

		@Override
		public Optional<String> getRepositoryBaseClassName() {
			String name = SimpleJpaRepository.class.getName();
			return Optional.of(name);
		}

		@Override
		public String getRepositoryFactoryBeanClassName() {
			return configuration.getRepositoryFactoryBeanClassName();
		}

		@Override
		public String getImplementationBeanName() {
			return configuration.getImplementationBeanName();
		}

		@Override
		public String getRepositoryBeanName() {
			return configuration.getRepositoryBeanName();
		}

		@Override
		public Streamable<TypeFilter> getExcludeFilters() {
			return configuration.getExcludeFilters();
		}

		@Override
		public ImplementationDetectionConfiguration toImplementationDetectionConfiguration(MetadataReaderFactory factory) {
			return configuration.toImplementationDetectionConfiguration(factory);
		}

		@Override
		public ImplementationLookupConfiguration toLookupConfiguration(MetadataReaderFactory factory) {
			return configuration.toLookupConfiguration(factory);
		}

		@Nullable
		@Override
		public String getResourceDescription() {
			return configuration.getResourceDescription();
		}
	}
}
