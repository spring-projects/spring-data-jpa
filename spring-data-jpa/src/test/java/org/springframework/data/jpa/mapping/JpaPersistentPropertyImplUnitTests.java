/*
 * Copyright 2013-2023 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;

import java.util.Collections;

import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Association;
import org.jmolecules.ddd.types.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.annotation.AccessType.Type;
import org.springframework.data.annotation.Version;
import org.springframework.data.util.TypeInformation;

/**
 * Unit tests for {@link JpaPersistentPropertyImpl}.
 *
 * @author Oliver Gierke
 * @author Greg Turnquist
 * @author Jens Schauder
 * @author Erik Pellizzon
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JpaPersistentPropertyImplUnitTests {

	@Mock Metamodel model;

	private JpaMetamodelMappingContext context;
	private JpaPersistentEntity<?> entity;

	@BeforeEach
	void setUp() {

		context = new JpaMetamodelMappingContext(Collections.singleton(model));
		entity = context.getRequiredPersistentEntity(Sample.class);
	}

	@Test // DATAJPA-284
	void considersOneToOneMappedPropertyAnAssociation() {

		JpaPersistentProperty property = entity.getRequiredPersistentProperty("other");

		assertThat(property.isAssociation()).isTrue();
		assertThat(property.getAssociationTargetType()).isEqualTo(Sample.class);
	}

	@Test // DATAJPA-376
	void considersJpaTransientFieldsAsTransient() {
		assertThat(entity.getPersistentProperty("transientProp")).isNull();
	}

	@Test // DATAJPA-484
	void considersEmbeddableAnEntity() {
		assertThat(context.getPersistentEntity(SampleEmbeddable.class)).isNotNull();
	}

	@Test // DATAJPA-484
	void doesNotConsiderAnEmbeddablePropertyAnAssociation() {
		assertThat(entity.getRequiredPersistentProperty("embeddable").isAssociation()).isFalse();
	}

	@Test // DATAJPA-484
	void doesNotConsiderAnEmbeddedPropertyAnAssociation() {
		assertThat(entity.getRequiredPersistentProperty("embedded").isAssociation()).isFalse();
	}

	@Test // DATAJPA-619
	void considersPropertyLevelAccessTypeDefinitions() {

		assertThat(getProperty(PropertyLevelPropertyAccess.class, "field").usePropertyAccess()).isFalse();
		assertThat(getProperty(PropertyLevelPropertyAccess.class, "property").usePropertyAccess()).isTrue();
	}

	@Test // DATAJPA-619
	void propertyLevelAccessTypeTrumpsTypeLevelDefinition() {

		assertThat(getProperty(PropertyLevelDefinitionTrumpsTypeLevelOne.class, "field").usePropertyAccess()).isFalse();
		assertThat(getProperty(PropertyLevelDefinitionTrumpsTypeLevelOne.class, "property").usePropertyAccess()).isTrue();

		assertThat(getProperty(PropertyLevelDefinitionTrumpsTypeLevelOne2.class, "field").usePropertyAccess()).isFalse();
		assertThat(getProperty(PropertyLevelDefinitionTrumpsTypeLevelOne2.class, "property").usePropertyAccess()).isTrue();
	}

	@Test // DATAJPA-619
	void considersJpaAccessDefinitionAnnotations() {
		assertThat(getProperty(TypeLevelPropertyAccess.class, "id").usePropertyAccess()).isTrue();
	}

	@Test // DATAJPA-619
	void springDataAnnotationTrumpsJpaIfBothOnTypeLevel() {
		assertThat(getProperty(CompetingTypeLevelAnnotations.class, "id").usePropertyAccess()).isFalse();
	}

	@Test // DATAJPA-619
	void springDataAnnotationTrumpsJpaIfBothOnPropertyLevel() {
		assertThat(getProperty(CompetingPropertyLevelAnnotations.class, "id").usePropertyAccess()).isFalse();
	}

	@Test // DATAJPA-605
	void detectsJpaVersionAnnotation() {
		assertThat(getProperty(JpaVersioned.class, "version").isVersionProperty()).isTrue();
	}

	@Test // DATAJPA-664
	@SuppressWarnings("rawtypes")
	void considersTargetEntityTypeForPropertyType() {

		JpaPersistentProperty property = getProperty(SpecializedAssociation.class, "api");

		assertThat(property.getType()).isEqualTo(Api.class);
		assertThat(property.getActualType()).isEqualTo(Implementation.class);

		Iterable<? extends TypeInformation<?>> entityType = property.getPersistentEntityTypeInformation();
		assertThat(entityType.iterator().hasNext()).isTrue();
		assertThat(entityType.iterator().next()).isEqualTo(TypeInformation.of(Implementation.class));
	}

	@Test // DATAJPA-716
	void considersNonUpdateablePropertyNotWriteable() {
		assertThat(getProperty(WithReadOnly.class, "name").isWritable()).isFalse();
		assertThat(getProperty(WithReadOnly.class, "updatable").isWritable()).isTrue();
	}

	@Test // DATAJPA-904
	void isEntityWorksEvenWithManagedTypeWithNullJavaType() {

		ManagedType<?> managedType = mock(ManagedType.class);
		doReturn(Collections.singleton(managedType)).when(model).getManagedTypes();

		assertThat(getProperty(Sample.class, "other").isEntity()).isFalse();
	}

	@Test // DATAJPA-1064
	void simplePropertyIsNotConsideredAnAssociation() {

		JpaPersistentEntityImpl<?> entity = context.getRequiredPersistentEntity(WithReadOnly.class);
		JpaPersistentProperty property = entity.getRequiredPersistentProperty("updatable");

		assertThat(property.isAssociation()).isFalse();
	}

	@Test
	void detectsJMoleculesAssociation() {

		JpaPersistentEntityImpl<?> entity = context.getRequiredPersistentEntity(JMoleculesSample.class);
		JpaPersistentProperty property = entity.getRequiredPersistentProperty("association");

		assertThat(property.isAssociation()).isTrue();
		assertThat(property.getAssociationTargetType()).isEqualTo(JMoleculesAggregate.class);
	}

	private JpaPersistentProperty getProperty(Class<?> ownerType, String propertyName) {

		JpaPersistentEntity<?> entity = context.getRequiredPersistentEntity(ownerType);
		return entity.getRequiredPersistentProperty(propertyName);
	}

	static interface Api {}

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

	private static class SpringDataVersioned {

		@Version long version;
	}

	private static class JpaVersioned {

		@jakarta.persistence.Version long version;
	}

	private static class SpecializedAssociation {

		@ManyToOne(targetEntity = Implementation.class) Api api;
	}

	private static class Implementation {}

	private static class WithReadOnly {
		@Column(updatable = false) String name;
		String updatable;
	}

	// jMolecules

	private static class JMoleculesSample {
		Association<JMoleculesAggregate, Identifier> association;
	}

	private interface JMoleculesAggregate extends AggregateRoot<JMoleculesAggregate, Identifier> {}
}
