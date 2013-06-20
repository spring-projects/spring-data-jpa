/*
 * Copyright 2011-2012 the original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.Serializable;

import javax.persistence.EntityManager;
import javax.persistence.MappedSuperclass;
import javax.persistence.PersistenceContext;
import javax.persistence.metamodel.Metamodel;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.data.jpa.domain.sample.SampleWithIdClass;
import org.springframework.data.jpa.domain.sample.SampleWithIdClassPK;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration tests for {@link JpaMetamodelEntityInformation}.
 * 
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "classpath:infrastructure.xml" })
public class JpaMetamodelEntityInformationIntegrationTests {

	@PersistenceContext EntityManager em;

	@Test
	public void detectsIdTypeForEntity() {

		JpaEntityInformation<User, ?> information = JpaEntityInformationSupport.getMetadata(User.class, em);
		assertThat(information.getIdType(), is(typeCompatibleWith(Integer.class)));
	}

	/**
	 * Ignored for Hibernate as it does not implement {@link Metamodel#managedType(Class)} correctly (does not consider
	 * {@link MappedSuperclass}es correctly).
	 * 
	 * @see https://hibernate.onjira.com/browse/HHH-6896
	 * @see DATAJPA-141
	 */
	@Test
	@Ignore
	public void detectsIdTypeForMappedSuperclass() {

		JpaEntityInformation<?, ?> information = JpaEntityInformationSupport.getMetadata(AbstractPersistable.class, em);
		assertEquals(Serializable.class, information.getIdType());
	}

	/**
	 * @see DATAJPA-50
	 */
	@Test
	public void detectsIdClass() {

		EntityInformation<SampleWithIdClass, ?> information = JpaEntityInformationSupport.getMetadata(
				SampleWithIdClass.class, em);
		assertThat(information.getIdType(), is(typeCompatibleWith(SampleWithIdClassPK.class)));
	}

	/**
	 * @see DATAJPA-50
	 */
	@Test
	public void returnsIdInstanceCorrectly() {

		SampleWithIdClass entity = new SampleWithIdClass(2L, 4L);

		JpaEntityInformation<SampleWithIdClass, ?> information = JpaEntityInformationSupport.getMetadata(
				SampleWithIdClass.class, em);
		Object id = information.getId(entity);

		assertThat(id, is(instanceOf(SampleWithIdClassPK.class)));
		assertThat(id, is((Object) new SampleWithIdClassPK(2L, 4L)));
	}
}
