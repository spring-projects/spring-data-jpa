/*
 * Copyright 2012-2014 the original author or authors.
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
package org.springframework.data.jpa.mapping;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.metamodel.Metamodel;

import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

/**
 * {@link MappingContext} implementation based on a Jpa {@link Metamodel}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @since 1.3
 */
public class JpaMetamodelMappingContext extends
		AbstractMappingContext<JpaPersistentEntityImpl<?>, JpaPersistentProperty> {

	private final Metamodel model;

	/**
	 * Creates a new JPA {@link Metamodel} based {@link MappingContext}.
	 * 
	 * @param model must not be {@literal null}.
	 */
	public JpaMetamodelMappingContext(Metamodel model) {

		Assert.notNull(model, "JPA Metamodel must not be null!");
		this.model = model;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#setInitialEntitySet(java.util.Set)
	 */
	@Override
	public void setInitialEntitySet(Set<? extends Class<?>> initialEntitySet) {

		Set<Class<?>> set = new HashSet<Class<?>>();

		for (Class<?> entityType : initialEntitySet) {

			// FIX for DATAJPA-525
			if (entityType == null) {
				continue;
			}

			set.add(entityType);
		}

		super.setInitialEntitySet(set);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#createPersistentEntity(org.springframework.data.util.TypeInformation)
	 */
	@Override
	protected <T> JpaPersistentEntityImpl<?> createPersistentEntity(TypeInformation<T> typeInformation) {
		return new JpaPersistentEntityImpl<T>(typeInformation, null);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#createPersistentProperty(java.lang.reflect.Field, java.beans.PropertyDescriptor, org.springframework.data.mapping.model.MutablePersistentEntity, org.springframework.data.mapping.model.SimpleTypeHolder)
	 */
	@Override
	protected JpaPersistentProperty createPersistentProperty(Field field, PropertyDescriptor descriptor,
			JpaPersistentEntityImpl<?> owner, SimpleTypeHolder simpleTypeHolder) {
		return new JpaPersistentPropertyImpl(model, field, descriptor, owner, simpleTypeHolder);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#shouldCreatePersistentEntityFor(org.springframework.data.util.TypeInformation)
	 */
	@Override
	protected boolean shouldCreatePersistentEntityFor(TypeInformation<?> type) {

		try {
			model.managedType(type.getType());
			return true;
		} catch (IllegalArgumentException o_O) {
			return false;
		}
	}
}
