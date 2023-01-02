/*
 * Copyright 2018-2023 the original author or authors.
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
package org.springframework.data.jpa.util;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.Type.PersistenceType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for {@link JpaMetamodel}.
 *
 * @author Oliver Gierke
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JpaMetamodelUnitTests {

	@Mock Metamodel metamodel;

	@Mock EntityType<?> type;

	@Test
	void skipsEntityTypesWithoutJavaTypeForIdentifierLookup() {

		doReturn(Collections.singleton(type)).when(metamodel).getEntities();

		assertThat(JpaMetamodel.of(metamodel).isSingleIdAttribute(Object.class, "id", Object.class)).isFalse();
	}

	@Test // DATAJPA-1446
	void cacheIsEffectiveUnlessCleared() {

		JpaMetamodel model = JpaMetamodel.of(metamodel);
		assertThat(model).isEqualTo(JpaMetamodel.of(metamodel));

		JpaMetamodel.clear();
		assertThat(model).isNotEqualTo(JpaMetamodel.of(metamodel));
	}

	@Test // #2421
	void doesNotConsiderNonNativeEmbeddablesJpaManaged() {

		JpaMetamodel model = JpaMetamodel.of(metamodel);

		ManagedType<?> entity = getEntity(Wrapper.class);
		ManagedType<?> embeddable = getEmbeddable(ExplicitEmbeddable.class);
		ManagedType<?> inner = getEmbeddable(Inner.class);

		doReturn(new HashSet<>(Arrays.asList(entity, embeddable, inner))).when(metamodel).getManagedTypes();
		doReturn(new HashSet<>(Arrays.asList(embeddable, inner))).when(metamodel).getEmbeddables();

		assertThat(model.isMappedType(Wrapper.class)).isTrue();
		assertThat(model.isMappedType(ExplicitEmbeddable.class)).isTrue();
		assertThat(model.isMappedType(Inner.class)).isFalse();
	}

	private EmbeddableType<?> getEmbeddable(Class<?> type) {

		EmbeddableType<?> managedType = getManagedType(type, EmbeddableType.class);
		doReturn(PersistenceType.EMBEDDABLE).when(managedType).getPersistenceType();

		return managedType;
	}

	private EntityType<?> getEntity(Class<?> type) {

		EntityType<?> managedType = getManagedType(type, EntityType.class);
		doReturn(PersistenceType.ENTITY).when(managedType).getPersistenceType();

		return managedType;
	}

	private <T extends ManagedType<?>> T getManagedType(Class<?> type, Class<T> baseType) {

		T managedType = mock(baseType);
		doReturn(type).when(managedType).getJavaType();
		doReturn(managedType).when(metamodel).managedType(type);

		return managedType;
	}

	@Entity
	static class Wrapper {}

	@Embeddable
	static class ExplicitEmbeddable {}

	static class Inner {}
}
