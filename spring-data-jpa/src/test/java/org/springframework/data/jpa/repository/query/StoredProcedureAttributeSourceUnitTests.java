/*
 * Copyright 2014-2025 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureParameter;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.annotation.AliasFor;
import org.springframework.data.jpa.domain.sample.Dummy;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.repository.query.Param;
import org.springframework.util.ReflectionUtils;

/**
 * Unit tests for {@link StoredProcedureAttributeSource}.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Diego Diez
 * @author Jeff Sheets
 * @author Jens Schauder
 * @author Gabriel Basilio
 * @author Greg Turnquist
 * @since 1.6
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StoredProcedureAttributeSourceUnitTests {

	private StoredProcedureAttributeSource creator;
	@Mock JpaEntityMetadata<?> entityMetadata;

	@BeforeEach
	void setup() {

		creator = StoredProcedureAttributeSource.INSTANCE;

		doReturn(User.class).when(entityMetadata).getJavaType();
		when(entityMetadata.getEntityName()).thenReturn("User");
	}

	@Test // DATAJPA-455
	void shouldCreateStoredProcedureAttributesFromProcedureMethodWithImplicitProcedureName() {

		StoredProcedureAttributes attr = creator.createFrom(method("plus1inout", Integer.class), entityMetadata);

		ProcedureParameter outputParameter = attr.getOutputProcedureParameters().get(0);

		assertThat(attr.getProcedureName()).isEqualTo("plus1inout");
		assertThat(outputParameter.getMode()).isEqualTo(ParameterMode.OUT);
		assertThat(outputParameter.getType()).isEqualTo(Integer.class);
		assertThat(outputParameter.getName()).isEqualTo(StoredProcedureAttributes.SYNTHETIC_OUTPUT_PARAMETER_NAME);
	}

	@Test // DATAJPA-455
	void shouldCreateStoredProcedureAttributesFromProcedureMethodWithExplictName() {

		StoredProcedureAttributes attr = creator.createFrom(method("explicitlyNamedPlus1inout", Integer.class),
				entityMetadata);

		ProcedureParameter outputParameter = attr.getOutputProcedureParameters().get(0);

		assertThat(attr.getProcedureName()).isEqualTo("plus1inout");
		assertThat(outputParameter.getMode()).isEqualTo(ParameterMode.OUT);
		assertThat(outputParameter.getType()).isEqualTo(Integer.class);
		assertThat(outputParameter.getName()).isEqualTo(StoredProcedureAttributes.SYNTHETIC_OUTPUT_PARAMETER_NAME);
	}

	@Test // DATAJPA-455
	void shouldCreateStoredProcedureAttributesFromProcedureMethodWithExplictProcedureNameValue() {

		StoredProcedureAttributes attr = creator.createFrom(method("explicitlyNamedPlus1inout", Integer.class),
				entityMetadata);

		ProcedureParameter outputParameter = attr.getOutputProcedureParameters().get(0);

		assertThat(attr.getProcedureName()).isEqualTo("plus1inout");
		assertThat(outputParameter.getMode()).isEqualTo(ParameterMode.OUT);
		assertThat(outputParameter.getType()).isEqualTo(Integer.class);
		assertThat(outputParameter.getName()).isEqualTo(StoredProcedureAttributes.SYNTHETIC_OUTPUT_PARAMETER_NAME);
	}

	@Test // DATAJPA-455
	void shouldCreateStoredProcedureAttributesFromProcedureMethodWithExplictProcedureNameAlias() {

		StoredProcedureAttributes attr = creator
				.createFrom(method("explicitPlus1inoutViaProcedureNameAlias", Integer.class), entityMetadata);

		ProcedureParameter outputParameter = attr.getOutputProcedureParameters().get(0);

		assertThat(attr.getProcedureName()).isEqualTo("plus1inout");
		assertThat(outputParameter.getMode()).isEqualTo(ParameterMode.OUT);
		assertThat(outputParameter.getType()).isEqualTo(Integer.class);
		assertThat(outputParameter.getName()).isEqualTo(StoredProcedureAttributes.SYNTHETIC_OUTPUT_PARAMETER_NAME);
	}

	@Test // DATAJPA-1297
	void shouldCreateStoredProcedureAttributesFromProcedureMethodWithExplictProcedureNameAliasAndOutputParameterName() {

		StoredProcedureAttributes attr = creator.createFrom(
				method("explicitPlus1inoutViaProcedureNameAliasAndOutputParameterName", Integer.class), entityMetadata);

		ProcedureParameter outputParameter = attr.getOutputProcedureParameters().get(0);

		assertThat(attr.getProcedureName()).isEqualTo("plus1inout");
		assertThat(outputParameter.getMode()).isEqualTo(ParameterMode.OUT);
		assertThat(outputParameter.getType()).isEqualTo(Integer.class);
		assertThat(outputParameter.getName()).isEqualTo("res");
	}

	@Test // DATAJPA-455
	void shouldCreateStoredProcedureAttributesFromProcedureMethodBackedWithExplicitlyNamedProcedure() {

		StoredProcedureAttributes attr = creator
				.createFrom(method("entityAnnotatedCustomNamedProcedurePlus1IO", Integer.class), entityMetadata);

		ProcedureParameter outputParameter = attr.getOutputProcedureParameters().get(0);

		assertThat(attr.getProcedureName()).isEqualTo("User.plus1IO");
		assertThat(outputParameter.getMode()).isEqualTo(ParameterMode.OUT);
		assertThat(outputParameter.getType()).isEqualTo(Integer.class);
		assertThat(outputParameter.getName()).isEqualTo("res");
	}

	@Test // DATAJPA-707
	void shouldCreateStoredProcedureAttributesFromProcedureMethodBackedWithExplicitlyNamedProcedureAndOutputParamName() {

		StoredProcedureAttributes attr = creator
				.createFrom(method("entityAnnotatedCustomNamedProcedureOutputParamNamePlus1IO", Integer.class), entityMetadata);

		ProcedureParameter outputParameter = attr.getOutputProcedureParameters().get(0);

		assertThat(attr.getProcedureName()).isEqualTo("User.plus1IO");
		assertThat(outputParameter.getMode()).isEqualTo(ParameterMode.OUT);
		assertThat(outputParameter.getType()).isEqualTo(Integer.class);
		assertThat(outputParameter.getName()).isEqualTo("override");
	}

	@Test // DATAJPA-707
	void shouldCreateStoredProcedureAttributesFromProcedureMethodBackedWithExplicitlyNamedProcedureAnd2OutParams() {

		StoredProcedureAttributes attr = creator
				.createFrom(method("entityAnnotatedCustomNamedProcedurePlus1IO2", Integer.class), entityMetadata);

		ProcedureParameter firstOutputParameter = attr.getOutputProcedureParameters().get(0);
		ProcedureParameter secondOutputParameter = attr.getOutputProcedureParameters().get(1);

		assertThat(attr.getProcedureName()).isEqualTo("User.plus1IO2");

		assertThat(firstOutputParameter.getMode()).isEqualTo(ParameterMode.OUT);
		assertThat(firstOutputParameter.getType()).isEqualTo(Integer.class);
		assertThat(firstOutputParameter.getName()).isEqualTo("res");

		assertThat(secondOutputParameter.getMode()).isEqualTo(ParameterMode.OUT);
		assertThat(secondOutputParameter.getType()).isEqualTo(Integer.class);
		assertThat(secondOutputParameter.getName()).isEqualTo("res2");
	}

	@Test // DATAJPA-455
	void shouldCreateStoredProcedureAttributesFromProcedureMethodBackedWithImplicitlyNamedProcedure() {

		StoredProcedureAttributes attr = creator.createFrom(method("plus1", Integer.class), entityMetadata);

		ProcedureParameter outputParameter = attr.getOutputProcedureParameters().get(0);

		assertThat(attr.getProcedureName()).isEqualTo("User.plus1");
		assertThat(outputParameter.getMode()).isEqualTo(ParameterMode.OUT);
		assertThat(outputParameter.getType()).isEqualTo(Integer.class);
		assertThat(outputParameter.getName()).isEqualTo("res");
	}

	@Test // DATAJPA-871
	void aliasedStoredProcedure() {

		StoredProcedureAttributes attr = creator
				.createFrom(method("plus1inoutWithComposedAnnotationOverridingProcedureName", Integer.class), entityMetadata);

		ProcedureParameter outputParameter = attr.getOutputProcedureParameters().get(0);

		assertThat(attr.getProcedureName()).isEqualTo("plus1inout");
		assertThat(outputParameter.getMode()).isEqualTo(ParameterMode.OUT);
		assertThat(outputParameter.getType()).isEqualTo(Integer.class);
		assertThat(outputParameter.getName()).isEqualTo(StoredProcedureAttributes.SYNTHETIC_OUTPUT_PARAMETER_NAME);
	}

	@Test // DATAJPA-871
	void aliasedStoredProcedure2() {

		StoredProcedureAttributes attr = creator
				.createFrom(method("plus1inoutWithComposedAnnotationOverridingName", Integer.class), entityMetadata);

		ProcedureParameter outputParameter = attr.getOutputProcedureParameters().get(0);

		assertThat(attr.getProcedureName()).isEqualTo("User.plus1");
		assertThat(outputParameter.getMode()).isEqualTo(ParameterMode.OUT);
		assertThat(outputParameter.getType()).isEqualTo(Integer.class);
		assertThat(outputParameter.getName()).isEqualTo("res");
	}

	@Test // DATAJPA-1657
	public void testSingleEntityFrom1RowResultSetAndNoInput() {

		StoredProcedureAttributes attr = creator.createFrom(method("singleEntityFrom1RowResultSetAndNoInput"),
				entityMetadata);

		ProcedureParameter outputParameter = attr.getOutputProcedureParameters().get(0);

		assertThat(attr.getProcedureName()).isEqualTo("0_input_1_row_resultset");
		assertThat(outputParameter.getMode()).isEqualTo(ParameterMode.OUT);
		assertThat(outputParameter.getType()).isEqualTo(Dummy.class);
		assertThat(outputParameter.getName()).isEqualTo(StoredProcedureAttributes.SYNTHETIC_OUTPUT_PARAMETER_NAME);
	}

	@Test // DATAJPA-1657
	public void testSingleEntityFrom1RowResultSetWithInput() {

		StoredProcedureAttributes attr = creator.createFrom(method("singleEntityFrom1RowResultSetWithInput", Integer.class),
				entityMetadata);

		ProcedureParameter outputParameter = attr.getOutputProcedureParameters().get(0);

		assertThat(attr.getProcedureName()).isEqualTo("1_input_1_row_resultset");
		assertThat(outputParameter.getMode()).isEqualTo(ParameterMode.OUT);
		assertThat(outputParameter.getType()).isEqualTo(Dummy.class);
		assertThat(outputParameter.getName()).isEqualTo(StoredProcedureAttributes.SYNTHETIC_OUTPUT_PARAMETER_NAME);
	}

	@Test // DATAJPA-1657
	public void testEntityListFromResultSetWithNoInput() {

		StoredProcedureAttributes attr = creator.createFrom(method("entityListFromResultSetWithNoInput"), entityMetadata);

		ProcedureParameter outputParameter = attr.getOutputProcedureParameters().get(0);

		assertThat(attr.getProcedureName()).isEqualTo("0_input_1_resultset");
		assertThat(outputParameter.getMode()).isEqualTo(ParameterMode.OUT);
		assertThat(outputParameter.getType()).isEqualTo(List.class);
		assertThat(outputParameter.getName()).isEqualTo(StoredProcedureAttributes.SYNTHETIC_OUTPUT_PARAMETER_NAME);
	}

	//
	@Test // DATAJPA-1657
	public void testEntityListFromResultSetWithInput() {

		StoredProcedureAttributes attr = creator.createFrom(method("entityListFromResultSetWithInput", Integer.class),
				entityMetadata);

		ProcedureParameter outputParameter = attr.getOutputProcedureParameters().get(0);

		assertThat(attr.getProcedureName()).isEqualTo("1_input_1_resultset");
		assertThat(outputParameter.getMode()).isEqualTo(ParameterMode.OUT);
		assertThat(outputParameter.getType()).isEqualTo(List.class);
		assertThat(outputParameter.getName()).isEqualTo(StoredProcedureAttributes.SYNTHETIC_OUTPUT_PARAMETER_NAME);
	}

	@Test // DATAJPA-1657
	public void testGenericObjectListFromResultSetWithInput() {

		StoredProcedureAttributes attr = creator
				.createFrom(method("genericObjectListFromResultSetWithInput", Integer.class), entityMetadata);

		ProcedureParameter outputParameter = attr.getOutputProcedureParameters().get(0);

		assertThat(attr.getProcedureName()).isEqualTo("1_input_1_resultset");
		assertThat(outputParameter.getMode()).isEqualTo(ParameterMode.OUT);
		assertThat(outputParameter.getType()).isEqualTo(List.class);
		assertThat(outputParameter.getName()).isEqualTo(StoredProcedureAttributes.SYNTHETIC_OUTPUT_PARAMETER_NAME);
	}

	@Test // DATAJPA-1657
	public void testEntityListFromResultSetWithInputAndNamedOutput() {

		StoredProcedureAttributes attr = creator
				.createFrom(method("entityListFromResultSetWithInputAndNamedOutput", Integer.class), entityMetadata);

		ProcedureParameter outputParameter = attr.getOutputProcedureParameters().get(0);

		assertThat(attr.getProcedureName()).isEqualTo("1_input_1_resultset");
		assertThat(outputParameter.getMode()).isEqualTo(ParameterMode.OUT);
		assertThat(outputParameter.getType()).isEqualTo(List.class);
		assertThat(outputParameter.getName()).isEqualTo("dummies");
	}

	@Test // DATAJPA-1657
	public void testEntityListFromResultSetWithInputAndNamedOutputAndCursor() {

		StoredProcedureAttributes attr = creator
				.createFrom(method("entityListFromResultSetWithInputAndNamedOutputAndCursor", Integer.class), entityMetadata);

		ProcedureParameter outputParameter = attr.getOutputProcedureParameters().get(0);

		assertThat(attr.getProcedureName()).isEqualTo("1_input_1_resultset");
		assertThat(outputParameter.getMode()).isEqualTo(ParameterMode.REF_CURSOR);
		assertThat(outputParameter.getType()).isEqualTo(List.class);
		assertThat(outputParameter.getName()).isEqualTo("dummies");
	}

	@ParameterizedTest // GH-3463
	@ValueSource(
			strings = { "inInOut", "inoutInOut", "inoutOut", "outInIn", "inInInoutOut", "inInoutInOut", "inInoutInoutOut" })
	void storedProcedureParameterInoutAndOutParameterPositionDetection(String methodName) {

		String[] paramPattern = methodName.split("(?=[A-Z])");
		Class<?>[] methodArgs = new Class<?>[paramPattern.length - 1];
		Arrays.fill(methodArgs, Integer.class);

		List<Integer> expectedOut = new ArrayList<>(2);
		int position = 0;
		for (String s : paramPattern) {
			position++;
			switch (s.toLowerCase()) {
				case "inout", "out" -> expectedOut.add(position);
			}
		}

		doReturn(InOut.class).when(entityMetadata).getJavaType();
		when(entityMetadata.getEntityName()).thenReturn("InOut");

		StoredProcedureAttributes attr = creator.createFrom(method(methodName, methodArgs), entityMetadata);
		assertThat(attr.getOutputProcedureParameters()).extracting(ProcedureParameter::getPosition) //
				.withFailMessage("Expecting method %s to have %s out parameters at positions %s but was %s.", methodName,
						expectedOut.size(), expectedOut, attr.getOutputProcedureParameters()) //
				.isEqualTo(expectedOut);
	}

	private static Method method(String name, Class<?>... paramTypes) {
		return ReflectionUtils.findMethod(DummyRepository.class, name, paramTypes);
	}

	/**
	 * @author Thomas Darimont
	 */
	@SuppressWarnings("unused")
	private interface DummyRepository {

		/**
		 * Explicitly mapped to a procedure with name "plus1inout" in database.
		 */
		@Procedure("plus1inout") // DATAJPA-455
		Integer explicitlyNamedPlus1inout(Integer arg);

		/**
		 * Explicitly mapped to a procedure with name "plus1inout" in database via alias.
		 */
		@Procedure(procedureName = "plus1inout") // DATAJPA-455
		Integer explicitPlus1inoutViaProcedureNameAlias(Integer arg);

		/**
		 * Explicitly mapped to a procedure with name "plus1inout" in database via alias and explicitly named output
		 * parameter.
		 */
		@Procedure(procedureName = "plus1inout", outputParameterName = "res") // DATAJPA-1297
		Integer explicitPlus1inoutViaProcedureNameAliasAndOutputParameterName(Integer arg);

		/**
		 * Implicitly mapped to a procedure with name "plus1inout" in database via alias.
		 */
		@Procedure // DATAJPA-455
		Integer plus1inout(Integer arg);

		/**
		 * Explicitly mapped to named stored procedure "User.plus1IO" in {@link EntityManager}.
		 */
		@Procedure(name = "User.plus1IO") // DATAJPA-455
		Integer entityAnnotatedCustomNamedProcedurePlus1IO(@Param("arg") Integer arg);

		/**
		 * Explicitly mapped to named stored procedure "User.plus1IO" in {@link EntityManager} with an outputParameterName.
		 */
		@Procedure(name = "User.plus1IO", outputParameterName = "override") // DATAJPA-707
		Integer entityAnnotatedCustomNamedProcedureOutputParamNamePlus1IO(@Param("arg") Integer arg);

		/**
		 * Explicitly mapped to named stored procedure "User.plus1IO2" in {@link EntityManager}.
		 */
		@Procedure(name = "User.plus1IO2") // DATAJPA-707
		Map<String, Integer> entityAnnotatedCustomNamedProcedurePlus1IO2(@Param("arg") Integer arg);

		/**
		 * Implicitly mapped to named stored procedure "User.plus1" in {@link EntityManager}.
		 */
		@Procedure // DATAJPA-455
		Integer plus1(@Param("arg") Integer arg);

		@ComposedProcedureUsingAliasFor(explicitProcedureName = "plus1inout")
		Integer plus1inoutWithComposedAnnotationOverridingProcedureName(Integer arg);

		@ComposedProcedureUsingAliasFor(emProcedureName = "User.plus1")
		Integer plus1inoutWithComposedAnnotationOverridingName(Integer arg);

		@Procedure("0_input_1_row_resultset") // DATAJPA-1657
		Dummy singleEntityFrom1RowResultSetAndNoInput();

		@Procedure("1_input_1_row_resultset") // DATAJPA-1657
		Dummy singleEntityFrom1RowResultSetWithInput(Integer arg);

		@Procedure("0_input_1_resultset") // DATAJPA-1657
		List<Dummy> entityListFromResultSetWithNoInput();

		@Procedure("1_input_1_resultset") // DATAJPA-1657
		List<Dummy> entityListFromResultSetWithInput(Integer arg);

		@Procedure("1_input_1_resultset") // DATAJPA-1657
		List<Object[]> genericObjectListFromResultSetWithInput(Integer arg);

		@Procedure(value = "1_input_1_resultset", outputParameterName = "dummies") // DATAJPA-1657
		List<Dummy> entityListFromResultSetWithInputAndNamedOutput(Integer arg);

		@Procedure(value = "1_input_1_resultset", outputParameterName = "dummies", refCursor = true) // DATAJPA-1657
		List<Dummy> entityListFromResultSetWithInputAndNamedOutputAndCursor(Integer arg);

		@Procedure(name = "InOut.in_in_out")
		Map<Object, Object> inInOut(Integer in1, Integer in2);

		@Procedure(name = "InOut.inout_in_out")
		Map<Object, Object> inoutInOut(Integer inout1, Integer in2);

		@Procedure(name = "InOut.inout_out")
		Map<Object, Object> inoutOut(Integer inout1);

		@Procedure(name = "InOut.out_in_in")
		Map<Object, Object> outInIn(Integer in1, Integer in2);

		@Procedure(name = "InOut.in_in_inout_out")
		Map<Object, Object> inInInoutOut(Integer in1, Integer in2, Integer inout);

		@Procedure(name = "InOut.in_inout_in_out")
		Map<Object, Object> inInoutInOut(Integer in1, Integer inout, Integer in2);

		@Procedure(name = "InOut.in_inout_inout_out")
		Map<Object, Object> inInoutInoutOut(Integer in1, Integer inout1, Integer inout2);
	}

	@SuppressWarnings("unused")
	@Procedure
	@Retention(RetentionPolicy.RUNTIME)
	private @interface ComposedProcedureUsingAliasFor {

		@AliasFor(annotation = Procedure.class, attribute = "value")
		String dbProcedureName() default "";

		@AliasFor(annotation = Procedure.class, attribute = "procedureName")
		String explicitProcedureName() default "";

		@AliasFor(annotation = Procedure.class, attribute = "name")
		String emProcedureName() default "";

		@AliasFor(annotation = Procedure.class, attribute = "outputParameterName")
		String outParamName() default "";
	}

	@NamedStoredProcedureQuery( //
			name = "InOut.in_in_out", //
			procedureName = "positional_in_in_out", //
			parameters = { @StoredProcedureParameter(mode = ParameterMode.IN, type = Integer.class),
					@StoredProcedureParameter(mode = ParameterMode.IN, type = Integer.class),
					@StoredProcedureParameter(mode = ParameterMode.OUT, type = Integer.class) })
	@NamedStoredProcedureQuery( //
			name = "InOut.inout_in_out", //
			procedureName = "positional_inout_in_out", //
			parameters = { @StoredProcedureParameter(mode = ParameterMode.INOUT, type = Integer.class),
					@StoredProcedureParameter(mode = ParameterMode.IN, type = Integer.class),
					@StoredProcedureParameter(mode = ParameterMode.OUT, type = Integer.class) })
	@NamedStoredProcedureQuery( //
			name = "InOut.inout_out", //
			procedureName = "positional_inout_out", //
			parameters = { @StoredProcedureParameter(mode = ParameterMode.INOUT, type = Integer.class),
					@StoredProcedureParameter(mode = ParameterMode.OUT, type = Integer.class) })
	@NamedStoredProcedureQuery( //
			name = "InOut.out_in_in", //
			procedureName = "positional_out_in_in", //
			parameters = { @StoredProcedureParameter(mode = ParameterMode.OUT, type = Integer.class),
					@StoredProcedureParameter(mode = ParameterMode.IN, type = Integer.class),
					@StoredProcedureParameter(mode = ParameterMode.IN, type = Integer.class) })
	@NamedStoredProcedureQuery( //
			name = "InOut.in_in_inout_out", //
			procedureName = "positional_in_in_inout_out", //
			parameters = { @StoredProcedureParameter(mode = ParameterMode.IN, type = Integer.class),
					@StoredProcedureParameter(mode = ParameterMode.IN, type = Integer.class),
					@StoredProcedureParameter(mode = ParameterMode.INOUT, type = Integer.class),
					@StoredProcedureParameter(mode = ParameterMode.OUT, type = Integer.class) })
	@NamedStoredProcedureQuery( //
			name = "InOut.in_inout_in_out", //
			procedureName = "positional_in_inout_in_out", //
			parameters = { @StoredProcedureParameter(mode = ParameterMode.IN, type = Integer.class),
					@StoredProcedureParameter(mode = ParameterMode.INOUT, type = Integer.class),
					@StoredProcedureParameter(mode = ParameterMode.IN, type = Integer.class),
					@StoredProcedureParameter(mode = ParameterMode.OUT, type = Integer.class) })
	@NamedStoredProcedureQuery( //
			name = "InOut.in_inout_inout_out", //
			procedureName = "positional_in_inout_inout_out", //
			parameters = { @StoredProcedureParameter(mode = ParameterMode.IN, type = Integer.class),
					@StoredProcedureParameter(mode = ParameterMode.INOUT, type = Integer.class),
					@StoredProcedureParameter(mode = ParameterMode.INOUT, type = Integer.class),
					@StoredProcedureParameter(mode = ParameterMode.OUT, type = Integer.class) })
	private static class InOut {
		@Id private Long id;
	}
}
