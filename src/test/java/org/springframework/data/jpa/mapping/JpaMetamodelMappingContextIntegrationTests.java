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
package org.springframework.data.jpa.mapping;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration tests for {@link JpaMetamodelMappingContext}.
 * 
 * @author Oliver Gierke
 * @since 1.3
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:infrastructure.xml")
public class JpaMetamodelMappingContextIntegrationTests {

	JpaMetamodelMappingContext context;

	@PersistenceContext EntityManager em;

	@Before
	public void setUp() {
		context = new JpaMetamodelMappingContext(em.getMetamodel());
	}

	@Test
	public void setsUpMappingContextCorrectly() {

		JpaPersistentEntityImpl<?> entity = context.getPersistentEntity(User.class);
		assertThat(entity, is(notNullValue()));
	}

	@Test
	public void detectsIdProperty() {

		JpaPersistentEntityImpl<?> entity = context.getPersistentEntity(User.class);
		assertThat(entity.getIdProperty(), is(notNullValue()));
	}

	@Test
	public void detectsAssociation() {

		JpaPersistentEntityImpl<?> entity = context.getPersistentEntity(User.class);
		assertThat(entity, is(notNullValue()));

		JpaPersistentProperty property = entity.getPersistentProperty("manager");
		assertThat(property.isAssociation(), is(true));
	}

	@Test
	public void detectsPropertyIsEntity() {

		JpaPersistentEntityImpl<?> entity = context.getPersistentEntity(User.class);
		assertThat(entity, is(notNullValue()));

		JpaPersistentProperty property = entity.getPersistentProperty("manager");
		assertThat(property.isEntity(), is(true));

		property = entity.getPersistentProperty("lastname");
		assertThat(property.isEntity(), is(false));
	}

	/**
	 * @see DATAJPA-608
	 */
	@Test
	public void detectsEntityPropertyForCollections() {

		JpaPersistentEntityImpl<?> entity = context.getPersistentEntity(User.class);
		assertThat(entity, is(notNullValue()));

		assertThat(entity.getPersistentProperty("colleagues").isEntity(), is(true));
	}
}
