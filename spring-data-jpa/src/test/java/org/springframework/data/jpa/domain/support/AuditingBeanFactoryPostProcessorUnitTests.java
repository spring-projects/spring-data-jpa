/*
 * Copyright 2008-2025 the original author or authors.
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
package org.springframework.data.jpa.domain.support;

import static org.assertj.core.api.Assertions.*;

import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

/**
 * Unit test for {@link AuditingBeanFactoryPostProcessor}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Jens Schauder
 */
class AuditingBeanFactoryPostProcessorUnitTests {

	DefaultListableBeanFactory beanFactory;
	private AuditingBeanFactoryPostProcessor processor;

	@BeforeEach
	void setUp() {

		this.beanFactory = getBeanFactory();
		this.processor = new AuditingBeanFactoryPostProcessor();
	}

	String getConfigFile() {
		return "auditing-bfpp-context.xml";
	}

	DefaultListableBeanFactory getBeanFactory() {

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
		reader.loadBeanDefinitions(new ClassPathResource("auditing/" + getConfigFile()));

		return beanFactory;
	}

	@Test
	void beanConfigurerAspectShouldBeConfiguredAfterPostProcessing() {

		processor.postProcessBeanFactory(beanFactory);

		assertThat(beanFactory.isBeanNameInUse(AuditingBeanFactoryPostProcessor.BEAN_CONFIGURER_ASPECT_BEAN_NAME)).isTrue();
	}

	@Test // DATAJPA-265
	void rejectsConfigurationWithoutSpringConfigured() {
		assertThatIllegalStateException()
				.isThrownBy(() -> processor.postProcessBeanFactory(new DefaultListableBeanFactory()));
	}

	@Test // DATAJPA-265
	void setsDependsOnOnEntityManagerFactory() {

		processor.postProcessBeanFactory(beanFactory);

		String[] emfDefinitionNames = beanFactory.getBeanNamesForType(EntityManagerFactory.class);

		for (String emfDefinitionName : emfDefinitionNames) {
			BeanDefinition emfDefinition = beanFactory.getBeanDefinition(emfDefinitionName);
			assertThat(emfDefinition).isNotNull();
			assertThat(emfDefinition.getDependsOn())
					.containsExactly(AuditingBeanFactoryPostProcessor.BEAN_CONFIGURER_ASPECT_BEAN_NAME);
		}
	}

	@Test // DATAJPA-453
	void findsEntityManagerFactoryInParentBeanFactory() {

		DefaultListableBeanFactory childFactory = new DefaultListableBeanFactory(getBeanFactory());
		processor.postProcessBeanFactory(childFactory);
	}
}
