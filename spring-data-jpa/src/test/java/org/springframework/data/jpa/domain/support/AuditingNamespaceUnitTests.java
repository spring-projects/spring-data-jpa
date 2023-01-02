/*
 * Copyright 2008-2023 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;

/**
 * Unit test for the JPA {@code auditing} namespace element.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 */
class AuditingNamespaceUnitTests extends AuditingBeanFactoryPostProcessorUnitTests {

	@Override
	String getConfigFile() {
		return "auditing-namespace-context.xml";
	}

	@Test
	void registersBeanDefinitions() {

		BeanDefinition definition = beanFactory.getBeanDefinition(AuditingEntityListener.class.getName());
		PropertyValue propertyValue = definition.getPropertyValues().getPropertyValue("auditingHandler");
		assertThat(propertyValue).isNotNull();
	}
}
