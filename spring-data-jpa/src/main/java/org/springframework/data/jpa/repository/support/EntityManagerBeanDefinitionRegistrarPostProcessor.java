/*
 * Copyright 2014-2022 the original author or authors.
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
package org.springframework.data.jpa.repository.support;

import static org.springframework.data.jpa.util.BeanDefinitionUtils.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.Ordered;
import org.springframework.orm.jpa.SharedEntityManagerCreator;

/**
 * {@link BeanFactoryPostProcessor} to register a {@link SharedEntityManagerCreator} for every
 * {@link EntityManagerFactory} bean definition found in the application context to enable autowiring
 * {@link EntityManager} instances into constructor arguments. Adds the {@link EntityManagerFactory} bean name as
 * qualifier to the {@link EntityManager} {@link BeanDefinition} to enable explicit references in case of multiple
 * {@link EntityManagerFactory} instances.
 *
 * @author Oliver Gierke
 * @author Réda Housni Alaoui
 * @author Mark Paluch
 */
public class EntityManagerBeanDefinitionRegistrarPostProcessor implements BeanFactoryPostProcessor, Ordered {

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 10;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

		if (!ConfigurableListableBeanFactory.class.isInstance(beanFactory)) {
			return;
		}

		ConfigurableListableBeanFactory factory = beanFactory;

		for (EntityManagerFactoryBeanDefinition definition : getEntityManagerFactoryBeanDefinitions(factory)) {

			BeanFactory definitionFactory = definition.getBeanFactory();

			if (!(definitionFactory instanceof BeanDefinitionRegistry)) {
				continue;
			}

			String entityManagerBeanName = "jpaSharedEM_AWC_" + definition.getBeanName();
			BeanDefinitionRegistry definitionRegistry = (BeanDefinitionRegistry) definitionFactory;

			if (!beanFactory.containsBeanDefinition(entityManagerBeanName)
					&& !definitionRegistry.containsBeanDefinition(entityManagerBeanName)) {

				BeanDefinitionBuilder builder = BeanDefinitionBuilder
						.rootBeanDefinition("org.springframework.orm.jpa.SharedEntityManagerCreator");
				builder.setFactoryMethod("createSharedEntityManager");
				builder.addConstructorArgReference(definition.getBeanName());

				AbstractBeanDefinition emBeanDefinition = builder.getRawBeanDefinition();

				emBeanDefinition.setPrimary(definition.getBeanDefinition().isPrimary());
				emBeanDefinition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, definition.getBeanName()));
				emBeanDefinition.setScope(definition.getBeanDefinition().getScope());
				emBeanDefinition.setSource(definition.getBeanDefinition().getSource());
				emBeanDefinition.setLazyInit(true);

				definitionRegistry.registerBeanDefinition(entityManagerBeanName, emBeanDefinition);
			}
		}
	}
}
