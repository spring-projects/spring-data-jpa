/*
 * Copyright 2008-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.Subgraph;
import javax.persistence.metamodel.Metamodel;

import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.OpenJPAQuery;
import org.apache.openjpa.persistence.jdbc.FetchDirection;
import org.apache.openjpa.persistence.jdbc.JDBCFetchPlan;
import org.apache.openjpa.persistence.jdbc.LRSSizeAlgorithm;
import org.apache.openjpa.persistence.jdbc.ResultSetType;
import org.eclipse.persistence.jpa.JpaQuery;
import org.eclipse.persistence.queries.ScrollableCursor;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.ejb.HibernateQuery;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.data.jpa.repository.query.JpaEntityGraph;
import org.springframework.data.util.CloseableIterator;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Enumeration representing persistence providers to be used.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public enum PersistenceProvider implements QueryExtractor, ProxyIdAccessor {

	/**
	 * Hibernate persistence provider.
	 * <p>
	 * Since Hibernate 4.3 the location of the HibernateEntityManager moved to the org.hibernate.jpa package. In order to
	 * support both locations we interpret both classnames as a Hibernate {@code PersistenceProvider}.
	 * 
	 * @see DATAJPA-444
	 */
	HIBERNATE(//
			Arrays.asList(HIBERNATE43_ENTITY_MANAGER_INTERFACE, HIBERNATE_ENTITY_MANAGER_INTERFACE), //
			Arrays.asList(HIBERNATE43_JPA_METAMODEL_TYPE, HIBERNATE_JPA_METAMODEL_TYPE)) {

		public String extractQueryString(Query query) {
			return ((HibernateQuery) query).getHibernateQuery().getQueryString();
		}

		/**
		 * Return custom placeholder ({@code *}) as Hibernate does create invalid queries for count queries for objects with
		 * compound keys.
		 * 
		 * @see HHH-4044
		 * @see HHH-3096
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
		@Override
		public <T> Collection<T> potentiallyConvertEmptyCollection(Collection<T> collection) {
			return collection == null || collection.isEmpty() ? null : collection;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.provider.PersistenceProvider#executeQueryWithResultStream(javax.persistence.Query)
		 */
		@Override
		public CloseableIterator<Object> executeQueryWithResultStream(Query jpaQuery) {
			return new HibernateScrollableResultsIterator<Object>(jpaQuery);
		}
	},

	/**
	 * EclipseLink persistence provider.
	 */
	ECLIPSELINK(Collections.singleton(ECLIPSELINK_ENTITY_MANAGER_INTERFACE), Collections
			.singleton(ECLIPSELINK_JPA_METAMODEL_TYPE)) {

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
		@Override
		public Object getIdentifierFrom(Object entity) {
			return null;
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.jpa.provider.PersistenceProvider#potentiallyConvertEmptyCollection(java.util.Collection)
		 */
		@Override
		public <T> Collection<T> potentiallyConvertEmptyCollection(Collection<T> collection) {
			return collection == null || collection.isEmpty() ? null : collection;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.provider.PersistenceProvider#executeQueryWithResultStream(javax.persistence.Query)
		 */
		@Override
		public CloseableIterator<Object> executeQueryWithResultStream(Query jpaQuery) {
			return new EclipseLinkScrollableResultsIterator<Object>(jpaQuery);
		}
	},

	/**
	 * OpenJpa persistence provider.
	 */
	OPEN_JPA(Collections.singleton(OPENJPA_ENTITY_MANAGER_INTERFACE), Collections.singleton(OPENJPA_JPA_METAMODEL_TYPE)) {

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.query.QueryExtractor#extractQueryString(javax.persistence.Query)
		 */
		@Override
		public String extractQueryString(Query query) {
			return ((OpenJPAQuery<?>) query).getQueryString();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.support.ProxyIdAccessor#isProxy(java.lang.Object)
		 */
		@Override
		public boolean shouldUseAccessorFor(Object entity) {
			return entity instanceof PersistenceCapable;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.support.ProxyIdAccessor#getIdentifierFrom(java.lang.Object)
		 */
		@Override
		public Object getIdentifierFrom(Object entity) {
			return ((PersistenceCapable) entity).pcFetchObjectId();
		}

		@Override
		public CloseableIterator<Object> executeQueryWithResultStream(Query jpaQuery) {
			return new OpenJpaResultStreamingIterator<Object>(jpaQuery);
		}
	},

	/**
	 * Unknown special provider. Use standard JPA.
	 */
	GENERIC_JPA(Collections.singleton(GENERIC_JPA_ENTITY_MANAGER_INTERFACE), Collections.<String> emptySet()) {

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.query.QueryExtractor#extractQueryString(javax.persistence.Query)
		 */
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
		@Override
		public Object getIdentifierFrom(Object entity) {
			return null;
		}
	};

	private static final String ROOT_GRAPH_KEY = ".";

	/**
	 * Holds the PersistenceProvider specific interface names.
	 * 
	 * @author Thomas Darimont
	 */
	static interface Constants {

		String GENERIC_JPA_ENTITY_MANAGER_INTERFACE = "javax.persistence.EntityManager";
		String OPENJPA_ENTITY_MANAGER_INTERFACE = "org.apache.openjpa.persistence.OpenJPAEntityManager";
		String ECLIPSELINK_ENTITY_MANAGER_INTERFACE = "org.eclipse.persistence.jpa.JpaEntityManager";
		String HIBERNATE_ENTITY_MANAGER_INTERFACE = "org.hibernate.ejb.HibernateEntityManager";
		String HIBERNATE43_ENTITY_MANAGER_INTERFACE = "org.hibernate.jpa.HibernateEntityManager";

		String HIBERNATE_JPA_METAMODEL_TYPE = "org.hibernate.ejb.metamodel.MetamodelImpl";
		String HIBERNATE43_JPA_METAMODEL_TYPE = "org.hibernate.jpa.internal.metamodel.MetamodelImpl";
		String ECLIPSELINK_JPA_METAMODEL_TYPE = "org.eclipse.persistence.internal.jpa.metamodel.MetamodelImpl";
		String OPENJPA_JPA_METAMODEL_TYPE = "org.apache.openjpa.persistence.meta.MetamodelImpl";
	}

	private final Iterable<String> entityManagerClassNames;
	private final Iterable<String> metamodelClassNames;

	/**
	 * Creates a new {@link PersistenceProvider}.
	 * 
	 * @param entityManagerClassNames the names of the provider specific {@link EntityManager} implementations. Must not
	 *          be {@literal null} or empty.
	 */
	private PersistenceProvider(Iterable<String> entityManagerClassNames, Iterable<String> metamodelClassNames) {

		this.entityManagerClassNames = entityManagerClassNames;
		this.metamodelClassNames = metamodelClassNames;
	}

	/**
	 * Determines the {@link PersistenceProvider} from the given {@link EntityManager}. If no special one can be
	 * determined {@link #GENERIC_JPA} will be returned.
	 * 
	 * @param em must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public static PersistenceProvider fromEntityManager(EntityManager em) {

		Assert.notNull(em);

		for (PersistenceProvider provider : values()) {
			for (String entityManagerClassName : provider.entityManagerClassNames) {
				if (isEntityManagerOfType(em, entityManagerClassName)) {
					return provider;
				}
			}
		}

		return GENERIC_JPA;
	}

	public static PersistenceProvider fromMetamodel(Metamodel metamodel) {

		Assert.notNull(metamodel, "Metamodel must not be null!");

		for (PersistenceProvider provider : values()) {
			for (String metamodelClassName : provider.metamodelClassNames) {
				if (isMetamodelOfType(metamodel, metamodelClassName)) {
					return provider;
				}
			}
		}

		return GENERIC_JPA;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.jpa.repository.query.QueryExtractor#canExtractQuery
	 * ()
	 */
	public boolean canExtractQuery() {
		return true;
	}

	/**
	 * Returns the placeholder to be used for simple count queries. Default implementation returns {@code *}.
	 * 
	 * @return
	 */
	public String getCountQueryPlaceholder() {
		return "x";
	}

	/**
	 * Potentially converts an empty collection to the appropriate representation of this {@link PersistenceProvider},
	 * since some JPA providers cannot correctly handle empty collections.
	 * 
	 * @see DATAJPA-606
	 * @param collection
	 * @return
	 */
	public <T> Collection<T> potentiallyConvertEmptyCollection(Collection<T> collection) {
		return collection;
	}

	public CloseableIterator<Object> executeQueryWithResultStream(Query jpaQuery) {
		throw new UnsupportedOperationException("Streaming results is not implement for this PersistenceProvider: "
				+ name());
	}

	/**
	 * Creates a dynamic {@link EntityGraph} from the given {@link JpaEntityGraph} information.
	 * 
	 * @param em
	 * @param jpaEntityGraph
	 * @param entityType
	 * @return
	 * @since 1.9
	 */
	public EntityGraph<?> createDynamicEntityGraph(EntityManager em, JpaEntityGraph jpaEntityGraph, Class<?> entityType) {

		Assert.isTrue(jpaEntityGraph.isDynamicEntityGraph(), "The given " + jpaEntityGraph + " is not dynamic!");

		EntityGraph<?> entityGraph = em.createEntityGraph(entityType);

		configureFetchGraphFrom(jpaEntityGraph, entityGraph);

		return entityGraph;
	}

	/**
	 * Configures the given {@link EntityGraph} with the fetch graph information stored in {@link JpaEntityGraph}.
	 * 
	 * @param jpaEntityGraph
	 * @param entityGraph
	 */
	/* visible for testing */
	void configureFetchGraphFrom(JpaEntityGraph jpaEntityGraph, EntityGraph<?> entityGraph) {

		Map<String, List<String>> pathToPropertiesMap = new HashMap<String, List<String>>();
		Map<String, Subgraph<?>> pathToGraphMap = new HashMap<String, Subgraph<?>>();

		buildSubgraphsAndCollectEntityGraphPropertyPathsInto(pathToPropertiesMap, pathToGraphMap, jpaEntityGraph,
				entityGraph);

		for (Map.Entry<String, Subgraph<?>> entry : pathToGraphMap.entrySet()) {

			List<String> properties = pathToPropertiesMap.get(entry.getKey());
			if (properties.isEmpty()) {
				continue;
			}

			String[] propertyStrings = properties.toArray(new String[properties.size()]);

			if (entry.getKey().equals(ROOT_GRAPH_KEY)) {
				entityGraph.addAttributeNodes(propertyStrings);
				continue;
			}

			entry.getValue().addAttributeNodes(propertyStrings);
		}
	}

	private void buildSubgraphsAndCollectEntityGraphPropertyPathsInto(Map<String, List<String>> pathToPropertiesMap,
			Map<String, Subgraph<?>> pathToGraphMap, JpaEntityGraph jpaEntityGraph, EntityGraph<?> entityGraph) {

		String[] attributePaths = jpaEntityGraph.getAttributePaths().clone();
		for (int i = 0; i < attributePaths.length; i++) {

			String path = attributePaths[i];

			if (!path.contains(".")) {

				registerRootGraphAttribute(path, pathToPropertiesMap, pathToGraphMap);
				continue;
			}

			String[] pathComponents = StringUtils.delimitedListToStringArray(path, ".");

			String subgraphKey = createAndRegisterRequiredSubgraphs(pathToGraphMap, entityGraph, pathComponents);

			List<String> subgraphProperties = null;
			if ((subgraphProperties = pathToPropertiesMap.get(subgraphKey)) == null) {
				pathToPropertiesMap.put(subgraphKey, subgraphProperties = new ArrayList<String>());
			}
			subgraphProperties.add(pathComponents[pathComponents.length - 1]);
		}
	}

	private String createAndRegisterRequiredSubgraphs(Map<String, Subgraph<?>> pathToGraphMap,
			EntityGraph<?> entityGraph, String[] pathComponents) {

		StringBuilder parentPath = new StringBuilder();

		Subgraph<?> parent = null;
		for (int c = 0; c < pathComponents.length - 1; c++) {

			parentPath.append('.').append(pathComponents[c]);

			if (pathToGraphMap.containsKey(parentPath.toString())) {
				continue;
			}

			if (c == 0) {
				parent = entityGraph.addSubgraph(pathComponents[c]);
			} else {
				parent = parent.addSubgraph(pathComponents[c]);
			}

			pathToGraphMap.put(parentPath.toString(), parent);
		}

		return parentPath.toString();
	}

	private void registerRootGraphAttribute(String path, Map<String, List<String>> pathToPropertiesMap,
			Map<String, Subgraph<?>> pathToGraphMap) {
		pathToGraphMap.put(ROOT_GRAPH_KEY, null);

		List<String> rootProperties = null;
		if ((rootProperties = pathToPropertiesMap.get(ROOT_GRAPH_KEY)) == null) {
			pathToPropertiesMap.put(ROOT_GRAPH_KEY, rootProperties = new ArrayList<String>());
		}

		rootProperties.add(path);
	}

	/**
	 * {@link CloseableIterator} for Hibernate.
	 * 
	 * @author Thomas Darimont
	 * @author Oliver Gierke
	 * @param <T> the domain type to return≠
	 * @since 1.8
	 */
	@SuppressWarnings("unchecked")
	private static class HibernateScrollableResultsIterator<T> implements CloseableIterator<T> {

		private final ScrollableResults scrollableResults;

		private static final boolean IS_HIBERNATE3 = ClassUtils.isPresent("org.hibernate.ejb.QueryImpl",
				HibernateScrollableResultsIterator.class.getClassLoader());

		/**
		 * Creates a new {@link HibernateScrollableResultsIterator} for the given {@link Query}.
		 * 
		 * @param jpaQuery must not be {@literal null}.
		 */
		public HibernateScrollableResultsIterator(Query jpaQuery) {

			org.hibernate.Query query = IS_HIBERNATE3 ? extractHibernate3QueryFrom(jpaQuery)
					: extractHibernate4Query(jpaQuery);

			this.scrollableResults = query.setReadOnly(TransactionSynchronizationManager.isCurrentTransactionReadOnly())
					.scroll(ScrollMode.FORWARD_ONLY);
		}

		/*
		 * (non-Javadoc)
		 * @see java.util.Iterator#next()
		 */
		@Override
		public T next() {
			return (T) scrollableResults.get()[0];
		}

		/*
		 * (non-Javadoc)
		 * @see java.util.Iterator#hasNext()
		 */
		@Override
		public boolean hasNext() {
			return scrollableResults == null ? false : scrollableResults.next();
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

		private static org.hibernate.Query extractHibernate4Query(Query jpaQuery) {

			Object query = jpaQuery;

			if (jpaQuery.getClass().getName().equals("org.hibernate.jpa.criteria.compile.CriteriaQueryTypeQueryAdapter")) {
				query = new DirectFieldAccessor(jpaQuery).getPropertyValue("jpqlQuery");
			}

			return extractHibernateQueryFromQueryImpl(query);
		}

		private static org.hibernate.Query extractHibernate3QueryFrom(Query jpaQuery) {

			Object query = jpaQuery;

			if (jpaQuery.getClass().isAnonymousClass()
					&& jpaQuery.getClass().getEnclosingClass().getName()
							.equals("org.hibernate.ejb.criteria.CriteriaQueryCompiler")) {
				query = new DirectFieldAccessor(jpaQuery).getPropertyValue("val$jpaqlQuery");
			}

			return extractHibernateQueryFromQueryImpl(query);
		}

		private static org.hibernate.Query extractHibernateQueryFromQueryImpl(Object queryImpl) {
			return (org.hibernate.Query) new DirectFieldAccessor(queryImpl).getPropertyValue("query");
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

		private final ScrollableCursor scrollableCursor;

		/**
		 * Creates a new {@link EclipseLinkScrollableResultsIterator} for the given JPA {@link Query}.
		 * 
		 * @param jpaQuery must not be {@literal null}.
		 */
		public EclipseLinkScrollableResultsIterator(Query jpaQuery) {

			jpaQuery.setHint("eclipselink.cursor.scrollable", true);

			this.scrollableCursor = (ScrollableCursor) jpaQuery.getSingleResult();
		}

		/*
		 * (non-Javadoc)
		 * @see java.util.Iterator#hasNext()
		 */
		@Override
		public boolean hasNext() {
			return scrollableCursor == null ? false : scrollableCursor.hasNext();
		}

		/*
		 * (non-Javadoc)
		 * @see java.util.Iterator#next()
		 */
		@Override
		public T next() {
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

	/**
	 * {@link CloseableIterator} for OpenJpa.
	 * 
	 * @author Thomas Darimont
	 * @author Oliver Gierke
	 * @param <T> the domain type to return
	 * @since 1.8
	 */
	private static class OpenJpaResultStreamingIterator<T> implements CloseableIterator<T> {

		private final Iterator<T> iterator;

		/**
		 * Createsa new {@link OpenJpaResultStreamingIterator} for the given JPA {@link Query}.
		 * 
		 * @param jpaQuery must not be {@literal null}.
		 */
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public OpenJpaResultStreamingIterator(Query jpaQuery) {

			OpenJPAQuery kq = OpenJPAPersistence.cast(jpaQuery);

			JDBCFetchPlan fetch = (JDBCFetchPlan) kq.getFetchPlan();
			fetch.setFetchBatchSize(20);
			fetch.setResultSetType(ResultSetType.SCROLL_SENSITIVE);
			fetch.setFetchDirection(FetchDirection.FORWARD);
			fetch.setLRSSizeAlgorithm(LRSSizeAlgorithm.LAST);

			List<T> resultList = kq.getResultList();
			iterator = resultList.iterator();
		}

		/*
		 * (non-Javadoc)
		 * @see java.util.Iterator#hasNext()
		 */
		@Override
		public boolean hasNext() {
			return iterator == null ? false : iterator.hasNext();
		}

		/*
		 * (non-Javadoc)
		 * @see java.util.Iterator#next()
		 */
		@Override
		public T next() {
			return iterator.next();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.util.CloseableIterator#close()
		 */
		@Override
		public void close() {

			if (iterator != null) {
				OpenJPAPersistence.close(iterator);
			}
		}
	}
}
