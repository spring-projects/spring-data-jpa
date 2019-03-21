/*
 * Copyright 2018-2019 the original author or authors.
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
package org.springframework.data.jpa.util;

import java.util.Optional;

import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.util.ProxyUtils.ProxyDetector;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * {@link ProxyDetector} to explicitly check for Hibernate's {@link HibernateProxy}.
 * 
 * @author Oliver Gierke
 */
class HibernateProxyDetector implements ProxyDetector {

	private static final Optional<Class<?>> HIBERNATE_PROXY = Optional.ofNullable(loadHibernateProxyType());

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.util.ProxyUtils.ProxyDetector#getUserType(java.lang.Class)
	 */
	@Override
	public Class<?> getUserType(Class<?> type) {

		return HIBERNATE_PROXY //
				.map(it -> it.isAssignableFrom(type) ? type.getSuperclass() : type) //
				.filter(it -> !Object.class.equals(it)) //
				.orElse(type);
	}

	@Nullable
	private static Class<?> loadHibernateProxyType() {

		try {
			return ClassUtils.forName("org.hibernate.proxy.HibernateProxy", HibernateProxyDetector.class.getClassLoader());
		} catch (ClassNotFoundException o_O) {
			return null;
		}
	}
}
