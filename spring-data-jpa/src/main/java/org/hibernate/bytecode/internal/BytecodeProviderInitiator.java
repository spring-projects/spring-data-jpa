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
package org.hibernate.bytecode.internal;

import javax.swing.*;
import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.cfg.Environment;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.springframework.data.jpa.support.hibernate.SpringByteCodeProvider;

/**
 * @author Christoph Strobl
 * @since 2023/01
 */
public class BytecodeProviderInitiator implements StandardServiceInitiator<BytecodeProvider> {

	/**
	 * Singleton access
	 */
	public static final StandardServiceInitiator<BytecodeProvider> INSTANCE = new BytecodeProviderInitiator();

	@Override
	public BytecodeProvider initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		// TODO in 6 this will no longer use Environment, which is configured via global environment variables,
		// but move to a component which can be reconfigured differently in each registry.
		System.out.println("bytecode provider initiator");
		return new SpringByteCodeProvider();
	}

	@Override
	public Class<BytecodeProvider> getServiceInitiated() {
		return BytecodeProvider.class;
	}
}
