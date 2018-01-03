/*
 * Copyright 2012-2018 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;

import javax.persistence.LockModeType;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.support.CrudMethodMetadataPostProcessor.CrudMethodMetadataPopulatingMethodInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Unit tests for {@link CrudMethodMetadataPopulatingMethodInterceptor}.
 *
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class CrudMethodMetadataPopulatingMethodInterceptorUnitTests {

	@Mock MethodInvocation invocation;

	@Test // DATAJPA-268
	public void cleansUpBoundResources() throws Throwable {

		Method method = prepareMethodInvocation("someMethod");

		CrudMethodMetadataPopulatingMethodInterceptor interceptor = CrudMethodMetadataPopulatingMethodInterceptor.INSTANCE;
		interceptor.invoke(invocation);

		assertThat(TransactionSynchronizationManager.getResource(method), is(nullValue()));
	}

	@Test // DATAJPA-839
	public void looksUpCrudMethodMetadataForEveryInvocation() throws Throwable {

		CrudMethodMetadata metadata = new CrudMethodMetadataPostProcessor().getCrudMethodMetadata();

		expectLockModeType(metadata, LockModeType.OPTIMISTIC).someMethod();
		expectLockModeType(metadata, LockModeType.PESSIMISTIC_READ).someOtherMethod();
	}

	private Method prepareMethodInvocation(String name) throws Throwable {

		Method method = Sample.class.getMethod(name);
		ExposeInvocationInterceptor.INSTANCE.invoke(invocation);
		when(invocation.getMethod()).thenReturn(method);

		return method;
	}

	private static Sample expectLockModeType(final CrudMethodMetadata metadata, final LockModeType type) {

		ProxyFactory factory = new ProxyFactory(new Object());
		factory.addInterface(Sample.class);
		factory.addAdvice(ExposeInvocationInterceptor.INSTANCE);
		factory.addAdvice(CrudMethodMetadataPopulatingMethodInterceptor.INSTANCE);
		factory.addAdvice(new MethodInterceptor() {

			@Override
			public Object invoke(MethodInvocation invocation) {
				assertThat(metadata.getLockModeType(), is(type));
				return null;
			}
		});

		return (Sample) factory.getProxy();
	}

	interface Sample {

		@Lock(LockModeType.OPTIMISTIC)
		void someMethod();

		@Lock(LockModeType.PESSIMISTIC_READ)
		void someOtherMethod();
	}
}
