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
import reactor.core.scheduler.Scheduler;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.springframework.data.repository.Repository;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.DelegatingTransactionDefinition;
import org.springframework.util.Assert;

/**
 * Executor to use JPA's {@link EntityManager} and {@link org.springframework.data.jpa.repository.JpaRepository JPA
 * repositories} in a reactive application. JPA and JDBC are inherently blocking and assuming an imperative programming
 * model. This executor implementationoffloads its workload to an underlying {@link reactor.core.scheduler.Scheduler} to
 * prevent blocking of the subscribing {@link Thread}.
 * <p/>
 * <strong>This {@link DefaultJpaExecutor} moves blocking behavior off the subscribing {@link Thread} to a dedicated
 * {@link reactor.core.scheduler.Scheduler}. It does not solve the blocking aspect itself, it moves work to a place
 * where it hurts less.</strong>
 *
 * @author Mark Paluch
 * @see 2.1
 * @see BlockingResourceAdapter
 * @see BlockingRepositoryAdapter
 */
public class DefaultJpaExecutor implements JpaExecutor {

	private final EntityManagerFactory entityManagerFactory;
	private final PlatformTransactionManager transactionManager;
	private final Scheduler scheduler;

	/**
	 * Create a new {@link DefaultJpaExecutor} given {@link EntityManagerFactory}, {@link PlatformTransactionManager}, and
	 * {@link Scheduler}.
	 *
	 * @param entityManagerFactory must not be {@literal null}.
	 * @param transactionManager must not be {@literal null}.
	 * @param scheduler must not be {@literal null}.
	 */
	public DefaultJpaExecutor(EntityManagerFactory entityManagerFactory, PlatformTransactionManager transactionManager,
			Scheduler scheduler) {

		Assert.notNull(entityManagerFactory, "EntityManagerFactory must not be null!");
		Assert.notNull(transactionManager, "PlatformTransactionManager must not be null!");
		Assert.notNull(scheduler, "Scheduler must not be null!");

		this.entityManagerFactory = entityManagerFactory;
		this.transactionManager = transactionManager;
		this.scheduler = scheduler;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.jpa.support.BlockingResourceAdapter#read(Function)
	 */
	@Override
	public <T> Mono<T> read(Function<EntityManager, T> function) {
		return transactional().readOnly().doInTransaction(function);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.jpa.support.BlockingRepositoryAdapter#read(R, Function)
	 */
	@Override
	public <R extends Repository<?, ?>, T> Mono<T> read(R repository, Function<R, T> function) {
		return transactional().readOnly().doInTransaction(repository, function);
	}

	@Override
	public TransactionalJpaExecutor transactional() {
		return new DefaultTransactionalJpaExecutor(entityManagerFactory, transactionManager, scheduler);
	}

	/**
	 * Default JPA executor implementation executing JPA callbacks within a transactional scope.
	 */
	static class DefaultTransactionalJpaExecutor implements TransactionalJpaExecutor {

		private final EntityManagerFactory entityManagerFactory;
		private final PlatformTransactionManager transactionManager;
		private final Scheduler scheduler;
		private final TransactionDefinition transactionDefinition;
		private final Set<Class<? extends Throwable>> rollbackOn;
		private final Set<Class<? extends Throwable>> noRollbackOn;

		DefaultTransactionalJpaExecutor(EntityManagerFactory entityManagerFactory,
				PlatformTransactionManager transactionManager, Scheduler scheduler) {

			this.entityManagerFactory = entityManagerFactory;
			this.transactionManager = transactionManager;
			this.scheduler = scheduler;
			this.transactionDefinition = new DefaultTransactionDefinition();
			this.rollbackOn = Collections.emptySet();
			this.noRollbackOn = Collections.emptySet();
		}

		private DefaultTransactionalJpaExecutor(EntityManagerFactory entityManagerFactory,
				PlatformTransactionManager transactionManager, Scheduler scheduler, TransactionDefinition transactionDefinition,
				Set<Class<? extends Throwable>> rollbackOn, Set<Class<? extends Throwable>> noRollbackOn) {

			this.entityManagerFactory = entityManagerFactory;
			this.transactionManager = transactionManager;
			this.scheduler = scheduler;
			this.transactionDefinition = transactionDefinition;
			this.rollbackOn = rollbackOn;
			this.noRollbackOn = noRollbackOn;
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.jpa.support.JpaExecutor.TransactionalJpaExecutor#doInTransaction(Function)
		 */
		@Override
		public <T> Mono<T> doInTransaction(Function<EntityManager, T> function) {
			return Mono.defer(() -> Mono.fromFuture(createFuture(() -> doInTransactionImpl(function))));
		}

		@Nullable
		private <T> T doInTransactionImpl(Function<EntityManager, T> function) {

			return execute(ts -> {

				EntityManager entityManager = entityManagerFactory.createEntityManager();
				try {
					return function.apply(entityManager);
				} finally {
					entityManager.close();
				}
			});
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.jpa.support.JpaExecutor.TransactionalJpaExecutor#doInTransaction(R, Function)
		 */
		@Override
		public <R extends Repository<?, ?>, T> Mono<T> doInTransaction(R repository, Function<R, T> function) {
			return Mono.defer(() -> Mono.fromFuture(createFuture(() -> doInTransactionImpl(repository, function))));
		}

		@Nullable
		private <R extends Repository<?, ?>, T> T doInTransactionImpl(R repository, Function<R, T> function) {
			return execute(ts -> function.apply(repository));
		}

		@Nullable
		private <T> T execute(Function<TransactionStatus, T> callable) {

			TransactionStatus status = this.transactionManager.getTransaction(transactionDefinition);
			T result = null;
			try {
				result = callable.apply(status);
			} catch (RuntimeException | Error ex) {

				// Transactional code threw application exception -> rollback
				if (!rollbackOnException(status, ex)) {
					this.transactionManager.commit(status);
				}

				throw ex;
			} catch (Throwable ex) {

				// Transactional code threw unexpected exception -> rollback

				if (!rollbackOnException(status, ex)) {
					this.transactionManager.commit(status);
				}

				throw new UndeclaredThrowableException(ex, "TransactionCallback threw undeclared checked exception");
			}
			this.transactionManager.commit(status);
			return result;
		}

		private boolean rollbackOnException(TransactionStatus status, Throwable ex) {

			if (!rollbackOn.isEmpty()) {
				if (rollbackOn.stream().noneMatch(it -> it.isInstance(ex))) {
					return false;
				}
			} else if (noRollbackOn.stream().anyMatch(it -> it.isInstance(ex))) {
				return false;
			}

			try {
				this.transactionManager.rollback(status);
			} catch (TransactionSystemException e) {
				e.initApplicationException(ex);
				throw e;
			}

			return true;
		}

		private <T> CompletableFuture<T> createFuture(Supplier<T> supplier) {

			CompletableFuture<T> future = new CompletableFuture<>();
			scheduler.schedule(() -> {

				try {
					future.complete(supplier.get());
				} catch (Exception e) {
					future.completeExceptionally(e);
				}
			});

			return future;
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.jpa.support.JpaExecutor.TransactionalJpaExecutor#withRollbackOn(java.lang.Class[])
		 */
		@Override
		@SafeVarargs
		public final TransactionalJpaExecutor withRollbackOn(Class<? extends Throwable>... classes) {

			Set<Class<? extends Throwable>> rollbackOn = new HashSet<>(this.rollbackOn.size() + classes.length);
			rollbackOn.addAll(this.rollbackOn);
			rollbackOn.addAll(Arrays.asList(classes));

			return newExecutor(transactionDefinition, rollbackOn, noRollbackOn);
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.jpa.support.JpaExecutor.TransactionalJpaExecutor#withNoRollbackOn(java.lang.Class[])
		 */
		@Override
		@SafeVarargs
		public final TransactionalJpaExecutor withNoRollbackOn(Class<? extends Throwable>... classes) {

			Set<Class<? extends Throwable>> noRollbackOn = new HashSet<>(this.noRollbackOn.size() + classes.length);
			noRollbackOn.addAll(this.rollbackOn);
			noRollbackOn.addAll(Arrays.asList(classes));

			return newExecutor(transactionDefinition, rollbackOn, noRollbackOn);

		}

		/* (non-Javadoc)
		 * @see org.springframework.data.jpa.support.JpaExecutor.TransactionalJpaExecutor#withTransaction(org.springframework.transaction.TransactionDefinition)
		 */
		@Override
		public TransactionalJpaExecutor withTransaction(TransactionDefinition definition) {
			return newExecutor(definition, rollbackOn, noRollbackOn);
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.jpa.support.JpaExecutor.TransactionalJpaExecutor#readOnly()
		 */
		@Override
		public TransactionalJpaExecutor readOnly() {

			TransactionDefinition transactionDefinition = new DelegatingTransactionDefinition(this.transactionDefinition) {

				@Override
				public boolean isReadOnly() {
					return true;
				}
			};

			return newExecutor(transactionDefinition, rollbackOn, noRollbackOn);
		}

		private DefaultTransactionalJpaExecutor newExecutor(TransactionDefinition transactionDefinition,
				Set<Class<? extends Throwable>> rollbackOn, Set<Class<? extends Throwable>> noRollbackOn) {
			return new DefaultTransactionalJpaExecutor(entityManagerFactory, transactionManager, scheduler,
					transactionDefinition, rollbackOn, noRollbackOn);
		}
	}
}
