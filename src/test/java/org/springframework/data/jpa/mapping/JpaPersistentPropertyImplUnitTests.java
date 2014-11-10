/*
 * Copyright 2013-2014 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import javax.persistence.metamodel.Metamodel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.annotation.AccessType.Type;
import org.springframework.data.annotation.Version;

/**
 * Unit tests for {@link JpaPersistentPropertyImpl}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class JpaPersistentPropertyImplUnitTests {

	@Mock Metamodel model;

	JpaMetamodelMappingContext context;
	JpaPersistentEntity<?> entity;

	@Before
	public void setUp() {

		context = new JpaMetamodelMappingContext(model);
		entity = context.getPersistentEntity(Sample.class);
	}

	/**
	 * @see DATAJPA-284
	 */
	@Test
	public void considersOneToOneMappedPropertyAnAssociation() {

		JpaPersistentProperty property = entity.getPersistentProperty("other");
		assertThat(property.isAssociation(), is(true));
	}

	/**
	 * @see DATAJPA-376
	 */
	@Test
	public void considersJpaTransientFieldsAsTransient() {
		assertThat(entity.getPersistentProperty("transientProp"), is(nullValue()));
	}

	/**
	 * @see DATAJPA-484
	 */
	@Test
	public void considersEmbeddableAnEntity() {
		assertThat(context.getPersistentEntity(SampleEmbeddable.class), is(notNullValue()));
	}

	/**
	 * @see DATAJPA-484
	 */
	@Test
	public void considersEmbeddablePropertyAnAssociation() {
		assertThat(entity.getPersistentProperty("embeddable").isAssociation(), is(true));
	}

	/**
	 * @see DATAJPA-484
	 */
	@Test
	public void considersEmbeddedPropertyAnAssociation() {
		assertThat(entity.getPersistentProperty("embedded").isAssociation(), is(true));
	}

	/**
	 * @see DATAJPA-619
	 */
	@Test
	public void considersPropertyLevelAccessTypeDefinitions() {

		assertThat(getProperty(PropertyLevelPropertyAccess.class, "field").usePropertyAccess(), is(false));
		assertThat(getProperty(PropertyLevelPropertyAccess.class, "property").usePropertyAccess(), is(true));
	}

	/**
	 * @see DATAJPA-619
	 */
	@Test
	public void propertyLevelAccessTypeTrumpsTypeLevelDefinition() {

		assertThat(getProperty(PropertyLevelDefinitionTrumpsTypeLevelOne.class, "field").usePropertyAccess(), is(false));
		assertThat(getProperty(PropertyLevelDefinitionTrumpsTypeLevelOne.class, "property").usePropertyAccess(), is(true));

		assertThat(getProperty(PropertyLevelDefinitionTrumpsTypeLevelOne2.class, "field").usePropertyAccess(), is(false));
		assertThat(getProperty(PropertyLevelDefinitionTrumpsTypeLevelOne2.class, "property").usePropertyAccess(), is(true));
	}

	/**
	 * @see DATAJPA-619
	 */
	@Test
	public void considersJpaAccessDefinitionAnnotations() {
		assertThat(getProperty(TypeLevelPropertyAccess.class, "id").usePropertyAccess(), is(true));
	}

	/**
	 * @see DATAJPA-619
	 */
	@Test
	public void springDataAnnotationTrumpsJpaIfBothOnTypeLevel() {
		assertThat(getProperty(CompetingTypeLevelAnnotations.class, "id").usePropertyAccess(), is(false));
	}

	/**
	 * @see DATAJPA-619
	 */
	@Test
	public void springDataAnnotationTrumpsJpaIfBothOnPropertyLevel() {
		assertThat(getProperty(CompetingPropertyLevelAnnotations.class, "id").usePropertyAccess(), is(false));
	}

	/**
	 * @see DATAJPA-605
	 */
	@Test
	public void detectsJpaVersionAnnotation() {
		assertThat(getProperty(JpaVersioned.class, "version").isVersionProperty(), is(true));
	}

	/**
	 * @see DATAJPA-605
	 */
	@Test
	public void detectsSpringDataVersionAnnotation() {
		assertThat(getProperty(SpringDataVersioned.class, "version").isVersionProperty(), is(true));
	}

	private JpaPersistentProperty getProperty(Class<?> ownerType, String propertyName) {

		JpaPersistentEntity<?> entity = context.getPersistentEntity(ownerType);
		return entity.getPersistentProperty(propertyName);
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

		@Version long version;
	}
}
