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

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.spi.PersistenceUnitInfo;

import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.PersistenceSettings;
import org.hibernate.cfg.QuerySettings;
import org.hibernate.cfg.SchemaToolingSettings;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Lazy;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.persistenceunit.SpringPersistenceUnitInfo;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * AOT metamodel implementation that uses Hibernate to build the metamodel.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Oliver Drotbohm
 * @author Oscar Fanchin
 * @since 4.0
 */
class AotMetamodel implements Metamodel {

	private static final Logger log = LoggerFactory.getLogger(AotMetamodel.class);

	/**
	 * Collection of know properties causing problems during AOT if set differntly
	 */
	private static final Map<String, Object> FAILSAFE_AOT_PROPERTIES = Map.of( //
			JdbcSettings.ALLOW_METADATA_ON_BOOT, false, //
			JdbcSettings.CONNECTION_PROVIDER, NoOpConnectionProvider.INSTANCE, //
			QuerySettings.QUERY_STARTUP_CHECKING, false, //
			PersistenceSettings.JPA_CALLBACKS_ENABLED, false, //
			SchemaToolingSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, "none" // has also precedence over HBM2DDL_AUTO
	);
	private final Lazy<EntityManagerFactory> entityManagerFactory;
	private final Lazy<EntityManager> entityManager = Lazy
			.of(() -> getEntityManagerFactory().createEntityManager(Map.of()));

	public AotMetamodel(PersistenceManagedTypes managedTypes, Map<String, Object> jpaProperties) {
		this(managedTypes.getManagedClassNames(), managedTypes.getPersistenceUnitRootUrl(), jpaProperties);
	}

	public AotMetamodel(Collection<String> managedTypes, @Nullable URL persistenceUnitRootUrl,
			Map<String, Object> jpaProperties) {

		// managedTypes may be backed by a bootstrap-loaded JDK collection whose class loader is null.
		// Use the application class loader so that the persistence unit can resolve managed entity classes.
		ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
		SpringPersistenceUnitInfo persistenceUnitInfo = new SpringPersistenceUnitInfo(
				classLoader != null ? classLoader : AotMetamodel.class.getClassLoader());
		persistenceUnitInfo.setPersistenceUnitName("AotMetamodel");
		persistenceUnitInfo.setPersistenceUnitRootUrl(persistenceUnitRootUrl);
		persistenceUnitInfo.setExcludeUnlistedClasses(true);

		this.entityManagerFactory = init(() -> {

			managedTypes.forEach(persistenceUnitInfo::addManagedClassName);

			persistenceUnitInfo.setPersistenceProviderClassName(HibernatePersistenceProvider.class.getName());
			return new PersistenceUnitInfoDescriptor(persistenceUnitInfo.asStandardPersistenceUnitInfo());
		}, jpaProperties);
	}

	public AotMetamodel(PersistenceUnitInfo unitInfo, Map<String, Object> jpaProperties) {
		this.entityManagerFactory = init(() -> new PersistenceUnitInfoDescriptor(unitInfo), jpaProperties);
	}

	static Lazy<EntityManagerFactory> init(Supplier<PersistenceUnitInfoDescriptor> unitInfo,
			Map<String, Object> jpaProperties) {
		return Lazy.of(() -> new EntityManagerFactoryBuilderImpl(unitInfo.get(), initProperties(jpaProperties)).build());
	}

	static Map<String, Object> initProperties(Map<String, Object> jpaProperties) {

		Map<String, Object> properties = CollectionUtils
				.newLinkedHashMap(jpaProperties.size() + FAILSAFE_AOT_PROPERTIES.size() + 1);

		// we allow explicit Dialect Overrides, but put in a default one to avoid potential db access
		properties.put(JdbcSettings.DIALECT, AotDialectFactory.INSTANCE);

		// apply user defined properties
		properties.putAll(jpaProperties);

		// override properties known to cause trouble
		applyPropertyOverrides(properties);

		return properties;
	}

	private static void applyPropertyOverrides(Map<String, Object> properties) {

		for (Map.Entry<String, Object> entry : FAILSAFE_AOT_PROPERTIES.entrySet()) {

			if (log.isDebugEnabled() && properties.containsKey(entry.getKey())) {
				log.debug("Overriding property [%s] with value [%s] for AOT Repository processing.".formatted(entry.getKey(),
						entry.getValue()));
			}

			properties.put(entry.getKey(), entry.getValue());
		}
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

	static class NoOpConnectionProvider extends UserSuppliedConnectionProviderImpl {

		static final NoOpConnectionProvider INSTANCE = new NoOpConnectionProvider();

		@Override
		public String toString() {
			return "NoOpConnectionProvider";
		}
	}

}
