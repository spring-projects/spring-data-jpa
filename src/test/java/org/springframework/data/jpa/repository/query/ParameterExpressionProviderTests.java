package org.springframework.data.jpa.repository.query;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.lang.reflect.Method;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.ParameterExpression;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.query.JpaQueryCreator.ParameterExpressionProvider;
import org.springframework.data.repository.query.Parameters;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration tests for {@link ParameterExpressionProvider}.
 * 
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:infrastructure.xml")
public class ParameterExpressionProviderTests {

	@PersistenceContext
	EntityManager em;

	/**
	 * @see DATADOC-99
	 */
	@Test
	@SuppressWarnings("rawtypes")
	public void createsParameterExpressionWithMostConcreteType() throws Exception {

		Method method = SampleRepository.class.getMethod("findByIdGreaterThan", int.class);
		Parameters parameters = new Parameters(method);

		ParameterExpressionProvider provider = new ParameterExpressionProvider(em.getCriteriaBuilder(), parameters);
		ParameterExpression<? extends Comparable> expression = provider.next(Comparable.class);
		assertThat(expression.getParameterType(), is(typeCompatibleWith(int.class)));
	}

	interface SampleRepository {

		User findByIdGreaterThan(int id);
	}
}
