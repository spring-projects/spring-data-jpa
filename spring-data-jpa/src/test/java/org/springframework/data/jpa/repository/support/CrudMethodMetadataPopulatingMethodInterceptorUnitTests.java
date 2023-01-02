/*
 * Copyright 2012-2023 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;

import jakarta.persistence.LockModeType;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.support.CrudMethodMetadataPostProcessor.CrudMethodMetadataPopulatingMethodInterceptor;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Unit tests for {@link CrudMethodMetadataPopulatingMethodInterceptor}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Jens Schauder
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CrudMethodMetadataPopulatingMethodInterceptorUnitTests {

	@Mock MethodInvocation invocation;
	@Mock RepositoryInformation information;

	private static Sample expectLockModeType(CrudMethodMetadata metadata, RepositoryInformation information,
			LockModeType type) {

		ProxyFactory factory = new ProxyFactory(new Object());
		factory.addInterface(Sample.class);
		factory.addAdvice(new CrudMethodMetadataPopulatingMethodInterceptor(information));
		factory.addAdvice(new MethodInterceptor() {

			@Override
			public Object invoke(MethodInvocation invocation) {
				assertThat(metadata.getLockModeType()).isEqualTo(type);
				return null;
			}
		});

		return (Sample) factory.getProxy();
	}

	@Test // DATAJPA-268
	@SuppressWarnings("unchecked")
	void cleansUpBoundResources() throws Throwable {

		Method method = prepareMethodInvocation("someMethod");
		when(information.isQueryMethod(method)).thenReturn(false);
		when(information.getRepositoryInterface()).thenReturn((Class) Sample.class);

		CrudMethodMetadataPopulatingMethodInterceptor interceptor = new CrudMethodMetadataPopulatingMethodInterceptor(
				information);
		interceptor.invoke(invocation);

		assertThat(TransactionSynchronizationManager.getResource(method)).isNull();
	}

	@Test // DATAJPA-839, DATAJPA-1368
	@SuppressWarnings("unchecked")
	void looksUpCrudMethodMetadataForEveryInvocation() {

		CrudMethodMetadata metadata = new CrudMethodMetadataPostProcessor().getCrudMethodMetadata();
		when(information.isQueryMethod(any())).thenReturn(false);
		when(information.getRepositoryInterface()).thenReturn((Class) Sample.class);

		expectLockModeType(metadata, information, LockModeType.OPTIMISTIC).someMethod();
		expectLockModeType(metadata, information, LockModeType.PESSIMISTIC_READ).someOtherMethod();
	}

	private Method prepareMethodInvocation(String name) throws Throwable {

		Method method = Sample.class.getMethod(name);
		when(invocation.getMethod()).thenReturn(method);

		return method;
	}

	interface Sample {

		@Lock(LockModeType.OPTIMISTIC)
		void someMethod();

		@Lock(LockModeType.PESSIMISTIC_READ)
		void someOtherMethod();
	}
}
