/*
 * Copyright 2011-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License
import org.springframework.aop.framework.Advised;
");
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
package org.springframework.data.jpa.repository.query;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.util.ReflectionTestUtils.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TemporalType;

import org.hibernate.Version;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.provider.HibernateUtils;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.Temporal;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.Param;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

/**
 * Integration tests for {@link PartTreeJpaQuery}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Michael Cramer
 * @author Jens Schauder
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:infrastructure.xml")
public class PartTreeJpaQueryIntegrationTests {

	private static String PROPERTY = "h.target." + getQueryProperty();

	@Rule public ExpectedException thrown = ExpectedException.none();

	@PersistenceContext EntityManager entityManager;

	PersistenceProvider provider;

	@Before
	public void setUp() {
		this.provider = PersistenceProvider.fromEntityManager(entityManager);
	}

	@Test // DATADOC-90
	public void test() throws Exception {

		JpaQueryMethod queryMethod = getQueryMethod("findByFirstname", String.class, Pageable.class);
		PartTreeJpaQuery jpaQuery = new PartTreeJpaQuery(queryMethod, entityManager, provider, EscapeCharacter.DEFAULT);

		jpaQuery.createQuery(new Object[] { "Matthews", PageRequest.of(0, 1) });
		jpaQuery.createQuery(new Object[] { "Matthews", PageRequest.of(0, 1) });
	}

	@Test
	public void cannotIgnoreCaseIfNotString() throws Exception {

		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Unable to ignore case of java.lang.Integer types, the property 'id' must reference a String");
		testIgnoreCase("findByIdIgnoringCase", 3);
	}

	@Test
	public void cannotIgnoreCaseIfNotStringUnlessIgnoringAll() throws Exception {

		testIgnoreCase("findByIdAllIgnoringCase", 3);
	}

	@Test // DATAJPA-121
	public void recreatesQueryIfNullValueIsGiven() throws Exception {

		JpaQueryMethod queryMethod = getQueryMethod("findByFirstname", String.class, Pageable.class);
		PartTreeJpaQuery jpaQuery = new PartTreeJpaQuery(queryMethod, entityManager, provider, EscapeCharacter.DEFAULT);

		Query query = jpaQuery.createQuery(new Object[] { "Matthews", PageRequest.of(0, 1) });

		assertThat(HibernateUtils.getHibernateQuery(getValue(query, PROPERTY))).endsWith("firstname=:param0");

		query = jpaQuery.createQuery(new Object[] { null, PageRequest.of(0, 1) });

		assertThat(HibernateUtils.getHibernateQuery(getValue(query, PROPERTY))).endsWith("firstname is null");
	}

	@Test // DATAJPA-920
	public void shouldLimitExistsProjectionQueries() throws Exception {

		JpaQueryMethod queryMethod = getQueryMethod("existsByFirstname", String.class);
		PartTreeJpaQuery jpaQuery = new PartTreeJpaQuery(queryMethod, entityManager, provider, EscapeCharacter.DEFAULT);

		Query query = jpaQuery.createQuery(new Object[] { "Matthews" });

		assertThat(query.getMaxResults()).isEqualTo(1);
	}

	@Test // DATAJPA-920
	public void shouldSelectAliasedIdForExistsProjectionQueries() throws Exception {

		JpaQueryMethod queryMethod = getQueryMethod("existsByFirstname", String.class);
		PartTreeJpaQuery jpaQuery = new PartTreeJpaQuery(queryMethod, entityManager, provider, EscapeCharacter.DEFAULT);

		Query query = jpaQuery.createQuery(new Object[] { "Matthews" });

		assertThat(HibernateUtils.getHibernateQuery(getValue(query, PROPERTY))).contains(".id from User as");
	}

	@Test // DATAJPA-1074
	public void isEmptyCollection() throws Exception {

		JpaQueryMethod queryMethod = getQueryMethod("findByRolesIsEmpty");
		PartTreeJpaQuery jpaQuery = new PartTreeJpaQuery(queryMethod, entityManager, provider, EscapeCharacter.DEFAULT);

		Query query = jpaQuery.createQuery(new Object[] {});

		assertThat(HibernateUtils.getHibernateQuery(getValue(query, PROPERTY))).endsWith("roles is empty");
	}

	@Test // DATAJPA-1074
	public void isNotEmptyCollection() throws Exception {

		JpaQueryMethod queryMethod = getQueryMethod("findByRolesIsNotEmpty");
		PartTreeJpaQuery jpaQuery = new PartTreeJpaQuery(queryMethod, entityManager, provider, EscapeCharacter.DEFAULT);

		Query query = jpaQuery.createQuery(new Object[] {});

		assertThat(HibernateUtils.getHibernateQuery(getValue(query, PROPERTY))).endsWith("roles is not empty");
	}

	@Test(expected = IllegalArgumentException.class) // DATAJPA-1074
	public void rejectsIsEmptyOnNonCollectionProperty() throws Exception {

		JpaQueryMethod method = getQueryMethod("findByFirstnameIsEmpty");
		AbstractJpaQuery jpaQuery = new PartTreeJpaQuery(method, entityManager, provider, EscapeCharacter.DEFAULT);

		jpaQuery.createQuery(new Object[] { "Oliver" });
	}

	@Test // DATAJPA-863
	public void errorsDueToMismatchOfParametersContainNameOfMethodAndInterface() throws Exception {

		JpaQueryMethod method = getQueryMethod("findByFirstname");

		assertThatExceptionOfType(IllegalArgumentException.class) //
				.isThrownBy(() -> new PartTreeJpaQuery(method, entityManager, provider, EscapeCharacter.DEFAULT)) //
				.withMessageContaining("findByFirstname") // the method being analyzed
				.withMessageContaining(" firstname ") // the property we are looking for
				.withMessageContaining("UserRepository"); // the repository
	}

	@Test // DATAJPA-863
	public void errorsDueToMissingPropertyContainNameOfMethodAndInterface() throws Exception {

		JpaQueryMethod method = getQueryMethod("findByNoSuchProperty", String.class);

		assertThatExceptionOfType(IllegalArgumentException.class) //
				.isThrownBy(() -> new PartTreeJpaQuery(method, entityManager, provider, EscapeCharacter.DEFAULT)) //
				.withMessageContaining("findByNoSuchProperty") // the method being analyzed
				.withMessageContaining(" noSuchProperty ") // the property we are looking for
				.withMessageContaining("UserRepository"); // the repository
	}

	private void testIgnoreCase(String methodName, Object... values) throws Exception {

		Class<?>[] parameterTypes = new Class[values.length];

		for (int i = 0; i < values.length; i++) {
			parameterTypes[i] = values[i].getClass();
		}

		JpaQueryMethod queryMethod = getQueryMethod(methodName, parameterTypes);
		PartTreeJpaQuery jpaQuery = new PartTreeJpaQuery(queryMethod, entityManager,
				PersistenceProvider.fromEntityManager(entityManager), EscapeCharacter.DEFAULT);
		jpaQuery.createQuery(values);
	}

	private JpaQueryMethod getQueryMethod(String methodName, Class<?>... parameterTypes) throws Exception {
		Method method = UserRepository.class.getMethod(methodName, parameterTypes);
		return new JpaQueryMethod(method, new DefaultRepositoryMetadata(UserRepository.class),
				new SpelAwareProxyProjectionFactory(), PersistenceProvider.fromEntityManager(entityManager));
	}

	@SuppressWarnings("unchecked")
	private static <T> T getValue(Object source, String path) {

		Iterator<String> split = Arrays.asList(path.split("\\.")).iterator();
		Object result = source;

		while (split.hasNext()) {

			Assert.notNull(source, "result must not be null.");
			result = getField(result, split.next());
		}

		Assert.notNull(result, "result must not be null.");
		return (T) result;
	}

	private static String getQueryProperty() {
		return isHibernate43() || isHibernate5() ? "jpqlQuery" : "val$jpaqlQuery";
	}

	private static boolean isHibernate43() {
		return Version.getVersionString().startsWith("4.3");
	}

	private static boolean isHibernate5() {
		return Version.getVersionString().startsWith("5.");
	}

	@SuppressWarnings("unused")
	interface UserRepository extends Repository<User, Long> {

		Page<User> findByFirstname(String firstname, Pageable pageable);

		User findByIdIgnoringCase(Integer id);

		User findByIdAllIgnoringCase(Integer id);

		boolean existsByFirstname(String firstname);

		List<User> findByCreatedAtAfter(@Temporal(TemporalType.TIMESTAMP) @Param("refDate") Date refDate);

		List<User> findByRolesIsEmpty();

		List<User> findByRolesIsNotEmpty();

		List<User> findByFirstnameIsEmpty();

		// Wrong number of parameters
		User findByFirstname();

		// Wrong property name
		User findByNoSuchProperty(String x);
	}

}
