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
package org.springframework.data.jpa.repository;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor;
import org.springframework.test.context.ContextConfiguration;

/**
 * Use namespace context to run tests. Checks for existence of required PostProcessors, too.
 *
 * @author Oliver Gierke
 * @author Eberhard Wolff
 */
@ContextConfiguration(locations = "classpath:config/namespace-application-context.xml", inheritLocations = false)
public class NamespaceUserRepositoryTests extends UserRepositoryTests {

	@Autowired
	ListableBeanFactory beanFactory;

	@Test
	public void registersPostProcessors() {
		hasAtLeastOneBeanOfType(PersistenceAnnotationBeanPostProcessor.class);
	}

	private void hasAtLeastOneBeanOfType(Class<?> beanType) {

		Map<String, ?> beans = beanFactory.getBeansOfType(beanType);
		assertFalse(beans.entrySet().isEmpty());
	}
}
