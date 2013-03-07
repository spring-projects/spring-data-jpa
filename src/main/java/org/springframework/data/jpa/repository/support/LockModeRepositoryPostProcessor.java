/*
 * Copyright 2011-2013 the original author or authors.
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

import javax.persistence.LockModeType;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.core.support.RepositoryProxyPostProcessor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * {@link RepositoryProxyPostProcessor} that sets up interceptors to read {@link LockModeType} information from the
 * invoked method. This is necessary to allow redeclaration of CRUD methods in repository interfaces and configure
 * locking information on them.
 * 
 * @author Oliver Gierke
 */
public enum LockModeRepositoryPostProcessor implements RepositoryProxyPostProcessor {

	INSTANCE;

	private static final Object NULL = new Object();

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryProxyPostProcessor#postProcess(org.springframework.aop.framework.ProxyFactory)
	 */
	public void postProcess(ProxyFactory factory) {
		factory.addAdvice(LockModePopulatingMethodIntercceptor.INSTANCE);
	}

	/**
	 * Returns the {@link LockMetadataProvider} to lookup the lock information captured by the interceptors.
	 * 
	 * @return
	 */
	public LockMetadataProvider getLockMetadataProvider() {
		return ThreadBoundLockMetadata.INSTANCE;
	}

	/**
	 * {@link MethodInterceptor} to inspect the currently invoked {@link Method} for a {@link Lock} annotation. Will bind
	 * the found information to a {@link TransactionSynchronizationManager} for later lookup.
	 * 
	 * @see ThreadBoundLockMetadata
	 * @author Oliver Gierke
	 */
	static enum LockModePopulatingMethodIntercceptor implements MethodInterceptor {

		INSTANCE;

		/* 
		 * (non-Javadoc)
		 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
		 */
		public Object invoke(MethodInvocation invocation) throws Throwable {

			Method method = invocation.getMethod();
			Object lockInfo = TransactionSynchronizationManager.getResource(method);

			if (lockInfo != null) {
				return invocation.proceed();
			}

			Lock annotation = AnnotationUtils.findAnnotation(method, Lock.class);
			LockModeType lockMode = (LockModeType) AnnotationUtils.getValue(annotation);
			TransactionSynchronizationManager.bindResource(method, lockMode == null ? NULL : lockMode);

			try {
				return invocation.proceed();
			} finally {
				TransactionSynchronizationManager.unbindResource(method);
			}
		}
	}

	/**
	 * {@link LockMetadataProvider} that looks up locking metadata from the {@link TransactionSynchronizationManager}
	 * using the current method invocation as key.
	 * 
	 * @author Oliver Gierke
	 */
	private static enum ThreadBoundLockMetadata implements LockMetadataProvider {

		INSTANCE;

		public LockModeType getLockModeType() {

			MethodInvocation invocation = ExposeInvocationInterceptor.currentInvocation();
			Object lockModeType = TransactionSynchronizationManager.getResource(invocation.getMethod());

			return lockModeType == NULL ? null : (LockModeType) lockModeType;
		}
	}
}
