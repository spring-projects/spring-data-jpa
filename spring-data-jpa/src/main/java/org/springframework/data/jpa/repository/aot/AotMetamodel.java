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

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.cfg.JdbcSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.jspecify.annotations.Nullable;

import org.springframework.data.repository.config.AotRepositoryContext;
import org.springframework.data.util.Lazy;
import org.springframework.instrument.classloading.SimpleThrowawayClassLoader;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;

/**
 * AOT metamodel implementation that uses Hibernate to build the metamodel.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 4.0
 */
class AotMetamodel implements Metamodel {

	private final Lazy<EntityManagerFactory> entityManagerFactory;
	private final Lazy<EntityManager> entityManager = Lazy.of(() -> getEntityManagerFactory().createEntityManager());

	public AotMetamodel(AotRepositoryContext repositoryContext) {
		this(repositoryContext.getResolvedTypes().stream().filter(AotMetamodel::isJakartaAnnotated).map(Class::getName)
				.toList(), null);
	}

	private static boolean isJakartaAnnotated(Class<?> cls) {

		return cls.isAnnotationPresent(jakarta.persistence.Entity.class)
				|| cls.isAnnotationPresent(jakarta.persistence.Embeddable.class)
				|| cls.isAnnotationPresent(jakarta.persistence.MappedSuperclass.class)
				|| cls.isAnnotationPresent(jakarta.persistence.Converter.class);
	}

	public AotMetamodel(PersistenceManagedTypes managedTypes) {
		this(managedTypes.getManagedClassNames(), managedTypes.getPersistenceUnitRootUrl());
	}

	public AotMetamodel(Collection<String> managedTypes, @Nullable URL persistenceUnitRootUrl) {

		MutablePersistenceUnitInfo persistenceUnitInfo = new MutablePersistenceUnitInfo() {
			@Override
			public ClassLoader getNewTempClassLoader() {
				return new SimpleThrowawayClassLoader(this.getClass().getClassLoader());
			}

			@Override
			public void addTransformer(ClassTransformer classTransformer) {
				// just ignore it
			}
		};
		persistenceUnitInfo.setPersistenceUnitName("AotMetaModel");

		this.entityManagerFactory = init(() -> {

			managedTypes.forEach(persistenceUnitInfo::addManagedClassName);

			persistenceUnitInfo.setPersistenceProviderClassName(HibernatePersistenceProvider.class.getName());

			return new PersistenceUnitInfoDescriptor(persistenceUnitInfo) {

				@Override
				public List<String> getManagedClassNames() {
					return persistenceUnitInfo.getManagedClassNames();
				}

				@Override
				public URL getPersistenceUnitRootUrl() {
					return persistenceUnitRootUrl != null ? persistenceUnitRootUrl : super.getPersistenceUnitRootUrl();
				}

			};
		});
	}

	public AotMetamodel(PersistenceUnitInfo unitInfo) {
		this.entityManagerFactory = init(() -> new PersistenceUnitInfoDescriptor(unitInfo));
	}

	static Lazy<EntityManagerFactory> init(Supplier<PersistenceUnitInfoDescriptor> unitInfo) {

		return Lazy.of(() -> new EntityManagerFactoryBuilderImpl(unitInfo.get(),
				Map.of(JdbcSettings.DIALECT, H2Dialect.class.getName(), //
						JdbcSettings.ALLOW_METADATA_ON_BOOT, "false", //
						JdbcSettings.CONNECTION_PROVIDER, new UserSuppliedConnectionProviderImpl()))
				.build());
	}

	private Metamodel getMetamodel() {
		return getEntityManagerFactory().getMetamodel();
	}

	public <X> EntityType<X> entity(Class<X> cls) {
		return getMetamodel().entity(cls);
	}

	@Override
	public EntityType<?> entity(String s) {
		return getMetamodel().entity(s);
	}

	public <X> ManagedType<X> managedType(Class<X> cls) {
		return getMetamodel().managedType(cls);
	}

	public <X> EmbeddableType<X> embeddable(Class<X> cls) {
		return getMetamodel().embeddable(cls);
	}

	public Set<ManagedType<?>> getManagedTypes() {
		return getMetamodel().getManagedTypes();
	}

	public Set<EntityType<?>> getEntities() {
		return getMetamodel().getEntities();
	}

	public Set<EmbeddableType<?>> getEmbeddables() {
		return getMetamodel().getEmbeddables();
	}

	public EntityManager entityManager() {
		return entityManager.get();
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return entityManagerFactory.get();
	}

}
