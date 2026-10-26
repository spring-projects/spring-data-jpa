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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.Collections;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.data.jpa.domain.sample.*;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Integration tests for identifier extraction and newness through the JPA mapping model.
 *
 * @author Mark Paluch
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:hibernate-infrastructure.xml")
class JpaIdMappingIntegrationTests {

	@PersistenceContext EntityManager em;

	JpaMetamodelMappingContext context;

	@BeforeEach
	void setUp() {
		context = new JpaMetamodelMappingContext(Collections.singleton(em.getMetamodel()));
	}

	@Test
	void extractsAllIdClassComponents() {

		Item item = new Item(2, 1);
		ItemId expected = new ItemId(2, 1);
		JpaEntityInformation<Item, ?> information = JpaEntityInformationSupport.getEntityInformation(Item.class, em);
		JpaPersistentEntityImpl<?> persistentEntity = context.getRequiredPersistentEntity(Item.class);

		assertThat(information.getId(item)).isEqualTo(expected);
		assertThat(persistentEntity.getIdentifierAccessor(item).getIdentifier()).isEqualTo(expected);
	}

	@Test
	void considersEntityWithAssignedIdAndZeroPrimitiveVersionNotNew() {

		PrimitiveVersionProperty entity = new PrimitiveVersionProperty();
		entity.id = 100L;
		entity.version = 0L;

		JpaEntityInformation<PrimitiveVersionProperty, ?> information = JpaEntityInformationSupport
				.getEntityInformation(PrimitiveVersionProperty.class, em);
		JpaPersistentEntityImpl<?> persistentEntity = context.getRequiredPersistentEntity(PrimitiveVersionProperty.class);

		assertThat(information.isNew(entity)).as("Entity information newness").isFalse();
		assertThat(persistentEntity.isNew(entity)).as("Persistent entity newness").isFalse();
	}

	@Test
	void returnsIdClassForSingleIdentifierAttribute() {

		PersistableWithSingleIdClass entity = new PersistableWithSingleIdClass(2L);
		PersistableWithSingleIdClassPK expectedId = new PersistableWithSingleIdClassPK(2L);
		JpaEntityInformation<PersistableWithSingleIdClass, ?> information = JpaEntityInformationSupport
				.getEntityInformation(PersistableWithSingleIdClass.class, em);
		JpaPersistentEntityImpl<?> persistentEntity = context
				.getRequiredPersistentEntity(PersistableWithSingleIdClass.class);

		assertThat(information.getId(entity)).isEqualTo(expectedId);
		assertThat(persistentEntity.getIdentifierAccessor(entity).getIdentifier()).isEqualTo(expectedId);
	}

	@Test
	void extractsScalarIdentifier() {

		Site entity = new Site(3);
		JpaEntityInformation<Site, ?> information = JpaEntityInformationSupport.getEntityInformation(Site.class, em);
		JpaPersistentEntity<?> persistentEntity = context.getRequiredPersistentEntity(Site.class);

		assertThat(information.getId(entity)).isEqualTo(3);
		assertThat(persistentEntity.getIdentifierAccessor(entity).getIdentifier()).isEqualTo(3);
	}

	@Test
	void extractsEmbeddedIdentifier() {

		EmbeddedIdExampleEmployee entity = new EmbeddedIdExampleEmployee();
		EmbeddedIdExampleEmployeePK id = new EmbeddedIdExampleEmployeePK(2L, 1L);
		entity.setEmployeePk(id);
		EmbeddedIdExampleDepartment department = new EmbeddedIdExampleDepartment();
		department.setDepartmentId(1L);
		entity.setDepartment(department);
		JpaEntityInformation<EmbeddedIdExampleEmployee, ?> information = JpaEntityInformationSupport
				.getEntityInformation(EmbeddedIdExampleEmployee.class, em);
		JpaPersistentEntity<?> persistentEntity = context.getRequiredPersistentEntity(EmbeddedIdExampleEmployee.class);

		assertThat(information.getId(entity)).isEqualTo(id);
		assertThat(persistentEntity.getIdentifierAccessor(entity).getIdentifier()).isEqualTo(id);
	}

	@Test
	void extractsDerivedIdentifier() {

		ItemSite entity = new ItemSite(new Item(2, 1), new Site(3));
		ItemSiteId expected = new ItemSiteId(new ItemId(2, 1), 3);
		JpaEntityInformation<ItemSite, ?> information = JpaEntityInformationSupport.getEntityInformation(ItemSite.class,
				em);
		JpaPersistentEntity<?> persistentEntity = context.getRequiredPersistentEntity(ItemSite.class);

		assertThat(information.getId(entity)).isEqualTo(expected);
		assertThat(persistentEntity.getIdentifierAccessor(entity).getIdentifier()).isEqualTo(expected);
	}

	@Test
	void preservesPresentAssociationWithUnassignedIdentifier() {

		ItemSite entity = new ItemSite(new Item(), null);
		ItemSiteId expected = new ItemSiteId(new ItemId(null, null), null);
		JpaPersistentEntity<?> persistentEntity = context.getRequiredPersistentEntity(ItemSite.class);

		assertThat(persistentEntity.getIdentifierAccessor(entity).getIdentifier()).isEqualTo(expected);
		assertThat(persistentEntity.isNew(entity)).isFalse();
	}

	@Test
	void usesWrapperVersionForNewnessWithAssignedIdentifier() {

		VersionedUser entity = new VersionedUser();
		entity.setId(100L);
		JpaEntityInformation<VersionedUser, ?> information = JpaEntityInformationSupport
				.getEntityInformation(VersionedUser.class, em);
		JpaPersistentEntity<?> persistentEntity = context.getRequiredPersistentEntity(VersionedUser.class);

		assertThat(information.isNew(entity)).isTrue();
		assertThat(persistentEntity.isNew(entity)).isTrue();

		entity.setVersion(0L);

		assertThat(information.isNew(entity)).isFalse();
		assertThat(persistentEntity.isNew(entity)).isFalse();
	}

	@Test
	void ignoresPrimitiveVersionForNewnessWithAbsentIdentifier() {

		PrimitiveVersionProperty entity = new PrimitiveVersionProperty();
		entity.version = 1L;
		JpaEntityInformation<PrimitiveVersionProperty, ?> information = JpaEntityInformationSupport
				.getEntityInformation(PrimitiveVersionProperty.class, em);
		JpaPersistentEntity<?> persistentEntity = context.getRequiredPersistentEntity(PrimitiveVersionProperty.class);

		assertThat(information.isNew(entity)).isTrue();
		assertThat(persistentEntity.isNew(entity)).isTrue();
	}

	@Test
	void delegatesIdentifierAndNewnessToPersistable() {

		PersistableWithIdClass entity = new PersistableWithIdClass(null, null);
		PersistableWithIdClassPK expected = new PersistableWithIdClassPK(null, null);
		JpaEntityInformation<PersistableWithIdClass, ?> information = JpaEntityInformationSupport
				.getEntityInformation(PersistableWithIdClass.class, em);
		JpaPersistentEntity<?> persistentEntity = context.getRequiredPersistentEntity(PersistableWithIdClass.class);

		assertThat(information.getId(entity)).isEqualTo(expected);
		assertThat(persistentEntity.getIdentifierAccessor(entity).getIdentifier()).isEqualTo(expected);
		assertThat(information.isNew(entity)).isTrue();
		assertThat(persistentEntity.isNew(entity)).isTrue();

		entity.setNotNew();

		assertThat(information.isNew(entity)).isFalse();
		assertThat(persistentEntity.isNew(entity)).isFalse();
	}

	@Test
	void honorsPersistableIdentifierForProxies() {

		PersistableWithIdClass entity = mock(PersistableWithIdClass.class,
				withSettings().extraInterfaces(HibernateProxy.class));
		LazyInitializer initializer = mock(LazyInitializer.class);
		PersistableWithIdClassPK expected = new PersistableWithIdClassPK(2L, 1L);
		when(((HibernateProxy) entity).getHibernateLazyInitializer()).thenReturn(initializer);
		when(initializer.getIdentifier()).thenReturn(new PersistableWithIdClassPK(3L, 4L));
		when(entity.getId()).thenReturn(expected);
		JpaEntityInformation<PersistableWithIdClass, ?> information = JpaEntityInformationSupport
				.getEntityInformation(PersistableWithIdClass.class, em);
		JpaPersistentEntity<?> persistentEntity = context.getRequiredPersistentEntity(PersistableWithIdClass.class);

		assertThat(information.getId(entity)).isEqualTo(expected);
		assertThat(persistentEntity.getIdentifierAccessor(entity).getIdentifier()).isEqualTo(expected);
	}

}
