/*
 * Copyright 2015-2018 the original author or authors.
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
package org.springframework.data.jpa.repository.sample;

import java.util.List;

import org.springframework.data.jpa.domain.sample.Dummy;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface DummyRepository extends CrudRepository<Dummy, Long> {

	@Procedure("procedure_in1_out1")
	Integer adHocProcedureWith1InputAnd1OutputParameter(Integer in);

	@Procedure("procedure_in1_out0")
	void adHocProcedureWith1InputAndNoOutputParameter(Integer in);

	@Procedure("procedure_in0_out1")
	Integer adHocProcedureWithNoInputAnd1OutputParameter();

	@Procedure("procedure_in1_out0_return_rs_no_update")
	List<Dummy> adHocProcedureWith1InputAnd1OutputParameterWithResultSet(String in);

	@Procedure("procedure_in1_out0_return_rs_with_update")
	List<Dummy> adHocProcedureWith1InputAnd1OutputParameterWithResultSetWithUpdate(String in);

	@Procedure("procedure_in1_out0_no_return_with_update")
	void adHocProcedureWith1InputAndNoOutputParameterWithUpdate(String in);

	@Procedure
	Integer procedureWith1InputAnd1OutputParameter(Integer in);

	@Procedure
	void procedureWith1InputAndNoOutputParameter(Integer in);

	@Procedure
	Integer procedureWithNoInputAnd1OutputParameter();

	@Procedure
	List<Dummy> procedureWith1InputAnd1OutputParameterWithResultSet(String in);

	@Procedure
	List<Dummy> procedureWith1InputAnd1OutputParameterWithResultSetWithUpdate(String in);

	@Procedure
	void procedureWith1InputAndNoOutputParameterWithUpdate(String in);
}
