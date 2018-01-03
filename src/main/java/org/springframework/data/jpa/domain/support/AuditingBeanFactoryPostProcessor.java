/*
 * Copyright 2008-2018 the original author or authors.
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

import static org.springframework.data.jpa.util.BeanDefinitionUtils.*;
import static org.springframework.util.StringUtils.*;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.aspectj.AnnotationBeanConfigurerAspect;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

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
			getBeanDefinition(BEAN_CONFIGURER_ASPECT_BEAN_NAME, beanFactory);
		} catch (NoSuchBeanDefinitionException o_O) {
			throw new IllegalStateException(
					"Invalid auditing setup! Make sure you've used @EnableJpaAuditing or <jpa:auditing /> correctly!", o_O);
		}

		for (String beanName : getEntityManagerFactoryBeanNames(beanFactory)) {
			BeanDefinition definition = getBeanDefinition(beanName, beanFactory);
			definition.setDependsOn(addStringToArray(definition.getDependsOn(), BEAN_CONFIGURER_ASPECT_BEAN_NAME));
		}
	}
}
