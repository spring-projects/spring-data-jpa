/*
 * Copyright 2013-2019 the original author or authors.
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

import java.io.Serializable;

/**
 * @author Thomas Darimont
 */
public class IdClassExampleEmployeePK implements Serializable {
	private static final long serialVersionUID = 1L;

	private long empId;
	private long department;

	public IdClassExampleEmployeePK() {}

	public IdClassExampleEmployeePK(long empId, long department) {
		this.empId = empId;
		this.department = department;
	}

	public long getEmpId() {
		return this.empId;
	}

	public void setEmpId(long empId) {
		this.empId = empId;
	}

	public long getDepartment() {
		return this.department;
	}

	public void setDepartment(long department) {
		this.department = department;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (department ^ (department >>> 32));
		result = prime * result + (int) (empId ^ (empId >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		IdClassExampleEmployeePK other = (IdClassExampleEmployeePK) obj;
		if (department != other.department) {
			return false;
		}
		if (empId != other.empId) {
			return false;
		}
		return true;
	}
}
