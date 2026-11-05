/*
 * Copyright 2011-present the original author or authors.
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

import jakarta.persistence.metamodel.SingularAttribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.data.jpa.mapping.JpaPersistentProperty;
import org.springframework.data.jpa.repository.query.JpaEntityMetadata;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.repository.core.EntityInformation;

/**
 * Extension of {@link EntityInformation} to capture additional JPA specific information about entities.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 */
public interface JpaEntityInformation<T, ID> extends EntityInformation<T, ID>, JpaEntityMetadata<T> {

	/**
	 * Returns the id attribute of the entity.
	 */
	@Nullable
	SingularAttribute<? super T, ?> getIdAttribute();

	/**
	 * Returns the required identifier type.
	 *
	 * @return the identifier type.
	 * @throws IllegalArgumentException in case no id type could be obtained.
	 * @since 2.0
	 */
	default SingularAttribute<? super T, ?> getRequiredIdAttribute() throws IllegalArgumentException {

		SingularAttribute<? super T, ?> id = getIdAttribute();

		if (id != null) {
			return id;
		}

		throw new IllegalArgumentException(
				String.format("Could not obtain required identifier attribute for type %s", getEntityName()));
	}

	/**
	 * Returns {@literal true} if the entity has a composite id.
	 */
	boolean hasCompositeId();

	/**
	 * Returns the attribute names of the id attributes. If the entity has a composite id, then all id attribute names are
	 * returned. If the entity has a single id attribute, then this single attribute name is returned.
	 */
	@Deprecated
	default Collection<String> getIdAttributeNames() {

		List<String> names = new ArrayList<>();
		for (String path : getIdAttributePaths()) {

			int dot = path.indexOf('.');
			if (dot == -1) {
				names.add(path);
			} else {
				names.add(path.substring(0, dot));
			}
		}

		return names;
	}

	/**
	 * Returns the attribute paths of the id attributes. If the entity has a composite id, then all id attribute paths are
	 * returned. If the entity has a single id attribute, then this single attribute path is returned.
	 *
	 * @since 4.0.5
	 */
	Collection<String> getIdAttributePaths();

	/**
	 * Extracts the value for the given id attribute from a composite id
	 *
	 * @param id the composite id from which to extract the attribute.
	 * @param idAttribute the attribute name to extract.
	 */
	@Nullable
	Object getCompositeIdAttributeValue(Object id, String idAttribute);

	/**
	 * Return properties of the composite identifier type, or an empty list if component metadata is unavailable.
	 *
	 * @since 4.1
	 */
	List<JpaPersistentProperty> getIdentifierProperties();

	/**
	 * Obtain access to a supplied identifier using {@link #getIdentifierProperties()}.
	 *
	 * @param id the identifier object, not the entity containing it.
	 * @throws UnsupportedOperationException if component metadata is unavailable.
	 * @since 4.1
	 */
	PersistentPropertyAccessor<?> getIdentifierPropertyAccessor(Object id);

	/**
	 * Extract a keyset for {@code propertyPaths} and the primary key (including composite key components if applicable).
	 *
	 * @param propertyPaths the property paths that make up the keyset in combination with the composite key components.
	 * @param entity the entity to extract values from
	 * @return a map mapping String representations of the paths to values from the entity.
	 * @since 3.1
	 */
	Map<String, Object> getKeyset(Iterable<String> propertyPaths, T entity);

}
