/*
 * Copyright 2008-2012 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

/**
 * Integration tests for {@link AuditingBeanDefinitionParser}.
 * 
 * @author Oliver Gierke
 */
public class AuditingBeanDefinitionParserTests {

	@Test
	public void settingDatesIsConfigured() {

		assertSetDatesIsSetTo("auditing/auditing-namespace-context.xml", "true");
	}

	@Test
	public void notSettingDatesIsConfigured() {

		assertSetDatesIsSetTo("auditing/auditing-namespace-context2.xml", "false");
	}

	/**
	 * @see DATAJPA-9
	 */
	@Test
	public void wiresDateTimeProviderIfConfigured() {

		String location = "auditing/auditing-namespace-context3.xml";
		BeanDefinition definition = getBeanDefinition(location);
		PropertyValue value = definition.getPropertyValues().getPropertyValue("dateTimeProvider");

		assertThat(value, is(notNullValue()));
		assertThat(value.getValue(), is(RuntimeBeanReference.class));
		assertThat(((RuntimeBeanReference) value.getValue()).getBeanName(), is("dateTimeProvider"));

		BeanFactory factory = loadFactoryFrom(location);
		Object bean = factory.getBean(AuditingBeanDefinitionParser.AUDITING_ENTITY_LISTENER_CLASS_NAME);
		assertThat(bean, is(notNullValue()));
	}

	private void assertSetDatesIsSetTo(String configFile, String value) {

		BeanDefinition definition = getBeanDefinition(configFile);
		PropertyValue propertyValue = definition.getPropertyValues().getPropertyValue("dateTimeForNow");
		assertThat(propertyValue, is(notNullValue()));
		assertThat((String) propertyValue.getValue(), is(value));
	}

	private BeanDefinition getBeanDefinition(String configFile) {

		DefaultListableBeanFactory factory = loadFactoryFrom(configFile);
		return factory.getBeanDefinition(AuditingBeanDefinitionParser.AUDITING_ENTITY_LISTENER_CLASS_NAME);
	}

	private DefaultListableBeanFactory loadFactoryFrom(String configFile) {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(factory);
		xmlBeanDefinitionReader.loadBeanDefinitions(new ClassPathResource(configFile));
		return factory;
	}
}
