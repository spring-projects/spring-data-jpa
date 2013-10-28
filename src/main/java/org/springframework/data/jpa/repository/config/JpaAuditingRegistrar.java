/*
 * Copyright 2013 the original author or authors.
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

import java.lang.annotation.Annotation;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.aspectj.AnnotationBeanConfigurerAspect;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.auditing.config.AuditingBeanDefinitionRegistrarSupport;
import org.springframework.data.jpa.domain.support.AuditingBeanFactoryPostProcessor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link ImportBeanDefinitionRegistrar} to enable {@link EnableJpaAuditing} annotation.
 * 
 * @author Thomas Darimont
 */
class JpaAuditingRegistrar extends AuditingBeanDefinitionRegistrarSupport {

	/**
	 * The bean name of the internally managed bean configurer aspect.
	 */
	private static final String BEAN_CONFIGURER_ASPECT_BEAN_NAME = "org.springframework.context.config.internalBeanConfigurerAspect";
	private static final String BEAN_CONFIGURER_ASPECT_CLASS_NAME = "org.springframework.beans.factory.aspectj.AnnotationBeanConfigurerAspect";

	/* (non-Javadoc)
	 * @see org.springframework.data.auditing.config.AuditingBeanDefinitionRegistrarSupport#getAnnotation()
	 */
	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableJpaAuditing.class;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.auditing.config.AuditingBeanDefinitionRegistrarSupport#registerBeanDefinitions(org.springframework.core.type.AnnotationMetadata, org.springframework.beans.factory.support.BeanDefinitionRegistry)
	 */
	@Override
	public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry registry) {

		Assert.notNull(annotationMetadata, "annotationMetadata must not be null!");
		Assert.notNull(annotationMetadata, "registry must not be null!");

		registerBeanConfigurerAspectIfNecessary(registry);
		super.registerBeanDefinitions(annotationMetadata, registry);
		registerInfrastructureBeanWithId(BeanDefinitionBuilder.rootBeanDefinition(AuditingBeanFactoryPostProcessor.class)
				.getRawBeanDefinition(), AuditingBeanFactoryPostProcessor.class.getName(), registry);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.auditing.config.AuditingBeanDefinitionRegistrarSupport#registerAuditListener(org.springframework.beans.factory.config.BeanDefinition, org.springframework.beans.factory.support.BeanDefinitionRegistry)
	 */
	@Override
	protected void registerAuditListenerBeanDefinition(BeanDefinition auditingHandlerDefinition,
			BeanDefinitionRegistry registry) {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(AuditingEntityListener.class);
		builder.addPropertyValue("auditingHandler", auditingHandlerDefinition);
		registerInfrastructureBeanWithId(builder.getRawBeanDefinition(), AuditingEntityListener.class.getName(), registry);
	}

	/**
	 * @param registry, the {@link BeanDefinitionRegistry} to be used to register the
	 *          {@link AnnotationBeanConfigurerAspect}.
	 */
	private void registerBeanConfigurerAspectIfNecessary(BeanDefinitionRegistry registry) {

		if (!registry.containsBeanDefinition(BEAN_CONFIGURER_ASPECT_BEAN_NAME)) {

			if (!ClassUtils.isPresent(BEAN_CONFIGURER_ASPECT_CLASS_NAME, getClass().getClassLoader())) {
				throw new BeanDefinitionStoreException("Could not configure Spring Data JPA auditing-feature because"
						+ " spring-aspects.jar is not on the classpath!\n"
						+ "If you want to use auditing please add spring-aspects.jar to the classpath.");
			}

			RootBeanDefinition def = new RootBeanDefinition();
			def.setBeanClassName(BEAN_CONFIGURER_ASPECT_CLASS_NAME);
			def.setFactoryMethodName("aspectOf");
			def.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

			registry.registerBeanDefinition(BEAN_CONFIGURER_ASPECT_BEAN_NAME, new BeanComponentDefinition(def,
					BEAN_CONFIGURER_ASPECT_BEAN_NAME).getBeanDefinition());
		}
	}
}
