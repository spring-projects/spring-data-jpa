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

import jakarta.persistence.Converter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.spi.PersistenceUnitInfo;

import java.util.List;
import java.util.function.Supplier;

import org.springframework.data.repository.config.AotRepositoryContext;
import org.springframework.data.util.Lazy;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.util.ObjectUtils;

/**
 * Wrapper for {@link EntityManagerFactory}. The wrapper object implements equality checks based on its creation
 * arguments and can be conveniently used as cache key.
 * <p>
 * Factory methods provide ways to create instances based on provided {@link EntityManagerFactory} or contextual holders
 * to extract managed types and create an in-memory {@link EntityManagerFactory} variant for metamodel introspection
 * during AOT processing.
 *
 * @author Mark Paluch
 * @since 4.0
 */
public class AotEntityManagerFactoryCreator {

	private final Supplier<EntityManagerFactory> factory;
	private final Object key;

	private AotEntityManagerFactoryCreator(Supplier<EntityManagerFactory> factory, Object key) {
		this.factory = Lazy.of(factory);
		this.key = key;
	}

	/**
	 * Create a {@code PersistenceUnitContext} from the given {@link AotRepositoryContext} using Jakarta
	 * Persistence-annotated classes.
	 * <p>
	 * The underlying {@link jakarta.persistence.metamodel.Metamodel} requires Hibernate to build metamodel information.
	 *
	 * @param repositoryContext repository context providing classes.
	 */
	public static AotEntityManagerFactoryCreator from(AotRepositoryContext repositoryContext) {

		List<String> typeNames = repositoryContext.getResolvedTypes().stream()
				.filter(AotEntityManagerFactoryCreator::isJakartaAnnotated).map(Class::getName).toList();

		return from(PersistenceManagedTypes.of(typeNames, List.of()), typeNames);
	}

	/**
	 * Create a {@code PersistenceUnitContext} from the given {@link PersistenceUnitInfo}.
	 * <p>
	 * The underlying {@link jakarta.persistence.metamodel.Metamodel} requires Hibernate to build metamodel information.
	 *
	 * @param persistenceUnitInfo persistence unit info to use.
	 */
	public static AotEntityManagerFactoryCreator from(PersistenceUnitInfo persistenceUnitInfo) {
		return from(() -> new AotMetamodel(persistenceUnitInfo), persistenceUnitInfo);
	}

	/**
	 * Create a {@code PersistenceUnitContext} from the given {@link PersistenceManagedTypes}.
	 * <p>
	 * The underlying {@link jakarta.persistence.metamodel.Metamodel} requires Hibernate to build metamodel information.
	 *
	 * @param managedTypes managed types to use.
	 */
	public static AotEntityManagerFactoryCreator from(PersistenceManagedTypes managedTypes) {
		return from(managedTypes, managedTypes);
	}

	private static AotEntityManagerFactoryCreator from(PersistenceManagedTypes managedTypes, Object cacheKey) {
		return from(() -> new AotMetamodel(managedTypes), cacheKey);
	}

	/**
	 * Create a {@code PersistenceUnitContext} from the given {@link EntityManagerFactory}.
	 *
	 * @param entityManagerFactory the entity manager factory to use.
	 */
	public static AotEntityManagerFactoryCreator just(EntityManagerFactory entityManagerFactory) {
		return new AotEntityManagerFactoryCreator(() -> entityManagerFactory, entityManagerFactory.getMetamodel());
	}

	private static AotEntityManagerFactoryCreator from(Supplier<? extends AotMetamodel> metamodel, Object key) {
		return new AotEntityManagerFactoryCreator(() -> metamodel.get().getEntityManagerFactory(), key);
	}

	private static boolean isJakartaAnnotated(Class<?> cls) {

		return cls.isAnnotationPresent(Entity.class) //
				|| cls.isAnnotationPresent(Embeddable.class) //
				|| cls.isAnnotationPresent(MappedSuperclass.class) //
				|| cls.isAnnotationPresent(Converter.class);
	}

	/**
	 * Return the {@link EntityManagerFactory}.
	 *
	 * @return the entity manager factory to use during AOT processing.
	 */
	public EntityManagerFactory getEntityManagerFactory() {
		return factory.get();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof AotEntityManagerFactoryCreator that)) {
			return false;
		}
		return ObjectUtils.nullSafeEquals(key, that.key);
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(key);
	}

	@Override
	public String toString() {
		return "AotEntityManagerFactory{" + key + '}';
	}

}
