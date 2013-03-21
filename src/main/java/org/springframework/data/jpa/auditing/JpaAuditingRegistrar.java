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

package org.springframework.data.jpa.auditing;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.auditing.config.AnnotationAuditingConfiguration;
import org.springframework.data.auditing.config.AnnotationAuditingConfigurationSupport;
import org.springframework.data.auditing.config.AuditingBeanDefinitionRegistrarSupport;

/**
 * JPA specific implementation for {@link AuditingBeanDefinitionRegistrarSupport}.
 *
 * @author Ranie Jade Ramiso
 */
public class JpaAuditingRegistrar extends AuditingBeanDefinitionRegistrarSupport {

	private static final String AUDITING_ENTITY_LISTENER_CLASS_NAME = "org.springframework.data.jpa.domain.support.AuditingEntityListener";
	private static final String AUDITING_BFPP_CLASS_NAME = "org.springframework.data.jpa.domain.support.AuditingBeanFactoryPostProcessor";

	@Override
	protected AnnotationAuditingConfiguration getConfiguration(AnnotationMetadata annotationMetadata) {
		return new AnnotationAuditingConfigurationSupport(annotationMetadata, EnableJpaAuditing.class);
	}

	@Override
	protected void postProcess(AnnotationAuditingConfiguration configuration, BeanDefinition auditingHandlerDefinition, BeanDefinitionRegistry registry) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(AUDITING_ENTITY_LISTENER_CLASS_NAME);
		builder.addPropertyValue("auditingHandler", auditingHandlerDefinition);
		builder.setScope("prototype");

		registerInfrastructureBeans(builder.getRawBeanDefinition(), AUDITING_ENTITY_LISTENER_CLASS_NAME, registry);

		RootBeanDefinition def = new RootBeanDefinition(AUDITING_BFPP_CLASS_NAME);
		registerInfrastructureBeans(def, AUDITING_BFPP_CLASS_NAME, registry);

	}

	private void registerInfrastructureBeans(AbstractBeanDefinition definition, String id, BeanDefinitionRegistry registry) {
		definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		registry.registerBeanDefinition(id, definition);
	}
}
