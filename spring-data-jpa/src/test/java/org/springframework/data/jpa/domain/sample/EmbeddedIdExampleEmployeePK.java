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

import java.io.Serial;
import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * @author Thomas Darimont
 */
@Embeddable
public class EmbeddedIdExampleEmployeePK implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	@Column(nullable = false) private Long employeeId;

	@Column(nullable = false) private Long departmentId;

	public EmbeddedIdExampleEmployeePK() {}

	public EmbeddedIdExampleEmployeePK(Long employeeId, Long departmentId) {
		this.employeeId = employeeId;
		this.departmentId = departmentId;
	}

	public Long getEmployeeId() {
		return this.employeeId;
	}

	public void setEmployeeId(Long employeeId) {
		this.employeeId = employeeId;
	}

	public Long getDepartmentId() {
		return this.departmentId;
	}

	public void setDepartmentId(Long departmentId) {
		this.departmentId = departmentId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (departmentId ^ (departmentId >>> 32));
		result = prime * result + (int) (employeeId ^ (employeeId >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EmbeddedIdExampleEmployeePK other = (EmbeddedIdExampleEmployeePK) obj;
		if (departmentId != other.departmentId)
			return false;
		if (employeeId != other.employeeId)
			return false;
		return true;
	}
}
