/*
 * Copyright 2008-2019 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.instrument.classloading.ShadowingClassLoader;

/**
 * Integration tests for {@link AuditingBeanDefinitionParser}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Jens Schauder
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

	@Test // DATAJPA-9
	public void wiresDateTimeProviderIfConfigured() {

		BeanDefinition definition = getBeanDefinition("auditing/auditing-namespace-context3.xml");
		PropertyValue value = definition.getPropertyValues().getPropertyValue("dateTimeProvider");

		assertThat(value).isNotNull();
		assertThat(value.getValue()).isInstanceOf(RuntimeBeanReference.class);
		assertThat(((RuntimeBeanReference) value.getValue()).getBeanName()).isEqualTo("dateTimeProvider");

		BeanFactory factory = loadFactoryFrom("auditing/auditing-namespace-context3.xml");
		Object bean = factory.getBean(AuditingBeanDefinitionParser.AUDITING_ENTITY_LISTENER_CLASS_NAME);
		assertThat(bean).isNotNull();
	}

	@Test(expected = BeanDefinitionParsingException.class) // DATAJPA-367
	public void shouldThrowBeanDefinitionParsingExceptionIfClassFromSpringAspectsJarCannotBeFound() {

		ShadowingClassLoader scl = new ShadowingClassLoader(getClass().getClassLoader());
		scl.excludeClass(AuditingBeanDefinitionParser.AUDITING_ENTITY_LISTENER_CLASS_NAME);
		loadFactoryFrom("auditing/auditing-namespace-context.xml", scl);
	}

	private void assertSetDatesIsSetTo(String configFile, String value) {

		BeanDefinition definition = getBeanDefinition(configFile);
		PropertyValue propertyValue = definition.getPropertyValues().getPropertyValue("dateTimeForNow");
		assertThat(propertyValue).isNotNull();
		assertThat((String) propertyValue.getValue()).isEqualTo(value);
	}

	private BeanDefinition getBeanDefinition(String configFile) {

		DefaultListableBeanFactory factory = loadFactoryFrom(configFile);

		BeanDefinition definition = factory
				.getBeanDefinition(AuditingBeanDefinitionParser.AUDITING_ENTITY_LISTENER_CLASS_NAME);
		BeanDefinition handlerDefinition = (BeanDefinition) definition.getPropertyValues()
				.getPropertyValue("auditingHandler").getValue();

		String beanName = handlerDefinition.getPropertyValues().getPropertyValue("targetBeanName").getValue().toString();
		return factory.getBeanDefinition(beanName);
	}

	private DefaultListableBeanFactory loadFactoryFrom(String configFile) {
		return loadFactoryFrom(configFile, getClass().getClassLoader());
	}

	private DefaultListableBeanFactory loadFactoryFrom(String configFile, ClassLoader classLoader) {

		Thread.currentThread().setContextClassLoader(classLoader);
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(factory);
		xmlBeanDefinitionReader.loadBeanDefinitions(new ClassPathResource(configFile));
		return factory;
	}
}
