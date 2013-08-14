/*
 * Copyright 2011-2013 the original author or authors.
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

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.MappedSuperclass;
import javax.persistence.Persistence;
import javax.persistence.PersistenceContext;
import javax.persistence.metamodel.Metamodel;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.data.jpa.domain.sample.SampleWithIdClass;
import org.springframework.data.jpa.domain.sample.SampleWithIdClassPK;
import org.springframework.data.jpa.domain.sample.SampleWithPrimitiveId;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.domain.sample.VersionedUser;
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

	/**
	 * @see DATAJPA-119
	 */
	@Test
	public void favoursVersionAnnotationIfPresent() {

		EntityInformation<VersionedUser, Long> information = new JpaMetamodelEntityInformation<VersionedUser, Long>(
				VersionedUser.class, em.getMetamodel());

		VersionedUser entity = new VersionedUser();
		assertThat(information.isNew(entity), is(true));
		entity.setId(1L);
		assertThat(information.isNew(entity), is(true));
		entity.setVersion(1L);
		assertThat(information.isNew(entity), is(false));
		entity.setId(null);
		assertThat(information.isNew(entity), is(false));
	}

	/**
	 * @see DATAJPA-348
	 */
	@Test
	public void findsIdClassOnMappedSuperclass() {

		EntityManagerFactory emf = Persistence.createEntityManagerFactory(getMetadadataPersitenceUnitName());
		EntityManager em = emf.createEntityManager();

		EntityInformation<Sample, BaseIdClass> information = new JpaMetamodelEntityInformation<Sample, BaseIdClass>(
				Sample.class, em.getMetamodel());

		assertThat(information.getIdType(), is((Object) BaseIdClass.class));
	}

	/**
	 * @see DATACMNS-357
	 */
	@Test
	public void detectsNewStateForEntityWithPrimitiveId() {

		EntityInformation<SampleWithPrimitiveId, Long> information = new JpaMetamodelEntityInformation<SampleWithPrimitiveId, Long>(
				SampleWithPrimitiveId.class, em.getMetamodel());

		SampleWithPrimitiveId sample = new SampleWithPrimitiveId();
		assertThat(information.isNew(sample), is(true));

		sample.setId(5L);
		assertThat(information.isNew(sample), is(false));
	}

	protected String getMetadadataPersitenceUnitName() {
		return "metadata";
	}

	@SuppressWarnings("serial")
	public static class BaseIdClass implements Serializable {

		Long id;
		Long feedRunId;
	}

	@MappedSuperclass
	@IdClass(BaseIdClass.class)
	@Access(AccessType.FIELD)
	public static abstract class Identifiable {

		@Id Long id;
		@Id Long feedRunId;
	}

	@Entity
	@Access(AccessType.FIELD)
	public static class Sample extends Identifiable {

	}
}
