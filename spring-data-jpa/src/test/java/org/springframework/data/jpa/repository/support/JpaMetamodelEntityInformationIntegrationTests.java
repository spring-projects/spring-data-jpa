/*
 * Copyright 2011-2023 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.jpa.repository.support.JpaEntityInformationSupport.*;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.data.jpa.domain.sample.*;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Integration tests for {@link JpaMetamodelEntityInformation}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @author Jens Schauder
 * @author Greg Turnquist
 */
public abstract class JpaMetamodelEntityInformationIntegrationTests {

	@PersistenceContext EntityManager em;

	abstract String getMetadadataPersistenceUnitName();

	@Test
	void detectsIdTypeForEntity() {

		JpaEntityInformation<User, ?> information = getEntityInformation(User.class, em);
		assertThat(information.getIdType()).isAssignableFrom(Integer.class);
	}

	@Test // DATAJPA-141
	void detectsIdTypeForMappedSuperclass() {

		JpaEntityInformation<?, ?> information = getEntityInformation(AbstractPersistable.class, em);
		assertThat(information.getIdType()).isEqualTo(Serializable.class);
	}

	@Test // DATAJPA-50
	void detectsIdClass() {

		EntityInformation<PersistableWithIdClass, ?> information = getEntityInformation(PersistableWithIdClass.class, em);
		assertThat(information.getIdType()).isAssignableFrom(PersistableWithIdClassPK.class);
	}

	@Test // DATAJPA-50
	void returnsIdOfPersistableInstanceCorrectly() {

		PersistableWithIdClass entity = new PersistableWithIdClass(2L, 4L);

		JpaEntityInformation<PersistableWithIdClass, ?> information = getEntityInformation(PersistableWithIdClass.class,
				em);
		Object id = information.getId(entity);

		assertThat(id).isEqualTo(new PersistableWithIdClassPK(2L, 4L));
	}

	@Test // GH-2330
	void returnsIdOfSingleAttributeIdClassCorrectly() {

		PersistableWithSingleIdClass entity = new PersistableWithSingleIdClass(2L);

		JpaEntityInformation<PersistableWithSingleIdClass, ?> information = getEntityInformation(
				PersistableWithSingleIdClass.class, em);
		Object id = information.getId(entity);

		assertThat(id).isEqualTo(new PersistableWithSingleIdClassPK(2L));
	}

	@Test // DATAJPA-413
	void returnsIdOfEntityWithIdClassCorrectly() {

		Item item = new Item(2, 1);

		JpaEntityInformation<Item, ?> information = getEntityInformation(Item.class, em);
		Object id = information.getId(item);

		assertThat(id).isEqualTo(new ItemId(2, 1));
	}

	@Test // DATAJPA-413
	void returnsDerivedIdOfEntityWithIdClassCorrectly() {

		Item item = new Item(1, 2);
		Site site = new Site(3);

		ItemSite itemSite = new ItemSite(item, site);

		JpaEntityInformation<ItemSite, ?> information = getEntityInformation(ItemSite.class, em);
		Object id = information.getId(itemSite);

		assertThat(id).isEqualTo(new ItemSiteId(new ItemId(1, 2), 3));
	}

	@Test // DATAJPA-413
	void returnsPartialEmptyDerivedIdOfEntityWithIdClassCorrectly() {

		Item item = new Item(1, null);
		Site site = new Site(3);

		ItemSite itemSite = new ItemSite(item, site);

		JpaEntityInformation<ItemSite, ?> information = getEntityInformation(ItemSite.class, em);
		Object id = information.getId(itemSite);

		assertThat(id).isEqualTo(new ItemSiteId(new ItemId(1, null), 3));
	}

	@Test // DATAJPA-119
	void favoursVersionAnnotationIfPresent() {

		EntityInformation<VersionedUser, Long> information = new JpaMetamodelEntityInformation<>(VersionedUser.class,
				em.getMetamodel(), em.getEntityManagerFactory().getPersistenceUnitUtil());

		VersionedUser entity = new VersionedUser();
		assertThat(information.isNew(entity)).isTrue();
		entity.setId(1L);
		assertThat(information.isNew(entity)).isTrue();
		entity.setVersion(1L);
		assertThat(information.isNew(entity)).isFalse();
		entity.setId(null);
		assertThat(information.isNew(entity)).isFalse();
	}

	@Test // DATAJPA-348
	void findsIdClassOnMappedSuperclass() {

		EntityManagerFactory emf = Persistence.createEntityManagerFactory(getMetadadataPersistenceUnitName());
		EntityManager em = emf.createEntityManager();

		EntityInformation<Sample, BaseIdClass> information = new JpaMetamodelEntityInformation<>(Sample.class,
				em.getMetamodel(), em.getEntityManagerFactory().getPersistenceUnitUtil());

		assertThat(information.getIdType()).isEqualTo(BaseIdClass.class);
	}

	@Test // DATACMNS-357
	void detectsNewStateForEntityWithPrimitiveId() {

		EntityInformation<SampleWithPrimitiveId, Long> information = new JpaMetamodelEntityInformation<>(
				SampleWithPrimitiveId.class, em.getMetamodel(), em.getEntityManagerFactory().getPersistenceUnitUtil());

		SampleWithPrimitiveId sample = new SampleWithPrimitiveId();
		assertThat(information.isNew(sample)).isTrue();

		sample.setId(5L);
		assertThat(information.isNew(sample)).isFalse();
	}

	@Test // DATAJPA-509
	void jpaMetamodelEntityInformationShouldRespectExplicitlyConfiguredEntityNameFromOrmXml() {

		JpaEntityInformation<Role, Integer> info = new JpaMetamodelEntityInformation<>(Role.class, em.getMetamodel(),
				em.getEntityManagerFactory().getPersistenceUnitUtil());

		assertThat(info.getEntityName()).isEqualTo("ROLE");
	}

	@Test // DATAJPA-561
	void considersEntityWithPrimitiveVersionPropertySetToDefaultNew() {

		EntityInformation<PrimitiveVersionProperty, Serializable> information = new JpaMetamodelEntityInformation<>(
				PrimitiveVersionProperty.class, em.getMetamodel(), em.getEntityManagerFactory().getPersistenceUnitUtil());

		assertThat(information.isNew(new PrimitiveVersionProperty())).isTrue();
	}

	@Test // DATAJPA-568
	void considersEntityAsNotNewWhenHavingIdSetAndUsingPrimitiveTypeForVersionProperty() {

		EntityInformation<PrimitiveVersionProperty, Serializable> information = new JpaMetamodelEntityInformation<>(
				PrimitiveVersionProperty.class, em.getMetamodel(), em.getEntityManagerFactory().getPersistenceUnitUtil());

		PrimitiveVersionProperty pvp = new PrimitiveVersionProperty();
		pvp.id = 100L;

		assertThat(information.isNew(pvp)).isFalse();
	}

	@Test // DATAJPA-568
	void fallsBackToIdInspectionForAPrimitiveVersionProperty() {

		EntityInformation<PrimitiveVersionProperty, Serializable> information = new JpaMetamodelEntityInformation<>(
				PrimitiveVersionProperty.class, em.getMetamodel(), em.getEntityManagerFactory().getPersistenceUnitUtil());

		PrimitiveVersionProperty pvp = new PrimitiveVersionProperty();
		pvp.version = 1L;

		assertThat(information.isNew(pvp)).isTrue();

		pvp.id = 1L;
		assertThat(information.isNew(pvp)).isFalse();
	}

	@Test // DATAJPA-582
	// @Disabled
	void considersEntityWithUnsetCompoundIdNew() {

		EntityInformation<SampleWithIdClass, ?> information = getEntityInformation(SampleWithIdClass.class, em);

		assertThat(information.isNew(new SampleWithIdClass())).isTrue();
	}

	@Test // DATAJPA-582
	void considersEntityWithSetTimestampVersionNotNew() {

		EntityInformation<SampleWithTimestampVersion, ?> information = getEntityInformation(
				SampleWithTimestampVersion.class, em);

		SampleWithTimestampVersion entity = new SampleWithTimestampVersion();
		entity.version = new Timestamp(new Date().getTime());

		assertThat(information.isNew(entity)).isFalse();
	}

	@Test // DATAJPA-582, DATAJPA-581
	void considersEntityWithNonPrimitiveNonNullIdTypeNotNew() {

		EntityInformation<User, ?> information = getEntityInformation(User.class, em);

		User user = new User();
		assertThat(information.isNew(user)).isTrue();

		user.setId(0);
		assertThat(information.isNew(user)).isFalse();
	}

	@Test // DATAJPA-820
	void detectsVersionPropertyOnMappedSuperClass() {

		EntityInformation<ConcreteType1, ?> information = getEntityInformation(ConcreteType1.class, em);

		assertThat(ReflectionTestUtils.getField(information, "versionAttribute")).isNotNull();
	}

	@Test // DATAJPA-1105
	void correctlyDeterminesIdValueForNestedIdClassesWithNonPrimitiveNonManagedType() {

		EntityManagerFactory emf = Persistence.createEntityManagerFactory(getMetadadataPersistenceUnitName());
		EntityManager em = emf.createEntityManager();

		JpaEntityInformation<EntityWithNestedIdClass, ?> information = getEntityInformation(EntityWithNestedIdClass.class,
				em);

		EntityWithNestedIdClass entity = new EntityWithNestedIdClass();
		entity.id = 23L;
		entity.reference = new EntityWithIdClass();
		entity.reference.id1 = "one";
		entity.reference.id2 = "two";

		Object id = information.getId(entity);

		assertThat(id).isNotNull();
	}

	@Test // DATAJPA-1416
	@Disabled
	void proxiedIdClassElement() {

		JpaEntityInformation<SampleWithIdClassIncludingEntity, ?> information = getEntityInformation(
				SampleWithIdClassIncludingEntity.class, em);

		SampleWithIdClassIncludingEntity entity = new SampleWithIdClassIncludingEntity();
		entity.setFirst(23L);
		SampleWithIdClassIncludingEntity.OtherEntity$$PsudoProxy inner = new SampleWithIdClassIncludingEntity.OtherEntity$$PsudoProxy();
		inner.setOtherId(42L);
		entity.setSecond(inner);

		Object id = information.getId(entity);

		assertThat(id).isInstanceOf(SampleWithIdClassIncludingEntity.SampleWithIdClassPK.class);

		SampleWithIdClassIncludingEntity.SampleWithIdClassPK pk = (SampleWithIdClassIncludingEntity.SampleWithIdClassPK) id;

		assertThat(pk.getFirst()).isEqualTo(23L);
		assertThat(pk.getSecond()).isEqualTo(42L);
	}

	@Test // DATAJPA-1576
	void prefersPrivateGetterOverFieldAccess() {

		EntityManagerFactory emf = Persistence.createEntityManagerFactory(getMetadadataPersistenceUnitName());
		EntityManager em = emf.createEntityManager();

		JpaEntityInformation<EntityWithPrivateIdGetter, ?> information = getEntityInformation(
				EntityWithPrivateIdGetter.class, em);

		EntityWithPrivateIdGetter entity = new EntityWithPrivateIdGetter();

		Object id = information.getId(entity);

		assertThat(id).isEqualTo(42L);
	}

	@SuppressWarnings("serial")
	private static class BaseIdClass implements Serializable {

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

	@Entity
	@Access(AccessType.FIELD)
	@IdClass(EntityWithNestedIdClassPK.class)
	public static class EntityWithNestedIdClass {

		@Id Long id;
		@Id
		@ManyToOne private EntityWithIdClass reference;
	}

	@Entity
	@Access(AccessType.FIELD)
	@IdClass(EntityWithIdClassPK.class)
	public static class EntityWithIdClass {

		@Id String id1;
		@Id String id2;
	}

	@Data
	public static class EntityWithIdClassPK implements Serializable {

		String id1;
		String id2;
	}

	@Data
	public static class EntityWithNestedIdClassPK implements Serializable {

		Long id;
		EntityWithIdClassPK reference;
	}

	@Entity
	public static class EntityWithPrivateIdGetter implements Serializable {

		private long id = 0;

		@Id
		private long getId() {
			return 42;
		}

		public void setId(long id) {
			this.id = id;
		}
	}

	@Entity
	public static class ExampleEntityWithStringId {

		private UUID clientId;

		public UUID getId() {
			return this.clientId;
		}

		public void setId(UUID clientId) {
			this.clientId = clientId;
		}

		public void setClientId(String clientId) {
			this.clientId = UUID.fromString(clientId);
		}

		@Id
		public String getClientId() {
			return clientId == null ? null : clientId.toString();
		}
	}

	@Entity
	public static class ExampleEntityWithUUIDId {

		@Id private UUID clientId;

		public UUID getId() {
			return this.clientId;
		}

		public void setId(UUID clientId) {
			this.clientId = clientId;
		}

		public void setClientId(String clientId) {
			this.clientId = UUID.fromString(clientId);
		}

		public String getClientId() {
			return clientId == null ? null : clientId.toString();
		}
	}
}
