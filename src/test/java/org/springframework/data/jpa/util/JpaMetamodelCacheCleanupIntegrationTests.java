/*
 * Copyright 2018 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import javax.persistence.metamodel.Metamodel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.data.repository.config.RepositoryConfigurationSource;

/**
 * Integration tests for {@link JpaMetamodelCacheCleanup}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class JpaMetamodelCacheCleanupIntegrationTests {

	@Mock Metamodel metamodel;

	@Test // DATAJPA-1446
	public void wipesJpaMetamodelCacheOnApplicationContextClose() {

		JpaMetamodel model = JpaMetamodel.of(metamodel);

		try (GenericApplicationContext context = new GenericApplicationContext()) {

			context.registerBean(JpaMetamodelCacheCleanup.class);
			context.refresh();

			assertThat(model).isSameAs(JpaMetamodel.of(metamodel));
		}

		assertThat(model).isNotSameAs(JpaMetamodel.of(metamodel));
	}

	@Test // DATAJPA-1487, DATAJPA-1446
	public void registersCleanupBeanAsNonLazy() {

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		RepositoryConfigurationSource configurationSource = mock(RepositoryConfigurationSource.class);

		RepositoryConfigurationExtension extension = new JpaRepositoryConfigExtension();
		extension.registerBeansForRoot(beanFactory, configurationSource);

		String[] cleanupBeanNames = beanFactory.getBeanNamesForType(JpaMetamodelCacheCleanup.class);

		assertThat(cleanupBeanNames.length).isEqualTo(1);
		assertThat(beanFactory.getBeanDefinition(cleanupBeanNames[0]).isLazyInit()).isFalse();
	}
}
