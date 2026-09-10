/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.data.jpa.mapping;


import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.util.Assert;

/**
 * Implementation of {@link JpaPersistentEntityInformation}.
 *
 * @author Gieun Nam
 * @since 4.1
 */
public class JpaPersistentEntityInformation<T, ID> {
    private final JpaPersistentEntity<T> entityMetadata;

    public JpaPersistentEntityInformation(JpaPersistentEntity<T> entityMetadata) {
        Assert.notNull(entityMetadata, "JpaPersistentEntity must not be null");
        this.entityMetadata = entityMetadata;
    }

    public ID getId(T entity) {
        Assert.notNull(entity, "Entity must not be null");

        IdentifierAccessor accessor = entityMetadata.getIdentifierAccessor(entity);
        return (ID) accessor.getIdentifier();
    }

    public Class<ID> getIdType() {
        return (Class<ID>) entityMetadata.getRequiredIdProperty().getType();
    }

    public boolean isNew(T entity) {
        return entityMetadata.isNew(entity);
    }

    public boolean hasCompositeId() {
        JpaPersistentProperty idProperty = entityMetadata.getRequiredIdProperty();
        return idProperty.isEmbeddable();
    }

    public Class<?> getIdAttribute() {
        return entityMetadata.getRequiredIdProperty().getActualType();
    }
}
