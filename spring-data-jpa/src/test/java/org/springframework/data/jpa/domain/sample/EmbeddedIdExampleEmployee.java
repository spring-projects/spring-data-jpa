/*
 * Copyright 2013-2025 the original author or authors.
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

import jakarta.persistence.CascadeType;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;

/**
 * @author Thomas Darimont
 * @author Mark Paluch
 */
@Entity
public class EmbeddedIdExampleEmployee {

	@EmbeddedId private EmbeddedIdExampleEmployeePK employeePk;

	@MapsId("departmentId")//
	@ManyToOne(cascade = CascadeType.ALL) private//
	EmbeddedIdExampleDepartment department;

	private String name;

	public EmbeddedIdExampleEmployeePK getEmployeePk() {
		return employeePk;
	}

	public void setEmployeePk(EmbeddedIdExampleEmployeePK employeePk) {
		this.employeePk = employeePk;
	}

	public EmbeddedIdExampleDepartment getDepartment() {
		return department;
	}

	public void setDepartment(EmbeddedIdExampleDepartment department) {
		this.department = department;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
