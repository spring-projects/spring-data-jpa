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
package org.springframework.data.jpa.repository.config;

import org.springframework.instrument.classloading.ShadowingClassLoader;

/**
 * Disposable {@link ClassLoader} used to inspect user-code classes within an isolated class loader without preventing
 * class transformation at a later time.
 *
 * @author Mark Paluch
 * @since 2.1
 */
class InspectionClassLoader extends ShadowingClassLoader {

	/**
	 * Create a new {@link InspectionClassLoader} instance.
	 *
	 * @param parent the parent classloader.
	 */
	InspectionClassLoader(ClassLoader parent) {

		super(parent, true);

		excludePackage("org.springframework.");
	}
}
