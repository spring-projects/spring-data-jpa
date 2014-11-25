/*
 * Copyright 2008-2014 the original author or authors.
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
package org.springframework.data.jpa.repository.support;

import static org.springframework.data.jpa.repository.utils.JpaClassUtils.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.openjpa.persistence.OpenJPAQuery;
import org.eclipse.persistence.jpa.JpaQuery;
import org.hibernate.ejb.HibernateQuery;
import org.springframework.data.jpa.repository.query.QueryExtractor;
import org.springframework.util.Assert;

/**
 * Enumeration representing persistence providers to be used.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public enum PersistenceProvider implements QueryExtractor {

	/**
	 * Hibernate persistence provider.
	 * <p>
	 * Since Hibernate 4.3 the location of the HibernateEntityManager moved to the org.hibernate.jpa package. In order to
	 * support both locations we interpret both classnames as a Hibernate {@code PersistenceProvider}.
	 * 
	 * @see DATAJPA-444
	 */
	HIBERNATE(Constants.HIBERNATE43_ENTITY_MANAGER_INTERFACE, Constants.HIBERNATE_ENTITY_MANAGER_INTERFACE) {

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
		protected String getCountQueryPlaceholder() {

			return "*";
		}

		/**
		 * Return null as surrogate for an empty collection as Hibernate cannot deal with empty collections in query
		 * parameters.
		 */
		@Override
		public <T> Collection<T> potentiallyConvertEmptyCollection(Collection<T> collection) {
			return collection == null || collection.isEmpty() ? null : collection;
		}
	},

	/**
	 * EclipseLink persistence provider.
	 */
	ECLIPSELINK(Constants.ECLIPSELINK_ENTITY_MANAGER_INTERFACE) {

		public String extractQueryString(Query query) {

			return ((JpaQuery<?>) query).getDatabaseQuery().getJPQLString();
		}

		/**
		 * Return null as surrogate for an empty collection as eclipse-link cannot deal with empty collections in query
		 * parameters.
		 */
		@Override
		public <T> Collection<T> potentiallyConvertEmptyCollection(Collection<T> collection) {
			return collection == null || collection.isEmpty() ? null : collection;
		}
	},

	/**
	 * OpenJpa persistence provider.
	 */
	OPEN_JPA(Constants.OPENJPA_ENTITY_MANAGER_INTERFACE) {

		public String extractQueryString(Query query) {

			return ((OpenJPAQuery<?>) query).getQueryString();
		}
	},

	/**
	 * Unknown special provider. Use standard JPA.
	 */
	GENERIC_JPA(Constants.GENERIC_JPA_ENTITY_MANAGER_INTERFACE) {

		public String extractQueryString(Query query) {

			return null;
		}

		@Override
		public boolean canExtractQuery() {

			return false;
		}
	};

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
	}

	private List<String> entityManagerClassNames;

	/**
	 * Creates a new {@link PersistenceProvider}.
	 * 
	 * @param entityManagerClassNames the names of the provider specific {@link EntityManager} implementations. Must not
	 *          be {@literal null} or empty.
	 */
	private PersistenceProvider(String... entityManagerClassNames) {

		Assert.notEmpty(entityManagerClassNames, "EntityManagerClassNames must not be empty!");

		this.entityManagerClassNames = Arrays.asList(entityManagerClassNames);
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
	protected String getCountQueryPlaceholder() {

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
}
