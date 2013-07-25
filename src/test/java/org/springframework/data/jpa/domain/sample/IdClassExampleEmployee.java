package org.springframework.data.jpa.domain.sample;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;

@IdClass(IdClassExampleEmployeePK.class)
@Entity
public class IdClassExampleEmployee {

	@Id long empId;
	@Id @ManyToOne IdClassExampleDepartment department;

	public long getEmpId() {
		return empId;
	}

	public void setEmpId(long empId) {
		this.empId = empId;
	}

	public IdClassExampleDepartment getDepartment() {
		return department;
	}

	public void setDepartment(IdClassExampleDepartment department) {
		this.department = department;
	}
}
