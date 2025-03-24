/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.data.jpa.repository.aot.generated;

import java.lang.reflect.Method;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.core.CrudMethods;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.util.Streamable;
import org.springframework.data.util.TypeInformation;

/**
 * @author Christoph Strobl
 */
class StubRepositoryInformation implements RepositoryInformation {

	private final RepositoryMetadata metadata;
	private final RepositoryComposition baseComposition;

	public StubRepositoryInformation(Class<?> repositoryInterface, @Nullable RepositoryComposition composition) {

		this.metadata = AbstractRepositoryMetadata.getMetadata(repositoryInterface);
		this.baseComposition = composition != null ? composition
				: RepositoryComposition.of(RepositoryFragment.structural(SimpleJpaRepository.class));
	}

	@Override
	public TypeInformation<?> getIdTypeInformation() {
		return metadata.getIdTypeInformation();
	}

	@Override
	public TypeInformation<?> getDomainTypeInformation() {
		return metadata.getDomainTypeInformation();
	}

	@Override
	public Class<?> getRepositoryInterface() {
		return metadata.getRepositoryInterface();
	}

	@Override
	public TypeInformation<?> getReturnType(Method method) {
		return metadata.getReturnType(method);
	}

	@Override
	public Class<?> getReturnedDomainClass(Method method) {
		return metadata.getReturnedDomainClass(method);
	}

	@Override
	public CrudMethods getCrudMethods() {
		return metadata.getCrudMethods();
	}

	@Override
	public boolean isPagingRepository() {
		return false;
	}

	@Override
	public Set<Class<?>> getAlternativeDomainTypes() {
		return null;
	}

	@Override
	public boolean isReactiveRepository() {
		return false;
	}

	@Override
	public Set<RepositoryFragment<?>> getFragments() {
		return null;
	}

	@Override
	public boolean isBaseClassMethod(Method method) {
		return baseComposition.findMethod(method).isPresent();
	}

	@Override
	public boolean isCustomMethod(Method method) {
		return false;
	}

	@Override
	public boolean isQueryMethod(Method method) {
		return false;
	}

	@Override
	public Streamable<Method> getQueryMethods() {
		return null;
	}

	@Override
	public Class<?> getRepositoryBaseClass() {
		return SimpleJpaRepository.class;
	}

	@Override
	public Method getTargetClassMethod(Method method) {
		return null;
	}
}
