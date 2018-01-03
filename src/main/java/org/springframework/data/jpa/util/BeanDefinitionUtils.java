/*
 * Copyright 2014-2018 the original author or authors.
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
package org.springframework.data.jpa.util;

import static java.util.Arrays.*;
import static org.springframework.beans.factory.BeanFactoryUtils.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManagerFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.util.ClassUtils;

/**
 * Utility methods to work with {@link BeanDefinition} instances from {@link BeanFactoryPostProcessor}s.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class BeanDefinitionUtils {

	private static final String JNDI_OBJECT_FACTORY_BEAN = "org.springframework.jndi.JndiObjectFactoryBean";
	private static final List<Class<?>> EMF_TYPES;

	static {

		List<Class<?>> types = new ArrayList<Class<?>>();
		types.add(EntityManagerFactory.class);
		types.add(AbstractEntityManagerFactoryBean.class);

		if (ClassUtils.isPresent(JNDI_OBJECT_FACTORY_BEAN, ClassUtils.getDefaultClassLoader())) {
			types.add(JndiObjectFactoryBean.class);
		}

		EMF_TYPES = Collections.unmodifiableList(types);
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
	public static Iterable<String> getEntityManagerFactoryBeanNames(ListableBeanFactory beanFactory) {

		Set<String> names = new HashSet<String>();
		names.addAll(asList(beanNamesForTypeIncludingAncestors(beanFactory, EntityManagerFactory.class, true, false)));

		for (String factoryBeanName : beanNamesForTypeIncludingAncestors(beanFactory,
				AbstractEntityManagerFactoryBean.class, true, false)) {
			names.add(transformedBeanName(factoryBeanName));
		}

		return names;
	}

	/**
	 * Returns {@link EntityManagerFactoryBeanDefinition} instances for all {@link BeanDefinition} registered in the given
	 * {@link ConfigurableListableBeanFactory} hierarchy.
	 *
	 * @param beanFactory must not be {@literal null}.
	 * @return
	 */
	public static Collection<EntityManagerFactoryBeanDefinition> getEntityManagerFactoryBeanDefinitions(
			ConfigurableListableBeanFactory beanFactory) {

		List<EntityManagerFactoryBeanDefinition> definitions = new ArrayList<EntityManagerFactoryBeanDefinition>();

		for (Class<?> type : EMF_TYPES) {

			for (String name : beanFactory.getBeanNamesForType(type, true, false)) {
				registerEntityManagerFactoryBeanDefinition(transformedBeanName(name), beanFactory, definitions);
			}
		}

		BeanFactory parentBeanFactory = beanFactory.getParentBeanFactory();

		if (parentBeanFactory instanceof ConfigurableListableBeanFactory) {
			definitions.addAll(getEntityManagerFactoryBeanDefinitions((ConfigurableListableBeanFactory) parentBeanFactory));
		}

		return definitions;
	}

	/**
	 * Registers an {@link EntityManagerFactoryBeanDefinition} for the bean with the given name. Drops
	 * {@link JndiObjectFactoryBean} instances that don't point to an {@link EntityManagerFactory} bean as expected type.
	 *
	 * @param name
	 * @param beanFactory
	 * @param definitions
	 */
	private static void registerEntityManagerFactoryBeanDefinition(String name,
			ConfigurableListableBeanFactory beanFactory, List<EntityManagerFactoryBeanDefinition> definitions) {

		BeanDefinition definition = beanFactory.getBeanDefinition(name);

		if (JNDI_OBJECT_FACTORY_BEAN.equals(definition.getBeanClassName())) {
			if (!EntityManagerFactory.class.getName().equals(definition.getPropertyValues().get("expectedType"))) {
				return;
			}
		}

		Class<?> type = beanFactory.getType(name);
		if (type == null || !EntityManagerFactory.class.isAssignableFrom(type)) {
			return;
		}

		definitions.add(new EntityManagerFactoryBeanDefinition(name, beanFactory));
	}

	/**
	 * Returns the {@link BeanDefinition} with the given name, obtained from the given {@link BeanFactory} or one of its
	 * parents.
	 *
	 * @param name
	 * @param beanFactory
	 * @return
	 */
	public static BeanDefinition getBeanDefinition(String name, ConfigurableListableBeanFactory beanFactory) {

		try {
			return beanFactory.getBeanDefinition(name);
		} catch (NoSuchBeanDefinitionException o_O) {

			BeanFactory parentBeanFactory = beanFactory.getParentBeanFactory();

			if (parentBeanFactory instanceof ConfigurableListableBeanFactory) {
				return getBeanDefinition(name, (ConfigurableListableBeanFactory) parentBeanFactory);
			}

			throw o_O;
		}
	}

	/**
	 * Value object to represent a {@link BeanDefinition} for an {@link EntityManagerFactory} with a dedicated bean name.
	 *
	 * @author Oliver Gierke
	 * @author Thomas Darimont
	 */
	public static class EntityManagerFactoryBeanDefinition {

		private final String beanName;
		private final ConfigurableListableBeanFactory beanFactory;

		/**
		 * Creates a new {@link EntityManagerFactoryBeanDefinition}.
		 *
		 * @param beanName
		 * @param beanFactory
		 */
		public EntityManagerFactoryBeanDefinition(String beanName, ConfigurableListableBeanFactory beanFactory) {

			this.beanName = beanName;
			this.beanFactory = beanFactory;
		}

		/**
		 * Returns the bean name of the {@link BeanDefinition} for the {@link EntityManagerFactory}.
		 *
		 * @return
		 */
		public String getBeanName() {
			return beanName;
		}

		/**
		 * Returns the underlying {@link BeanFactory}.
		 *
		 * @return
		 */
		public BeanFactory getBeanFactory() {
			return beanFactory;
		}

		/**
		 * Returns the {@link BeanDefinition} for the {@link EntityManagerFactory}.
		 *
		 * @return
		 */
		public BeanDefinition getBeanDefinition() {
			return beanFactory.getBeanDefinition(beanName);
		}
	}
}
