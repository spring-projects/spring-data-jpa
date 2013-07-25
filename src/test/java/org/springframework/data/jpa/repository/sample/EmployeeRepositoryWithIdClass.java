package org.springframework.data.jpa.repository.sample;

import org.springframework.data.jpa.domain.sample.IdClassExampleEmployee;
import org.springframework.data.jpa.domain.sample.IdClassExampleEmployeePK;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Demonstrates the support for composite primary keys with {@code @IdClass}
 * 
 * @author Thomas Darimont
 */
public interface EmployeeRepositoryWithIdClass extends JpaRepository<IdClassExampleEmployee, IdClassExampleEmployeePK> {}
