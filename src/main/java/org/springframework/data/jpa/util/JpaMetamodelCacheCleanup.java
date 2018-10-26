/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.data.jpa.util;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;

/**
 * Simple component to be reigstered as Spring bean to clear the {@link JpaMetamodel} cache to avoid a memory leak in
 * applications bootstrapping multiple {@link ApplicationContext}s.
 * 
 * @author Oliver Gierke
 * @author Sylvère Richard
 * @see org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension#registerBeansForRoot(org.springframework.beans.factory.support.BeanDefinitionRegistry,
 *      org.springframework.data.repository.config.RepositoryConfigurationSource)
 */
class JpaMetamodelCacheCleanup implements DisposableBean {

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	@Override
	public void destroy() throws Exception {
		JpaMetamodel.clear();
	}
}
