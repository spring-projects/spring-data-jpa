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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.aspectj.AnnotationBeanConfigurerAspect;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.util.Assert;

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

	static final String BEAN_CONFIGURER_ASPECT_BEAN_NAME = "org.springframework.context.config.internalBeanConfigurerAspect";

	private final static Logger logger = LoggerFactory.getLogger(AuditingBeanFactoryPostProcessor.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor#
	 * postProcessBeanFactory
	 * (org.springframework.beans.factory.config.ConfigurableListableBeanFactory
	 * )
	 */
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {

		if (!isSpringConfigured(beanFactory)) {
			logger.error("Could not configure Spring Data JPA auditing-feature because the bean "
					+ BEAN_CONFIGURER_ASPECT_BEAN_NAME + " is not present!\n"
					+ "If you want to use auditing please add the @EnableSpringConfigured annotation or\n"
					+ "the <context:spring-configured> element to your configuration.");
		}
	}

	/**
	 * Returns whether we have a bean factory for which {@code &lt;context:spring-configured&gt;} or
	 * {@link EnableSpringConfigured} was activated.
	 * 
	 * @param factory must not be {@literal null}.
	 * @return
	 */
	private boolean isSpringConfigured(BeanFactory factory) {

		Assert.notNull(factory, "beanFactory must not be null!");

		try {
			factory.getBean(BEAN_CONFIGURER_ASPECT_BEAN_NAME);
			return true;
		} catch (NoSuchBeanDefinitionException e) {
			return false;
		}
	}
}
