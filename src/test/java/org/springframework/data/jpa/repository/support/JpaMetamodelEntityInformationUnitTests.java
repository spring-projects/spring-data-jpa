/*
 * Copyright 2012-2019 the original author or authors.
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

import static java.util.Arrays.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.jpa.domain.sample.PersistableWithIdClass;
import org.springframework.data.jpa.domain.sample.PersistableWithIdClassPK;

/**
 * Unit tests for {@link JpaMetamodelEntityInformation}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class JpaMetamodelEntityInformationUnitTests {

	@Mock Metamodel metamodel;

	@Mock IdentifiableType<PersistableWithIdClass> type;
	@Mock SingularAttribute<PersistableWithIdClass, ?> first, second;

	@Mock @SuppressWarnings("rawtypes") Type idType;

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() {

		when(first.getName()).thenReturn("first");
		when(second.getName()).thenReturn("second");
		Set<SingularAttribute<? super PersistableWithIdClass, ?>> attributes = new HashSet<SingularAttribute<? super PersistableWithIdClass, ?>>(
				asList(first, second));

		when(type.getIdClassAttributes()).thenReturn(attributes);

		when(metamodel.managedType(Object.class)).thenThrow(IllegalArgumentException.class);
		when(metamodel.managedType(PersistableWithIdClass.class)).thenReturn(type);

		when(type.getIdType()).thenReturn(idType);
		when(idType.getJavaType()).thenReturn(PersistableWithIdClassPK.class);
	}

	@Test // DATAJPA-50
	public void doesNotCreateIdIfAllPartialAttributesAreNull() {

		JpaMetamodelEntityInformation<PersistableWithIdClass, Serializable> information = new JpaMetamodelEntityInformation<PersistableWithIdClass, Serializable>(
				PersistableWithIdClass.class, metamodel);

		PersistableWithIdClass entity = new PersistableWithIdClass(null, null);
		assertThat(information.getId(entity)).isNull();

		entity = new PersistableWithIdClass(2L, null);
		assertThat(information.getId(entity)).isNotNull();
	}
}
