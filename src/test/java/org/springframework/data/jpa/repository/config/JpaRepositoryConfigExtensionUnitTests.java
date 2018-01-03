/*
 * Copyright 2013-2018 the original author or authors.
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
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor;

/**
 * Unit tests for {@link JpaRepositoryConfigExtension}.
 *
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class JpaRepositoryConfigExtensionUnitTests {

	@Mock RepositoryConfigurationSource configSource;

	public @Rule ExpectedException exception = ExpectedException.none();

	@Test
	public void registersDefaultBeanPostProcessorsByDefault() {

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

		RepositoryConfigurationExtension extension = new JpaRepositoryConfigExtension();
		extension.registerBeansForRoot(factory, configSource);

		Iterable<String> names = Arrays.asList(factory.getBeanDefinitionNames());

		assertThat(names, hasItems(AnnotationConfigUtils.PERSISTENCE_ANNOTATION_PROCESSOR_BEAN_NAME));
	}

	@Test
	public void doesNotRegisterProcessorIfAlreadyPresent() {

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		RootBeanDefinition pabppDefinition = new RootBeanDefinition(PersistenceAnnotationBeanPostProcessor.class);
		String beanName = BeanDefinitionReaderUtils.generateBeanName(pabppDefinition, factory);
		factory.registerBeanDefinition(beanName, pabppDefinition);

		assertOnlyOnePersistenceAnnotationBeanPostProcessorRegistered(factory, beanName);
	}

	@Test
	public void doesNotRegisterProcessorIfAutoRegistered() {

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		RootBeanDefinition pabppDefinition = new RootBeanDefinition(PersistenceAnnotationBeanPostProcessor.class);
		String beanName = AnnotationConfigUtils.PERSISTENCE_ANNOTATION_PROCESSOR_BEAN_NAME;
		factory.registerBeanDefinition(beanName, pabppDefinition);

		assertOnlyOnePersistenceAnnotationBeanPostProcessorRegistered(factory, beanName);
	}

	@Test // DATAJPA-525
	public void guardsAgainstNullJavaTypesReturnedFromJpaMetamodel() throws Exception {

		ApplicationContext context = mock(ApplicationContext.class);
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		Metamodel metamodel = mock(Metamodel.class);
		ManagedType<?> managedType = mock(ManagedType.class);

		Set<ManagedType<?>> managedTypes = Collections.<ManagedType<?>> singleton(managedType);

		when(context.getBeansOfType(EntityManagerFactory.class)).thenReturn(Collections.singletonMap("emf", emf));
		when(emf.getMetamodel()).thenReturn(metamodel);
		when(metamodel.getManagedTypes()).thenReturn(managedTypes);

		JpaMetamodelMappingContextFactoryBean factoryBean = new JpaMetamodelMappingContextFactoryBean();
		factoryBean.setApplicationContext(context);

		factoryBean.createInstance().afterPropertiesSet();
	}

	private void assertOnlyOnePersistenceAnnotationBeanPostProcessorRegistered(DefaultListableBeanFactory factory,
			String expectedBeanName) {

		RepositoryConfigurationExtension extension = new JpaRepositoryConfigExtension();
		extension.registerBeansForRoot(factory, configSource);

		assertThat(factory.getBean(expectedBeanName), is(notNullValue()));
		exception.expect(NoSuchBeanDefinitionException.class);
		factory.getBeanDefinition("org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor#1");
	}
}
