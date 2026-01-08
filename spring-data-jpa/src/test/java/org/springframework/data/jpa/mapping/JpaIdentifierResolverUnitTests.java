/*
 * Copyright 2025-present the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.Tuple;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.Type;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.provider.ProxyIdAccessor;

@ExtendWith(MockitoExtension.class)
class JpaIdentifierResolverUnitTests {

    @Mock IdentifiableType<CompositeEntity> type;
    @Mock SingularAttribute<CompositeEntity, String> first;
    @Mock SingularAttribute<CompositeEntity, String> second;
    @Mock
    @SuppressWarnings("rawtypes") Type idType;
    @Mock PersistenceUnitUtil persistenceUnitUtil;

    @Test
    void usesProxyAccessorWhenApplicable() {

        ProxyIdAccessor proxyIdAccessor = mock(ProxyIdAccessor.class);
        Object entity = new Object();

        when(proxyIdAccessor.shouldUseAccessorFor(entity)).thenReturn(true);
        when(proxyIdAccessor.getIdentifierFrom(entity)).thenReturn("proxy-id");

        Object result = JpaIdentifierResolver.getProxyAwareIdentifier(entity, proxyIdAccessor, () -> "fallback");

        assertThat(result).isEqualTo("proxy-id");
        verify(proxyIdAccessor).shouldUseAccessorFor(entity);
        verify(proxyIdAccessor).getIdentifierFrom(entity);
    }

    @Test
    void fallsBackWhenProxyAccessorNotApplicable() {

        ProxyIdAccessor proxyIdAccessor = mock(ProxyIdAccessor.class);
        Object entity = new Object();

        when(proxyIdAccessor.shouldUseAccessorFor(entity)).thenReturn(false);

        Object result = JpaIdentifierResolver.getProxyAwareIdentifier(entity, proxyIdAccessor, () -> "fallback");

        assertThat(result).isEqualTo("fallback");
        verify(proxyIdAccessor).shouldUseAccessorFor(entity);
        verify(proxyIdAccessor, never()).getIdentifierFrom(entity);
    }

    @Test
    void returnsNullWhenCompositeIdIsEmpty() {

        stubCompositeIdBasic();

        JpaIdentifierResolver<CompositeEntity> resolver = new JpaIdentifierResolver<>(type,
            PersistenceProvider.GENERIC_JPA, persistenceUnitUtil);

        CompositeEntity entity = new CompositeEntity(null, null, "extra");

        assertThat(resolver.getId(entity)).isNull();
        verify(persistenceUnitUtil, never()).getIdentifier(entity);
    }

    @Test
    void returnsIdentifierWhenCompositeIdIsPartiallySet() {

        stubCompositeIdBasic();

        JpaIdentifierResolver<CompositeEntity> resolver = new JpaIdentifierResolver<>(type,
            PersistenceProvider.GENERIC_JPA, persistenceUnitUtil);

        CompositeEntity entity = new CompositeEntity("first-value", null, "extra");
        CompositeId id = new CompositeId("first-value", "second-value");

        when(persistenceUnitUtil.getIdentifier(entity)).thenReturn(id);

        assertThat(resolver.getId(entity)).isEqualTo(id);
        verify(persistenceUnitUtil).getIdentifier(entity);
    }

    @Test
    void returnsIdentifierWhenCompositeIdIsFullySet() {

        stubCompositeIdBasic();

        JpaIdentifierResolver<CompositeEntity> resolver = new JpaIdentifierResolver<>(type,
            PersistenceProvider.GENERIC_JPA, persistenceUnitUtil);

        CompositeEntity entity = new CompositeEntity("first-value", "second-value", "extra");
        CompositeId id = new CompositeId("first-value", "second-value");

        when(persistenceUnitUtil.getIdentifier(entity)).thenReturn(id);

        assertThat(resolver.getId(entity)).isEqualTo(id);
        verify(persistenceUnitUtil).getIdentifier(entity);
    }

    @Test
    void exposesCompositeIdentifierMetadata() {

        stubCompositeIdWithMetadata();

        JpaIdentifierResolver<CompositeEntity> resolver = new JpaIdentifierResolver<>(type,
            PersistenceProvider.GENERIC_JPA, persistenceUnitUtil);

        assertThat(resolver.hasCompositeId()).isTrue();
        assertThat(resolver.getIdAttributeNames()).containsExactly("first", "second");
        assertThat(resolver.getIdType()).isEqualTo(CompositeId.class);
    }

    @Test
    void returnsCompositeIdAttributeValue() {

        stubCompositeIdMinimal();

        JpaIdentifierResolver<CompositeEntity> resolver = new JpaIdentifierResolver<>(type,
            PersistenceProvider.GENERIC_JPA, persistenceUnitUtil);

        CompositeId id = new CompositeId("first-value", "second-value");

        assertThat(resolver.getCompositeIdAttributeValue(id, "first")).isEqualTo("first-value");
        assertThat(resolver.getCompositeIdAttributeValue(id, "second")).isEqualTo("second-value");
    }

    @Test
    void buildsKeysetForCompositeIdAndProperties() {

        stubCompositeIdBasic();

        JpaIdentifierResolver<CompositeEntity> resolver = new JpaIdentifierResolver<>(type,
            PersistenceProvider.GENERIC_JPA, persistenceUnitUtil);

        CompositeEntity entity = new CompositeEntity("first-value", "second-value", "extra-value");

        Map<String, Object> keyset = resolver.getKeyset(List.of("extra"), entity);

        assertThat(keyset)
            .hasSize(3)
            .containsEntry("first", "first-value")
            .containsEntry("second", "second-value")
            .containsEntry("extra", "extra-value");
    }

    @Test
    void buildsKeysetWithEmptyPropertiesList() {

        stubCompositeIdBasic();

        JpaIdentifierResolver<CompositeEntity> resolver = new JpaIdentifierResolver<>(type,
            PersistenceProvider.GENERIC_JPA, persistenceUnitUtil);

        CompositeEntity entity = new CompositeEntity("first-value", "second-value", "extra-value");

        Map<String, Object> keyset = resolver.getKeyset(Collections.emptyList(), entity);

        assertThat(keyset)
            .hasSize(2)
            .containsEntry("first", "first-value")
            .containsEntry("second", "second-value")
            .doesNotContainKey("extra");
    }

    @Test
    void buildsKeysetForSingleIdAndProperties() {

        stubSingleIdWithJavaType();

        JpaIdentifierResolver<CompositeEntity> resolver = new JpaIdentifierResolver<>(type,
            PersistenceProvider.GENERIC_JPA, persistenceUnitUtil);

        CompositeEntity entity = new CompositeEntity("single-id", null, "extra-value");

        when(persistenceUnitUtil.getIdentifier(entity)).thenReturn("single-id");

        Map<String, Object> keyset = resolver.getKeyset(List.of("extra"), entity);

        assertThat(keyset)
            .hasSize(2)
            .containsEntry("id", "single-id")
            .containsEntry("extra", "extra-value");
    }

    @Test
    void returnsIdentifierForSingleIdEntity() {

        stubSingleIdForIdentifierRetrieval();

        JpaIdentifierResolver<CompositeEntity> resolver = new JpaIdentifierResolver<>(type,
            PersistenceProvider.GENERIC_JPA, persistenceUnitUtil);

        CompositeEntity entity = new CompositeEntity("single-id", null, "extra");

        when(persistenceUnitUtil.getIdentifier(entity)).thenReturn("single-id");

        assertThat(resolver.getId(entity)).isEqualTo("single-id");
        verify(persistenceUnitUtil).getIdentifier(entity);
    }

    @Test
    void exposesIdentifierMetadata() {

        stubSingleIdWithMetadata();

        JpaIdentifierResolver<CompositeEntity> resolver = new JpaIdentifierResolver<>(type,
            PersistenceProvider.GENERIC_JPA, persistenceUnitUtil);

        assertThat(resolver.hasCompositeId()).isFalse();
        assertThat(resolver.getIdType()).isEqualTo(String.class);
        assertThat(resolver.getIdAttribute().getName()).isEqualTo("id");
        assertThat(resolver.getIdAttributeNames()).containsExactly("id");
    }

    @Test
    void readsTupleIdentifierForSimpleId() {

        IdentifiableType<Tuple> tupleType = mock(IdentifiableType.class);
        SingularAttribute<Tuple, String> tupleIdAttribute = mock(SingularAttribute.class);
        @SuppressWarnings("rawtypes")
        Type tupleIdType = mock(Type.class);

        when(tupleType.hasSingleIdAttribute()).thenReturn(true);
        when(tupleType.getIdClassAttributes()).thenReturn(Set.of());
        when(tupleType.getIdType()).thenReturn(tupleIdType);
        when(tupleIdType.getJavaType()).thenReturn(String.class);
        when(tupleIdAttribute.getName()).thenReturn("id");

        @SuppressWarnings("unchecked")
        IdentifiableType<Tuple> typedTupleType = tupleType;
        when(typedTupleType.getId(String.class)).thenReturn((SingularAttribute) tupleIdAttribute);

        JpaIdentifierResolver<Tuple> resolver = new JpaIdentifierResolver<>(tupleType,
            PersistenceProvider.GENERIC_JPA, persistenceUnitUtil);

        Tuple tuple = mock(Tuple.class);
        when(tuple.get("id")).thenReturn("tuple-id");

        assertThat(resolver.getId(tuple)).isEqualTo("tuple-id");
        verify(persistenceUnitUtil, never()).getIdentifier(any());
    }

    static class CompositeEntity {

        String first;
        String second;
        String extra;

        CompositeEntity(String first, String second, String extra) {
            this.first = first;
            this.second = second;
            this.extra = extra;
        }
    }

    static class CompositeId {

        final String first;
        final String second;

        CompositeId(String first, String second) {
            this.first = first;
            this.second = second;
        }
    }

    /**
     * Minimal composite id stubs for hasCompositeId() checks.
     */
    private void stubCompositeIdMinimal() {
        when(type.hasSingleIdAttribute()).thenReturn(false);
        when(type.getIdClassAttributes()).thenReturn(Set.of(first, second));
    }

    /**
     * Composite id stubs used for identifier extraction.
     */
    private void stubCompositeIdBasic() {
        when(first.getName()).thenReturn("first");
        when(second.getName()).thenReturn("second");
        when(type.getIdClassAttributes()).thenReturn(Set.of(first, second));
        when(type.hasSingleIdAttribute()).thenReturn(false);
    }

    /**
     * Composite id stubs used for metadata assertions.
     */
    private void stubCompositeIdWithMetadata() {
        stubCompositeIdBasic();
        when(type.getIdType()).thenReturn(idType);
        when(idType.getJavaType()).thenReturn(CompositeId.class);
    }

    /**
     * Single id stubs with getJavaType for getId/getKeyset paths.
     */
    private void stubSingleIdWithJavaType() {
        when(first.getName()).thenReturn("id");
        when(type.hasSingleIdAttribute()).thenReturn(true);
        when(type.getIdClassAttributes()).thenReturn(Set.of());
        when(type.getIdType()).thenReturn(idType);
        when(idType.getJavaType()).thenReturn(String.class);
        when(type.getId(String.class)).thenReturn((SingularAttribute) first);
        when(type.getJavaType()).thenReturn(CompositeEntity.class);
    }

    private void stubSingleIdForIdentifierRetrieval() {
        when(type.hasSingleIdAttribute()).thenReturn(true);
        when(type.getIdClassAttributes()).thenReturn(Set.of());
        when(type.getIdType()).thenReturn(idType);
        when(idType.getJavaType()).thenReturn(String.class);
        when(type.getId(String.class)).thenReturn((SingularAttribute) first);
        when(type.getJavaType()).thenReturn(CompositeEntity.class);
    }

    /**
     * Single id stubs without getJavaType for metadata assertions.
     */
    private void stubSingleIdWithMetadata() {
        when(first.getName()).thenReturn("id");
        when(type.hasSingleIdAttribute()).thenReturn(true);
        when(type.getIdClassAttributes()).thenReturn(Set.of());
        when(type.getIdType()).thenReturn(idType);
        when(idType.getJavaType()).thenReturn(String.class);
        when(type.getId(String.class)).thenReturn((SingularAttribute) first);
    }
}
