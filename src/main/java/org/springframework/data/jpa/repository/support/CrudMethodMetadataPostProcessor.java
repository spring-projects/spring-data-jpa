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
package org.springframework.data.jpa.repository.support;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.persistence.LockModeType;
import javax.persistence.QueryHint;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.beans.factory.BeanClassLoaderAware;
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

/**
 * {@link RepositoryProxyPostProcessor} that sets up interceptors to read metadata information from the invoked method.
 * This is necessary to allow redeclaration of CRUD methods in repository interfaces and configure locking information
 * or query hints on them.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @author Mark Paluch
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
		factory.addAdvice(CrudMethodMetadataPopulatingMethodInterceptor.INSTANCE);
	}

	/**
	 * Returns a {@link CrudMethodMetadata} proxy that will lookup the actual target object by obtaining a thread bound
	 * instance from the {@link TransactionSynchronizationManager} later.
	 */
	public CrudMethodMetadata getCrudMethodMetadata() {

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
	enum CrudMethodMetadataPopulatingMethodInterceptor implements MethodInterceptor {

		INSTANCE;

		private final ConcurrentMap<Method, CrudMethodMetadata> metadataCache = new ConcurrentHashMap<Method, CrudMethodMetadata>();

		/*
		 * (non-Javadoc)
		 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
		 */
		public Object invoke(MethodInvocation invocation) throws Throwable {

			Method method = invocation.getMethod();
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
		private final Map<String, Object> queryHints;
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
			this.queryHints = findQueryHints(method);
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

		private static Map<String, Object> findQueryHints(Method method) {

			Map<String, Object> queryHints = new HashMap<String, Object>();
			QueryHints queryHintsAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, QueryHints.class);

			if (queryHintsAnnotation != null) {

				for (QueryHint hint : queryHintsAnnotation.value()) {
					queryHints.put(hint.name(), hint.value());
				}
			}

			QueryHint queryHintAnnotation = AnnotationUtils.findAnnotation(method, QueryHint.class);

			if (queryHintAnnotation != null) {
				queryHints.put(queryHintAnnotation.name(), queryHintAnnotation.value());
			}

			return Collections.unmodifiableMap(queryHints);
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
		public Map<String, Object> getQueryHints() {
			return queryHints;
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
		public Object getTarget() throws Exception {

			MethodInvocation invocation = ExposeInvocationInterceptor.currentInvocation();
			return TransactionSynchronizationManager.getResource(invocation.getMethod());
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.aop.TargetSource#releaseTarget(java.lang.Object)
		 */
		@Override
		public void releaseTarget(Object target) throws Exception {}
	}
}
