/*
 * Copyright 2012 the original author or authors.
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

import javax.persistence.EntityManager;

import com.mysema.query.jpa.EclipseLinkTemplates;
import com.mysema.query.jpa.HQLTemplates;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.OpenJPATemplates;
import com.mysema.query.jpa.impl.JPAQuery;

/**
 * Utility methods for Querydsl.
 * 
 * @author Oliver Gierke
 */
class QuerydslUtils {

	/**
	 * Creates the {@link JPQLQuery} instance based on the given {@link EntityManager} and {@link PersistenceProvider}.
	 * 
	 * @return
	 */
	public static JPQLQuery createQueryInstance(EntityManager em, PersistenceProvider provider) {

		switch (provider) {
		case ECLIPSELINK:
			return new JPAQuery(em, EclipseLinkTemplates.DEFAULT);
		case HIBERNATE:
			return new JPAQuery(em, HQLTemplates.DEFAULT);
		case OPEN_JPA:
			return new JPAQuery(em, OpenJPATemplates.DEFAULT);
		case GENERIC_JPA:
		default:
			return new JPAQuery(em);
		}
	}
}
