/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.data.jpa.repository.aot;

import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;

import java.lang.annotation.Annotation;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.domain.sample.SpecialUser;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.support.JpaRepositoryFragmentsContributor;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.config.AotRepositoryContext;
import org.springframework.data.repository.config.AotRepositoryInformation;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.AnnotationRepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition;

/**
 * Test {@link AotRepositoryContext} implementation for JPA repositories.
 *
 * @author Christoph Strobl
 */
public class TestJpaAotRepositoryContext<T> implements AotRepositoryContext {

	private final AotRepositoryInformation repositoryInformation;
	private final Class<T> repositoryInterface;
	private final RepositoryConfigurationSource configurationSource;
	private @Nullable ConfigurableListableBeanFactory beanFactory;

	public TestJpaAotRepositoryContext(Class<T> repositoryInterface, @Nullable RepositoryComposition composition,
			RepositoryConfigurationSource configurationSource) {
		this.repositoryInterface = repositoryInterface;
		this.configurationSource = configurationSource;

		RepositoryMetadata metadata = AnnotationRepositoryMetadata.getMetadata(repositoryInterface);

		RepositoryComposition.RepositoryFragments fragments = JpaRepositoryFragmentsContributor.DEFAULT.describe(metadata);

		this.repositoryInformation = new AotRepositoryInformation(metadata, SimpleJpaRepository.class,
				composition.append(fragments).getFragments().stream().toList());
	}

	public Class<T> getRepositoryInterface() {
		return repositoryInterface;
	}

	@Override
	public ConfigurableListableBeanFactory getBeanFactory() {
		return beanFactory;
	}

	@Override
	public Environment getEnvironment() {
		return new StandardEnvironment();
	}

	@Override
	public TypeIntrospector introspectType(String typeName) {
		return null;
	}

	@Override
	public IntrospectedBeanDefinition introspectBeanDefinition(String beanName) {
		return null;
	}

	@Override
	public String getBeanName() {
		return "dummyRepository";
	}

	@Override
	public String getModuleName() {
		return "JPA";
	}

	@Override
	public RepositoryConfigurationSource getConfigurationSource() {
		return configurationSource;
	}

	@Override
	public Set<String> getBasePackages() {
		return Set.of("org.springframework.data.dummy.repository.aot");
	}

	@Override
	public Set<Class<? extends Annotation>> getIdentifyingAnnotations() {
		return Set.of(Entity.class, MappedSuperclass.class);
	}

	@Override
	public RepositoryInformation getRepositoryInformation() {
		return repositoryInformation;
	}

	@Override
	public Set<MergedAnnotation<Annotation>> getResolvedAnnotations() {
		return Set.of();
	}

	@Override
	public Set<Class<?>> getResolvedTypes() {
		return Set.of(User.class, SpecialUser.class, Role.class);
	}

	public void setBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}
}
