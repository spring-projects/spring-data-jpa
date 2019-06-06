/*
 * Copyright 2014-2019 the original author or authors.
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
package org.springframework.data.jpa.repository.query;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.annotation.AliasFor;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.repository.query.Param;
import org.springframework.util.ReflectionUtils;

import javax.persistence.EntityManager;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link StoredProcedureAttributeSource}.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Diego Diez
 * @author Jeff Sheets
 * @author Jens Schauder
 * @since 1.6
 */
@RunWith(MockitoJUnitRunner.class)
public class StoredProcedureAttributeSourceUnitTests {

	StoredProcedureAttributeSource creator;
	@Mock
	JpaEntityMetadata<User> entityMetadata;

	@Before
	public void setup() {

		creator = StoredProcedureAttributeSource.INSTANCE;

		when(entityMetadata.getJavaType()).thenReturn(User.class);
		when(entityMetadata.getEntityName()).thenReturn("User");
	}

	@Test // DATAJPA-455
	public void shouldCreateStoredProcedureAttributesFromProcedureMethodWithImplicitProcedureName() {

		StoredProcedureAttributes attr = creator.createFrom(method("plus1inout", Integer.class), entityMetadata);

		assertThat(attr.getProcedureName()).isEqualTo("plus1inout");
		assertThat(attr.getOutputParameterTypes().get(0)).isEqualTo(Integer.class);
		assertThat(attr.getOutputParameterNames().get(0)).isEqualTo(StoredProcedureAttributes.SYNTHETIC_OUTPUT_PARAMETER_NAME);
	}

	@Test // DATAJPA-455
	public void shouldCreateStoredProcedureAttributesFromProcedureMethodWithExplictName() {

		StoredProcedureAttributes attr = creator.createFrom(method("explicitlyNamedPlus1inout", Integer.class),
				entityMetadata);

		assertThat(attr.getProcedureName()).isEqualTo("plus1inout");
		assertThat(attr.getOutputParameterTypes().get(0)).isEqualTo(Integer.class);
		assertThat(attr.getOutputParameterNames().get(0)).isEqualTo(StoredProcedureAttributes.SYNTHETIC_OUTPUT_PARAMETER_NAME);
	}

	@Test // DATAJPA-455
	public void shouldCreateStoredProcedureAttributesFromProcedureMethodWithExplictProcedureNameValue() {

		StoredProcedureAttributes attr = creator.createFrom(method("explicitlyNamedPlus1inout", Integer.class),
				entityMetadata);

		assertThat(attr.getProcedureName()).isEqualTo("plus1inout");
		assertThat(attr.getOutputParameterTypes().get(0)).isEqualTo(Integer.class);
		assertThat(attr.getOutputParameterNames().get(0)).isEqualTo(StoredProcedureAttributes.SYNTHETIC_OUTPUT_PARAMETER_NAME);
	}

	@Test // DATAJPA-455
	public void shouldCreateStoredProcedureAttributesFromProcedureMethodWithExplictProcedureNameAlias() {

		StoredProcedureAttributes attr = creator
				.createFrom(method("explicitPlus1inoutViaProcedureNameAlias", Integer.class), entityMetadata);

		assertThat(attr.getProcedureName()).isEqualTo("plus1inout");
		assertThat(attr.getOutputParameterTypes().get(0)).isEqualTo(Integer.class);
		assertThat(attr.getOutputParameterNames().get(0)).isEqualTo(StoredProcedureAttributes.SYNTHETIC_OUTPUT_PARAMETER_NAME);
	}

	@Test // DATAJPA-1297
	public void shouldCreateStoredProcedureAttributesFromProcedureMethodWithExplictProcedureNameAliasAndOutputParameterName() {

		StoredProcedureAttributes attr = creator.createFrom(
				method("explicitPlus1inoutViaProcedureNameAliasAndOutputParameterName", Integer.class), entityMetadata);

		assertThat(attr.getProcedureName()).isEqualTo("plus1inout");
		assertThat(attr.getOutputParameterTypes().get(0)).isEqualTo(Integer.class);
		assertThat(attr.getOutputParameterNames().get(0)).isEqualTo("res");
	}

	@Test // DATAJPA-455
	public void shouldCreateStoredProcedureAttributesFromProcedureMethodBackedWithExplicitlyNamedProcedure() {

		StoredProcedureAttributes attr = creator
				.createFrom(method("entityAnnotatedCustomNamedProcedurePlus1IO", Integer.class), entityMetadata);

		assertThat(attr.getProcedureName()).isEqualTo("User.plus1IO");
		assertThat(attr.getOutputParameterTypes().get(0)).isEqualTo(Integer.class);
		assertThat(attr.getOutputParameterNames().get(0)).isEqualTo("res");
	}

	@Test // DATAJPA-707
	public void shouldCreateStoredProcedureAttributesFromProcedureMethodBackedWithExplicitlyNamedProcedureAndOutputParamName() {

		StoredProcedureAttributes attr = creator
				.createFrom(method("entityAnnotatedCustomNamedProcedureOutputParamNamePlus1IO", Integer.class), entityMetadata);

		assertThat(attr.getProcedureName()).isEqualTo("User.plus1IO");
		assertThat(attr.getOutputParameterTypes().get(0)).isEqualTo(Integer.class);
		assertThat(attr.getOutputParameterNames().get(0)).isEqualTo("override");
	}

	@Test // DATAJPA-707
	public void shouldCreateStoredProcedureAttributesFromProcedureMethodBackedWithExplicitlyNamedProcedureAnd2OutParams() {

		StoredProcedureAttributes attr = creator
				.createFrom(method("entityAnnotatedCustomNamedProcedurePlus1IO2", Integer.class), entityMetadata);

		assertThat(attr.getProcedureName()).isEqualTo("User.plus1IO2");
		assertThat(attr.getOutputParameterTypes().get(0)).isEqualTo(Integer.class);
		assertThat(attr.getOutputParameterNames().get(0)).isEqualTo("res");
		assertThat(attr.getOutputParameterTypes().get(1)).isEqualTo(Integer.class);
		assertThat(attr.getOutputParameterNames().get(1)).isEqualTo("res2");
	}

	@Test // DATAJPA-455
	public void shouldCreateStoredProcedureAttributesFromProcedureMethodBackedWithImplicitlyNamedProcedure() {

		StoredProcedureAttributes attr = creator.createFrom(method("plus1", Integer.class), entityMetadata);

		assertThat(attr.getProcedureName()).isEqualTo("User.plus1");
		assertThat(attr.getOutputParameterTypes().get(0)).isEqualTo(Integer.class);
		assertThat(attr.getOutputParameterNames().get(0)).isEqualTo("res");
	}

	@Test // DATAJPA-871
	public void aliasedStoredProcedure() {

		StoredProcedureAttributes attr = creator
				.createFrom(method("plus1inoutWithComposedAnnotationOverridingProcedureName", Integer.class), entityMetadata);

		assertThat(attr.getProcedureName()).isEqualTo("plus1inout");
		assertThat(attr.getOutputParameterTypes().get(0)).isEqualTo(Integer.class);
		assertThat(attr.getOutputParameterNames().get(0)).isEqualTo(StoredProcedureAttributes.SYNTHETIC_OUTPUT_PARAMETER_NAME);
	}

	@Test // DATAJPA-871
	public void aliasedStoredProcedure2() {

		StoredProcedureAttributes attr = creator
				.createFrom(method("plus1inoutWithComposedAnnotationOverridingName", Integer.class), entityMetadata);

		assertThat(attr.getProcedureName()).isEqualTo("User.plus1");
		assertThat(attr.getOutputParameterTypes().get(0)).isEqualTo(Integer.class);
		assertThat(attr.getOutputParameterNames().get(0)).isEqualTo("res");
	}

	private static Method method(String name, Class<?>... paramTypes) {
		return ReflectionUtils.findMethod(DummyRepository.class, name, paramTypes);
	}

	/**
	 * @author Thomas Darimont
	 */
	@SuppressWarnings("unused")
	interface DummyRepository {

		/**
		 * Explicitly mapped to a procedure with name "plus1inout" in database.
		 */
		@Procedure("plus1inout")
		// DATAJPA-455
		Integer explicitlyNamedPlus1inout(Integer arg);

		/**
		 * Explicitly mapped to a procedure with name "plus1inout" in database via alias.
		 */
		@Procedure(procedureName = "plus1inout")
		// DATAJPA-455
		Integer explicitPlus1inoutViaProcedureNameAlias(Integer arg);

		/**
		 * Explicitly mapped to a procedure with name "plus1inout" in database via alias and explicitly named ouput
		 * parameter.
		 */
		@Procedure(procedureName = "plus1inout", outputParameterName = "res")
		// DATAJPA-1297
		Integer explicitPlus1inoutViaProcedureNameAliasAndOutputParameterName(Integer arg);

		/**
		 * Implicitly mapped to a procedure with name "plus1inout" in database via alias.
		 */
		@Procedure
		// DATAJPA-455
		Integer plus1inout(Integer arg);

		/**
		 * Explicitly mapped to named stored procedure "User.plus1IO" in {@link EntityManager}.
		 */
		@Procedure(name = "User.plus1IO")
		// DATAJPA-455
		Integer entityAnnotatedCustomNamedProcedurePlus1IO(@Param("arg") Integer arg);

		/**
		 * Explicitly mapped to named stored procedure "User.plus1IO" in {@link EntityManager}.
		 * With a outputParameterName
		 */
		@Procedure(name = "User.plus1IO", outputParameterName = "override")
		// DATAJPA-707
		Integer entityAnnotatedCustomNamedProcedureOutputParamNamePlus1IO(@Param("arg") Integer arg);

		/**
		 * Explicitly mapped to named stored procedure "User.plus1IO2" in {@link EntityManager}.
		 */
		@Procedure(name = "User.plus1IO2")
		// DATAJPA-707
		Map<String, Integer> entityAnnotatedCustomNamedProcedurePlus1IO2(@Param("arg") Integer arg);

		/**
		 * Implicitly mapped to named stored procedure "User.plus1" in {@link EntityManager}.
		 */
		@Procedure
		// DATAJPA-455
		Integer plus1(@Param("arg") Integer arg);

		@ComposedProcedureUsingAliasFor(explicitProcedureName = "plus1inout")
		Integer plus1inoutWithComposedAnnotationOverridingProcedureName(Integer arg);

		@ComposedProcedureUsingAliasFor(emProcedureName = "User.plus1")
		Integer plus1inoutWithComposedAnnotationOverridingName(Integer arg);
	}

	@SuppressWarnings("unused")
	@Procedure
	@Retention(RetentionPolicy.RUNTIME)
	@interface ComposedProcedureUsingAliasFor {

		@AliasFor(annotation = Procedure.class, attribute = "value")
		String dbProcedureName() default "";

		@AliasFor(annotation = Procedure.class, attribute = "procedureName")
		String explicitProcedureName() default "";

		@AliasFor(annotation = Procedure.class, attribute = "name")
		String emProcedureName() default "";

		@AliasFor(annotation = Procedure.class, attribute = "outputParameterName")
		String outParamName() default "";
	}
}
