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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

/**
 * Unit test for {@link AuditingBeanFactoryPostProcessor}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class AuditingBeanFactoryPostProcessorUnitTests {

	DefaultListableBeanFactory beanFactory;
	AuditingBeanFactoryPostProcessor processor;

	@Before
	public void setUp() {

		beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
		reader.loadBeanDefinitions(new ClassPathResource("auditing/" + getConfigFile()));

		processor = new AuditingBeanFactoryPostProcessor();
	}

	protected String getConfigFile() {

		return "auditing-bfpp-context.xml";
	}

	@Test
	public void beanConfigurerAspectShouldBeConfiguredAfterPostProcessing() throws Exception {

		processor.postProcessBeanFactory(beanFactory);

		assertThat(beanFactory.isBeanNameInUse(AuditingBeanFactoryPostProcessor.BEAN_CONFIGURER_ASPECT_BEAN_NAME), is(true));
	}
}
