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

import org.hibernate.bytecode.spi.BasicProxyFactory;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.proxy.ProxyFactory;

/**
 * @author Christoph Strobl
 * @since 2023/01
 */
public class SpringProxyFactoryFactory implements ProxyFactoryFactory {

	public SpringProxyFactoryFactory() {
	}

	@Override
	public ProxyFactory buildProxyFactory(SessionFactoryImplementor sessionFactory) {
		return new SpringHibernateProxyFactory();
	}

	@Override
	public BasicProxyFactory buildBasicProxyFactory(Class superClassOrInterface) {
		return new SpringHibernateProxyFactory(superClassOrInterface);
	}
}
