/*
 * Copyright 2012 the original author or authors.
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

import static java.util.Arrays.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
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
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.jpa.domain.sample.SampleWithIdClass;
import org.springframework.data.jpa.domain.sample.SampleWithIdClassPK;

/**
 * Unit tests for {@link JpaMetamodelEntityInformation}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class JpaMetamodelEntityInformationUnitTests {

	@Mock
	Metamodel metamodel;

	@Mock
	IdentifiableType<SampleWithIdClass> type;
	@Mock
	SingularAttribute<SampleWithIdClass, ?> first, second;

	@Mock
	@SuppressWarnings("rawtypes")
	Type idType;

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() {

		when(first.getName()).thenReturn("first");
		when(second.getName()).thenReturn("second");
		Set<SingularAttribute<? super SampleWithIdClass, ?>> attributes = new HashSet<SingularAttribute<? super SampleWithIdClass, ?>>(
				asList(first, second));

		when(type.getIdClassAttributes()).thenReturn(attributes);

		when(metamodel.managedType(SampleWithIdClass.class)).thenReturn(type);

		when(type.getIdType()).thenReturn(idType);
		when(idType.getJavaType()).thenReturn(SampleWithIdClassPK.class);
	}

	/**
	 * @see DATAJPA-50
	 */
	@Test
	public void doesNotCreateIdIfAllPartialAttributesAreNull() {

		JpaMetamodelEntityInformation<SampleWithIdClass, Serializable> information = new JpaMetamodelEntityInformation<SampleWithIdClass, Serializable>(
				SampleWithIdClass.class, metamodel);

		SampleWithIdClass entity = new SampleWithIdClass(null, null);
		assertThat(information.getId(entity), is(nullValue()));

		entity = new SampleWithIdClass(2L, null);
		assertThat(information.getId(entity), is(notNullValue()));
	}
}
