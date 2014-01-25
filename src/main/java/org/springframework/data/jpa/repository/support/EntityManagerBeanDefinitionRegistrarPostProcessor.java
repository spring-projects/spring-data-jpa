/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.jpa.repository.support;

import static java.util.Arrays.*;
import static org.springframework.beans.factory.BeanFactoryUtils.*;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;

/**
 * {@link BeanFactoryPostProcessor} to register a {@link SharedEntityManagerCreator} for every
 * {@link EntityManagerFactory} bean definition found in the application context to enable autowiring
 * {@link EntityManager} instances into constructor arguments. Adds the {@link EntityManagerFactory} bean name as
 * qualifier to the {@link EntityManager} {@link BeanDefinition} to enable explicit references in case of multiple
 * {@link EntityManagerFactory} instances.
 * 
 * @author Oliver Gierke
 */
public class EntityManagerBeanDefinitionRegistrarPostProcessor implements BeanFactoryPostProcessor {

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor#postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)
	 */
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

		if (!(beanFactory instanceof BeanDefinitionRegistry)) {
			return;
		}

		for (String emfName : getEntityManagerFactoryBeanNames(beanFactory)) {

			BeanDefinitionBuilder builder = BeanDefinitionBuilder
					.rootBeanDefinition("org.springframework.orm.jpa.SharedEntityManagerCreator");
			builder.setFactoryMethod("createSharedEntityManager");
			builder.addConstructorArgReference(emfName);

			AbstractBeanDefinition emBeanDefinition = builder.getRawBeanDefinition();
			AbstractBeanDefinition emfBeanDefinition = (AbstractBeanDefinition) beanFactory.getBeanDefinition(emfName);

			emBeanDefinition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, emfName));
			emBeanDefinition.setScope(emfBeanDefinition.getScope());
			emBeanDefinition.setSource(emfBeanDefinition.getSource());

			BeanDefinitionReaderUtils.registerWithGeneratedName(emBeanDefinition, (BeanDefinitionRegistry) beanFactory);
		}
	}

	/**
	 * Return all bean names for bean definitions that will result in an {@link EntityManagerFactory} eventually. We're
	 * checking for {@link EntityManagerFactory} and the well-known factory beans here to avoid eager initialization of
	 * the factory beans. The double lookup is necessary especially for JavaConfig scenarios as people might declare an
	 * {@link EntityManagerFactory} directly.
	 * 
	 * @param beanFactory
	 * @return
	 */
	private static Iterable<String> getEntityManagerFactoryBeanNames(ListableBeanFactory beanFactory) {

		Set<String> names = new HashSet<String>();
		names.addAll(asList(beanNamesForTypeIncludingAncestors(beanFactory, EntityManagerFactory.class, true, false)));

		for (String factoryBeanName : beanNamesForTypeIncludingAncestors(beanFactory,
				AbstractEntityManagerFactoryBean.class, true, false)) {
			names.add(factoryBeanName.substring(1));
		}

		return names;
	}
}
