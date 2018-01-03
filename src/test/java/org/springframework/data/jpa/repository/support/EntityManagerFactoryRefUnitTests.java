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
package org.springframework.data.jpa.repository.support;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import javax.persistence.EntityManagerFactory;

import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

/**
 * Assures the injected repository instances are wired to the customly configured {@link EntityManagerFactory}.
 *
 * @author Oliver Gierke
 */
public class EntityManagerFactoryRefUnitTests {

	@Test
	public void repositoriesGetTheSecondEntityManagerFactoryInjected2() {

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
		reader.loadBeanDefinitions(new ClassPathResource("multiple-entity-manager-context.xml"));

		BeanDefinition bean = factory.getBeanDefinition("userRepository");
		Object value = getPropertyValue(bean, "entityManager");
		assertTrue(value instanceof BeanDefinition);
		BeanDefinition emCreator = (BeanDefinition) value;

		BeanReference reference = getConstructorBeanReference(emCreator, 0);
		assertThat(reference.getBeanName(), is("secondEntityManagerFactory"));
	}

	private Object getPropertyValue(BeanDefinition definition, String propertyName) {

		return definition.getPropertyValues().getPropertyValue(propertyName).getValue();
	}

	private BeanReference getConstructorBeanReference(BeanDefinition definition, int index) {

		Object value = definition.getConstructorArgumentValues().getIndexedArgumentValues().get(index).getValue();
		assertTrue(value instanceof BeanReference);
		return (BeanReference) value;
	}
}
