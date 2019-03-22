/*
 * Copyright 2008-2019 the original author or authors.
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

import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.metamodel.Metamodel;

import org.eclipse.persistence.jpa.JpaQuery;
import org.eclipse.persistence.queries.ScrollableCursor;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.util.CloseableIterator;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Enumeration representing persistence providers to be used.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 * @author Jens Schauder
 */
public enum PersistenceProvider implements QueryExtractor, ProxyIdAccessor {

	/**
	 * Hibernate persistence provider.
	 * <p>
	 * Since Hibernate 4.3 the location of the HibernateEntityManager moved to the org.hibernate.jpa package. In order to
	 * support both locations we interpret both classnames as a Hibernate {@code PersistenceProvider}.
	 *
	 * @see <a href="https://jira.spring.io/browse/DATAJPA-444">DATAJPA-444</a>
	 */
	HIBERNATE(//
			Collections.singletonList(HIBERNATE_ENTITY_MANAGER_INTERFACE), //
			Collections.singletonList(HIBERNATE_JPA_METAMODEL_TYPE)) {

		public String extractQueryString(Query query) {
			return HibernateUtils.getHibernateQuery(query);
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

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.support.ProxyIdAccessor#isProxy(java.lang.Object)
		 */
		@Override
		public boolean shouldUseAccessorFor(Object entity) {
			return entity instanceof HibernateProxy;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.support.ProxyIdAccessor#getIdentifierFrom(java.lang.Object)
		 */
		@Override
		public Object getIdentifierFrom(Object entity) {
			return ((HibernateProxy) entity).getHibernateLazyInitializer().getIdentifier();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.provider.PersistenceProvider#potentiallyConvertEmptyCollection(java.util.Collection)
		 */
		@Nullable
		@Override
		public <T> Collection<T> potentiallyConvertEmptyCollection(@Nullable Collection<T> collection) {
			return collection == null || collection.isEmpty() ? null : collection;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.provider.PersistenceProvider#executeQueryWithResultStream(javax.persistence.Query)
		 */
		@Override
		public CloseableIterator<Object> executeQueryWithResultStream(Query jpaQuery) {
			return new HibernateScrollableResultsIterator(jpaQuery);
		}
	},

	/**
	 * EclipseLink persistence provider.
	 */
	ECLIPSELINK(Collections.singleton(ECLIPSELINK_ENTITY_MANAGER_INTERFACE),
			Collections.singleton(ECLIPSELINK_JPA_METAMODEL_TYPE)) {

		public String extractQueryString(Query query) {
			return ((JpaQuery<?>) query).getDatabaseQuery().getJPQLString();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.support.ProxyIdAccessor#isProxy(java.lang.Object)
		 */
		@Override
		public boolean shouldUseAccessorFor(Object entity) {
			return false;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.support.ProxyIdAccessor#getIdentifierFrom(java.lang.Object)
		 */
		@Nullable
		@Override
		public Object getIdentifierFrom(Object entity) {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.provider.PersistenceProvider#potentiallyConvertEmptyCollection(java.util.Collection)
		 */
		@Nullable
		@Override
		public <T> Collection<T> potentiallyConvertEmptyCollection(@Nullable Collection<T> collection) {
			return collection == null || collection.isEmpty() ? null : collection;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.provider.PersistenceProvider#executeQueryWithResultStream(javax.persistence.Query)
		 */
		@Override
		public CloseableIterator<Object> executeQueryWithResultStream(Query jpaQuery) {
			return new EclipseLinkScrollableResultsIterator<>(jpaQuery);
		}
	},

	/**
	 * Unknown special provider. Use standard JPA.
	 */
	GENERIC_JPA(Collections.singleton(GENERIC_JPA_ENTITY_MANAGER_INTERFACE), Collections.emptySet()) {

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.query.QueryExtractor#extractQueryString(javax.persistence.Query)
		 */
		@Nullable
		@Override
		public String extractQueryString(Query query) {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.support.PersistenceProvider#canExtractQuery()
		 */
		@Override
		public boolean canExtractQuery() {
			return false;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.support.ProxyIdAccessor#isProxy(java.lang.Object)
		 */
		@Override
		public boolean shouldUseAccessorFor(Object entity) {
			return false;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.support.ProxyIdAccessor#getIdentifierFrom(java.lang.Object)
		 */
		@Nullable
		@Override
		public Object getIdentifierFrom(Object entity) {
			return null;
		}
	};

	static ConcurrentReferenceHashMap<Class<?>, PersistenceProvider> CACHE = new ConcurrentReferenceHashMap<>();

	/**
	 * Creates a new {@link PersistenceProvider}.
	 *
	 * @param entityManagerClassNames the names of the provider specific {@link EntityManager} implementations. Must not
	 *          be {@literal null} or empty.
	 * @param metamodelClassNames must not be {@literal null}.
	 */
	PersistenceProvider(Iterable<String> entityManagerClassNames, Iterable<String> metamodelClassNames) {

		this.entityManagerClassNames = entityManagerClassNames;
		this.metamodelClassNames = metamodelClassNames;
	}

	private final Iterable<String> entityManagerClassNames;
	private final Iterable<String> metamodelClassNames;

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
	 * Determines the {@link PersistenceProvider} from the given {@link EntityManager}. If no special one can be
	 * determined {@link #GENERIC_JPA} will be returned.
	 *
	 * @param em must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public static PersistenceProvider fromEntityManager(EntityManager em) {

		Assert.notNull(em, "EntityManager must not be null!");

		Class<?> entityManagerType = em.getDelegate().getClass();
		PersistenceProvider cachedProvider = CACHE.get(entityManagerType);

		if (cachedProvider != null) {
			return cachedProvider;
		}

		for (PersistenceProvider provider : values()) {
			for (String entityManagerClassName : provider.entityManagerClassNames) {
				if (isEntityManagerOfType(em, entityManagerClassName)) {
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

		Assert.notNull(metamodel, "Metamodel must not be null!");

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

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.QueryExtractor#canExtractQuery()
	 */
	public boolean canExtractQuery() {
		return true;
	}

	/**
	 * Potentially converts an empty collection to the appropriate representation of this {@link PersistenceProvider},
	 * since some JPA providers cannot correctly handle empty collections.
	 *
	 * @see <a href="https://jira.spring.io/browse/DATAJPA-606">DATAJPA-606</a>
	 * @param collection The collection to be converted. May be {@code null}.
	 * @return a potentially converted collection. May be {@code null}.
	 */
	@Nullable
	public <T> Collection<T> potentiallyConvertEmptyCollection(@Nullable Collection<T> collection) {
		return collection;
	}

	/**
	 * Holds the PersistenceProvider specific interface names.
	 *
	 * @author Thomas Darimont
	 * @author Jens Schauder
	 */
	interface Constants {

		String GENERIC_JPA_ENTITY_MANAGER_INTERFACE = "javax.persistence.EntityManager";
		String ECLIPSELINK_ENTITY_MANAGER_INTERFACE = "org.eclipse.persistence.jpa.JpaEntityManager";
		// needed as Spring only exposes that interface via the EM proxy
		String HIBERNATE_ENTITY_MANAGER_INTERFACE = "org.hibernate.jpa.HibernateEntityManager";

		String HIBERNATE_JPA_METAMODEL_TYPE = "org.hibernate.metamodel.internal.MetamodelImpl";
		String ECLIPSELINK_JPA_METAMODEL_TYPE = "org.eclipse.persistence.internal.jpa.metamodel.MetamodelImpl";
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

		private final @Nullable ScrollableResults scrollableResults;

		/**
		 * Creates a new {@link HibernateScrollableResultsIterator} for the given {@link Query}.
		 *
		 * @param jpaQuery must not be {@literal null}.
		 */
		HibernateScrollableResultsIterator(Query jpaQuery) {

			org.hibernate.query.Query<?> query = jpaQuery.unwrap(org.hibernate.query.Query.class);
			this.scrollableResults = query.setReadOnly(TransactionSynchronizationManager.isCurrentTransactionReadOnly())//
					.scroll(ScrollMode.FORWARD_ONLY);
		}

		/*
		 * (non-Javadoc)
		 * @see java.util.Iterator#next()
		 */
		@Override
		public Object next() {

			if (scrollableResults == null) {
				throw new NoSuchElementException("No ScrollableResults");
			}

			// Cast needed for Hibernate 6 compatibility
			Object[] row = (Object[]) scrollableResults.get();

			return row.length == 1 ? row[0] : row;
		}

		/*
		 * (non-Javadoc)
		 * @see java.util.Iterator#hasNext()
		 */
		@Override
		public boolean hasNext() {
			return scrollableResults != null && scrollableResults.next();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.util.CloseableIterator#close()
		 */
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

		/*
		 * (non-Javadoc)
		 * @see java.util.Iterator#hasNext()
		 */
		@Override
		public boolean hasNext() {
			return scrollableCursor != null && scrollableCursor.hasNext();
		}

		/*
		 * (non-Javadoc)
		 * @see java.util.Iterator#next()
		 */
		@Override
		public T next() {

			if (scrollableCursor == null) {
				throw new NoSuchElementException("No ScrollableCursor");
			}

			return (T) scrollableCursor.next();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.util.CloseableIterator#close()
		 */
		@Override
		public void close() {

			if (scrollableCursor != null) {
				scrollableCursor.close();
			}
		}
	}
}
