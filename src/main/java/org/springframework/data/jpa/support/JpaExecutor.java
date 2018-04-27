/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.data.jpa.support;

import reactor.core.publisher.Mono;

import java.util.function.Function;

import javax.persistence.EntityManager;

import org.springframework.data.repository.Repository;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.Assert;

/**
 * Executor to use JPA's {@link EntityManager} and {@link org.springframework.data.jpa.repository.JpaRepository JPA
 * repositories} in a reactive application. JPA and JDBC are inherently blocking and assuming an imperative programming
 * model. This executor offloads its workload to an underlying {@link reactor.core.scheduler.Scheduler} to prevent
 * blocking of the subscribing {@link Thread}.
 * <p/>
 * <strong>This {@link JpaExecutor} moves blocking behavior off the subscribing thread to a dedicated
 * {@link reactor.core.scheduler.Scheduler}. It does not solve the blocking aspect itself, it puts it to a place where
 * it hurts less.</strong>
 * <p/>
 * This interface declares methods to interact with JPA within read-only and regular transactions. Transaction
 * attributes are customizable through {@link #transactional()}. Methods of {@link TransactionalJpaExecutor} create
 * immutable {@link TransactionalJpaExecutor} instances each.
 *
 * @author Mark Paluch
 * @since 2.1
 * @see BlockingResourceAdapter
 * @see BlockingRepositoryAdapter
 */
public interface JpaExecutor extends BlockingResourceAdapter<EntityManager>, BlockingRepositoryAdapter {

	/**
	 * Obtain a {@link TransactionalJpaExecutor} object to customize transaction attributes in preparation of
	 * transactional execution.
	 *
	 * @return the {@link TransactionalJpaExecutor}.
	 */
	TransactionalJpaExecutor transactional();

	/**
	 * Transaction JPA executor allowing customization of transaction attributes and executing callbacks for JPA
	 * interaction within a transaction.
	 * <p/>
	 * {@code doInTransaction} execute the callback on a dedicated {@link reactor.core.scheduler.Scheduler} within a
	 * managed transaction. Transactions are started before callback invocation and committed upon successful execution
	 * (i.e. no {@link Exception} is thrown). By default, all {@link Exception}s cause a
	 * {@link org.springframework.transaction.PlatformTransactionManager#rollback(TransactionStatus) rollback}. This
	 * behavior can be customized through {@link #withNoRollbackOn(Class[])} and {@link #withRollbackOn(Class[])} methods.
	 *
	 * @author Mark Paluch
	 * @since 2.1
	 */
	interface TransactionalJpaExecutor {

		/**
		 * Execute a callback {@link Function} that obtains access to a managed {@link EntityManager} enclosed in a
		 * transaction.
		 *
		 * @param function must not be {@literal null}.
		 * @return
		 */
		<T> Mono<T> doInTransaction(Function<EntityManager, T> function);

		/**
		 * Execute a callback {@link Function} that obtains access to a provided {@link Repository} enclosed in a
		 * transaction.
		 *
		 * @param repository must not be {@literal null}.
		 * @param function must not be {@literal null}.
		 * @return
		 */
		<R extends Repository<?, ?>, T> Mono<T> doInTransaction(R repository, Function<R, T> function);

		/**
		 * Configure a read-only transaction.
		 *
		 * @return a new {@link TransactionalJpaExecutor} containing all previous settings and the configured read-only
		 *         attribute.
		 */
		TransactionalJpaExecutor readOnly();

		/**
		 * Configure {@link Throwable exception types} that force a rollback.
		 *
		 * @param exceptionClass exception class to enforce a rollback.
		 * @return a new {@link TransactionalJpaExecutor} containing all previous settings and the configured exception
		 *         types.
		 */
		@SuppressWarnings("unchecked")
		default TransactionalJpaExecutor withRollbackOn(Class<? extends Throwable> exceptionClass) {

			Assert.notNull(exceptionClass, "Exception class must not be null!");

			return withRollbackOn(new Class[] { exceptionClass });
		}

		/**
		 * Configure {@link Throwable exception types} that force a rollback.
		 *
		 * @param classes exception classes to enforce a rollback.
		 * @return a new {@link TransactionalJpaExecutor} containing all previous settings and the configured exception
		 *         types.
		 */
		TransactionalJpaExecutor withRollbackOn(Class<? extends Throwable>... classes);

		/**
		 * Configure {@link Throwable exception types} that do not lead to a rollback.
		 *
		 * @param exceptionClass exception classe to exclude from rolllback.
		 * @return a new {@link TransactionalJpaExecutor} containing all previous settings and the configured exception
		 *         types.
		 */
		@SuppressWarnings("unchecked")
		default TransactionalJpaExecutor withNoRollbackOn(Class<? extends Throwable> exceptionClass) {

			Assert.notNull(exceptionClass, "Exception class must not be null!");

			return withNoRollbackOn(new Class[] { exceptionClass });
		}

		/**
		 * Configure {@link Throwable exception types} that do not lead to a rollback.
		 *
		 * @param classes exception classes to exclude from rolllback.
		 * @return a new {@link TransactionalJpaExecutor} containing all previous settings and the configured exception
		 *         types.
		 */
		TransactionalJpaExecutor withNoRollbackOn(Class<? extends Throwable>... classes);

		/**
		 * Configure a {@link TransactionDefinition}.
		 *
		 * @return a new {@link TransactionalJpaExecutor} containing all previous settings and the configured
		 *         {@link TransactionDefinition}.
		 */
		TransactionalJpaExecutor withTransaction(TransactionDefinition definition);
	}
}
