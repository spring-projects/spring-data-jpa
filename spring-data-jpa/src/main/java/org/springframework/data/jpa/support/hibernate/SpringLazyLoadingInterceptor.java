/*
 * Copyright 2023 the original author or authors.
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
package org.springframework.data.jpa.support.hibernate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.proxy.ProxyConfiguration;
import org.hibernate.proxy.pojo.BasicLazyInitializer;
import org.hibernate.type.CompositeType;

/**
 * @author Christoph Strobl
 * @since 2023/01
 */
public class SpringLazyLoadingInterceptor extends BasicLazyInitializer implements ProxyConfiguration.Interceptor {

	private final Class<?>[] interfaces;

	public SpringLazyLoadingInterceptor(String entityName, Class<?> persistentClass, Class<?>[] interfaces, Object id, Method getIdentifierMethod, Method setIdentifierMethod, CompositeType componentIdType, SharedSessionContractImplementor session, boolean overridesEquals) {
		super(entityName, persistentClass, id, getIdentifierMethod, setIdentifierMethod, componentIdType, session, overridesEquals);
		this.interfaces = interfaces;
	}

	@Override
	public Object intercept(Object proxy, Method thisMethod, Object[] args) throws Throwable {
		System.out.println("Intercept lazy stuff");
		Object result = this.invoke(thisMethod, args, proxy);
		if (result == INVOKE_IMPLEMENTATION) {
			Object target = getImplementation();
			final Object returnValue;
			try {
				if (ReflectHelper.isPublic(persistentClass, thisMethod)) {
					if (!thisMethod.getDeclaringClass().isInstance(target)) {
						throw new ClassCastException(
								target.getClass().getName() + " incompatible with " + thisMethod.getDeclaringClass().getName());
					}
					returnValue = thisMethod.invoke(target, args);
				} else {
					thisMethod.setAccessible(true);
					returnValue = thisMethod.invoke(target, args);
				}

				if (returnValue == target) {
					if (returnValue.getClass().isInstance(proxy)) {
						return proxy;
					}
				}
				return returnValue;
			} catch (InvocationTargetException ite) {
				throw ite.getTargetException();
			}
		} else {
			return result;
		}
	}

	@Override
	protected Object serializableProxy() {
		return null;
	}
}
