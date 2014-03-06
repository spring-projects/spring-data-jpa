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
package org.springframework.data.jpa.repository;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.sample.EmbeddedIdExampleDepartment;
import org.springframework.data.jpa.domain.sample.EmbeddedIdExampleEmployee;
import org.springframework.data.jpa.domain.sample.EmbeddedIdExampleEmployeePK;
import org.springframework.data.jpa.domain.sample.IdClassExampleDepartment;
import org.springframework.data.jpa.domain.sample.IdClassExampleEmployee;
import org.springframework.data.jpa.domain.sample.IdClassExampleEmployeePK;
import org.springframework.data.jpa.repository.sample.EmployeeRepositoryWithEmbeddedId;
import org.springframework.data.jpa.repository.sample.EmployeeRepositoryWithIdClass;
import org.springframework.data.jpa.repository.sample.SampleConfig;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests some usage variants of composite keys with spring data jpa.
 * 
 * @author Thomas Darimont
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SampleConfig.class)
@Transactional
public class RepositoryWithCompositeKeyTests {

	@Rule public ExpectedException expectedException = ExpectedException.none();

	@Autowired EmployeeRepositoryWithIdClass employeeRepositoryWithIdClass;
	@Autowired EmployeeRepositoryWithEmbeddedId employeeRepositoryWithEmbeddedId;

	/**
	 * @see DATAJPA-269
	 * @see Final JPA 2.0 Specification 2.4.1.3 Derived Identities Example 2
	 */
	@Test
	public void shouldSupportSavingEntitiesWithCompositeKeyClassesWithIdClassAndDerivedIdentities() {

		IdClassExampleDepartment dep = new IdClassExampleDepartment();
		dep.setName("TestDepartment");
		dep.setDepartmentId(-1);

		IdClassExampleEmployee emp = new IdClassExampleEmployee();
		emp.setDepartment(dep);

		employeeRepositoryWithIdClass.save(emp);

		IdClassExampleEmployeePK key = new IdClassExampleEmployeePK();
		key.setDepartment(dep.getDepartmentId());
		key.setEmpId(emp.getEmpId());
		IdClassExampleEmployee persistedEmp = employeeRepositoryWithIdClass.findOne(key);

		assertThat(persistedEmp, is(notNullValue()));
		assertThat(persistedEmp.getDepartment(), is(notNullValue()));
		assertThat(persistedEmp.getDepartment().getName(), is(dep.getName()));
	}

	/**
	 * @see DATAJPA-269
	 * @see Final JPA 2.0 Specification 2.4.1.3 Derived Identities Example 3
	 */
	@Test
	public void shouldSupportSavingEntitiesWithCompositeKeyClassesWithEmbeddedIdsAndDerivedIdentities() {

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
		EmbeddedIdExampleEmployee persistedEmp = employeeRepositoryWithEmbeddedId.findOne(key);

		assertThat(persistedEmp, is(notNullValue()));
		assertThat(persistedEmp.getDepartment(), is(notNullValue()));
		assertThat(persistedEmp.getDepartment().getName(), is(dep.getName()));
	}

	/**
	 * @see DATAJPA-472
	 */
	@Test
	public void shouldSupportFindAllWithPageableAndEntityWithIdClass() throws Exception {

		if (Package.getPackage("org.hibernate.cfg").getImplementationVersion().startsWith("4.1.")) {

			// we expect this test to fail on 4.1.x - due to a bug in hibernate - remove as soon as 4.1.x fixes the issue.
			expectedException.expect(InvalidDataAccessApiUsageException.class);
			expectedException.expectMessage("No supertype found");
		}

		IdClassExampleDepartment dep = new IdClassExampleDepartment();
		dep.setName("TestDepartment");
		dep.setDepartmentId(-1);

		IdClassExampleEmployee emp = new IdClassExampleEmployee();
		emp.setDepartment(dep);
		emp = employeeRepositoryWithIdClass.save(emp);

		Page<IdClassExampleEmployee> page = employeeRepositoryWithIdClass.findAll(new PageRequest(0, 10));

		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(1L));
	}
}
