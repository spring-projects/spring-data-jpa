/*
 * Copyright 2013-2018 the original author or authors.
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
package org.springframework.data.jpa.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import java.util.Collections;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.annotation.AccessType.Type;
import org.springframework.data.annotation.Version;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

/**
 * Unit tests for {@link JpaPersistentPropertyImpl}.
 *
 * @author Oliver Gierke
 * @author Greg Turnquist
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class JpaPersistentPropertyImplUnitTests {

	@Mock Metamodel model;

	JpaMetamodelMappingContext context;
	JpaPersistentEntity<?> entity;

	@Before
	public void setUp() {

		context = new JpaMetamodelMappingContext(Collections.singleton(model));
		entity = context.getRequiredPersistentEntity(Sample.class);
	}

	@Test // DATAJPA-284
	public void considersOneToOneMappedPropertyAnAssociation() {

		JpaPersistentProperty property = entity.getRequiredPersistentProperty("other");
		assertThat(property.isAssociation(), is(true));
	}

	@Test // DATAJPA-376
	public void considersJpaTransientFieldsAsTransient() {
		assertThat(entity.getPersistentProperty("transientProp"), is(nullValue()));
	}

	@Test // DATAJPA-484
	public void considersEmbeddableAnEntity() {
		assertThat(context.getPersistentEntity(SampleEmbeddable.class), is(notNullValue()));
	}

	@Test // DATAJPA-484
	public void doesNotConsiderAnEmbeddablePropertyAnAssociation() {
		assertThat(entity.getRequiredPersistentProperty("embeddable").isAssociation(), is(false));
	}

	@Test // DATAJPA-484
	public void doesNotConsiderAnEmbeddedPropertyAnAssociation() {
		assertThat(entity.getRequiredPersistentProperty("embedded").isAssociation(), is(false));
	}

	@Test // DATAJPA-619
	public void considersPropertyLevelAccessTypeDefinitions() {

		assertThat(getProperty(PropertyLevelPropertyAccess.class, "field").usePropertyAccess(), is(false));
		assertThat(getProperty(PropertyLevelPropertyAccess.class, "property").usePropertyAccess(), is(true));
	}

	@Test // DATAJPA-619
	public void propertyLevelAccessTypeTrumpsTypeLevelDefinition() {

		assertThat(getProperty(PropertyLevelDefinitionTrumpsTypeLevelOne.class, "field").usePropertyAccess(), is(false));
		assertThat(getProperty(PropertyLevelDefinitionTrumpsTypeLevelOne.class, "property").usePropertyAccess(), is(true));

		assertThat(getProperty(PropertyLevelDefinitionTrumpsTypeLevelOne2.class, "field").usePropertyAccess(), is(false));
		assertThat(getProperty(PropertyLevelDefinitionTrumpsTypeLevelOne2.class, "property").usePropertyAccess(), is(true));
	}

	@Test // DATAJPA-619
	public void considersJpaAccessDefinitionAnnotations() {
		assertThat(getProperty(TypeLevelPropertyAccess.class, "id").usePropertyAccess(), is(true));
	}

	@Test // DATAJPA-619
	public void springDataAnnotationTrumpsJpaIfBothOnTypeLevel() {
		assertThat(getProperty(CompetingTypeLevelAnnotations.class, "id").usePropertyAccess(), is(false));
	}

	@Test // DATAJPA-619
	public void springDataAnnotationTrumpsJpaIfBothOnPropertyLevel() {
		assertThat(getProperty(CompetingPropertyLevelAnnotations.class, "id").usePropertyAccess(), is(false));
	}

	@Test // DATAJPA-605
	public void detectsJpaVersionAnnotation() {
		assertThat(getProperty(JpaVersioned.class, "version").isVersionProperty(), is(true));
	}

	@Test // DATAJPA-664
	@SuppressWarnings("rawtypes")
	public void considersTargetEntityTypeForPropertyType() {

		JpaPersistentProperty property = getProperty(SpecializedAssociation.class, "api");

		assertThat(property.getType(), is(typeCompatibleWith(Api.class)));
		assertThat(property.getActualType(), is(typeCompatibleWith(Implementation.class)));

		Iterable<? extends TypeInformation<?>> entityType = property.getPersistentEntityTypes();
		assertThat(entityType.iterator().hasNext(), is(true));
		assertThat(entityType.iterator().next(), is((TypeInformation) ClassTypeInformation.from(Implementation.class)));
	}

	@Test // DATAJPA-716
	public void considersNonUpdateablePropertyNotWriteable() {
		assertThat(getProperty(WithReadOnly.class, "name").isWritable(), is(false));
		assertThat(getProperty(WithReadOnly.class, "updatable").isWritable(), is(true));
	}

	@Test // DATAJPA-904
	public void isEntityWorksEvenWithManagedTypeWithNullJavaType() {

		ManagedType<?> managedType = mock(ManagedType.class);
		doReturn(Collections.singleton(managedType)).when(model).getManagedTypes();

		assertThat(getProperty(Sample.class, "other").isEntity(), is(false));
	}

	@Test // DATAJPA-1064
	public void simplePropertyIsNotConsideredAnAssociation() {

		JpaPersistentEntityImpl<?> entity = context.getRequiredPersistentEntity(WithReadOnly.class);
		JpaPersistentProperty property = entity.getRequiredPersistentProperty("updatable");

		assertThat(property.isAssociation()).isFalse();
	}

	private JpaPersistentProperty getProperty(Class<?> ownerType, String propertyName) {

		JpaPersistentEntity<?> entity = context.getRequiredPersistentEntity(ownerType);
		return entity.getRequiredPersistentProperty(propertyName);
	}

	static class Sample {

		@OneToOne Sample other;
		@Transient String transientProp;
		SampleEmbeddable embeddable;
		@Embedded SampleEmbedded embedded;
	}

	@Embeddable
	static class SampleEmbeddable {

	}

	static class SampleEmbedded {

	}

	@Access(AccessType.PROPERTY)
	static class TypeLevelPropertyAccess {

		private String id;

		public String getId() {
			return id;
		}
	}

	static class PropertyLevelPropertyAccess {

		String field;
		String property;

		/**
		 * @return the property
		 */
		@org.springframework.data.annotation.AccessType(Type.PROPERTY)
		public String getProperty() {
			return property;
		}
	}

	@Access(AccessType.FIELD)
	static class PropertyLevelDefinitionTrumpsTypeLevelOne {

		String field;
		String property;

		/**
		 * @return the property
		 */
		@org.springframework.data.annotation.AccessType(Type.PROPERTY)
		public String getProperty() {
			return property;
		}
	}

	@org.springframework.data.annotation.AccessType(Type.PROPERTY)
	static class PropertyLevelDefinitionTrumpsTypeLevelOne2 {

		@Access(AccessType.FIELD) String field;
		String property;

		/**
		 * @return the property
		 */
		public String getProperty() {
			return property;
		}
	}

	@org.springframework.data.annotation.AccessType(Type.FIELD)
	@Access(AccessType.PROPERTY)
	static class CompetingTypeLevelAnnotations {

		private String id;

		public String getId() {
			return id;
		}
	}

	@org.springframework.data.annotation.AccessType(Type.FIELD)
	@Access(AccessType.PROPERTY)
	static class CompetingPropertyLevelAnnotations {

		private String id;

		@org.springframework.data.annotation.AccessType(Type.FIELD)
		@Access(AccessType.PROPERTY)
		public String getId() {
			return id;
		}
	}

	static class SpringDataVersioned {

		@Version long version;
	}

	static class JpaVersioned {

		@javax.persistence.Version long version;
	}

	static class SpecializedAssociation {

		@ManyToOne(targetEntity = Implementation.class) Api api;
	}

	static interface Api {}

	static class Implementation {}

	static class WithReadOnly {
		@Column(updatable = false) String name;
		String updatable;
	}
}
