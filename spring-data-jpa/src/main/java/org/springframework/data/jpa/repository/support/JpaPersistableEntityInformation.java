/*
 * Copyright 2011-2025 the original author or authors.
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

import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.metamodel.Metamodel;

import org.springframework.data.domain.Persistable;
import org.springframework.lang.Nullable;

/**
 * Extension of {@link JpaMetamodelEntityInformation} that consideres methods of {@link Persistable} to lookup the id.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 */
public class JpaPersistableEntityInformation<T extends Persistable<ID>, ID>
		extends JpaMetamodelEntityInformation<T, ID> {

	/**
	 * Creates a new {@link JpaPersistableEntityInformation} for the given domain class and {@link Metamodel}.
	 * 
	 * @param domainClass must not be {@literal null}.
	 * @param metamodel must not be {@literal null}.
	 * @param persistenceUnitUtil must not be {@literal null}.
	 */
	public JpaPersistableEntityInformation(Class<T> domainClass, Metamodel metamodel,
			PersistenceUnitUtil persistenceUnitUtil) {
		super(domainClass, metamodel, persistenceUnitUtil);
	}

	@Override
	public boolean isNew(T entity) {
		return entity.isNew();
	}

	@Nullable
	@Override
	public ID getId(T entity) {
		return entity.getId();
	}
}
