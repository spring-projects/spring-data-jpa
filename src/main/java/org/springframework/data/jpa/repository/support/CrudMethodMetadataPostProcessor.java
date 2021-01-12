/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.jpa.repository.support;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;

import javax.persistence.LockModeType;
import javax.persistence.QueryHint;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryProxyPostProcessor;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link RepositoryProxyPostProcessor} that sets up interceptors to read metadata information from the invoked method.
 * This is necessary to allow redeclaration of CRUD methods in repository interfaces and configure locking information
 * or query hints on them.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Jens Schauder
 */
class CrudMethodMetadataPostProcessor implements RepositoryProxyPostProcessor, BeanClassLoaderAware {

	private @Nullable ClassLoader classLoader = ClassUtils.getDefaultClassLoader();

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanClassLoaderAware#setBeanClassLoader(java.lang.ClassLoader)
	 */
	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryProxyPostProcessor#postProcess(org.springframework.aop.framework.ProxyFactory, org.springframework.data.repository.core.RepositoryInformation)
	 */
	@Override
	public void postProcess(ProxyFactory factory, RepositoryInformation repositoryInformation) {
		factory.addAdvice(new CrudMethodMetadataPopulatingMethodInterceptor(repositoryInformation));
	}

	/**
	 * Returns a {@link CrudMethodMetadata} proxy that will lookup the actual target object by obtaining a thread bound
	 * instance from the {@link TransactionSynchronizationManager} later.
	 */
	CrudMethodMetadata getCrudMethodMetadata() {

		ProxyFactory factory = new ProxyFactory();

		factory.addInterface(CrudMethodMetadata.class);
		factory.setTargetSource(new ThreadBoundTargetSource());

		return (CrudMethodMetadata) factory.getProxy(this.classLoader);
	}

	/**
	 * {@link MethodInterceptor} to build and cache {@link DefaultCrudMethodMetadata} instances for the invoked methods.
	 * Will bind the found information to a {@link TransactionSynchronizationManager} for later lookup.
	 *
	 * @see DefaultCrudMethodMetadata
	 * @author Oliver Gierke
	 * @author Thomas Darimont
	 */
	static class CrudMethodMetadataPopulatingMethodInterceptor implements MethodInterceptor {

		private static final ThreadLocal<MethodInvocation> currentInvocation = new NamedThreadLocal<>(
				"Current AOP method invocation");

		private final ConcurrentMap<Method, CrudMethodMetadata> metadataCache = new ConcurrentHashMap<>();
		private final Set<Method> implementations = new HashSet<>();

		CrudMethodMetadataPopulatingMethodInterceptor(RepositoryInformation repositoryInformation) {

			ReflectionUtils.doWithMethods(repositoryInformation.getRepositoryInterface(), implementations::add,
					method -> !repositoryInformation.isQueryMethod(method));
		}

		/**
		 * Return the AOP Alliance {@link MethodInvocation} object associated with the current invocation.
		 *
		 * @return the invocation object associated with the current invocation.
		 * @throws IllegalStateException if there is no AOP invocation in progress, or if the
		 *           {@link CrudMethodMetadataPopulatingMethodInterceptor} was not added to this interceptor chain.
		 */
		static MethodInvocation currentInvocation() throws IllegalStateException {

			MethodInvocation mi = currentInvocation.get();

			if (mi == null)
				throw new IllegalStateException(
						"No MethodInvocation found: Check that an AOP invocation is in progress, and that the "
								+ "CrudMethodMetadataPopulatingMethodInterceptor is upfront in the interceptor chain.");
			return mi;
		}

		/*
		 * (non-Javadoc)
		 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
		 */
		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {

			Method method = invocation.getMethod();

			if (!implementations.contains(method)) {
				return invocation.proceed();
			}

			MethodInvocation oldInvocation = currentInvocation.get();
			currentInvocation.set(invocation);

			try {

				CrudMethodMetadata metadata = (CrudMethodMetadata) TransactionSynchronizationManager.getResource(method);

				if (metadata != null) {
					return invocation.proceed();
				}

				CrudMethodMetadata methodMetadata = metadataCache.get(method);

				if (methodMetadata == null) {

					methodMetadata = new DefaultCrudMethodMetadata(method);
					CrudMethodMetadata tmp = metadataCache.putIfAbsent(method, methodMetadata);

					if (tmp != null) {
						methodMetadata = tmp;
					}
				}

				TransactionSynchronizationManager.bindResource(method, methodMetadata);

				try {
					return invocation.proceed();
				} finally {
					TransactionSynchronizationManager.unbindResource(method);
				}
			} finally {
				currentInvocation.set(oldInvocation);
			}
		}
	}

	/**
	 * Default implementation of {@link CrudMethodMetadata} that will inspect the backing method for annotations.
	 *
	 * @author Oliver Gierke
	 * @author Thomas Darimont
	 */
	private static class DefaultCrudMethodMetadata implements CrudMethodMetadata {

		private final @Nullable LockModeType lockModeType;
		private final org.springframework.data.jpa.repository.support.QueryHints queryHints;
		private final org.springframework.data.jpa.repository.support.QueryHints queryHintsForCount;
		private final Optional<EntityGraph> entityGraph;
		private final Method method;

		/**
		 * Creates a new {@link DefaultCrudMethodMetadata} for the given {@link Method}.
		 *
		 * @param method must not be {@literal null}.
		 */
		DefaultCrudMethodMetadata(Method method) {

			Assert.notNull(method, "Method must not be null!");

			this.lockModeType = findLockModeType(method);
			this.queryHints = findQueryHints(method, it -> true);
			this.queryHintsForCount = findQueryHints(method, QueryHints::forCounting);
			this.entityGraph = findEntityGraph(method);
			this.method = method;
		}

		private static Optional<EntityGraph> findEntityGraph(Method method) {
			return Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(method, EntityGraph.class));
		}

		@Nullable
		private static LockModeType findLockModeType(Method method) {

			Lock annotation = AnnotatedElementUtils.findMergedAnnotation(method, Lock.class);
			return annotation == null ? null : (LockModeType) AnnotationUtils.getValue(annotation);
		}

		private static org.springframework.data.jpa.repository.support.QueryHints findQueryHints(Method method,
				Predicate<QueryHints> annotationFilter) {

			MutableQueryHints queryHints = new MutableQueryHints();

			QueryHints queryHintsAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, QueryHints.class);

			if (queryHintsAnnotation != null && annotationFilter.test(queryHintsAnnotation)) {

				for (QueryHint hint : queryHintsAnnotation.value()) {
					queryHints.add(hint.name(), hint.value());
				}
			}

			QueryHint queryHintAnnotation = AnnotationUtils.findAnnotation(method, QueryHint.class);

			if (queryHintAnnotation != null) {
				queryHints.add(queryHintAnnotation.name(), queryHintAnnotation.value());
			}

			return queryHints;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.support.CrudMethodMetadata#getLockModeType()
		 */
		@Nullable
		@Override
		public LockModeType getLockModeType() {
			return lockModeType;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.support.CrudMethodMetadata#getQueryHints()
		 */
		@Override
		public org.springframework.data.jpa.repository.support.QueryHints getQueryHints() {
			return queryHints;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.support.CrudMethodMetadata#getQueryHintsForCount()
		 */
		@Override
		public org.springframework.data.jpa.repository.support.QueryHints getQueryHintsForCount() {
			return queryHintsForCount;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.support.CrudMethodMetadata#getEntityGraph()
		 */
		@Override
		public Optional<EntityGraph> getEntityGraph() {
			return entityGraph;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.support.CrudMethodMetadata#getMethod()
		 */
		@Override
		public Method getMethod() {
			return method;
		}
	}

	private static class ThreadBoundTargetSource implements TargetSource {

		/*
		 * (non-Javadoc)
		 * @see org.springframework.aop.TargetSource#getTargetClass()
		 */
		@Override
		public Class<?> getTargetClass() {
			return CrudMethodMetadata.class;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.aop.TargetSource#isStatic()
		 */
		@Override
		public boolean isStatic() {
			return false;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.aop.TargetSource#getTarget()
		 */
		@Override
		public Object getTarget() {

			MethodInvocation invocation = CrudMethodMetadataPopulatingMethodInterceptor.currentInvocation();
			return TransactionSynchronizationManager.getResource(invocation.getMethod());
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.aop.TargetSource#releaseTarget(java.lang.Object)
		 */
		@Override
		public void releaseTarget(Object target) {}
	}
}
