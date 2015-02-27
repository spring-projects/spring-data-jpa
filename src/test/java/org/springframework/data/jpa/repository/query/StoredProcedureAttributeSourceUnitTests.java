/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.jpa.repository.query;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.object.IsCompatibleType.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.repository.query.Param;
import org.springframework.util.ReflectionUtils;

/**
 * Unit tests for {@link StoredProcedureAttributeSource}.
 * 
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @since 1.6
 */
@RunWith(MockitoJUnitRunner.class)
public class StoredProcedureAttributeSourceUnitTests {

	StoredProcedureAttributeSource creator;
	@Mock JpaEntityMetadata<User> entityMetadata;

	@Before
	public void setup() {

		creator = StoredProcedureAttributeSource.INSTANCE;

		when(entityMetadata.getJavaType()).thenReturn(User.class);
		when(entityMetadata.getEntityName()).thenReturn("User");
	}

	/**
	 * @see DATAJPA-455
	 */
	@Test
	public void shouldCreateStoredProcedureAttributesFromProcedureMethodWithImplicitProcedureName() {

		StoredProcedureAttributes attr = creator.createFrom(method("plus1inout", Integer.class), entityMetadata);

		assertThat(attr.getProcedureName(), is("plus1inout"));
		assertThat(attr.getOutputParameterType(), is(typeCompatibleWith(Integer.class)));
		assertThat(attr.getOutputParameterName(), is(StoredProcedureAttributes.SYNTHETIC_OUTPUT_PARAMETER_NAME));
	}

	/**
	 * @see DATAJPA-455
	 */
	@Test
	public void shouldCreateStoredProcedureAttributesFromProcedureMethodWithExplictName() {

		StoredProcedureAttributes attr = creator.createFrom(method("explicitlyNamedPlus1inout", Integer.class),
				entityMetadata);

		assertThat(attr.getProcedureName(), is("plus1inout"));
		assertThat(attr.getOutputParameterType(), is(typeCompatibleWith(Integer.class)));
		assertThat(attr.getOutputParameterName(), is(StoredProcedureAttributes.SYNTHETIC_OUTPUT_PARAMETER_NAME));
	}

	/**
	 * @see DATAJPA-455
	 */
	@Test
	public void shouldCreateStoredProcedureAttributesFromProcedureMethodWithExplictProcedureNameValue() {

		StoredProcedureAttributes attr = creator.createFrom(method("explicitlyNamedPlus1inout", Integer.class),
				entityMetadata);

		assertThat(attr.getProcedureName(), is("plus1inout"));
		assertThat(attr.getOutputParameterType(), is(typeCompatibleWith(Integer.class)));
		assertThat(attr.getOutputParameterName(), is(StoredProcedureAttributes.SYNTHETIC_OUTPUT_PARAMETER_NAME));
	}

	/**
	 * @see DATAJPA-455
	 */
	@Test
	public void shouldCreateStoredProcedureAttributesFromProcedureMethodWithExplictProcedureNameAlias() {

		StoredProcedureAttributes attr = creator.createFrom(
				method("explicitPlus1inoutViaProcedureNameAlias", Integer.class), entityMetadata);

		assertThat(attr.getProcedureName(), is("plus1inout"));
		assertThat(attr.getOutputParameterType(), is(typeCompatibleWith(Integer.class)));
		assertThat(attr.getOutputParameterName(), is(StoredProcedureAttributes.SYNTHETIC_OUTPUT_PARAMETER_NAME));
	}

	/**
	 * @see DATAJPA-455
	 */
	@Test
	public void shouldCreateStoredProcedureAttributesFromProcedureMethodBackedWithExplicitlyNamedProcedure() {

		StoredProcedureAttributes attr = creator.createFrom(
				method("entityAnnotatedCustomNamedProcedurePlus1IO", Integer.class), entityMetadata);

		assertThat(attr.getProcedureName(), is("User.plus1IO"));
		assertThat(attr.getOutputParameterType(), is(typeCompatibleWith(Integer.class)));
		assertThat(attr.getOutputParameterName(), is("res"));
	}

	/**
	 * @see DATAJPA-455
	 */
	@Test
	public void shouldCreateStoredProcedureAttributesFromProcedureMethodBackedWithImplicitlyNamedProcedure() {

		StoredProcedureAttributes attr = creator.createFrom(method("plus1", Integer.class), entityMetadata);

		assertThat(attr.getProcedureName(), is("User.plus1"));
		assertThat(attr.getOutputParameterType(), is(typeCompatibleWith(Integer.class)));
		assertThat(attr.getOutputParameterName(), is("res"));
	}

	private static Method method(String name, Class<?>... paramTypes) {
		return ReflectionUtils.findMethod(DummyRepository.class, name, paramTypes);
	}

	/**
	 * @author Thomas Darimont
	 */
	static interface DummyRepository {

		/**
		 * Explicitly mapped to a procedure with name "plus1inout" in database.
		 * 
		 * @see DATAJPA-455
		 */
		@Procedure("plus1inout")
		Integer explicitlyNamedPlus1inout(Integer arg);

		/**
		 * Explicitly mapped to a procedure with name "plus1inout" in database via alias.
		 * 
		 * @see DATAJPA-455
		 */
		@Procedure(procedureName = "plus1inout")
		Integer explicitPlus1inoutViaProcedureNameAlias(Integer arg);

		/**
		 * Implicitly mapped to a procedure with name "plus1inout" in database via alias.
		 * 
		 * @see DATAJPA-455
		 */
		@Procedure
		Integer plus1inout(Integer arg);

		/**
		 * Explicitly mapped to named stored procedure "User.plus1IO" in {@link EntityManager}.
		 * 
		 * @see DATAJPA-455
		 */
		@Procedure(name = "User.plus1IO")
		Integer entityAnnotatedCustomNamedProcedurePlus1IO(@Param("arg") Integer arg);

		/**
		 * Implicitly mapped to named stored procedure "User.plus1" in {@link EntityManager}.
		 * 
		 * @see DATAJPA-455
		 */
		@Procedure
		Integer plus1(@Param("arg") Integer arg);
	}
}
