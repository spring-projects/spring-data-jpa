/*
 * Copyright 2011-2018 the original author or authors.
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
package org.springframework.data.jpa.repository.cdi;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * @author Oliver Gierke
 * @author Mark Paluch
 */
class QualifiedEntityManagerProducer {

	@Produces
	@PersonDB
	public EntityManager createPersonDBEntityManager(EntityManagerFactory entityManagerFactory) {
		return entityManagerFactory.createEntityManager();
	}

	public void closePersonDB(@Disposes @PersonDB EntityManager entityManager) {
		entityManager.close();
	}

	@Produces
	@UserDB
	public EntityManager createUserDBEntityManager(EntityManagerFactory entityManagerFactory) {
		return entityManagerFactory.createEntityManager();
	}

	public void closeUserDB(@Disposes @UserDB EntityManager entityManager) {
		entityManager.close();
	}
}
