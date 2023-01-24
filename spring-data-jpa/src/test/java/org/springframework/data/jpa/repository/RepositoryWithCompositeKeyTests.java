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
package org.springframework.data.jpa.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.sample.EmbeddedIdExampleDepartment;
import org.springframework.data.jpa.domain.sample.EmbeddedIdExampleEmployee;
import org.springframework.data.jpa.domain.sample.EmbeddedIdExampleEmployeePK;
import org.springframework.data.jpa.domain.sample.IdClassExampleDepartment;
import org.springframework.data.jpa.domain.sample.IdClassExampleEmployee;
import org.springframework.data.jpa.domain.sample.IdClassExampleEmployeePK;
import org.springframework.data.jpa.domain.sample.QEmbeddedIdExampleEmployee;
import org.springframework.data.jpa.domain.sample.QIdClassExampleEmployee;
import org.springframework.data.jpa.repository.sample.EmployeeRepositoryWithEmbeddedId;
import org.springframework.data.jpa.repository.sample.EmployeeRepositoryWithIdClass;
import org.springframework.data.jpa.repository.sample.SampleConfig;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests some usage variants of composite keys with spring data jpa.
 *
 * @author Thomas Darimont
 * @author Mark Paluch
 * @author Jens Schauder
 * @author Ernst-Jan van der Laan
 * @author Krzysztof Krason
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SampleConfig.class)
@Transactional
class RepositoryWithCompositeKeyTests {

	@Autowired EmployeeRepositoryWithIdClass employeeRepositoryWithIdClass;
	@Autowired EmployeeRepositoryWithEmbeddedId employeeRepositoryWithEmbeddedId;
	@Autowired EntityManager em;

	/**
	 * @see <a href="download.oracle.com/otn-pub/jcp/persistence-2_1-fr-eval-spec/JavaPersistence.pdf">Final JPA 2.0
	 *      Specification 2.4.1.3 Derived Identities Example 2</a>
	 */
	@Test // DATAJPA-269
	void shouldSupportSavingEntitiesWithCompositeKeyClassesWithIdClassAndDerivedIdentities() {

		IdClassExampleDepartment dep = new IdClassExampleDepartment();
		dep.setName("TestDepartment");
		dep.setDepartmentId(-1);

		IdClassExampleEmployee emp = new IdClassExampleEmployee();
		emp.setDepartment(dep);

		employeeRepositoryWithIdClass.save(emp);

		IdClassExampleEmployeePK key = new IdClassExampleEmployeePK();
		key.setDepartment(dep.getDepartmentId());
		key.setEmpId(emp.getEmpId());
		IdClassExampleEmployee persistedEmp = employeeRepositoryWithIdClass.findById(key).get();

		assertThat(persistedEmp).isNotNull();
		assertThat(persistedEmp.getDepartment()).isNotNull();
		assertThat(persistedEmp.getDepartment().getName()).isEqualTo(dep.getName());
	}

	/**
	 * @see <a href="download.oracle.com/otn-pub/jcp/persistence-2_1-fr-eval-spec/JavaPersistence.pdf">Final JPA 2.0
	 *      Specification 2.4.1.3 Derived Identities Example 3</a>
	 */
	@Test // DATAJPA-269
	void shouldSupportSavingEntitiesWithCompositeKeyClassesWithEmbeddedIdsAndDerivedIdentities() {

		EmbeddedIdExampleDepartment dep = new EmbeddedIdExampleDepartment();
		dep.setName("TestDepartment");
		dep.setDepartmentId(-1L);

		EmbeddedIdExampleEmployee emp = new EmbeddedIdExampleEmployee();
		emp.setDepartment(dep);
		emp.setEmployeePk(new EmbeddedIdExampleEmployeePK());

		emp = employeeRepositoryWithEmbeddedId.save(emp);

		EmbeddedIdExampleEmployeePK key = new EmbeddedIdExampleEmployeePK();
		key.setDepartmentId(emp.getDepartment().getDepartmentId());
		key.setEmployeeId(emp.getEmployeePk().getEmployeeId());
		EmbeddedIdExampleEmployee persistedEmp = employeeRepositoryWithEmbeddedId.findById(key).get();

		assertThat(persistedEmp).isNotNull();
		assertThat(persistedEmp.getDepartment()).isNotNull();
		assertThat(persistedEmp.getDepartment().getName()).isEqualTo(dep.getName());
	}

	@Test // DATAJPA-472, DATAJPA-912
	void shouldSupportFindAllWithPageableAndEntityWithIdClass() {

		IdClassExampleDepartment dep = new IdClassExampleDepartment();
		dep.setName("TestDepartment");
		dep.setDepartmentId(-1);

		IdClassExampleEmployee emp = new IdClassExampleEmployee();
		emp.setDepartment(dep);
		employeeRepositoryWithIdClass.save(emp);

		Page<IdClassExampleEmployee> page = employeeRepositoryWithIdClass.findAll(PageRequest.of(0, 1));

		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isOne();
	}

	@Test // DATAJPA-2414
	void shouldSupportDeleteAllByIdInBatchWithIdClass() {

		IdClassExampleDepartment dep = new IdClassExampleDepartment();
		dep.setName("TestDepartment");
		dep.setDepartmentId(-1);

		IdClassExampleEmployee emp = new IdClassExampleEmployee();
		emp.setDepartment(dep);
		emp = employeeRepositoryWithIdClass.save(emp);

		IdClassExampleEmployeePK key = new IdClassExampleEmployeePK(emp.getEmpId(), dep.getDepartmentId());
		assertThat(employeeRepositoryWithIdClass.findById(key)).isNotEmpty();

		employeeRepositoryWithIdClass.deleteAllByIdInBatch(Collections.singletonList(key));

		em.flush();
		em.clear();

		assertThat(employeeRepositoryWithIdClass.findById(key)).isEmpty();
	}

	@Test // DATAJPA-497
	void sortByEmbeddedPkFieldInCompositePkWithEmbeddedIdInQueryDsl() {

		EmbeddedIdExampleDepartment dep1 = new EmbeddedIdExampleDepartment();
		dep1.setDepartmentId(1L);
		dep1.setName("Dep1");

		EmbeddedIdExampleDepartment dep2 = new EmbeddedIdExampleDepartment();
		dep2.setDepartmentId(2L);
		dep2.setName("Dep2");

		EmbeddedIdExampleEmployee emp1 = new EmbeddedIdExampleEmployee();
		emp1.setEmployeePk(new EmbeddedIdExampleEmployeePK(3L, null));
		emp1.setDepartment(dep2);
		emp1 = employeeRepositoryWithEmbeddedId.save(emp1);

		EmbeddedIdExampleEmployee emp2 = new EmbeddedIdExampleEmployee();
		emp2.setEmployeePk(new EmbeddedIdExampleEmployeePK(2L, null));
		emp2.setDepartment(dep1);
		employeeRepositoryWithEmbeddedId.save(emp2);

		EmbeddedIdExampleEmployee emp3 = new EmbeddedIdExampleEmployee();
		emp3.setEmployeePk(new EmbeddedIdExampleEmployeePK(1L, null));
		emp3.setDepartment(dep2);
		emp3 = employeeRepositoryWithEmbeddedId.save(emp3);

		QEmbeddedIdExampleEmployee emp = QEmbeddedIdExampleEmployee.embeddedIdExampleEmployee;
		List<EmbeddedIdExampleEmployee> result = employeeRepositoryWithEmbeddedId
				.findAll(emp.employeePk.departmentId.eq(dep2.getDepartmentId()), emp.employeePk.employeeId.asc());

		assertThat(result).isNotNull();
		assertThat(result).hasSize(2);
		assertThat(result.get(0)).isEqualTo(emp3);
		assertThat(result.get(1)).isEqualTo(emp1);
	}

	@Test // DATAJPA-497
	void sortByEmbeddedPkFieldInCompositePkWithIdClassInQueryDsl() {

		IdClassExampleDepartment dep1 = new IdClassExampleDepartment();
		dep1.setDepartmentId(1L);
		dep1.setName("Dep1");

		IdClassExampleDepartment dep2 = new IdClassExampleDepartment();
		dep2.setDepartmentId(2L);
		dep2.setName("Dep2");

		IdClassExampleEmployee emp1 = new IdClassExampleEmployee();
		emp1.setEmpId(3L);
		emp1.setDepartment(dep2);
		emp1 = employeeRepositoryWithIdClass.save(emp1);

		IdClassExampleEmployee emp2 = new IdClassExampleEmployee();
		emp2.setEmpId(2L);
		emp2.setDepartment(dep1);
		employeeRepositoryWithIdClass.save(emp2);

		IdClassExampleEmployee emp3 = new IdClassExampleEmployee();
		emp3.setEmpId(1L);
		emp3.setDepartment(dep2);
		emp3 = employeeRepositoryWithIdClass.save(emp3);

		QIdClassExampleEmployee emp = QIdClassExampleEmployee.idClassExampleEmployee;
		List<IdClassExampleEmployee> result = employeeRepositoryWithIdClass
				.findAll(emp.department.departmentId.eq(dep2.getDepartmentId()), emp.empId.asc());

		assertThat(result).isNotNull();
		assertThat(result).hasSize(2);
		assertThat(result.get(0)).isEqualTo(emp3);
		assertThat(result.get(1)).isEqualTo(emp1);
	}

	@Test // DATAJPA-527, DATAJPA-1148
	void testExistsWithIdClass() {

		IdClassExampleDepartment dep = new IdClassExampleDepartment();
		dep.setName("TestDepartment");
		dep.setDepartmentId(-1);

		IdClassExampleEmployee emp = new IdClassExampleEmployee();
		emp.setDepartment(dep);

		employeeRepositoryWithIdClass.save(emp);

		IdClassExampleEmployeePK key = new IdClassExampleEmployeePK();
		key.setDepartment(dep.getDepartmentId());
		key.setEmpId(emp.getEmpId());

		assertThat(employeeRepositoryWithIdClass.existsById(key)).isTrue();
		assertThat(employeeRepositoryWithIdClass.existsById(new IdClassExampleEmployeePK(0L, 0L))).isFalse();
	}

	@Test // DATAJPA-527
	void testExistsWithEmbeddedId() {

		EmbeddedIdExampleDepartment dep1 = new EmbeddedIdExampleDepartment();
		dep1.setDepartmentId(1L);
		dep1.setName("Dep1");

		EmbeddedIdExampleEmployeePK key = new EmbeddedIdExampleEmployeePK();
		key.setDepartmentId(1L);
		key.setEmployeeId(1L);

		EmbeddedIdExampleEmployee emp = new EmbeddedIdExampleEmployee();
		emp.setDepartment(dep1);
		emp.setEmployeePk(key);

		emp = employeeRepositoryWithEmbeddedId.save(emp);

		key.setDepartmentId(emp.getDepartment().getDepartmentId());
		key.setEmployeeId(emp.getEmployeePk().getEmployeeId());

		assertThat(employeeRepositoryWithEmbeddedId.existsById(key)).isTrue();
	}

	@Test // DATAJPA-611
	void shouldAllowFindAllWithIdsForEntitiesWithCompoundIdClassKeys() {

		IdClassExampleDepartment dep2 = new IdClassExampleDepartment();
		dep2.setDepartmentId(2L);
		dep2.setName("Dep2");

		IdClassExampleEmployee emp1 = new IdClassExampleEmployee();
		emp1.setEmpId(3L);
		emp1.setDepartment(dep2);
		employeeRepositoryWithIdClass.save(emp1);

		IdClassExampleDepartment dep1 = new IdClassExampleDepartment();
		dep1.setDepartmentId(1L);
		dep1.setName("Dep1");

		IdClassExampleEmployee emp2 = new IdClassExampleEmployee();
		emp2.setEmpId(2L);
		emp2.setDepartment(dep1);
		employeeRepositoryWithIdClass.save(emp2);

		IdClassExampleEmployeePK emp1PK = new IdClassExampleEmployeePK();
		emp1PK.setDepartment(2L);
		emp1PK.setEmpId(3L);

		IdClassExampleEmployeePK emp2PK = new IdClassExampleEmployeePK();
		emp2PK.setDepartment(1L);
		emp2PK.setEmpId(2L);

		List<IdClassExampleEmployee> result = employeeRepositoryWithIdClass.findAllById(Arrays.asList(emp1PK, emp2PK));

		assertThat(result).hasSize(2);
	}

	@Test // DATAJPA-920
	void shouldExecuteExistsQueryForEntitiesWithEmbeddedId() {

		EmbeddedIdExampleDepartment dep1 = new EmbeddedIdExampleDepartment();
		dep1.setDepartmentId(1L);
		dep1.setName("Dep1");

		EmbeddedIdExampleEmployeePK key = new EmbeddedIdExampleEmployeePK();
		key.setDepartmentId(1L);
		key.setEmployeeId(1L);

		EmbeddedIdExampleEmployee emp = new EmbeddedIdExampleEmployee();
		emp.setDepartment(dep1);
		emp.setEmployeePk(key);
		emp.setName("White");

		employeeRepositoryWithEmbeddedId.save(emp);

		assertThat(employeeRepositoryWithEmbeddedId.existsByName(emp.getName())).isTrue();
	}

	@Test // DATAJPA-920
	void shouldExecuteExistsQueryForEntitiesWithCompoundIdClassKeys() {

		IdClassExampleDepartment dep2 = new IdClassExampleDepartment();
		dep2.setDepartmentId(2L);
		dep2.setName("Dep2");

		IdClassExampleEmployee emp1 = new IdClassExampleEmployee();
		emp1.setEmpId(3L);
		emp1.setDepartment(dep2);
		emp1.setName("White");

		employeeRepositoryWithIdClass.save(emp1);

		assertThat(employeeRepositoryWithIdClass.existsByName(emp1.getName())).isTrue();
		assertThat(employeeRepositoryWithIdClass.existsByName("Walter")).isFalse();
	}
}
