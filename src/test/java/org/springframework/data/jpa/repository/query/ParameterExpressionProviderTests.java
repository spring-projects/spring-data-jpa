package org.springframework.data.jpa.repository.query;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.lang.reflect.Method;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.ParameterExpression;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.repository.query.DefaultParameters;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration tests for {@link ParameterMetadataProvider}.
 * 
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:infrastructure.xml")
public class ParameterExpressionProviderTests {

	@PersistenceContext EntityManager em;

	/**
	 * @see DATADOC-99
	 */
	@Test
	@SuppressWarnings("rawtypes")
	public void createsParameterExpressionWithMostConcreteType() throws Exception {

		Method method = SampleRepository.class.getMethod("findByIdGreaterThan", int.class);
		Parameters<?, ?> parameters = new DefaultParameters(method);
		ParametersParameterAccessor accessor = new ParametersParameterAccessor(parameters, new Object[] { 1 });
		Part part = new Part("IdGreaterThan", User.class);

		CriteriaBuilder builder = em.getCriteriaBuilder();
		PersistenceProvider persistenceProvider = PersistenceProvider.fromEntityManager(em);
		ParameterMetadataProvider provider = new ParameterMetadataProvider(builder, accessor, persistenceProvider);
		ParameterExpression<? extends Comparable> expression = provider.next(part, Comparable.class).getExpression();
		assertThat(expression.getParameterType(), is(typeCompatibleWith(int.class)));
	}

	interface SampleRepository {

		User findByIdGreaterThan(int id);
	}
}
