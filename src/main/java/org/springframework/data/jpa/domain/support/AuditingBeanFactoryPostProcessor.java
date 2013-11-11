/*
 * Copyright 2008-2013 the original author or authors.
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
package org.springframework.data.jpa.domain.support;

import static java.util.Arrays.*;
import static org.springframework.beans.factory.BeanFactoryUtils.*;
import static org.springframework.util.StringUtils.*;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManagerFactory;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.aspectj.AnnotationBeanConfigurerAspect;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.data.domain.AuditorAware;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;

/**
 * {@link BeanFactoryPostProcessor} that ensures that the {@link AnnotationBeanConfigurerAspect} aspect is up and
 * running <em>before</em> the {@link javax.persistence.EntityManagerFactory} gets created as this already instantiates
 * entity listeners and we need to get injection into {@link org.springframework.beans.factory.annotation.Configurable}
 * to work in them.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class AuditingBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

	public static final String BEAN_CONFIGURER_ASPECT_BEAN_NAME = "org.springframework.context.config.internalBeanConfigurerAspect";

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor#postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)
	 */
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {

		try {
			beanFactory.getBeanDefinition(BEAN_CONFIGURER_ASPECT_BEAN_NAME);
		} catch (NoSuchBeanDefinitionException o_O) {
			throw new IllegalStateException(
					"Invalid auditing setup! Make sure you've used @EnableJpaAuditing or <jpa:auditing /> correctly!", o_O);
		}

		for (String beanName : getEntityManagerFactoryBeanNames(beanFactory)) {
			BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
			definition.setDependsOn(addStringToArray(definition.getDependsOn(), BEAN_CONFIGURER_ASPECT_BEAN_NAME));
		}

		for (String beanName : beanNamesForTypeIncludingAncestors(beanFactory, AuditorAware.class, true, false)) {
			BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
			definition.setLazyInit(true);
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
	private Iterable<String> getEntityManagerFactoryBeanNames(ListableBeanFactory beanFactory) {

		Set<String> names = new HashSet<String>();
		names.addAll(asList(beanNamesForTypeIncludingAncestors(beanFactory, EntityManagerFactory.class, true, false)));

		for (String factoryBeanName : beanNamesForTypeIncludingAncestors(beanFactory,
				AbstractEntityManagerFactoryBean.class, true, false)) {
			names.add(factoryBeanName.substring(1));
		}

		return names;
	}
}
