/*
 * Copyright 2013-2019 the original author or authors.
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.data.jpa.domain.support.AuditingBeanFactoryPostProcessor;

/**
 * Unit tests for {@link JpaAuditingRegistrar}.
 *
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class JpaAuditingRegistrarUnitTests {

	JpaAuditingRegistrar registrar = new JpaAuditingRegistrar();

	@Mock AnnotationMetadata metadata;
	@Mock BeanDefinitionRegistry registry;

	@Test // DATAJPA-265
	public void rejectsNullAnnotationMetadata() {

		assertThatExceptionOfType(IllegalArgumentException.class) //
				.isThrownBy(() -> registrar.registerBeanDefinitions(null, registry));
	}

	@Test // DATAJPA-265
	public void rejectsNullBeanDefinitionRegistry() {

		assertThatExceptionOfType(IllegalArgumentException.class) //
				.isThrownBy(() -> registrar.registerBeanDefinitions(metadata, null));
	}

	@Test // DATAJPA-1448
	public void doesNotRegisterBeanConfigurerTwice() throws Exception {

		SimpleMetadataReaderFactory factory = new SimpleMetadataReaderFactory();
		MetadataReader reader = factory.getMetadataReader(Sample.class.getName());
		AnnotationMetadata annotationMetadata = reader.getAnnotationMetadata();

		// Given a bean already present
		String beanName = AuditingBeanFactoryPostProcessor.BEAN_CONFIGURER_ASPECT_BEAN_NAME;
		when(registry.containsBeanDefinition(beanName)).thenReturn(true);

		// When invoking configuration
		registrar.registerBeanDefinitions(annotationMetadata, registry);

		// Then the bean is not registered again
		verify(registry, times(0)).registerBeanDefinition(eq(beanName), any());

		registrar.registerBeanDefinitions(annotationMetadata, registry);
	}

	@EnableJpaAuditing
	static class Sample {}
}
