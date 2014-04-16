/*
 * Copyright 2011-2014 the original author or authors.
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

import org.springframework.data.repository.core.support.RepositoryProxyPostProcessor;

/**
 * Contract to redeclare CRUD methods in repository interfaces.
 * Allows to configure locking information or query hints on the repository methods.
 * Extends {@link RepositoryProxyPostProcessor} to be able to set up interceptors
 * to read metadata information from the invoked method.
 * 
 * @author Arnaud Cogoluègnes
 */
public interface CrudMethodMetadataProvider extends RepositoryProxyPostProcessor {

	/**
	 * Returns a {@link CrudMethodMetadata} for the method that is currently invoked.
	 * The implementation is thus supposed to know about this method.
	 * @return
	 */
	public CrudMethodMetadata getLockMetadataProvider();
	
}
