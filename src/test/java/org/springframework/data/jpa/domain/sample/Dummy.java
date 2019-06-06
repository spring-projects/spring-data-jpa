/*
 * Copyright 2015-2019 the original author or authors.
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
package org.springframework.data.jpa.domain.sample;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedStoredProcedureQueries;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureParameter;

import org.springframework.util.ObjectUtils;

/**
 * Sample domain class used for Stored Procedure tests.
 *
 * @author Thomas Darimont
 */
@Entity
@NamedStoredProcedureQueries({ //
		@NamedStoredProcedureQuery(name = "Dummy.procedureWith1InputAnd1OutputParameter",
				procedureName = "procedure_in1_out1", parameters = {
						@StoredProcedureParameter(mode = ParameterMode.IN, type = Integer.class),
						@StoredProcedureParameter(mode = ParameterMode.OUT, type = Integer.class) }) //
		,
		@NamedStoredProcedureQuery(name = "Dummy.procedureWith1InputAndNoOutputParameter",
				procedureName = "procedure_in1_out0", parameters = { @StoredProcedureParameter(mode = ParameterMode.IN,
						type = Integer.class) }) //
		,
		@NamedStoredProcedureQuery(name = "Dummy.procedureWithNoInputAnd1OutputParameter",
				procedureName = "procedure_in0_out1", parameters = { @StoredProcedureParameter(mode = ParameterMode.OUT,
						type = Integer.class) }) //
		,
		@NamedStoredProcedureQuery(name = "Dummy.procedureWith1InputAnd1OutputParameterWithResultSet",
				procedureName = "procedure_in1_out0_return_rs_no_update", parameters = {
						@StoredProcedureParameter(mode = ParameterMode.IN, type = String.class),
						@StoredProcedureParameter(mode = ParameterMode.REF_CURSOR, type = void.class) }) //
		,
		@NamedStoredProcedureQuery(name = "Dummy.procedureWith1InputAnd1OutputParameterWithResultSetWithUpdate",
				procedureName = "procedure_in1_out0_return_rs_with_update", parameters = {
						@StoredProcedureParameter(mode = ParameterMode.IN, type = String.class),
						@StoredProcedureParameter(mode = ParameterMode.REF_CURSOR, type = void.class) }) //
		,
		@NamedStoredProcedureQuery(name = "Dummy.procedureWith1InputAndNoOutputParameterWithUpdate",
				procedureName = "procedure_in1_out0_no_return_with_update", parameters = { @StoredProcedureParameter(
						mode = ParameterMode.IN, type = String.class) }) //
})
public class Dummy {

	@Id @GeneratedValue private Integer id;
	private String name;

	public Dummy() {}

	public Dummy(String name) {
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Dummy [id=" + id + ", name=" + name + "]";
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(name);
	}

	@Override
	public boolean equals(Object that) {

		if (that == this) {
			return true;
		}

		if (that == null) {
			return false;
		}

		if (!(that instanceof Dummy)) {
			return false;
		}

		return ObjectUtils.nullSafeEquals(this.name, ((Dummy) that).name);
	}
}
