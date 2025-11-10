/*
 * Copyright 2025-present the original author or authors.
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

import jakarta.persistence.Converter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.spi.PersistenceUnitInfo;

import java.util.List;
import java.util.function.Supplier;

import org.springframework.data.repository.config.AotRepositoryContext;
import org.springframework.data.util.Lazy;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.util.ConcurrentLruCache;

/**
 * @author Christoph Strobl
 */
public class PersistenceUnitContexts {

	private static final ConcurrentLruCache<Object, PersistenceUnitContextFactory> PERSISTENCE_UNIT_FACTORY_CACHE = new ConcurrentLruCache<>(
			16, PersistenceUnitContexts::createFactory);

	private static PersistenceUnitContextFactory createFactory(Object source) {

		if (source instanceof AotRepositoryContext ctx) {

			List<String> typeNames = ctx.getResolvedTypes().stream()
					.filter(DefaultPersistenceUnitContextFactory::isJakartaAnnotated).map(Class::getName).toList();

			return from(() -> new AotMetamodel(PersistenceManagedTypes.of(typeNames, List.of())));
		}
		if (source instanceof PersistenceUnitInfo pui) {
			return from(() -> new AotMetamodel(pui));
		}
		if (source instanceof PersistenceManagedTypes managedTypes) {
			return from(() -> new AotMetamodel(managedTypes));
		}
		if (source instanceof EntityManagerFactory emf) {
			return new DefaultPersistenceUnitContextFactory(
					() -> new EntityManagerPersistenceUnitContext(emf, emf.getMetamodel()));
		}

		throw new IllegalArgumentException(String.format("Cannot create PersistenceUnitContexts for %s", source));
	}

	/**
	 * Create a {@code PersistenceUnitContext} from the given {@link AotRepositoryContext} using Jakarta
	 * Persistence-annotated classes.
	 *
	 * @param repositoryContext repository context providing classes.
	 */
	public static PersistenceUnitContext from(AotRepositoryContext repositoryContext) {
		return factory().from(repositoryContext).create();
	}

	/**
	 * Create a {@code PersistenceUnitContext} from the given {@link PersistenceUnitInfo}.
	 *
	 * @param persistenceUnitInfo persistence unit info to use.
	 */
	public static PersistenceUnitContext from(PersistenceUnitInfo persistenceUnitInfo) {
		return factory().from(persistenceUnitInfo).create();
	}

	/**
	 * Create a {@code PersistenceUnitContext} from the given {@link PersistenceManagedTypes}.
	 *
	 * @param managedTypes managed types to use.
	 */
	public static PersistenceUnitContext from(PersistenceManagedTypes managedTypes) {
		return factory().from(managedTypes).create();
	}

	/**
	 * Create a {@code PersistenceUnitContext} from the given {@link EntityManagerFactory} and its {@link Metamodel}.
	 *
	 * @param entityManagerFactory the entity manager factory to use.
	 */
	public static PersistenceUnitContext from(EntityManagerFactory entityManagerFactory) {
		return factory().from(entityManagerFactory).create();
	}

	private static PersistenceUnitContextFactory from(Supplier<? extends AotMetamodel> metamodel) {
		return new DefaultPersistenceUnitContextFactory(() -> new AotMetamodelContext(metamodel.get()));
	}

	public static PeristenceUnitFactoryBuilder factory() {

		return new PeristenceUnitFactoryBuilder() {

			@Override
			public PersistenceUnitContextFactory from(EntityManagerFactory entityManagerFactory) {
				return PERSISTENCE_UNIT_FACTORY_CACHE.get(entityManagerFactory);
			}

			@Override
			public PersistenceUnitContextFactory from(PersistenceManagedTypes managedTypes) {
				return PERSISTENCE_UNIT_FACTORY_CACHE.get(managedTypes);
			}

			@Override
			public PersistenceUnitContextFactory from(PersistenceUnitInfo persistenceUnitInfo) {
				return PERSISTENCE_UNIT_FACTORY_CACHE.get(persistenceUnitInfo);
			}

			@Override
			public PersistenceUnitContextFactory from(AotRepositoryContext repositoryContext) {
				return PERSISTENCE_UNIT_FACTORY_CACHE.get(repositoryContext);
			}
		};
	}

	public interface PeristenceUnitFactoryBuilder {
		PersistenceUnitContextFactory from(EntityManagerFactory entityManagerFactory);

		PersistenceUnitContextFactory from(PersistenceManagedTypes entityManagerFactory);

		PersistenceUnitContextFactory from(PersistenceUnitInfo entityManagerFactory);

		PersistenceUnitContextFactory from(AotRepositoryContext entityManagerFactory);
	}

	/**
	 * Persistence unit context backed by an {@link AotMetamodel}.
	 */
	record AotMetamodelContext(AotMetamodel metamodel) implements PersistenceUnitContext {

		@Override
		public EntityManagerFactory getEntityManagerFactory() {
			return metamodel.getEntityManagerFactory();
		}

		@Override
		public Metamodel getMetamodel() {
			return metamodel;
		}
	}

	/**
	 * Persistence unit context backed by an {@link EntityManagerFactory}.
	 */
	record EntityManagerPersistenceUnitContext(EntityManagerFactory factory,
			Metamodel metamodel) implements PersistenceUnitContext {

		public EntityManagerPersistenceUnitContext(EntityManagerFactory factory) {
			this(factory, factory.getMetamodel());
		}

		@Override
		public Metamodel getMetamodel() {
			return metamodel();
		}

		@Override
		public EntityManagerFactory getEntityManagerFactory() {
			return factory();
		}

	}

	public interface PersistenceUnitContextFactory {
		PersistenceUnitContext create();
	}

	/**
	 * Factory for deferred {@link PersistenceUnitContext} creation. Factory objects implement equality checks based on
	 * their creation and can be used conveniently as cache keys.
	 */
	public static class DefaultPersistenceUnitContextFactory implements PersistenceUnitContextFactory {

		private final Supplier<? extends PersistenceUnitContext> persistenceUnitContextSupplier;

		private DefaultPersistenceUnitContextFactory(
				Supplier<? extends PersistenceUnitContext> persistenceUnitContextSupplier) {

			this.persistenceUnitContextSupplier = Lazy.of(persistenceUnitContextSupplier);
		}

		private static boolean isJakartaAnnotated(Class<?> cls) {

			return cls.isAnnotationPresent(Entity.class) //
					|| cls.isAnnotationPresent(Embeddable.class) //
					|| cls.isAnnotationPresent(MappedSuperclass.class) //
					|| cls.isAnnotationPresent(Converter.class);
		}

		public PersistenceUnitContext create() {
			return persistenceUnitContextSupplier.get();
		}
	}
}
