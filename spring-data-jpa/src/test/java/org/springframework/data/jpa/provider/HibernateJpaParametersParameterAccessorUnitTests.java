package org.springframework.data.jpa.provider;

import jakarta.persistence.EntityManager;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.query.JpaParameters;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Unit tests for {@link HibernateJpaParametersParameterAccessor}.
 *
 * @author Cedomir Igaly
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:hjppa-test.xml")
class HibernateJpaParametersParameterAccessorUnitTests {

	@Autowired private EntityManager em;

	@Autowired private PlatformTransactionManager transactionManager;

	@Test
	void withoutTransaction() throws NoSuchMethodException {
		parametersCanGetAccessesOutsideTransaction();
	}

	@Test
	void withinTransaction() throws Exception {

		TransactionStatus tx = transactionManager.getTransaction(new DefaultTransactionDefinition());
		try {
			parametersCanGetAccessesOutsideTransaction();
		} finally {
			transactionManager.rollback(tx);
		}
	}

	private void parametersCanGetAccessesOutsideTransaction() throws NoSuchMethodException {

		Method method = EntityManager.class.getMethod("flush");
		JpaParameters parameters = new JpaParameters(method);
		HibernateJpaParametersParameterAccessor accessor = new HibernateJpaParametersParameterAccessor(parameters,
				new Object[] {}, em);
		Assertions.assertEquals(0, accessor.getValues().length);
		Assertions.assertEquals(parameters, accessor.getParameters());
	}
}
