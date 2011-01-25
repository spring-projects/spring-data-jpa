/*
 * Copyright 2008-2011 the original author or authors.
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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;


/**
 * Integration tests for {@link AuditingBeanDefinitionParser}.
 * 
 * @author Oliver Gierke
 */
public class AuditingBeanDefinitionParserTests {

    @Test
    public void settingDatesIsConfigured() throws Exception {

        assertSetDatesIsSetTo("auditing/auditing-namespace-context.xml", "true");
    }


    @Test
    public void notSettingDatesIsConfigured() throws Exception {

        assertSetDatesIsSetTo("auditing/auditing-namespace-context2.xml",
                "false");
    }


    private void assertSetDatesIsSetTo(String configFile, String value) {

        XmlBeanFactory factory =
                new XmlBeanFactory(new ClassPathResource(configFile));
        BeanDefinition definition =
                factory.getBeanDefinition(AuditingBeanDefinitionParser.AUDITING_ENTITY_LISTENER_CLASS_NAME);
        PropertyValue propertyValue =
                definition.getPropertyValues().getPropertyValue(
                        "dateTimeForNow");
        assertThat(propertyValue, is(notNullValue()));
        assertThat((String) propertyValue.getValue(), is(value));
    }
}
