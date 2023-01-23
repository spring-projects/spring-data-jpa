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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.cfg.AvailableSettings;

/**
 * @author Christoph Strobl
 * @since 2023/01
 */
public class SpringByteCodeProvider implements BytecodeProvider {

	private static final Log LOGGER = LogFactory.getLog(SpringByteCodeProvider.class);

	public SpringByteCodeProvider() {
		System.out.println("Create new SpringByteCodeProvider");
	}

	@Override
	public ProxyFactoryFactory getProxyFactoryFactory() {
		LOGGER.debug("Obtain proxy factory from SpringByteCodeProvider");
		return new SpringProxyFactoryFactory();
	}

	@Override
	public ReflectionOptimizer getReflectionOptimizer(Class clazz, String[] getterNames, String[] setterNames,
			Class[] types) {
		throw new HibernateException(
				"Using the ReflectionOptimizer is not possible when the configured BytecodeProvider is 'none'. Disable "
						+ AvailableSettings.USE_REFLECTION_OPTIMIZER + " or use a different BytecodeProvider");
	}

	@Override
	public Enhancer getEnhancer(EnhancementContext enhancementContext) {
		LOGGER.debug("request enhanceer: ");
		return new BytecodeProviderImpl().getEnhancer(enhancementContext);
		// return null;
	}
}
