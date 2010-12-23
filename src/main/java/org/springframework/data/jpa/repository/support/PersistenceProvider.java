/*
 * Copyright 2008-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.data.jpa.repository.support;

import static org.springframework.data.jpa.repository.utils.JpaClassUtils.*;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.openjpa.persistence.OpenJPAQuery;
import org.eclipse.persistence.jpa.JpaQuery;
import org.hibernate.ejb.HibernateQuery;
import org.springframework.data.jpa.repository.query.QueryExtractor;


/**
 * Enumeration representing peristence providers to be used.
 * 
 * @author Oliver Gierke
 */
enum PersistenceProvider implements QueryExtractor {

    /**
     * Hibernate persistence provider.
     */
    HIBERNATE("org.hibernate.ejb.HibernateEntityManager") {

        public String extractQueryString(Query query) {

            return ((HibernateQuery) query).getHibernateQuery()
                    .getQueryString();
        }


        /**
         * Return custom placeholder ({@code *}) as Hibernate does create
         * invalid queries for count queries for objects with compound keys.
         * 
         * @see HHH-4044
         * @see HHH-3096
         */
        @Override
        protected String getCountQueryPlaceholder() {

            return "*";
        }
    },

    /**
     * EclipseLink persistence provider.
     */
    ECLIPSELINK("org.eclipse.persistence.jpa.JpaEntityManager") {

        public String extractQueryString(Query query) {

            return ((JpaQuery<?>) query).getDatabaseQuery().getJPQLString();
        }

    },

    /**
     * OpenJpa persistence provider.
     */
    OPEN_JPA("org.apache.openjpa.persistence.OpenJPAEntityManager") {

        public String extractQueryString(Query query) {

            return ((OpenJPAQuery<?>) query).getQueryString();
        }
    },

    /**
     * Unknown special provider. Use standard JPA.
     */
    GENERIC_JPA("javax.persistence.EntityManager") {

        public String extractQueryString(Query query) {

            return null;
        }


        @Override
        public boolean canExtractQuery() {

            return false;
        }
    };

    private String entityManagerClassName;


    /**
     * Creates a new {@link PersistenceProvider}.
     * 
     * @param entityManagerClassName the name of the provider specific
     *            {@link EntityManager} implementation
     */
    private PersistenceProvider(String entityManagerClassName) {

        this.entityManagerClassName = entityManagerClassName;
    }


    /**
     * Determines the {@link PersistenceProvider} from the given
     * {@link EntityManager}. If no special one can be determined
     * {@value #GENERIC_JPA} will be returned.
     * 
     * @param em
     * @return
     */
    public static PersistenceProvider fromEntityManager(EntityManager em) {

        for (PersistenceProvider provider : values()) {
            if (isEntityManagerOfType(em, provider.entityManagerClassName)) {
                return provider;
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
     * Returns the placeholder to be used for simple count queries. Default
     * implementation returns {@code *}.
     * 
     * @return
     */
    protected String getCountQueryPlaceholder() {

        return "x";
    }
}
