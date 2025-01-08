/*
 * Copyright 2013-2025 the original author or authors.
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
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.Metamodel;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.instrument.classloading.ShadowingClassLoader;
import org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor;

/**
 * Unit tests for {@link JpaRepositoryConfigExtension}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Jens Schauder
 * @author Yanming Zhou
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JpaRepositoryConfigExtensionUnitTests {

	@Mock RepositoryConfigurationSource configSource;

	@Test
	void registersDefaultBeanPostProcessorsByDefault() {

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

		RepositoryConfigurationExtension extension = new JpaRepositoryConfigExtension();
		extension.registerBeansForRoot(factory, configSource);

		Iterable<String> names = Arrays.asList(factory.getBeanDefinitionNames());

		assertThat(names).contains(AnnotationConfigUtils.PERSISTENCE_ANNOTATION_PROCESSOR_BEAN_NAME);
	}

	@Test
	void doesNotRegisterProcessorIfAlreadyPresent() {

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		RootBeanDefinition pabppDefinition = new RootBeanDefinition(PersistenceAnnotationBeanPostProcessor.class);
		String beanName = BeanDefinitionReaderUtils.generateBeanName(pabppDefinition, factory);
		factory.registerBeanDefinition(beanName, pabppDefinition);

		assertOnlyOnePersistenceAnnotationBeanPostProcessorRegistered(factory, beanName);
	}

	@Test
	void doesNotRegisterProcessorIfAutoRegistered() {

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		RootBeanDefinition pabppDefinition = new RootBeanDefinition(PersistenceAnnotationBeanPostProcessor.class);
		String beanName = AnnotationConfigUtils.PERSISTENCE_ANNOTATION_PROCESSOR_BEAN_NAME;
		factory.registerBeanDefinition(beanName, pabppDefinition);

		assertOnlyOnePersistenceAnnotationBeanPostProcessorRegistered(factory, beanName);
	}

	@Test // DATAJPA-525
	void guardsAgainstNullJavaTypesReturnedFromJpaMetamodel() {

		ApplicationContext context = mock(ApplicationContext.class);
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		Metamodel metamodel = mock(Metamodel.class);

		when(context.getBeansOfType(EntityManagerFactory.class)).thenReturn(Collections.singletonMap("emf", emf));
		when(emf.getMetamodel()).thenReturn(metamodel);

		JpaMetamodelMappingContextFactoryBean factoryBean = new JpaMetamodelMappingContextFactoryBean();
		factoryBean.setApplicationContext(context);

		factoryBean.createInstance().afterPropertiesSet();
	}

	@Test // DATAJPA-1250
	void shouldUseInspectionClassLoader() {

		JpaRepositoryConfigExtension extension = new JpaRepositoryConfigExtension();
		ClassLoader classLoader = extension.getConfigurationInspectionClassLoader(new GenericApplicationContext());

		assertThat(classLoader).isInstanceOf(InspectionClassLoader.class);
	}

	@Test // DATAJPA-1250
	void shouldNotUseInspectionClassLoaderWithoutEclipseLink() {

		ShadowingClassLoader shadowingClassLoader = new ShadowingClassLoader(getClass().getClassLoader(), false) {

			@Override
			public Class<?> loadClass(String name) throws ClassNotFoundException {

				if (name.startsWith("org.springframework.instrument.") || name.startsWith("org.eclipse.")) {
					throw new ClassNotFoundException("Excluded: " + name);
				}

				return getClass().getClassLoader().loadClass(name);
			}
		};

		GenericApplicationContext context = new GenericApplicationContext();
		context.setClassLoader(shadowingClassLoader);

		JpaRepositoryConfigExtension extension = new JpaRepositoryConfigExtension();
		ClassLoader classLoader = extension.getConfigurationInspectionClassLoader(context);

		assertThat(classLoader).isNotInstanceOf(InspectionClassLoader.class);
	}

	@Test // GH-2628
	void exposesJpaAotProcessor() {

		assertThat(new JpaRepositoryConfigExtension().getRepositoryAotProcessor())
				.isEqualTo(JpaRepositoryConfigExtension.JpaRepositoryRegistrationAotProcessor.class);
	}

	@Test // GH-2730
	void shouldNotRegisterEntityManagerAsSynthetic() {

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

		RepositoryConfigurationExtension extension = new JpaRepositoryConfigExtension();
		extension.registerBeansForRoot(factory, configSource);

		AbstractBeanDefinition bd = (AbstractBeanDefinition) factory.getBeanDefinition("jpaSharedEM_"
				+ configSource.getAttribute("entityManagerFactoryRef").orElse("entityManagerFactory"));

		assertThat(bd.isSynthetic()).isEqualTo(false);
	}

	private void assertOnlyOnePersistenceAnnotationBeanPostProcessorRegistered(DefaultListableBeanFactory factory,
			String expectedBeanName) {

		RepositoryConfigurationExtension extension = new JpaRepositoryConfigExtension();
		extension.registerBeansForRoot(factory, configSource);

		assertThat(factory.getBean(expectedBeanName)).isNotNull();

		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() -> factory
				.getBeanDefinition("org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor#1"));
	}
}
