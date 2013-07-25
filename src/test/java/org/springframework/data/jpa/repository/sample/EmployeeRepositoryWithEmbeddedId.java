package org.springframework.data.jpa.repository.sample;

import org.springframework.data.jpa.domain.sample.EmbeddedIdExampleEmployee;
import org.springframework.data.jpa.domain.sample.EmbeddedIdExampleEmployeePK;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Demonstrates the support for composite primary keys with {@code @IdClass}
 * 
 * @author Thomas Darimont
 */
public interface EmployeeRepositoryWithEmbeddedId extends
		JpaRepository<EmbeddedIdExampleEmployee, EmbeddedIdExampleEmployeePK> {}
