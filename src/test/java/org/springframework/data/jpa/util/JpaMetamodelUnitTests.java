/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.data.jpa.util;

import static java.util.Arrays.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;

import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.Type.PersistenceType;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link JpaMetamodel}.
 * 
 * @author Oliver Gierke
 * @author Jens Schauder
 */
@RunWith(MockitoJUnitRunner.class)
public class JpaMetamodelUnitTests {

	@Mock Metamodel metamodel;

	@Mock EntityType<?> type;

	@Test
	public void skipsEntityTypesWithoutJavaTypeForIdentifierLookup() {

		doReturn(Collections.singleton(type)).when(metamodel).getEntities();

		assertThat(new JpaMetamodel(metamodel).isSingleIdAttribute(Object.class, "id", Object.class)).isFalse();
	}

	@Test // DATAJPA-1384
	public void ignoresManagedTypesThatArentEntityTypes() {

		HashSet<ManagedType<?>> managedTypes = new HashSet<>(asList(mockManagedType(String.class, PersistenceType.ENTITY),
				mockManagedType(Integer.class, PersistenceType.BASIC),
				mockManagedType(null, PersistenceType.BASIC),
				mockManagedType(Number.class, PersistenceType.EMBEDDABLE),
				mockManagedType(Date.class, PersistenceType.MAPPED_SUPERCLASS), mockManagedType(Map.class, null)));

		when(metamodel.getManagedTypes()).thenReturn(managedTypes);

		SoftAssertions softly = new SoftAssertions();

		softly.assertThat(new JpaMetamodel(metamodel).isJpaManaged(String.class)).describedAs("String - Entity").isTrue();
		softly.assertThat(new JpaMetamodel(metamodel).isJpaManaged(Integer.class)).describedAs("Integer - Basic").isFalse();
		softly.assertThat(new JpaMetamodel(metamodel).isJpaManaged(Number.class)).describedAs("Number - Embeddable")
				.isTrue();
		softly.assertThat(new JpaMetamodel(metamodel).isJpaManaged(Date.class)).describedAs("Date - MappedSuperclass")
				.isFalse();
		softly.assertThat(new JpaMetamodel(metamodel).isJpaManaged(Map.class)).describedAs("Map - Null").isFalse();

		softly.assertAll();
	}

	private <T> ManagedType<T> mockManagedType(Class<T> type, PersistenceType entity) {

		ManagedType<T> entityManagedType = mock(ManagedType.class);
		when(entityManagedType.getPersistenceType()).thenReturn(entity);
		when(entityManagedType.getJavaType()).thenReturn(type);
		return entityManagedType;
	}

}
