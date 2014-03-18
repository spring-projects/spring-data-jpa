/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository.config;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.data.jpa.repository.support.EntityManagerBeanDefinitionRegistrarPostProcessor;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor;
import org.springframework.util.Assert;

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
 */
public class JpaRepositoryConfigExtension extends RepositoryConfigurationExtensionSupport {

	public static final String JPA_MAPPING_CONTEXT_BEAN_NAME = "jpaMapppingContext";

	private static final Class<?> PAB_POST_PROCESSOR = PersistenceAnnotationBeanPostProcessor.class;
	private static final String DEFAULT_TRANSACTION_MANAGER_BEAN_NAME = "transactionManager";

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config14.RepositoryConfigurationExtension#getRepositoryInterface()
	 */
	public String getRepositoryFactoryClassName() {
		return JpaRepositoryFactoryBean.class.getName();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config14.RepositoryConfigurationExtensionSupport#getModulePrefix()
	 */
	@Override
	protected String getModulePrefix() {
		return "jpa";
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#postProcess(org.springframework.beans.factory.support.BeanDefinitionBuilder, org.springframework.data.repository.config.RepositoryConfigurationSource)
	 */
	@Override
	public void postProcess(BeanDefinitionBuilder builder, RepositoryConfigurationSource source) {

		String transactionManagerRef = source.getAttribute("transactionManagerRef");
		builder.addPropertyValue("transactionManager",
				transactionManagerRef == null ? DEFAULT_TRANSACTION_MANAGER_BEAN_NAME : transactionManagerRef);

		String entityManagerFactoryRef = getEntityManagerFactoryRef(source);

		if (entityManagerFactoryRef != null) {
			builder.addPropertyValue("entityManager", getEntityManagerBeanDefinitionFor(entityManagerFactoryRef, source));
		}

		builder.addPropertyReference("mappingContext", JPA_MAPPING_CONTEXT_BEAN_NAME);
	}

	/**
	 * @param source
	 * @return
	 */
	private String getEntityManagerFactoryRef(RepositoryConfigurationSource source) {

		String entityManagerFactoryRef = source.getAttribute("entityManagerFactoryRef");
		return entityManagerFactoryRef == null ? "entityManagerFactory" : entityManagerFactoryRef;
	}

	/**
	 * Creates an anonymous factory to extract the actual {@link javax.persistence.EntityManager} from the
	 * {@link javax.persistence.EntityManagerFactory} bean name reference.
	 * 
	 * @param entityManagerFactoryBeanName
	 * @param source
	 * @return
	 */
	private BeanDefinition getEntityManagerBeanDefinitionFor(String entityManagerFactoryBeanName, Object source) {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.rootBeanDefinition("org.springframework.orm.jpa.SharedEntityManagerCreator");
		builder.setFactoryMethod("createSharedEntityManager");
		builder.addConstructorArgReference(entityManagerFactoryBeanName);

		AbstractBeanDefinition bean = builder.getRawBeanDefinition();
		bean.setSource(source);

		return bean;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#registerBeansForRoot(org.springframework.beans.factory.support.BeanDefinitionRegistry, org.springframework.data.repository.config.RepositoryConfigurationSource)
	 */
	@Override
	public void registerBeansForRoot(BeanDefinitionRegistry registry, RepositoryConfigurationSource configurationSource) {

		super.registerBeansForRoot(registry, configurationSource);

		Object source = configurationSource.getSource();
		registerWithSourceAndGeneratedBeanName(registry, new RootBeanDefinition(
				EntityManagerBeanDefinitionRegistrarPostProcessor.class), source);

		BeanDefinition entityManagerBeanDefinitionFor = getEntityManagerBeanDefinitionFor(
				getEntityManagerFactoryRef(configurationSource), source);

		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.rootBeanDefinition(JpaMetamodelMappingContextFactoryBean.class);
		builder.addPropertyValue("entityManager", entityManagerBeanDefinitionFor);

		AbstractBeanDefinition definition = builder.getBeanDefinition();
		definition.setSource(source);
		registry.registerBeanDefinition(JPA_MAPPING_CONTEXT_BEAN_NAME, definition);

		if (!hasBean(PAB_POST_PROCESSOR, registry)
				&& !registry.containsBeanDefinition(AnnotationConfigUtils.PERSISTENCE_ANNOTATION_PROCESSOR_BEAN_NAME)) {

			registerWithSourceAndGeneratedBeanName(registry, new RootBeanDefinition(PAB_POST_PROCESSOR), source);
		}
	}

	/**
	 * {@link FactoryBean} to setup {@link JpaMetamodelMappingContext} instances from Spring configuration.
	 * 
	 * @author Oliver Gierke
	 * @since 1.6
	 */
	static class JpaMetamodelMappingContextFactoryBean extends AbstractFactoryBean<JpaMetamodelMappingContext> {

		private EntityManager entityManager;

		/**
		 * Configures the {@link EntityManager} to use to create the {@link JpaMetamodelMappingContext}.
		 * 
		 * @param entityManager must not be {@literal null}.
		 */
		public void setEntityManager(EntityManager entityManager) {

			Assert.notNull(entityManager, "EntityManager must not be null!");
			this.entityManager = entityManager;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.beans.factory.config.AbstractFactoryBean#getObjectType()
		 */
		@Override
		public Class<?> getObjectType() {
			return JpaMetamodelMappingContext.class;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.beans.factory.config.AbstractFactoryBean#createInstance()
		 */
		@Override
		protected JpaMetamodelMappingContext createInstance() throws Exception {

			Metamodel metamodel = entityManager.getMetamodel();

			Set<ManagedType<?>> managedTypes = metamodel.getManagedTypes();
			Set<Class<?>> entitySources = new HashSet<Class<?>>(managedTypes.size());

			for (ManagedType<?> type : managedTypes) {
				entitySources.add(type.getJavaType());
			}

			JpaMetamodelMappingContext context = new JpaMetamodelMappingContext(metamodel);
			context.setInitialEntitySet(entitySources);
			context.initialize();

			return context;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.beans.factory.config.AbstractFactoryBean#afterPropertiesSet()
		 */
		@Override
		public void afterPropertiesSet() throws Exception {

			Assert.notNull(entityManager, "EntityManager must not be null!");

			super.afterPropertiesSet();
		}
	}
}
