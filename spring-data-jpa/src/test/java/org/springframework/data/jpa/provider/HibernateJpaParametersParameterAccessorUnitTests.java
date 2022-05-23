package org.springframework.data.jpa.provider;

import jakarta.persistence.EntityManager;
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

import java.lang.reflect.Method;

@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:hjppa-test.xml")
public class HibernateJpaParametersParameterAccessorUnitTests {

	@Autowired
	private EntityManager em;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Test
	void withoutTransaction() throws NoSuchMethodException {
		simpleTest();
	}

	@Test
	void withinTransaction() throws Exception {
		final TransactionStatus tx = transactionManager.getTransaction(new DefaultTransactionDefinition());
		try {
			simpleTest();
		} finally {
			transactionManager.rollback(tx);
		}
	}

	private void simpleTest() throws NoSuchMethodException {
		final Method method = EntityManager.class.getMethod("flush");
		final JpaParameters parameters = new JpaParameters(method);
		final HibernateJpaParametersParameterAccessor accessor = new HibernateJpaParametersParameterAccessor(parameters, new Object[]{}, em);
		Assertions.assertEquals(0, accessor.getValues().length);
		Assertions.assertEquals(parameters, accessor.getParameters());
	}
}
