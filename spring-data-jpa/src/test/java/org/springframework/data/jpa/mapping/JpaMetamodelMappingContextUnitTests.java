/*
 * Copyright 2015-present the original author or authors.
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

import java.util.Collections;
import java.util.Set;

import jakarta.persistence.IdClass;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;

import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Version;

/**
 * Unit tests for {@link JpaMetamodelMappingContext}.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
class JpaMetamodelMappingContextUnitTests {

	@Test // DATAJPA-775
	void jpaPersistentEntityRejectsSpringDataAtVersionAnnotation() {

		Metamodel metamodel = mock(Metamodel.class);

		JpaMetamodelMappingContext context = new JpaMetamodelMappingContext(Collections.singleton(metamodel));
		context.getPersistentEntity(Sample.class);
	}

	@Test // GH-4037
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void doesNotRescanManagedTypesForUnmanagedTypes() {

		Metamodel metamodel = mock(Metamodel.class);
		EntityType<SampleWithIdClass> entityType = mock(EntityType.class);
		when(entityType.getJavaType()).thenReturn(SampleWithIdClass.class);
		when(metamodel.getManagedTypes()).thenReturn((Set) Set.of(entityType));
		when(metamodel.managedType(any())).thenThrow(IllegalArgumentException.class);

		JpaMetamodelMappingContext context = new JpaMetamodelMappingContext(Collections.singleton(metamodel));

		context.getPersistentEntity(Unmanaged.class);
		long scans = getManagedTypesInvocations(metamodel);

		context.getPersistentEntity(OtherUnmanaged.class);
		context.getPersistentEntity(Sample.class);

		assertThat(getManagedTypesInvocations(metamodel)).isEqualTo(scans);
		verify(metamodel, never()).managedType(any());
		verify(entityType, times(1)).getIdClassAttributes();
	}

	@Test // GH-4037
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void resolvesIdClassOfManagedType() {

		Metamodel metamodel = mock(Metamodel.class);
		EntityType<SampleWithIdClass> entityType = mock(EntityType.class);
		when(entityType.getJavaType()).thenReturn(SampleWithIdClass.class);
		when(metamodel.getManagedTypes()).thenReturn((Set) Set.of(entityType));

		JpaMetamodelMappingContext context = new JpaMetamodelMappingContext(Collections.singleton(metamodel));

		assertThat(context.getPersistentEntity(SampleId.class)).isNotNull();
		assertThat(context.getPersistentEntity(Unmanaged.class)).isNull();
	}

	@Test // GH-4037
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void resolvesIdTypeOfManagedTypeWithIdClassAttributes() {

		Metamodel metamodel = mock(Metamodel.class);
		EntityType<Sample> entityType = mock(EntityType.class);
		ManagedType idType = mock(ManagedType.class);
		when(idType.getJavaType()).thenReturn(SampleId.class);
		when(entityType.getJavaType()).thenReturn(Sample.class);
		when(entityType.getIdClassAttributes()).thenReturn((Set) Set.of(mock(SingularAttribute.class)));
		when(entityType.getIdType()).thenReturn(idType);
		when(metamodel.getManagedTypes()).thenReturn((Set) Set.of(entityType));

		JpaMetamodelMappingContext context = new JpaMetamodelMappingContext(Collections.singleton(metamodel));

		assertThat(context.getPersistentEntity(SampleId.class)).isNotNull();
	}

	private static long getManagedTypesInvocations(Metamodel metamodel) {
		return mockingDetails(metamodel).getInvocations().stream()
				.filter(it -> it.getMethod().getName().equals("getManagedTypes")).count();
	}

	private static class Sample {
		@Version Long version;
	}

	@IdClass(SampleId.class)
	private static class SampleWithIdClass {
		Long first;
		Long second;
	}

	private static class SampleId {
		Long first;
		Long second;
	}

	private static class Unmanaged {
		String value;
	}

	private static class OtherUnmanaged {
		String value;
	}
}
