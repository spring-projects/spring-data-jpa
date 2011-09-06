package org.springframework.data.jpa.repository.query;

import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.QueryHint;
import javax.persistence.TypedQuery;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.jpa.repository.support.PersistenceProvider;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration test for {@link AbstractJpaQuery}.
 * 
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:infrastructure.xml")
public class AbstractJpaQueryTests {

	@PersistenceContext
	EntityManager em;

	Query query;
	TypedQuery<Long> countQuery;

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() {
		query = mock(Query.class);
		countQuery = mock(TypedQuery.class);
	}

	/**
	 * @see DATADOC-97
	 * @throws Exception
	 */
	@Test
	public void addsHintsToQueryObject() throws Exception {

		Method method = SampleRepository.class.getMethod("findByLastname", String.class);
		QueryExtractor provider = PersistenceProvider.fromEntityManager(em);
		JpaQueryMethod queryMethod = new JpaQueryMethod(method, new DefaultRepositoryMetadata(SampleRepository.class),
				provider);

		AbstractJpaQuery jpaQuery = new DummyJpaQuery(queryMethod, em);

		Query result = jpaQuery.createQuery(new Object[] { "Matthews" });
		verify(result).setHint("foo", "bar");

		result = jpaQuery.createCountQuery(new Object[] { "Matthews" });
		verify(result).setHint("foo", "bar");
	}

	interface SampleRepository extends Repository<User, Integer> {

		@QueryHints({ @QueryHint(name = "foo", value = "bar") })
		List<User> findByLastname(String lastname);
	}

	class DummyJpaQuery extends AbstractJpaQuery {

		public DummyJpaQuery(JpaQueryMethod method, EntityManager em) {
			super(method, em);
		}

		@Override
		protected Query doCreateQuery(Object[] values) {
			return query;
		}

		@Override
		protected TypedQuery<Long> doCreateCountQuery(Object[] values) {
			return (TypedQuery<Long>) countQuery;
		}
	}
}
