/*
 * Copyright 2011-2018 the original author or authors.
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

import javax.persistence.EntityManager;
import javax.persistence.metamodel.Metamodel;

import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.repository.query.DefaultJpaEntityMetadata;
import org.springframework.data.jpa.repository.query.JpaEntityMetadata;
import org.springframework.data.repository.core.support.AbstractEntityInformation;
import org.springframework.util.Assert;

/**
 * Base class for {@link JpaEntityInformation} implementations to share common method implementations.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public abstract class JpaEntityInformationSupport<T, ID> extends AbstractEntityInformation<T, ID>
		implements JpaEntityInformation<T, ID> {

	private JpaEntityMetadata<T> metadata;

	/**
	 * Creates a new {@link JpaEntityInformationSupport} with the given domain class.
	 *
	 * @param domainClass must not be {@literal null}.
	 */
	public JpaEntityInformationSupport(Class<T> domainClass) {
		super(domainClass);
		this.metadata = new DefaultJpaEntityMetadata<T>(domainClass);
	}

	/**
	 * Creates a {@link JpaEntityInformation} for the given domain class and {@link EntityManager}.
	 *
	 * @param domainClass must not be {@literal null}.
	 * @param em must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T> JpaEntityInformation<T, ?> getEntityInformation(Class<T> domainClass, EntityManager em) {

		Assert.notNull(domainClass, "Domain class must not be null!");
		Assert.notNull(em, "EntityManager must not be null!");

		Metamodel metamodel = em.getMetamodel();

		if (Persistable.class.isAssignableFrom(domainClass)) {
			return new JpaPersistableEntityInformation(domainClass, metamodel);
		} else {
			return new JpaMetamodelEntityInformation(domainClass, metamodel);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.support.JpaEntityMetadata#getEntityName()
	 */
	public String getEntityName() {
		return metadata.getEntityName();
	}
}
