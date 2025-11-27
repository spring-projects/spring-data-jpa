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
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import jakarta.persistence.spi.PersistenceUnitInfo;

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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.cfg.MappingSettings;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.aot.AotContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.aot.AotEntityManagerFactoryCreator;
import org.springframework.data.jpa.repository.aot.JpaRepositoryContributor;
import org.springframework.data.jpa.repository.support.DefaultJpaContext;
import org.springframework.data.jpa.repository.support.JpaEvaluationContextExtension;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.AotRepositoryContext;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryRegistrationAotProcessor;
import org.springframework.data.repository.config.XmlRepositoryConfigurationSource;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentLruCache;
import org.springframework.util.ObjectUtils;
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
 * @author Hyunsang Han
 */
public class JpaRepositoryConfigExtension extends RepositoryConfigurationExtensionSupport {

	private static final Class<?> PAB_POST_PROCESSOR = PersistenceAnnotationBeanPostProcessor.class;
	private static final String DEFAULT_TRANSACTION_MANAGER_BEAN_NAME = "transactionManager";
	private static final String ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE = "enableDefaultTransactions";
	private static final String JPA_METAMODEL_CACHE_CLEANUP_CLASSNAME = "org.springframework.data.jpa.util.JpaMetamodelCacheCleanup";
	private static final String ESCAPE_CHARACTER_PROPERTY = "escapeCharacter";
	private static final Logger log = LoggerFactory.getLogger(JpaRepositoryConfigExtension.class);

	private final Map<Object, String> entityManagerRefs = new LinkedHashMap<>();

	@Override
	public String getModuleName() {
		return "JPA";
	}

	@Override
	public String getRepositoryBaseClassName() {
		return SimpleJpaRepository.class.getName();
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
			builder.addPropertyValue("entityManager",
					new RuntimeBeanReference(entityManagerRefs.get(source), EntityManager.class));
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

	private void registerSharedEntityMangerIfNotAlreadyRegistered(BeanDefinitionRegistry registry,
			RepositoryConfigurationSource config) {

		String entityManagerBeanRef = getEntityManagerBeanRef(config);
		String sharedEntityManagerBeanRef = lookupSharedEntityManagerBeanRef(entityManagerBeanRef, registry);

		if (sharedEntityManagerBeanRef != null) {
			entityManagerRefs.put(config, sharedEntityManagerBeanRef);
			return;
		}

		String entityManagerBeanName = "jpaSharedEM_" + entityManagerBeanRef;

		if (!registry.containsBeanDefinition(entityManagerBeanName)) {

			AbstractBeanDefinition entityManager = getEntityManagerBeanDefinitionFor(config, null);
			entityManager.setRole(BeanDefinition.ROLE_SUPPORT);
			entityManager.setPrimary(false);
			entityManager.setAutowireCandidate(false);

			registry.registerBeanDefinition(entityManagerBeanName, entityManager);
		}

		entityManagerRefs.put(config, entityManagerBeanName);
	}

	private @Nullable String lookupSharedEntityManagerBeanRef(String entityManagerBeanRef,
			BeanDefinitionRegistry registry) {

		if (!registry.containsBeanDefinition(entityManagerBeanRef)) {
			return null;
		}

		BeanDefinitionRegistry introspect = registry;

		if (introspect instanceof ConfigurableApplicationContext cac
				&& cac.getBeanFactory() instanceof BeanDefinitionRegistry br) {
			introspect = br;
		}

		if (!(introspect instanceof ConfigurableBeanFactory cbf)) {
			return null;
		}

		BeanDefinition beanDefinition = cbf.getMergedBeanDefinition(entityManagerBeanRef);

		if (ObjectUtils.isEmpty(beanDefinition.getBeanClassName())) {
			return null;
		}

		Class<?> beanClass = org.springframework.data.util.ClassUtils.loadIfPresent(beanDefinition.getBeanClassName(),
				getClass().getClassLoader());

		// AbstractEntityManagerFactoryBean is able to create a SharedEntityManager
		return beanClass != null && AbstractEntityManagerFactoryBean.class.isAssignableFrom(beanClass)
				? entityManagerBeanRef
				: null;
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

		public static final String USE_ENTITY_MANAGER = "spring.aot.jpa.repositories.use-entitymanager";

		private static final String MODULE_NAME = "jpa";

		private final ConcurrentLruCache<AotEntityManagerFactoryCreator, EntityManagerFactory> factoryCache = new ConcurrentLruCache<>(
				16, AotEntityManagerFactoryCreator::getEntityManagerFactory);

		@Override
		protected void configureTypeContributions(AotRepositoryContext repositoryContext,
				GenerationContext generationContext) {
			super.configureTypeContributions(repositoryContext, generationContext);
		}

		@Override
		protected void configureTypeContribution(Class<?> type, AotContext aotContext) {
			aotContext.typeConfiguration(type, config -> config.contributeAccessors().forQuerydsl());
		}

		@Override
		protected @Nullable JpaRepositoryContributor contributeAotRepository(AotRepositoryContext repositoryContext) {

			if (!repositoryContext.isGeneratedRepositoriesEnabled(MODULE_NAME)) {
				return null;
			}

			ConfigurableListableBeanFactory beanFactory = repositoryContext.getBeanFactory();
			Environment environment = repositoryContext.getEnvironment();
			boolean useEntityManager = environment.getProperty(USE_ENTITY_MANAGER, Boolean.class, false);

			if (useEntityManager) {

				Optional<String> entityManagerFactoryRef = repositoryContext.getConfigurationSource()
						.getAttribute("entityManagerFactoryRef");

				log.debug(
						"Using EntityManager '%s' for AOT repository generation".formatted(entityManagerFactoryRef.orElse("")));

				EntityManagerFactory emf = entityManagerFactoryRef
						.map(it -> beanFactory.getBean(it, EntityManagerFactory.class))
						.orElseGet(() -> beanFactory.getBean(EntityManagerFactory.class));
				return new JpaRepositoryContributor(repositoryContext, emf);
			}

			JpaProperties properties = new JpaProperties(environment);
			ObjectProvider<PersistenceManagedTypes> managedTypesProvider = beanFactory
					.getBeanProvider(PersistenceManagedTypes.class);
			PersistenceManagedTypes managedTypes = managedTypesProvider.getIfUnique();

			if (managedTypes != null) {

				log.debug("Using PersistenceManagedTypes for AOT repository generation");
				return contribute(repositoryContext,
						AotEntityManagerFactoryCreator.from(managedTypes, properties.getJpaProperties()));
			}

			ObjectProvider<PersistenceUnitInfo> infoProvider = beanFactory.getBeanProvider(PersistenceUnitInfo.class);
			PersistenceUnitInfo unitInfo = infoProvider.getIfUnique();

			if (unitInfo != null) {

				log.debug("Using PersistenceUnitInfo for AOT repository generation");
				return contribute(repositoryContext,
						AotEntityManagerFactoryCreator.from(unitInfo, properties.getJpaProperties()));
			}

			log.debug("Using scanned types for AOT repository generation");
			return contribute(repositoryContext,
					AotEntityManagerFactoryCreator.from(repositoryContext, properties.getJpaProperties()));
		}

		private JpaRepositoryContributor contribute(AotRepositoryContext repositoryContext,
				AotEntityManagerFactoryCreator factory) {
			return new JpaRepositoryContributor(repositoryContext, factoryCache.get(factory));
		}

	}

	static class JpaProperties {

		private final Map<String, Object> jpaProperties;

		public JpaProperties(Environment environment) {

			this.jpaProperties = new LinkedHashMap<>();

			String implicitStrategy = getFirstAvailable(environment, "spring.jpa.hibernate.naming.implicitStrategy",
					"spring.jpa.hibernate.naming.implicit-strategy");
			if (StringUtils.hasText(implicitStrategy)) {
				jpaProperties.put(MappingSettings.IMPLICIT_NAMING_STRATEGY, implicitStrategy);
			}

			String physicalStrategy = getFirstAvailable(environment, "spring.jpa.hibernate.naming.physicalStrategy",
					"spring.jpa.hibernate.naming.physical-strategy");
			if (StringUtils.hasText(physicalStrategy)) {
				jpaProperties.put(MappingSettings.PHYSICAL_NAMING_STRATEGY, physicalStrategy);
			}

			if (environment instanceof ConfigurableEnvironment ce) {

				ce.getPropertySources().forEach(propertySource -> {

					if (propertySource instanceof EnumerablePropertySource<?> eps) {

						String prefix = "spring.jpa.properties.";
						Map<String, Object> partialProperties = Stream.of(eps.getPropertyNames())
								.filter(propertyName -> propertyName.startsWith(prefix))
								.collect(Collectors.toMap(k -> k.substring(prefix.length()), propertySource::getProperty));

						jpaProperties.putAll(partialProperties);
					}
				});
			}
		}

		@Nullable
		String getFirstAvailable(Environment environment, String... propertyNames) {

			for (String propertyName : propertyNames) {
				String value = environment.getProperty(propertyName);
				if (value != null) {
					return value;
				}
			}
			return null;
		}

		public Map<String, Object> getJpaProperties() {
			return jpaProperties;
		}

	}

}
