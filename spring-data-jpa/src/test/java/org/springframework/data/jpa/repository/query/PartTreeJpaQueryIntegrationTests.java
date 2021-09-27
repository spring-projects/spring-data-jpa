/*
 * Copyright 2011-2021 the original author or authors.
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
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TemporalType;

import org.hibernate.Version;
import org.hibernate.query.internal.QueryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.Assert;

/**
 * Integration tests for {@link PartTreeJpaQuery}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Michael Cramer
 * @author Jens Schauder
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:infrastructure.xml")
public class PartTreeJpaQueryIntegrationTests {

	private static String PROPERTY = "h.target." + getQueryProperty();

	@PersistenceContext EntityManager entityManager;

	private PersistenceProvider provider;

	@BeforeEach
	void setUp() {
		this.provider = PersistenceProvider.fromEntityManager(entityManager);
	}

	@Test // DATADOC-90
	void test() throws Exception {

		JpaQueryMethod queryMethod = getQueryMethod("findByFirstname", String.class, Pageable.class);
		PartTreeJpaQuery jpaQuery = new PartTreeJpaQuery(queryMethod, entityManager);

		jpaQuery.createQuery(getAccessor(queryMethod, new Object[] { "Matthews", PageRequest.of(0, 1) }));
		jpaQuery.createQuery((getAccessor(queryMethod, new Object[] { "Matthews", PageRequest.of(0, 1) })));
	}

	@Test
	void cannotIgnoreCaseIfNotString() {

		assertThatIllegalArgumentException().isThrownBy(() -> testIgnoreCase("findByIdIgnoringCase", 3))
				.withMessageContaining(
						"Unable to ignore case of java.lang.Integer types, the property 'id' must reference a String");
	}

	@Test
	void cannotIgnoreCaseIfNotStringUnlessIgnoringAll() throws Exception {

		testIgnoreCase("findByIdAllIgnoringCase", 3);
	}

	@Test // DATAJPA-121
	void recreatesQueryIfNullValueIsGiven() throws Exception {

		JpaQueryMethod queryMethod = getQueryMethod("findByFirstname", String.class, Pageable.class);
		PartTreeJpaQuery jpaQuery = new PartTreeJpaQuery(queryMethod, entityManager);

		Query query = jpaQuery.createQuery((getAccessor(queryMethod, new Object[] { "Matthews", PageRequest.of(0, 1) })));

		assertThat(HibernateUtils.getHibernateQuery(query.unwrap(QueryImpl.class))).endsWith("firstname=:param0");

		query = jpaQuery.createQuery((getAccessor(queryMethod, new Object[] { null, PageRequest.of(0, 1) })));

		assertThat(HibernateUtils.getHibernateQuery(query.unwrap(QueryImpl.class))).endsWith("firstname is null");
	}

	@Test // DATAJPA-920
	void shouldLimitExistsProjectionQueries() throws Exception {

		JpaQueryMethod queryMethod = getQueryMethod("existsByFirstname", String.class);
		PartTreeJpaQuery jpaQuery = new PartTreeJpaQuery(queryMethod, entityManager);

		Query query = jpaQuery.createQuery((getAccessor(queryMethod, new Object[] { "Matthews" })));

		assertThat(query.getMaxResults()).isEqualTo(1);
	}

	@Test // DATAJPA-920
	void shouldSelectAliasedIdForExistsProjectionQueries() throws Exception {

		JpaQueryMethod queryMethod = getQueryMethod("existsByFirstname", String.class);
		PartTreeJpaQuery jpaQuery = new PartTreeJpaQuery(queryMethod, entityManager);

		Query query = jpaQuery.createQuery((getAccessor(queryMethod, new Object[] { "Matthews" })));

		assertThat(HibernateUtils.getHibernateQuery(query.unwrap(QueryImpl.class))).contains(".id from User as");
	}

	@Test // DATAJPA-1074
	void isEmptyCollection() throws Exception {

		JpaQueryMethod queryMethod = getQueryMethod("findByRolesIsEmpty");
		PartTreeJpaQuery jpaQuery = new PartTreeJpaQuery(queryMethod, entityManager);

		Query query = jpaQuery.createQuery((getAccessor(queryMethod, new Object[] {})));

		assertThat(HibernateUtils.getHibernateQuery(query.unwrap(QueryImpl.class))).endsWith("roles is empty");
	}

	@Test // DATAJPA-1074
	void isNotEmptyCollection() throws Exception {

		JpaQueryMethod queryMethod = getQueryMethod("findByRolesIsNotEmpty");
		PartTreeJpaQuery jpaQuery = new PartTreeJpaQuery(queryMethod, entityManager);

		Query query = jpaQuery.createQuery((getAccessor(queryMethod, new Object[] {})));

		assertThat(HibernateUtils.getHibernateQuery(query.unwrap(QueryImpl.class))).endsWith("roles is not empty");
	}

	@Test // DATAJPA-1074
	void rejectsIsEmptyOnNonCollectionProperty() throws Exception {

		JpaQueryMethod method = getQueryMethod("findByFirstnameIsEmpty");

		assertThatIllegalArgumentException().isThrownBy(() -> new PartTreeJpaQuery(method, entityManager));
	}

	@Test // DATAJPA-1182
	void rejectsInPredicateWithNonIterableParameter() throws Exception {

		JpaQueryMethod method = getQueryMethod("findByIdIn", Integer.class);

		assertThatExceptionOfType(RuntimeException.class) //
				.isThrownBy(() -> new PartTreeJpaQuery(method, entityManager)) //
				.withMessageContaining("findByIdIn") //
				.withMessageContaining(" IN ") //
				.withMessageContaining("Collection") //
				.withMessageContaining("Integer");
	}

	@Test // DATAJPA-1182
	void rejectsOtherThanInPredicateWithIterableParameter() throws Exception {

		JpaQueryMethod method = getQueryMethod("findById", Collection.class);

		assertThatExceptionOfType(RuntimeException.class) //
				.isThrownBy(() -> new PartTreeJpaQuery(method, entityManager)) //
				.withMessageContaining("findById") //
				.withMessageContaining(" SIMPLE_PROPERTY ") //
				.withMessageContaining(" scalar ") //
				.withMessageContaining("Collection");
	}

	@Test // DATAJPA-1619
	void acceptsInPredicateWithIterableParameter() throws Exception {

		JpaQueryMethod method = getQueryMethod("findByFirstnameIn", Iterable.class);

		new PartTreeJpaQuery(method, entityManager);

		assertThat(method).isNotNull();
	}

	@Test // DATAJPA-863
	void errorsDueToMismatchOfParametersContainNameOfMethodInterfaceAndPropertyPath() throws Exception {

		JpaQueryMethod method = getQueryMethod("findByFirstname");

		assertThatExceptionOfType(IllegalArgumentException.class) //
				.isThrownBy(() -> new PartTreeJpaQuery(method, entityManager)) //
				.withMessageContaining("findByFirstname") // the method being analyzed
				.withMessageContaining(" firstname ") // the property we are looking for
				.withMessageContaining("UserRepository"); // the repository
	}

	@Test // DATAJPA-863
	void errorsDueToMissingPropertyContainNameOfMethodAndInterface() throws Exception {

		JpaQueryMethod method = getQueryMethod("findByNoSuchProperty", String.class);

		assertThatExceptionOfType(IllegalArgumentException.class) //
				.isThrownBy(() -> new PartTreeJpaQuery(method, entityManager)) //
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
		PartTreeJpaQuery jpaQuery = new PartTreeJpaQuery(queryMethod, entityManager);
		jpaQuery.createQuery((getAccessor(queryMethod, values)));
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

			Assert.notNull(result, "result must not be null.");
			result = getField(result, split.next());
		}

		Assert.notNull(result, "result must not be null.");
		return (T) result;
	}

	private JpaParametersParameterAccessor getAccessor(JpaQueryMethod queryMethod, Object[] values) {
		return new JpaParametersParameterAccessor(queryMethod.getParameters(), values);
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
	interface UserRepository extends Repository<User, Integer> {

		Page<User> findByFirstname(String firstname, Pageable pageable);

		User findByIdIgnoringCase(Integer id);

		User findByIdAllIgnoringCase(Integer id);

		boolean existsByFirstname(String firstname);

		List<User> findByCreatedAtAfter(@Temporal(TemporalType.TIMESTAMP) @Param("refDate") Date refDate);

		List<User> findByRolesIsEmpty();

		List<User> findByRolesIsNotEmpty();

		List<User> findByFirstnameIsEmpty();

		// should fail, since we can't compare scalar values to collections
		List<User> findById(Collection<Integer> ids);

		// should fail, since we can't do an IN on a scalar
		List<User> findByIdIn(Integer id);

		// should succeed
		List<User> findByFirstnameIn(Iterable<String> id);

		// Wrong number of parameters
		User findByFirstname();

		// Wrong property name
		User findByNoSuchProperty(String x);
	}

}
