/*
 * Copyright 2013-2015 the original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import javax.persistence.metamodel.Metamodel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

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
	 * @see DATAJPA-664
	 */
	@Test
	public void considersTargetEntityTypeForPropertyType() {

		JpaPersistentProperty property = getProperty(SpecializedAssociation.class, "api");

		assertThat(property.getType(), is(typeCompatibleWith(Api.class)));
		assertThat(property.getActualType(), is(typeCompatibleWith(Implementation.class)));
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

	// DATAJPA-664

	static class SpecializedAssociation {

		@ManyToOne(targetEntity = Implementation.class) Api api;
	}

	static interface Api {}

	static class Implementation {}
}
