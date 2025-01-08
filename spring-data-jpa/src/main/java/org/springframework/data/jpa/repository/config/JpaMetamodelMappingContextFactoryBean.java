/*
 * Copyright 2017-2025 the original author or authors.
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

import java.util.Collection;
import java.util.Set;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.Metamodel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.data.util.StreamUtils;
import org.springframework.lang.Nullable;

/**
 * {@link FactoryBean} to setup {@link JpaMetamodelMappingContext} instances from Spring configuration.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @since 1.6
 */
public class JpaMetamodelMappingContextFactoryBean extends AbstractFactoryBean<JpaMetamodelMappingContext>
		implements ApplicationContextAware {

	private static final Log LOG = LogFactory.getLog(JpaMetamodelMappingContextFactoryBean.class);

	private @Nullable ListableBeanFactory beanFactory;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.beanFactory = applicationContext;
	}

	@Override
	public Class<?> getObjectType() {
		return JpaMetamodelMappingContext.class;
	}

	@Override
	protected JpaMetamodelMappingContext createInstance() {

		if (LOG.isDebugEnabled()) {
			LOG.debug("Initializing JpaMetamodelMappingContext…");
		}

		JpaMetamodelMappingContext context = new JpaMetamodelMappingContext(getMetamodels());
		context.initialize();

		if (LOG.isDebugEnabled()) {
			LOG.debug("Finished initializing JpaMetamodelMappingContext");
		}

		return context;
	}

	/**
	 * Obtains all {@link Metamodel} instances of the current {@link ApplicationContext}.
	 *
	 * @return
	 */
	private Set<Metamodel> getMetamodels() {

		if (beanFactory == null) {
			throw new IllegalStateException("BeanFactory must not be null");
		}

		Collection<EntityManagerFactory> factories = BeanFactoryUtils
				.beansOfTypeIncludingAncestors(beanFactory, EntityManagerFactory.class).values();

		return factories.stream() //
				.map(EntityManagerFactory::getMetamodel) //
				.collect(StreamUtils.toUnmodifiableSet());
	}
}
