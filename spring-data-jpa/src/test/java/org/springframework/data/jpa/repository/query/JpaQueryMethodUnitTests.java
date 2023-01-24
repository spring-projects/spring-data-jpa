/*
 * Copyright 2008-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.annotation.AliasFor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.util.TypeInformation;

/**
 * Unit test for {@link QueryMethod}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @author Jens Schauder
 * @author Mark Paluch
 * @author Erik Pellizzon
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JpaQueryMethodUnitTests {

	private static final String METHOD_NAME = "findByFirstname";

	@Mock QueryExtractor extractor;
	@Mock RepositoryMetadata metadata;
	private ProjectionFactory factory = new SpelAwareProxyProjectionFactory();

	private Method invalidReturnType;
	private Method pageableAndSort;
	private Method pageableTwice;
	private Method sortableTwice;
	private Method findWithLockMethod;
	private Method findsProjections;
	private Method findsProjection;
	private Method queryMethodWithCustomEntityFetchGraph;

	/**
	 * @throws Exception
	 */
	@BeforeEach
	void setUp() throws Exception {

		invalidReturnType = InvalidRepository.class.getMethod(METHOD_NAME, String.class, Pageable.class);
		pageableAndSort = InvalidRepository.class.getMethod(METHOD_NAME, String.class, Pageable.class, Sort.class);
		pageableTwice = InvalidRepository.class.getMethod(METHOD_NAME, String.class, Pageable.class, Pageable.class);

		sortableTwice = InvalidRepository.class.getMethod(METHOD_NAME, String.class, Sort.class, Sort.class);

		findWithLockMethod = ValidRepository.class.getMethod("findOneLocked", Integer.class);

		findsProjections = ValidRepository.class.getMethod("findsProjections");
		findsProjection = ValidRepository.class.getMethod("findsProjection");

		queryMethodWithCustomEntityFetchGraph = ValidRepository.class.getMethod("queryMethodWithCustomEntityFetchGraph",
				Integer.class);

		when(metadata.getReturnType(any(Method.class)))
				.thenAnswer(invocation -> TypeInformation.fromReturnTypeOf(invocation.getArgument(0)));
	}

	@Test
	void testname() throws Exception {

		JpaQueryMethod method = getQueryMethod(UserRepository.class, "findByLastname", String.class);

		assertThat(method.getNamedQueryName()).isEqualTo("User.findByLastname");
		assertThat(method.isCollectionQuery()).isTrue();
		assertThat(method.getAnnotatedQuery()).isNull();
		assertThat(method.isNativeQuery()).isFalse();
	}

	@Test
	void preventsNullRepositoryMethod() {

		assertThatIllegalArgumentException().isThrownBy(() -> new JpaQueryMethod(null, metadata, factory, extractor));
	}

	@Test
	void preventsNullQueryExtractor() throws Exception {

		Method method = UserRepository.class.getMethod("findByLastname", String.class);
		assertThatIllegalArgumentException().isThrownBy(() -> new JpaQueryMethod(method, metadata, factory, null));
	}

	@Test
	void returnsCorrectName() throws Exception {

		JpaQueryMethod method = getQueryMethod(UserRepository.class, "findByLastname", String.class);
		assertThat(method.getName()).isEqualTo("findByLastname");
	}

	@Test
	void returnsQueryIfAvailable() throws Exception {

		JpaQueryMethod method = getQueryMethod(UserRepository.class, "findByLastname", String.class);
		assertThat(method.getAnnotatedQuery()).isNull();

		method = getQueryMethod(UserRepository.class, "findByAnnotatedQuery", String.class);
		assertThat(method.getAnnotatedQuery()).isNotNull();
	}

	@Test
	void rejectsInvalidReturntypeOnPagebleFinder() {

		assertThatIllegalStateException()
				.isThrownBy(() -> new JpaQueryMethod(invalidReturnType, metadata, factory, extractor));
	}

	@Test
	void rejectsPageableAndSortInFinderMethod() {

		assertThatIllegalStateException()
				.isThrownBy(() -> new JpaQueryMethod(pageableAndSort, metadata, factory, extractor));
	}

	@Test
	void rejectsTwoPageableParameters() {

		assertThatIllegalStateException().isThrownBy(() -> new JpaQueryMethod(pageableTwice, metadata, factory, extractor));
	}

	@Test
	void rejectsTwoSortableParameters() {

		assertThatIllegalStateException().isThrownBy(() -> new JpaQueryMethod(sortableTwice, metadata, factory, extractor));
	}

	@Test
	void recognizesModifyingMethod() throws Exception {

		JpaQueryMethod method = getQueryMethod(UserRepository.class, "renameAllUsersTo", String.class);
		assertThat(method.isModifyingQuery()).isTrue();
	}

	@Test
	void rejectsModifyingMethodWithPageable() throws Exception {

		Method method = InvalidRepository.class.getMethod("updateMethod", String.class, Pageable.class);

		assertThatIllegalArgumentException().isThrownBy(() -> new JpaQueryMethod(method, metadata, factory, extractor));
	}

	@Test
	void rejectsModifyingMethodWithSort() throws Exception {

		Method method = InvalidRepository.class.getMethod("updateMethod", String.class, Sort.class);

		assertThatIllegalArgumentException().isThrownBy(() -> new JpaQueryMethod(method, metadata, factory, extractor));
	}

	@Test
	void discoversHintsCorrectly() throws Exception {

		JpaQueryMethod method = getQueryMethod(UserRepository.class, "findByLastname", String.class);
		List<QueryHint> hints = method.getHints();

		assertThat(hints).isNotNull();
		assertThat(hints.get(0).name()).isEqualTo("foo");
		assertThat(hints.get(0).value()).isEqualTo("bar");
	}

	private JpaQueryMethod getQueryMethod(Class<?> repositoryInterface, String methodName, Class<?>... parameterTypes)
			throws Exception {

		Method method = repositoryInterface.getMethod(methodName, parameterTypes);
		DefaultRepositoryMetadata repositoryMetadata = new DefaultRepositoryMetadata(repositoryInterface);
		return new JpaQueryMethod(method, repositoryMetadata, factory, extractor);
	}

	@Test
	void calculatesNamedQueryNamesCorrectly() throws Exception {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(UserRepository.class);

		JpaQueryMethod queryMethod = getQueryMethod(UserRepository.class, "findByLastname", String.class);
		assertThat(queryMethod.getNamedQueryName()).isEqualTo("User.findByLastname");

		Method method = UserRepository.class.getMethod("renameAllUsersTo", String.class);
		queryMethod = new JpaQueryMethod(method, metadata, factory, extractor);
		assertThat(queryMethod.getNamedQueryName()).isEqualTo("User.renameAllUsersTo");

		method = UserRepository.class.getMethod("findSpecialUsersByLastname", String.class);
		queryMethod = new JpaQueryMethod(method, metadata, factory, extractor);
		assertThat(queryMethod.getNamedQueryName()).isEqualTo("SpecialUser.findSpecialUsersByLastname");
	}

	@Test // DATAJPA-117
	void discoversNativeQuery() throws Exception {

		JpaQueryMethod method = getQueryMethod(ValidRepository.class, "findByLastname", String.class);
		assertThat(method.isNativeQuery()).isTrue();
	}

	@Test // DATAJPA-129
	void considersAnnotatedNamedQueryName() throws Exception {

		JpaQueryMethod queryMethod = getQueryMethod(ValidRepository.class, "findByNamedQuery");
		assertThat(queryMethod.getNamedQueryName()).isEqualTo("HateoasAwareSpringDataWebConfiguration.bar");
	}

	@Test // DATAJPA-73
	void discoversLockModeCorrectly() throws Exception {

		JpaQueryMethod method = getQueryMethod(ValidRepository.class, "findOneLocked", Integer.class);
		LockModeType lockMode = method.getLockModeType();

		assertThat(lockMode).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
	}

	@Test // DATAJPA-142
	void returnsDefaultCountQueryName() throws Exception {

		JpaQueryMethod method = getQueryMethod(UserRepository.class, "findByLastname", String.class);
		assertThat(method.getNamedCountQueryName()).isEqualTo("User.findByLastname.count");
	}

	@Test // DATAJPA-142
	void returnsDefaultCountQueryNameBasedOnConfiguredNamedQueryName() throws Exception {

		JpaQueryMethod method = getQueryMethod(ValidRepository.class, "findByNamedQuery");
		assertThat(method.getNamedCountQueryName()).isEqualTo("HateoasAwareSpringDataWebConfiguration.bar.count");
	}

	@Test // DATAJPA-185
	void rejectsInvalidNamedParameter() {

		assertThatThrownBy(() -> getQueryMethod(InvalidRepository.class, "findByAnnotatedQuery", String.class))
				.isInstanceOf(IllegalStateException.class)
				// Parameter from query
				.hasMessageContaining("foo")
				// Parameter name from annotation
				.hasMessageContaining("param")
				// Method name
				.hasMessageContaining("findByAnnotatedQuery");

	}

	@Test // DATAJPA-207
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void returnsTrueIfReturnTypeIsEntity() {

		when(metadata.getDomainType()).thenReturn((Class) User.class);
		when(metadata.getReturnedDomainClass(findsProjections)).thenReturn((Class) Integer.class);
		when(metadata.getReturnedDomainClass(findsProjection)).thenReturn((Class) Integer.class);

		assertThat(new JpaQueryMethod(findsProjections, metadata, factory, extractor).isQueryForEntity()).isFalse();
		assertThat(new JpaQueryMethod(findsProjection, metadata, factory, extractor).isQueryForEntity()).isFalse();
	}

	@Test // DATAJPA-345
	void detectsLockAndQueryHintsOnIfUsedAsMetaAnnotation() throws Exception {

		JpaQueryMethod method = getQueryMethod(ValidRepository.class, "withMetaAnnotation");

		assertThat(method.getLockModeType()).isEqualTo(LockModeType.OPTIMISTIC_FORCE_INCREMENT);
		assertThat(method.getHints()).hasSize(1);
		assertThat(method.getHints().get(0).name()).isEqualTo("foo");
		assertThat(method.getHints().get(0).value()).isEqualTo("bar");
	}

	@Test // DATAJPA-466
	void shouldStoreJpa21FetchGraphInformationAsHint() {

		doReturn(User.class).when(metadata).getDomainType();
		doReturn(User.class).when(metadata).getReturnedDomainClass(queryMethodWithCustomEntityFetchGraph);

		JpaQueryMethod method = new JpaQueryMethod(queryMethodWithCustomEntityFetchGraph, metadata, factory, extractor);

		assertThat(method.getEntityGraph()).isNotNull();
		assertThat(method.getEntityGraph().getName()).isEqualTo("User.propertyLoadPath");
		assertThat(method.getEntityGraph().getType()).isEqualTo(EntityGraphType.LOAD);
	}

	@Test // DATAJPA-612
	void shouldFindEntityGraphAnnotationOnOverriddenSimpleJpaRepositoryMethod() throws Exception {

		doReturn(User.class).when(metadata).getDomainType();
		doReturn(User.class).when(metadata).getReturnedDomainClass((Method) any());

		JpaQueryMethod method = new JpaQueryMethod(JpaRepositoryOverride.class.getMethod("findAll"), metadata, factory,
				extractor);

		assertThat(method.getEntityGraph()).isNotNull();
		assertThat(method.getEntityGraph().getName()).isEqualTo("User.detail");
		assertThat(method.getEntityGraph().getType()).isEqualTo(EntityGraphType.FETCH);
	}

	@Test // DATAJPA-689
	void shouldFindEntityGraphAnnotationOnOverriddenSimpleJpaRepositoryMethodFindOne() throws Exception {

		doReturn(User.class).when(metadata).getDomainType();
		doReturn(User.class).when(metadata).getReturnedDomainClass((Method) any());

		JpaQueryMethod method = new JpaQueryMethod(JpaRepositoryOverride.class.getMethod("findOne", Integer.class),
				metadata, factory, extractor);

		assertThat(method.getEntityGraph()).isNotNull();
		assertThat(method.getEntityGraph().getName()).isEqualTo("User.detail");
		assertThat(method.getEntityGraph().getType()).isEqualTo(EntityGraphType.FETCH);
	}

	/**
	 * DATAJPA-696
	 */
	@Test
	void shouldFindEntityGraphAnnotationOnQueryMethodGetOneByWithDerivedName() throws Exception {

		doReturn(User.class).when(metadata).getDomainType();
		doReturn(User.class).when(metadata).getReturnedDomainClass((Method) any());

		JpaQueryMethod method = new JpaQueryMethod(JpaRepositoryOverride.class.getMethod("getOneById", Integer.class),
				metadata, factory, extractor);

		assertThat(method.getEntityGraph()).isNotNull();
		assertThat(method.getEntityGraph().getName()).isEqualTo("User.getOneById");
		assertThat(method.getEntityGraph().getType()).isEqualTo(EntityGraphType.FETCH);
	}

	@Test // DATAJPA-758
	void allowsPositionalBindingEvenIfParametersAreNamed() throws Exception {
		getQueryMethod(ValidRepository.class, "queryWithPositionalBinding", String.class);
	}

	@Test // DATAJPA-871
	void usesAliasedValueForLockLockMode() throws Exception {

		JpaQueryMethod method = getQueryMethod(ValidRepository.class, "withMetaAnnotationUsingAliasFor");

		assertThat(method.getLockModeType()).isEqualTo(LockModeType.PESSIMISTIC_FORCE_INCREMENT);
	}

	@Test // DATAJPA-871
	void usesAliasedValueForQueryHints() throws Exception {

		JpaQueryMethod method = getQueryMethod(ValidRepository.class, "withMetaAnnotationUsingAliasFor");

		assertThat(method.getHints()).hasSize(1);
		assertThat(method.getHints().get(0).name()).isEqualTo("foo");
		assertThat(method.getHints().get(0).value()).isEqualTo("bar");

	}

	@Test // DATAJPA-871
	void usesAliasedValueForQueryHintsCounting() throws Exception {

		JpaQueryMethod method = getQueryMethod(ValidRepository.class, "withMetaAnnotationUsingAliasFor");

		assertThat(method.applyHintsToCountQuery()).isTrue();
	}

	@Test // DATAJPA-871
	void usesAliasedValueForModifyingClearAutomatically() throws Exception {

		JpaQueryMethod method = getQueryMethod(ValidRepository.class, "withMetaAnnotationUsingAliasFor");

		assertThat(method.isModifyingQuery()).isTrue();
		assertThat(method.getClearAutomatically()).isTrue();
	}

	@Test // DATAJPA-871
	void usesAliasedValueForHintsApplyToCountQuery() throws Exception {

		JpaQueryMethod method = getQueryMethod(ValidRepository.class, "withMetaAnnotationUsingAliasFor");

		assertThat(method.applyHintsToCountQuery()).isTrue();
	}

	@Test // DATAJPA-871
	void usesAliasedValueForQueryValue() throws Exception {

		JpaQueryMethod method = getQueryMethod(ValidRepository.class, "withMetaAnnotationUsingAliasFor");

		assertThat(method.getAnnotatedQuery()).isEqualTo("select u from User u where u.firstname = ?1");
	}

	@Test // DATAJPA-871
	void usesAliasedValueForQueryCountQuery() throws Exception {

		JpaQueryMethod method = getQueryMethod(ValidRepository.class, "withMetaAnnotationUsingAliasFor");

		assertThat(method.getCountQuery()).isEqualTo("select u from User u where u.lastname = ?1");
	}

	@Test // DATAJPA-871
	void usesAliasedValueForQueryCountQueryProjection() throws Exception {

		JpaQueryMethod method = getQueryMethod(ValidRepository.class, "withMetaAnnotationUsingAliasFor");

		assertThat(method.getCountQueryProjection()).isEqualTo("foo-bar");
	}

	@Test // DATAJPA-871
	void usesAliasedValueForQueryNamedQueryName() throws Exception {

		JpaQueryMethod method = getQueryMethod(ValidRepository.class, "withMetaAnnotationUsingAliasFor");

		assertThat(method.getNamedQueryName()).isEqualTo("namedQueryName");
	}

	@Test // DATAJPA-871
	void usesAliasedValueForQueryNamedCountQueryName() throws Exception {

		JpaQueryMethod method = getQueryMethod(ValidRepository.class, "withMetaAnnotationUsingAliasFor");

		assertThat(method.getNamedCountQueryName()).isEqualTo("namedCountQueryName");
	}

	@Test // DATAJPA-871
	void usesAliasedValueForQueryNativeQuery() throws Exception {

		JpaQueryMethod method = getQueryMethod(ValidRepository.class, "withMetaAnnotationUsingAliasFor");

		assertThat(method.isNativeQuery()).isTrue();
	}

	@Test // DATAJPA-871
	void usesAliasedValueForEntityGraph() throws Exception {

		doReturn(User.class).when(metadata).getDomainType();
		doReturn(User.class).when(metadata).getReturnedDomainClass((Method) any());

		JpaQueryMethod method = new JpaQueryMethod(
				JpaRepositoryOverride.class.getMethod("getOneWithCustomEntityGraphAnnotation"), metadata, factory, extractor);

		assertThat(method.getEntityGraph()).isNotNull();
		assertThat(method.getEntityGraph().getName()).isEqualTo("User.detail");
		assertThat(method.getEntityGraph().getType()).isEqualTo(EntityGraphType.LOAD);
	}

	/**
	 * Interface to define invalid repository methods for testing.
	 *
	 * @author Oliver Gierke
	 */
	interface InvalidRepository extends Repository<User, Integer> {

		// Invalid return type
		User findByFirstname(String firstname, Pageable pageable);

		// Should not use Pageable *and* Sort
		Page<User> findByFirstname(String firstname, Pageable pageable, Sort sort);

		// Must not use two Pageables
		Page<User> findByFirstname(String firstname, Pageable first, Pageable second);

		// Must not use two Pageables
		Page<User> findByFirstname(String firstname, Sort first, Sort second);

		// Not backed by a named query or @Query annotation
		@Modifying
		void updateMethod(String firstname);

		// Modifying and Pageable is not allowed
		@Modifying
		Page<String> updateMethod(String firstname, Pageable pageable);

		// Modifying and Sort is not allowed
		@Modifying
		void updateMethod(String firstname, Sort sort);

		// Typo in named parameter
		@Query("select u from User u where u.firstname = :foo")
		List<User> findByAnnotatedQuery(@Param("param") String param);
	}

	interface ValidRepository extends Repository<User, Integer> {

		@Query(value = "select u from User u where u.lastname = ?1", nativeQuery = true)
		List<User> findByLastname(String lastname);

		@Query(name = "HateoasAwareSpringDataWebConfiguration.bar")
		List<User> findByNamedQuery();

		@Lock(LockModeType.PESSIMISTIC_WRITE)
		@Query("select u from User u where u.id = ?1")
		List<User> findOneLocked(Integer primaryKey);

		List<Integer> findsProjections();

		Integer findsProjection();

		@CustomAnnotation
		void withMetaAnnotation();

		// DATAJPA-466
		@EntityGraph(value = "User.propertyLoadPath", type = EntityGraphType.LOAD)
		User queryMethodWithCustomEntityFetchGraph(Integer id);

		@Query("select u from User u where u.firstname = ?1")
		User queryWithPositionalBinding(@Param("firstname") String firstname);

		@CustomComposedAnnotationWithAliasFor
		void withMetaAnnotationUsingAliasFor();
	}

	interface JpaRepositoryOverride extends JpaRepository<User, Integer> {

		/**
		 * DATAJPA-612
		 */
		@Override
		@EntityGraph("User.detail")
		List<User> findAll();

		/**
		 * DATAJPA-689
		 */
		@EntityGraph("User.detail")
		Optional<User> findOne(Integer id);

		/**
		 * DATAJPA-696
		 */
		@EntityGraph
		User getOneById(Integer id);

		@CustomComposedEntityGraphAnnotationWithAliasFor
		User getOneWithCustomEntityGraphAnnotation();

	}

	@Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
	@QueryHints(@QueryHint(name = "foo", value = "bar"))
	@Retention(RetentionPolicy.RUNTIME)
	private static @interface CustomAnnotation {

	}

	@Modifying
	@Query
	@Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
	@QueryHints(@QueryHint(name = "foo", value = "bar"))
	@Retention(RetentionPolicy.RUNTIME)
	private static @interface CustomComposedAnnotationWithAliasFor {

		@AliasFor(annotation = Modifying.class, attribute = "clearAutomatically")
		boolean doClear() default true;

		@AliasFor(annotation = Query.class, attribute = "value")
		String querystring() default "select u from User u where u.firstname = ?1";

		@AliasFor(annotation = Query.class, attribute = "countQuery")
		String countQueryString() default "select u from User u where u.lastname = ?1";

		@AliasFor(annotation = Query.class, attribute = "countProjection")
		String countProjectionString() default "foo-bar";

		@AliasFor(annotation = Query.class, attribute = "nativeQuery")
		boolean isNativeQuery() default true;

		@AliasFor(annotation = Query.class, attribute = "name")
		String namedQueryName() default "namedQueryName";

		@AliasFor(annotation = Query.class, attribute = "countName")
		String namedCountQueryName() default "namedCountQueryName";

		@AliasFor(annotation = Lock.class, attribute = "value")
		LockModeType lock() default LockModeType.PESSIMISTIC_FORCE_INCREMENT;

		@AliasFor(annotation = QueryHints.class, attribute = "value")
		QueryHint[] hints() default @QueryHint(name = "foo", value = "bar");

		@AliasFor(annotation = QueryHints.class, attribute = "forCounting")
		boolean doCount() default true;
	}

	@EntityGraph
	@Retention(RetentionPolicy.RUNTIME)
	private static @interface CustomComposedEntityGraphAnnotationWithAliasFor {

		@AliasFor(annotation = EntityGraph.class, attribute = "value")
		String graphName() default "User.detail";

		@AliasFor(annotation = EntityGraph.class, attribute = "type")
		EntityGraphType graphType() default EntityGraphType.LOAD;

		@AliasFor(annotation = EntityGraph.class, attribute = "attributePaths")
		String[] paths() default { "foo", "bar" };
	}
}
