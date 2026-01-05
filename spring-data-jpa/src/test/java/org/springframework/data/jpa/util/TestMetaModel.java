/*
 * Copyright 2024-present the original author or authors.
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
package org.springframework.data.jpa.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;

import java.util.Map;
import java.util.Set;

import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.springframework.data.util.Lazy;
import org.springframework.orm.jpa.persistenceunit.SpringPersistenceUnitInfo;

/**
 * @author Christoph Strobl
 */
public class TestMetaModel implements Metamodel {

	private final String persistenceUnit;
	private final Set<Class<?>> managedTypes;
	private final Lazy<EntityManagerFactory> entityManagerFactory = Lazy.of(this::init);
	private final Lazy<Metamodel> metamodel = Lazy.of(() -> entityManagerFactory.get().getMetamodel());
	private final Lazy<EntityManager> entityManager = Lazy.of(() -> entityManagerFactory.get().createEntityManager());

	private TestMetaModel(Set<Class<?>> managedTypes) {
		this("dynamic-tests", managedTypes);
	}

	private TestMetaModel(String persistenceUnit, Set<Class<?>> managedTypes) {
		this.persistenceUnit = persistenceUnit;
		this.managedTypes = managedTypes;
	}

	public static TestMetaModel hibernateModel(Class<?>... types) {
		return new TestMetaModel(Set.of(types));
	}

	public static TestMetaModel hibernateModel(String persistenceUnit, Class<?>... types) {
		return new TestMetaModel(persistenceUnit, Set.of(types));
	}

	public <X> EntityType<X> entity(Class<X> cls) {
		return metamodel.get().entity(cls);
	}

	@Override
	public EntityType<?> entity(String s) {
		return metamodel.get().entity(s);
	}

	public <X> ManagedType<X> managedType(Class<X> cls) {
		return metamodel.get().managedType(cls);
	}

	public <X> EmbeddableType<X> embeddable(Class<X> cls) {
		return metamodel.get().embeddable(cls);
	}

	public Set<ManagedType<?>> getManagedTypes() {
		return metamodel.get().getManagedTypes();
	}

	public Set<EntityType<?>> getEntities() {
		return metamodel.get().getEntities();
	}

	public Set<EmbeddableType<?>> getEmbeddables() {
		return metamodel.get().getEmbeddables();
	}

	public EntityManager entityManager() {
		return entityManager.get();
	}

	EntityManagerFactory init() {

		SpringPersistenceUnitInfo persistenceUnitInfo = new SpringPersistenceUnitInfo(this.getClass().getClassLoader());

		persistenceUnitInfo.setPersistenceUnitName(persistenceUnit);
		this.managedTypes.stream().map(Class::getName).forEach(persistenceUnitInfo::addManagedClassName);
		persistenceUnitInfo.setPersistenceProviderClassName(HibernatePersistenceProvider.class.getName());

		return new EntityManagerFactoryBuilderImpl(
				new PersistenceUnitInfoDescriptor(persistenceUnitInfo.asStandardPersistenceUnitInfo()) {},
				Map.of("hibernate.dialect", "org.hibernate.dialect.H2Dialect")).build();
	}
}
