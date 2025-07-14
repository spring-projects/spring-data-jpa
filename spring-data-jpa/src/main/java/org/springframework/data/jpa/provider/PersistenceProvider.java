/*
 * Copyright 2008-2025 the original author or authors.
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
package org.springframework.data.jpa.provider;

import static org.springframework.data.jpa.provider.JpaClassUtils.*;
import static org.springframework.data.jpa.provider.PersistenceProvider.Constants.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.stream.Stream;

import org.eclipse.persistence.config.QueryHints;
import org.eclipse.persistence.internal.queries.DatabaseQueryMechanism;
import org.eclipse.persistence.internal.queries.JPQLCallQueryMechanism;
import org.eclipse.persistence.jpa.JpaQuery;
import org.eclipse.persistence.queries.DatabaseQuery;
import org.eclipse.persistence.queries.ScrollableCursor;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.query.SelectionQuery;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.data.util.CloseableIterator;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;

/**
 * Enumeration representing persistence providers to be used.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 * @author Jens Schauder
 * @author Greg Turnquist
 * @author Yuriy Tsarkov
 * @author Ariel Morelli Andres
 */
public enum PersistenceProvider implements QueryExtractor, ProxyIdAccessor, QueryComment {

	/**
	 * Hibernate persistence provider.
	 */
	HIBERNATE(List.of(HIBERNATE_ENTITY_MANAGER_FACTORY_INTERFACE), //
			List.of(HIBERNATE_JPA_METAMODEL_TYPE)) {

		@Override
		public @Nullable String extractQueryString(Object query) {
			return HibernateUtils.getHibernateQuery(query);
		}

		@Override
		public boolean isNativeQuery(Object query) {
			return HibernateUtils.isNativeQuery(query);
		}

		/**
		 * Return custom placeholder ({@code *}) as Hibernate does create invalid queries for count queries for objects with
		 * compound keys.
		 *
		 * @see <a href="https://hibernate.atlassian.net/browse/HHH-4044">HHH-4044</a>
		 * @see <a href="https://hibernate.atlassian.net/browse/HHH-3096">HHH-3096</a>
		 */
		@Override
		public String getCountQueryPlaceholder() {
			return "*";
		}

		@Override
		public boolean shouldUseAccessorFor(Object entity) {
			return entity instanceof HibernateProxy;
		}

		@Override
		public Object getIdentifierFrom(Object entity) {
			return ((HibernateProxy) entity).getHibernateLazyInitializer().getIdentifier();
		}

		@Override
		public <T> Set<SingularAttribute<? super T, ?>> getIdClassAttributes(IdentifiableType<T> type) {
			return type.hasSingleIdAttribute() ? Collections.emptySet() : super.getIdClassAttributes(type);
		}

		@Override
		public CloseableIterator<Object> executeQueryWithResultStream(Query jpaQuery) {
			return new HibernateScrollableResultsIterator(jpaQuery);
		}

		@Override
		public String getCommentHintKey() {
			return "org.hibernate.comment";
		}

		@Override
		public long getResultCount(Query resultQuery, LongSupplier countSupplier) {

			if (TransactionSynchronizationManager.isActualTransactionActive()
					&& resultQuery instanceof SelectionQuery<?> sq) {
				return sq.getResultCount();
			}

			return super.getResultCount(resultQuery, countSupplier);
		}

	},

	/**
	 * EclipseLink persistence provider.
	 */
	ECLIPSELINK(List.of(ECLIPSELINK_ENTITY_MANAGER_FACTORY_INTERFACE), List.of(ECLIPSELINK_JPA_METAMODEL_TYPE)) {

		@Override
		public String extractQueryString(Object query) {

			if (query instanceof JpaQuery<?> jpaQuery) {

				DatabaseQuery databaseQuery = jpaQuery.getDatabaseQuery();

				if (StringUtils.hasText(databaseQuery.getJPQLString())) {
					return databaseQuery.getJPQLString();
				}

				if (StringUtils.hasText(databaseQuery.getSQLString())) {
					return databaseQuery.getSQLString();
				}
			}

			return "";
		}

		@Override
		public boolean isNativeQuery(Object query) {

			if (query instanceof JpaQuery<?> jpaQuery) {

				DatabaseQueryMechanism call = jpaQuery.getDatabaseQuery().getQueryMechanism();

				if (call instanceof JPQLCallQueryMechanism) {
					return false;
				}

				return true;
			}

			return false;
		}

		@Override
		public boolean shouldUseAccessorFor(Object entity) {
			return false;
		}

		@Override
		public @Nullable Object getIdentifierFrom(Object entity) {
			return null;
		}

		@Override
		public CloseableIterator<Object> executeQueryWithResultStream(Query jpaQuery) {
			return new EclipseLinkScrollableResultsIterator<>(jpaQuery);
		}

		@Override
		public String getCommentHintKey() {
			return QueryHints.HINT;
		}

		@Override
		public String getCommentHintValue(String comment) {
			return "/* " + comment + " */";
		}

	},

	/**
	 * Unknown special provider. Use standard JPA.
	 */
	GENERIC_JPA(List.of(GENERIC_JPA_ENTITY_MANAGER_FACTORY_INTERFACE), Collections.emptySet()) {

		@Override
		public @Nullable String extractQueryString(Object query) {
			return null;
		}

		@Override
		public boolean isNativeQuery(Object query) {
			return false;
		}

		@Override
		public boolean canExtractQuery() {
			return false;
		}

		@Override
		public boolean shouldUseAccessorFor(Object entity) {
			return false;
		}

		@Override
		public @Nullable Object getIdentifierFrom(Object entity) {
			return null;
		}

		@Override
		public @Nullable String getCommentHintKey() {
			return null;
		}

	};

	private static final @Nullable Class<?> typedParameterValueClass;

	static {

		Class<?> type;
		try {
			type = ClassUtils.forName("org.hibernate.query.TypedParameterValue", PersistenceProvider.class.getClassLoader());
		} catch (ClassNotFoundException e) {
			type = null;
		}
		typedParameterValueClass = type;
	}

	private static final Collection<PersistenceProvider> ALL = List.of(HIBERNATE, ECLIPSELINK, GENERIC_JPA);

	private static final ConcurrentReferenceHashMap<Class<?>, PersistenceProvider> CACHE = new ConcurrentReferenceHashMap<>();
	final Iterable<String> entityManagerFactoryClassNames;
	private final Iterable<String> metamodelClassNames;

	private final boolean present;

	/**
	 * Creates a new {@link PersistenceProvider}.
	 *
	 * @param entityManagerFactoryClassNames the names of the provider specific
	 *          {@link jakarta.persistence.EntityManagerFactory} implementations. Must not be {@literal null} or empty.
	 * @param metamodelClassNames the names of the provider specific {@link Metamodel} implementations. Must not be
	 *          {@literal null} or empty.
	 */
	PersistenceProvider(Collection<String> entityManagerFactoryClassNames, Collection<String> metamodelClassNames) {

		this.entityManagerFactoryClassNames = entityManagerFactoryClassNames;
		this.metamodelClassNames = metamodelClassNames;
		this.present = Stream.concat(entityManagerFactoryClassNames.stream(), metamodelClassNames.stream())
				.anyMatch(it -> ClassUtils.isPresent(it, PersistenceProvider.class.getClassLoader()));
	}

	/**
	 * Caches the given {@link PersistenceProvider} for the given source type.
	 *
	 * @param type must not be {@literal null}.
	 * @param provider must not be {@literal null}.
	 * @return the {@code PersistenceProvider} passed in as an argument. Guaranteed to be not {@code null}.
	 */
	private static PersistenceProvider cacheAndReturn(Class<?> type, PersistenceProvider provider) {
		CACHE.put(type, provider);
		return provider;
	}

	/**
	 * Determines the {@link PersistenceProvider} from the given {@link EntityManager} by introspecting
	 * {@link EntityManagerFactory} via {@link EntityManager#getEntityManagerFactory()}. If no special one can be
	 * determined {@link #GENERIC_JPA} will be returned.
	 * <p>
	 * This method avoids {@link EntityManager} initialization when using
	 * {@link org.springframework.orm.jpa.SharedEntityManagerCreator} by accessing
	 * {@link EntityManager#getEntityManagerFactory()}.
	 *
	 * @param em must not be {@literal null}.
	 * @return will never be {@literal null}.
	 * @see org.springframework.orm.jpa.SharedEntityManagerCreator
	 */
	public static PersistenceProvider fromEntityManager(EntityManager em) {

		Assert.notNull(em, "EntityManager must not be null");

		return fromEntityManagerFactory(em.getEntityManagerFactory());
	}

	/**
	 * Determines the {@link PersistenceProvider} from the given {@link EntityManagerFactory}. If no special one can be
	 * determined {@link #GENERIC_JPA} will be returned.
	 *
	 * @param emf must not be {@literal null}.
	 * @return will never be {@literal null}.
	 * @since 3.5.1
	 */
	public static PersistenceProvider fromEntityManagerFactory(EntityManagerFactory emf) {

		Assert.notNull(emf, "EntityManagerFactory must not be null");

		EntityManagerFactory unwrapped = emf;

		while (Proxy.isProxyClass(unwrapped.getClass()) || AopUtils.isAopProxy(unwrapped)) {

			if (Proxy.isProxyClass(unwrapped.getClass())) {

				Class<EntityManagerFactory> unwrapTo = Proxy.getInvocationHandler(unwrapped).getClass().getName()
						.contains("org.springframework.orm.jpa.") ? null : EntityManagerFactory.class;
				unwrapped = unwrapped.unwrap(unwrapTo);
			} else if (AopUtils.isAopProxy(unwrapped)) {
				unwrapped = (EntityManagerFactory) AopProxyUtils.getSingletonTarget(unwrapped);
			}
		}

		Class<?> entityManagerType = unwrapped.getClass();
		PersistenceProvider cachedProvider = CACHE.get(entityManagerType);

		if (cachedProvider != null) {
			return cachedProvider;
		}

		for (PersistenceProvider provider : ALL) {
			for (String emfClassName : provider.entityManagerFactoryClassNames) {
				if (isOfType(unwrapped, emfClassName, unwrapped.getClass().getClassLoader())) {
					return cacheAndReturn(entityManagerType, provider);
				}
			}
		}

		return cacheAndReturn(entityManagerType, GENERIC_JPA);
	}

	/**
	 * Determines the {@link PersistenceProvider} from the given {@link Metamodel}. If no special one can be determined
	 * {@link #GENERIC_JPA} will be returned.
	 *
	 * @param metamodel must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public static PersistenceProvider fromMetamodel(Metamodel metamodel) {

		Assert.notNull(metamodel, "Metamodel must not be null");

		Class<? extends Metamodel> metamodelType = metamodel.getClass();
		PersistenceProvider cachedProvider = CACHE.get(metamodelType);

		if (cachedProvider != null) {
			return cachedProvider;
		}

		for (PersistenceProvider provider : values()) {
			for (String metamodelClassName : provider.metamodelClassNames) {
				if (isMetamodelOfType(metamodel, metamodelClassName)) {
					return cacheAndReturn(metamodelType, provider);
				}
			}
		}

		return cacheAndReturn(metamodelType, GENERIC_JPA);
	}

	/**
	 * Returns the placeholder to be used for simple count queries. Default implementation returns {@code x}.
	 *
	 * @return a placeholder for count queries. Guaranteed to be not {@code null}.
	 */
	public String getCountQueryPlaceholder() {
		return "x";
	}

	@Override
	public boolean canExtractQuery() {
		return true;
	}

	/**
	 * @param type the entity type.
	 * @return the set of identifier attributes used in a {@code @IdClass} for {@code type}. Empty when {@code type} does
	 *         not use {@code @IdClass}.
	 * @since 2.5.6
	 */
	public <T> Set<SingularAttribute<? super T, ?>> getIdClassAttributes(IdentifiableType<T> type) {
		try {
			return type.getIdClassAttributes();
		} catch (IllegalArgumentException e) {
			return Collections.emptySet();
		}
	}

	/**
	 * Because Hibernate's {@literal TypedParameterValue} is only used to wrap a {@literal null}, swap it out with
	 * {@code null} for query creation.
	 *
	 * @param value
	 * @return the original value or null.
	 * @since 3.0
	 */
	public static @Nullable Object unwrapTypedParameterValue(@Nullable Object value) {

		return typedParameterValueClass != null && typedParameterValueClass.isInstance(value) //
				? null //
				: value;
	}

	public boolean isPresent() {
		return this.present;
	}

	/**
	 * Obtain the result count from a {@link Query} returning the result or fall back to {@code countSupplier} if the
	 * query does not provide the result count.
	 *
	 * @param resultQuery the query that has returned {@link Query#getResultList()}
	 * @param countSupplier fallback supplier to provide the count if the query does not provide it.
	 * @return the result count.
	 * @since 4.0
	 */
	public long getResultCount(Query resultQuery, LongSupplier countSupplier) {
		return countSupplier.getAsLong();
	}

	/**
	 * Holds the PersistenceProvider specific interface names.
	 *
	 * @author Thomas Darimont
	 * @author Jens Schauder
	 */
	interface Constants {

		String GENERIC_JPA_ENTITY_MANAGER_FACTORY_INTERFACE = "jakarta.persistence.EntityManagerFactory";
		String GENERIC_JPA_ENTITY_MANAGER_INTERFACE = "jakarta.persistence.EntityManager";

		String ECLIPSELINK_ENTITY_MANAGER_FACTORY_INTERFACE = "org.eclipse.persistence.jpa.JpaEntityManagerFactory";
		String ECLIPSELINK_ENTITY_MANAGER_INTERFACE = "org.eclipse.persistence.jpa.JpaEntityManager";
		String ECLIPSELINK_JPA_METAMODEL_TYPE = "org.eclipse.persistence.internal.jpa.metamodel.MetamodelImpl";

		// needed as Spring only exposes that interface via the EM proxy
		String HIBERNATE_ENTITY_MANAGER_FACTORY_INTERFACE = "org.hibernate.SessionFactory";
		String HIBERNATE_ENTITY_MANAGER_INTERFACE = "org.hibernate.Session";
		String HIBERNATE_JPA_METAMODEL_TYPE = "org.hibernate.metamodel.model.domain.JpaMetamodel";

	}

	public CloseableIterator<Object> executeQueryWithResultStream(Query jpaQuery) {
		throw new UnsupportedOperationException(
				"Streaming results is not implement for this PersistenceProvider: " + name());
	}

	/**
	 * {@link CloseableIterator} for Hibernate.
	 *
	 * @author Thomas Darimont
	 * @author Oliver Gierke
	 * @since 1.8
	 */
	private static class HibernateScrollableResultsIterator implements CloseableIterator<Object> {

		private final @Nullable ScrollableResults<Object[]> scrollableResults;

		/**
		 * Creates a new {@link HibernateScrollableResultsIterator} for the given {@link Query}.
		 *
		 * @param jpaQuery must not be {@literal null}.
		 */
		HibernateScrollableResultsIterator(Query jpaQuery) {

			org.hibernate.query.Query<Object[]> query = jpaQuery.unwrap(org.hibernate.query.Query.class);
			this.scrollableResults = query.setReadOnly(TransactionSynchronizationManager.isCurrentTransactionReadOnly())//
					.scroll(ScrollMode.FORWARD_ONLY);
		}

		@Override
		public Object next() {

			if (scrollableResults == null) {
				throw new NoSuchElementException("No ScrollableResults");
			}

			// Cast needed for Hibernate 6 compatibility
			Object[] row = scrollableResults.get();

			return row.length == 1 ? row[0] : row;
		}

		@Override
		public boolean hasNext() {
			return scrollableResults != null && scrollableResults.next();
		}

		@Override
		public void close() {

			if (scrollableResults != null) {
				scrollableResults.close();
			}
		}

	}

	/**
	 * {@link CloseableIterator} for EclipseLink.
	 *
	 * @author Thomas Darimont
	 * @author Oliver Gierke
	 * @param <T>
	 * @since 1.8
	 */
	@SuppressWarnings("unchecked")
	private static class EclipseLinkScrollableResultsIterator<T> implements CloseableIterator<T> {

		private final @Nullable ScrollableCursor scrollableCursor;

		/**
		 * Creates a new {@link EclipseLinkScrollableResultsIterator} for the given JPA {@link Query}.
		 *
		 * @param jpaQuery must not be {@literal null}.
		 */
		EclipseLinkScrollableResultsIterator(Query jpaQuery) {

			jpaQuery.setHint("eclipselink.cursor.scrollable", true);

			this.scrollableCursor = (ScrollableCursor) jpaQuery.getSingleResult();
		}

		@Override
		public boolean hasNext() {
			return scrollableCursor != null && scrollableCursor.hasNext();
		}

		@Override
		public T next() {

			if (scrollableCursor == null) {
				throw new NoSuchElementException("No ScrollableCursor");
			}

			return (T) scrollableCursor.next();
		}

		@Override
		public void close() {

			if (scrollableCursor != null) {
				scrollableCursor.close();
			}
		}

	}

}
