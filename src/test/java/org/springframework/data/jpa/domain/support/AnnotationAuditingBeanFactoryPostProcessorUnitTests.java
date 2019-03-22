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
package org.springframework.data.jpa.domain.support;

import static org.mockito.Mockito.*;

import javax.persistence.EntityManagerFactory;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

/**
 * Unit tests to check that the {@link AuditingBeanFactoryPostProcessor} does its job for annotation based
 * configuration.
 *
 * @author Oliver Gierke
 */
public class AnnotationAuditingBeanFactoryPostProcessorUnitTests extends AuditingBeanFactoryPostProcessorUnitTests {

	@Configuration
	@EnableJpaAuditing
	static class TestConfig {

		@Bean
		public EntityManagerFactory entityManagerFactory() {
			return mock(EntityManagerFactory.class);
		}

		@Bean
		public LocalContainerEntityManagerFactoryBean entityManagerFactoryBean() {
			return mock(LocalContainerEntityManagerFactoryBean.class);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.domain.support.AuditingBeanFactoryPostProcessorUnitTests#getBeanFactory()
	 */
	@Override
	protected DefaultListableBeanFactory getBeanFactory() {

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("testConfig", new RootBeanDefinition(TestConfig.class));

		ConfigurationClassPostProcessor processor = new ConfigurationClassPostProcessor();
		processor.postProcessBeanDefinitionRegistry(beanFactory);

		return beanFactory;
	}
}
