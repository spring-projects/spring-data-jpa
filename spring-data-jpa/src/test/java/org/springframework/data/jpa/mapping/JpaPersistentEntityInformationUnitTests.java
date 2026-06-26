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

import jakarta.persistence.metamodel.Metamodel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link JpaPersistentEntityInformation}.
 *
 * @author Gieun Nam
 */
class JpaPersistentEntityInformationUnitTests {
    private JpaMetamodelMappingContext mappingContext;
    @BeforeEach
    void setUp() {
        Metamodel metamodel = mock(Metamodel.class);
        this.mappingContext = new JpaMetamodelMappingContext(Collections.singleton(metamodel));
    }

    @Test // GH-4037
    void validationScenario() {
        JpaPersistentEntity<?> metaData = mappingContext.getRequiredPersistentEntity(User.class);

        JpaPersistentEntityInformation<User, Long> entityInfo =
                new JpaPersistentEntityInformation<>((JpaPersistentEntity<User>) metaData);

        User user = new User();
        user.id = 77L;

        assertThat(entityInfo.getId(user)).isEqualTo(77L);

        assertThat(entityInfo.getIdType()).isEqualTo(Long.class);

        assertThat(entityInfo.isNew(user)).isFalse();

        assertThat(entityInfo.hasCompositeId()).isFalse();

        assertThat(entityInfo.getIdAttribute()).isEqualTo(Long.class);
    }

    @Test
    void identifiesCompositeIdCorrectly() {
        JpaPersistentEntity<?> metaData = mappingContext.getRequiredPersistentEntity(Order.class);
        JpaPersistentEntityInformation<Order, OrderId> entityInfo =
                new JpaPersistentEntityInformation<>((JpaPersistentEntity<Order>) metaData);

        assertThat(entityInfo.hasCompositeId()).isTrue();

        assertThat(entityInfo.getIdAttribute()).isEqualTo(OrderId.class);
    }

    // Test Entities
    static class User {
        @jakarta.persistence.Id Long id;
    }

    static class Order {
        @jakarta.persistence.EmbeddedId OrderId id;
    }

    @jakarta.persistence.Embeddable
    static class OrderId implements java.io.Serializable {
        Long orderId;
        Long userId;
    }
}