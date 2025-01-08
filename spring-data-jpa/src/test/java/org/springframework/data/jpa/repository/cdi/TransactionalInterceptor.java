/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.jpa.repository.cdi;

import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

@Transactional
@Interceptor
class TransactionalInterceptor {

	@Inject
	@Any
	private EntityManager entityManager;

	@AroundInvoke
	public Object runInTransaction(InvocationContext ctx) throws Exception {
		EntityTransaction entityTransaction = this.entityManager.getTransaction();
		boolean isNew = !entityTransaction.isActive();
		try {
			if (isNew) {
				entityTransaction.begin();
			}
			Object result = ctx.proceed();
			if (isNew) {
				entityTransaction.commit();
			}
			return result;
		} catch (RuntimeException r) {
			if (isNew) {
				entityTransaction.rollback();
			}
			throw r;
		}
	}
}
