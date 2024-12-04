/*
 * Copyright 2008-2024 the original author or authors.
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
package org.springframework.data.jpa.repository.sample;

import jakarta.persistence.EntityManager;
import jakarta.persistence.QueryHint;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.OffsetScrollPosition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.domain.sample.SpecialUser;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.querydsl.ListQuerydslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository interface for {@code User}s.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Kevin Peters
 * @author Jeff Sheets
 * @author Andrey Kovalev
 * @author JyotirmoyVS
 * @author Greg Turnquist
 * @author Simon Paradies
 * @author Diego Krupitza
 * @author Geoffrey Deremetz
 * @author Yanming Zhou
 */
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User>,
		UserRepositoryCustom, ListQuerydslPredicateExecutor<User> {

	/**
	 * Retrieve users by their lastname. The finder {@literal User.findByLastname} is declared in
	 * {@literal META-INF/orm.xml} .
	 */
	@QueryHints({ @QueryHint(name = "foo", value = "bar") })
	List<User> findByLastname(String lastname);

	List<User> findUserByLastname(@Nullable String lastname);

	/**
	 * Redeclaration of {@link CrudRepository#findById(java.lang.Object)} to change transaction configuration.
	 */
	@Transactional
	@Override
	java.util.Optional<User> findById(Integer primaryKey);

	/**
	 * Redeclaration of {@link CrudRepository#deleteById(java.lang.Object)}. to make sure the transaction
	 * configuration of the original method is considered if the redeclaration does not carry a {@link Transactional}
	 * annotation.
	 */
	@Override
	void deleteById(Integer id); // DATACMNS-649

	/**
	 * Retrieve users by their email address. The finder {@literal User.findByEmailAddress} is declared as annotation at
	 * {@code User}.
	 */
	User findByEmailAddress(String emailAddress);

	@Query("select u from User u left outer join u.manager as manager")
	Page<User> findAllPaged(Pageable pageable);

	/**
	 * Retrieves users by the given email and lastname. Acts as a dummy method declaration to test finder query creation.
	 */
	User findByEmailAddressAndLastname(String emailAddress, String lastname);

	/**
	 * Retrieves users by email address and lastname or firstname. Acts as a dummy method declaration to test finder query
	 * creation.
	 */
	List<User> findByEmailAddressAndLastnameOrFirstname(String emailAddress, String lastname, String username);

	/**
	 * Retrieves a user by its username using the query annotated to the method.
	 */
	@Query("select u from User u where u.emailAddress = ?1")
	@Transactional(readOnly = true)
	User findByAnnotatedQuery(String emailAddress);

	/**
	 * Method to directly create query from and adding a {@link Pageable} parameter to be regarded on query execution.
	 */
	Page<User> findByLastname(Pageable pageable, String lastname);

	/**
	 * Method to directly create query from and adding a {@link Pageable} parameter to be regarded on query execution.
	 * Just returns the queried {@link Page}'s contents.
	 */
	List<User> findByFirstname(String firstname, Pageable pageable);

	Page<User> findByFirstnameIn(Pageable pageable, String... firstnames);

	Window<User> findTop3ByFirstnameStartingWithOrderByFirstnameAscEmailAddressAsc(String firstname,
			ScrollPosition position);

	Window<User> findByFirstnameStartingWithOrderByFirstnameAscEmailAddressAsc(String firstname, ScrollPosition position);

	List<User> findByFirstnameNotIn(Collection<String> firstnames);

	// DATAJPA-292
	@Query("select u from User u where u.firstname like ?1%")
	List<User> findByFirstnameLike(String firstname);

	// DATAJPA-292, GH-3041
	@Query("select u from User u where u.firstname like :firstname% or u.firstname like %:firstname")
	List<User> findByFirstnameLikeNamed(@Param("firstname") String firstname);

	// DATAJPA-292, GH-3041
	@Query("select u from User u where u.firstname like ?1% or u.firstname like %?1")
	List<User> findByFirstnameLikePositional(String firstname);

	/**
	 * Manipulating query to set all {@link User}'s names to the given one.
	 */
	@Modifying
	@Query("update User u set u.lastname = ?1")
	void renameAllUsersTo(String lastname);

	@Query("select count(u) from User u where u.firstname = ?1")
	Long countWithFirstname(String firstname);

	/**
	 * Method where parameters will be applied by name. Note that the order of the parameters is then not crucial anymore.
	 */
	@Query("select u from User u where u.lastname = :lastname or u.firstname = :firstname")
	List<User> findByLastnameOrFirstname(@Param("firstname") String foo, @Param("lastname") String bar);

	@Query("select u from User u where u.lastname = :lastname or u.firstname = :firstname")
	List<User> findByLastnameOrFirstnameUnannotated(String firstname, String lastname);

	/**
	 * Method to check query creation and named parameter usage go well hand in hand.
	 */
	List<User> findByFirstnameOrLastname(@Param("lastname") String lastname, @Param("firstname") String firstname);

	List<User> findByLastnameLikeOrderByFirstnameDesc(String lastname);

	List<User> findByLastnameNotLike(String lastname);

	List<User> findByLastnameNot(@Nullable String lastname);

	List<User> findByManagerLastname(String name);

	List<User> findByColleaguesLastname(String lastname);

	List<User> findByLastnameNotNull();

	List<User> findByLastnameNull();

	List<User> findByEmailAddressLike(String email, Sort sort);

	List<User> findByEmailAddressLike(String email, Pageable pageable);

	List<SpecialUser> findSpecialUsersByLastname(String lastname);

	List<User> findBySpringDataNamedQuery(String lastname);

	List<User> findByLastnameIgnoringCase(String lastname);

	Page<User> findByLastnameIgnoringCase(Pageable pageable, String lastname);

	Window<User> findByLastnameOrderByFirstname(Limit limit, ScrollPosition scrollPosition, String lastname);

	Window<User> findByLastnameOrderByFirstname(String lastname, Pageable page);

	Window<NameOnly> findTop1ByLastnameOrderByFirstname(ScrollPosition scrollPosition, String lastname);

	List<User> findByLastnameIgnoringCaseLike(String lastname);

	List<User> findByLastnameAndFirstnameAllIgnoringCase(String lastname, String firstname);

	List<User> findByAgeGreaterThanEqual(int age);

	List<User> findByAgeLessThanEqual(int age);

	@Query("select u.lastname from User u group by u.lastname")
	Page<String> findByLastnameGrouped(Pageable pageable);

	// DATAJPA-117
	@Query(value = "SELECT * FROM SD_User WHERE lastname = ?1", nativeQuery = true)
	List<User> findNativeByLastname(String lastname);

	// DATAJPA-132
	List<User> findByActiveTrue();

	// DATAJPA-132
	List<User> findByActiveFalse();

	@Query("select u.colleagues from User u where u = ?1")
	List<User> findColleaguesFor(User user);

	// DATAJPA-188
	List<User> findByCreatedAtBefore(Date date);

	// DATAJPA-188
	List<User> findByCreatedAtAfter(Date date);

	// DATAJPA-180
	List<User> findByFirstnameStartingWith(String firstname);

	// DATAJPA-180
	List<User> findByFirstnameEndingWith(String firstname);

	// DATAJPA-180
	List<User> findByFirstnameContaining(String firstname);

	@Query(value = "SELECT 1 FROM SD_User", nativeQuery = true)
	List<Integer> findOnesByNativeQuery();

	// DATAJPA-231
	long countByLastname(String lastname);

	// DATAJPA-231
	int countUsersByFirstname(String firstname);

	// DATAJPA-920
	boolean existsByLastname(String lastname);

	// DATAJPA-391
	@Query("select u.firstname from User u where u.lastname = ?1")
	List<String> findFirstnamesByLastname(String lastname);

	// DATAJPA-415
	Collection<User> findByIdIn(@Param("ids") Integer... ids);

	// DATAJPA-461
	@Query("select u from User u where u.id in ?1")
	Collection<User> findByIdsCustomWithPositionalVarArgs(Integer... ids);

	// DATAJPA-461
	@Query("select u from User u where u.id in :ids")
	Collection<User> findByIdsCustomWithNamedVarArgs(@Param("ids") Integer... ids);

	// DATAJPA-415
	@Modifying
	@Query("update #{#entityName} u set u.active = :activeState where u.id in :ids")
	void updateUserActiveState(@Param("activeState") boolean activeState, @Param("ids") Integer... ids);

	// DATAJPA-405
	List<User> findAllByOrderByLastnameAsc();

	// DATAJPA-454
	List<User> findByBinaryData(byte[] data);

	// DATAJPA-486
	Slice<User> findSliceByLastname(String lastname, Pageable pageable);

	// DATAJPA-496
	List<User> findByAttributesIn(Set<String> attributes);

	// DATAJPA-460
	Long removeByLastname(String lastname);

	// DATAJPA-460
	List<User> deleteByLastname(String lastname);

	/**
	 * @see <a href="https://issues.apache.org/jira/browse/OPENJPA-2484">OPENJPA-2484</a>
	 */
	// DATAJPA-505
	// @Query(value = "select u.binaryData from User u where u.id = :id")
	// byte[] findBinaryDataByIdJpaQl(@Param("id") Integer id);

	/**
	 * Explicitly mapped to a procedure with name "plus1inout" in database.
	 */
	@Procedure("plus1inout") // DATAJPA-455
	Integer explicitlyNamedPlus1inout(Integer arg);

	/**
	 * Implicitly mapped to a procedure with name "plus1inout" in database via alias.
	 */
	@Procedure(procedureName = "plus1inout") // DATAJPA-455
	Integer plus1inout(Integer arg);

	/**
	 * Implicitly mapped to a procedure with name "plus1inout" in database via alias. Showing that outputParameterName is
	 * ignored when not a NamedStoredProcedure
	 */
	@Procedure(procedureName = "plus1inout", outputParameterName = "fakeName") // DATAJPA-707
	Integer plus1inoutInvalidOutParamName(Integer arg);

	/**
	 * Explicitly mapped to named stored procedure "User.plus1IO" in {@link EntityManager}.
	 */
	@Procedure(name = "User.plus1IO") // DATAJPA-455
	Integer entityAnnotatedCustomNamedProcedurePlus1IO(@Param("arg") Integer arg);

	/**
	 * Explicitly mapped to named stored procedure "User.plus1IO" in {@link EntityManager}. with an invalid
	 * outputParameterName - test will fail
	 */
	@Procedure(name = "User.plus1IO", outputParameterName = "fakeName") // DATAJPA-707
	Integer entityAnnotatedCustomNamedProcedurePlus1IOInvalidOutParamName(@Param("arg") Integer arg);

	/**
	 * Explicitly mapped to named stored procedure "User.plus1IO2" in {@link EntityManager}. Stored Proc has 2 out params,
	 * but naming one out param here so it only returns one
	 */
	@Procedure(name = "User.plus1IO2", outputParameterName = "res2") // DATAJPA-707
	Integer entityAnnotatedCustomNamedProcedurePlus1IO2TwoOutParamsButNamingOne(@Param("arg") Integer arg);

	/**
	 * Explicitly mapped to named stored procedure "User.plus1IO2" in {@link EntityManager}. Returns 2 out params as a
	 * Map.
	 */
	@Procedure(name = "User.plus1IO2") // DATAJPA-707 DATAJPA-1579
	Map<String, Integer> entityAnnotatedCustomNamedProcedurePlus1IO2(@Param("arg") Integer arg);

	/**
	 * Explicitly mapped to named stored procedure "User.plus1IOoptional" in {@link EntityManager}. Returns 2 out params
	 * as a Map, second one amoung which is null.
	 */
	@Procedure(name = "User.plus1IOoptional") // DATAJPA-1579
	Map<String, Integer> entityAnnotatedCustomNamedProcedurePlus1IOoptional(@Param("arg") Integer arg);

	/**
	 * Implicitly mapped to named stored procedure "User.plus1" in {@link EntityManager}.
	 */
	@Procedure // DATAJPA-455
	Integer plus1(@Param("arg") Integer arg);

	// DATAJPA-456
	@Query(value = "select u from User u where u.firstname like ?1%", countProjection = "u.firstname")
	Page<User> findAllByFirstnameLike(String firstname, Pageable page);

	// DATAJPA-456
	@Query(name = "User.findBySpringDataNamedQuery", countProjection = "u.firstname")
	Page<User> findByNamedQueryAndCountProjection(String firstname, Pageable page);

	// DATAJPA-551
	User findFirstByOrderByAgeDesc();

	// DATAJPA-551
	User findFirst1ByOrderByAgeDesc();

	// DATAJPA-551
	User findTopByOrderByAgeDesc();

	// DATAJPA-551
	User findTopByOrderByAgeAsc();

	// DATAJPA-551
	User findTop1ByOrderByAgeAsc();

	// DATAJPA-551
	List<User> findTop2ByOrderByAgeDesc();

	// DATAJPA-551
	List<User> findFirst2ByOrderByAgeDesc();

	// DATAJPA-551
	List<User> findFirst2UsersBy(Sort sort);

	// DATAJPA-551
	List<User> findTop2UsersBy(Sort sort);

	// DATAJPA-551
	Page<User> findFirst3UsersBy(Pageable page);

	// DATAJPA-551
	Page<User> findFirst2UsersBy(Pageable page);

	// DATAJPA-551
	Slice<User> findTop3UsersBy(Pageable page);

	// DATAJPA-551
	Slice<User> findTop2UsersBy(Pageable page);

	// DATAJPA-506
	@NativeQuery("select u.binaryData from SD_User u where u.id = ?1")
	byte[] findBinaryDataByIdNative(Integer id);

	// DATAJPA-506
	@Query("select u from User u where u.emailAddress = ?1")
	Optional<User> findOptionalByEmailAddress(String emailAddress);

	// DATAJPA-564
	@Query("select u from User u where u.firstname = ?#{[0]} and u.firstname = ?1 and u.lastname like %?#{[1]}% and u.lastname like %?2%")
	List<User> findByFirstnameAndLastnameWithSpelExpression(String firstname, String lastname);

	@Query(value = "select * from SD_User", countQuery = "select count(1) from SD_User u where u.lastname = :#{#lastname}", nativeQuery = true)
	Page<User> findByWithSpelParameterOnlyUsedForCountQuery(String lastname, Pageable page);

	// DATAJPA-564
	@Query("select u from User u where u.lastname like %:#{[0]}% and u.lastname like %:lastname%")
	List<User> findByLastnameWithSpelExpression(@Param("lastname") String lastname);

	// DATAJPA-564
	@Query("select u from User u where u.firstname = ?#{'Oliver'}")
	List<User> findOliverBySpELExpressionWithoutArgumentsWithQuestionmark();

	// DATAJPA-564
	@Query("select u from User u where u.firstname = :#{'Oliver'}")
	List<User> findOliverBySpELExpressionWithoutArgumentsWithColon();

	// DATAJPA-564
	@Query("select u from User u where u.age = ?#{[0]}")
	List<User> findUsersByAgeForSpELExpressionByIndexedParameter(int age);

	// DATAJPA-564
	@Query("select u from User u where u.firstname = :firstname and u.firstname = :#{#firstname}")
	List<User> findUsersByFirstnameForSpELExpression(@Param("firstname") String firstname);

	// DATAJPA-564
	@Query("select u from User u where u.emailAddress = ?#{principal.emailAddress}")
	List<User> findCurrentUserWithCustomQuery();

	// DATAJPA-564
	@Query("select u from User u where u.firstname = ?1 and u.firstname=?#{[0]} and u.emailAddress = ?#{principal.emailAddress}")
	List<User> findByFirstnameAndCurrentUserWithCustomQuery(String firstname);

	// DATAJPA-564
	@Query("select u from User u where u.firstname = :#{#firstname}")
	List<User> findUsersByFirstnameForSpELExpressionWithParameterVariableOnly(@Param("firstname") String firstname);

	// DATAJPA-564
	@Query("select u from User u where u.firstname = ?#{[0]}")
	List<User> findUsersByFirstnameForSpELExpressionWithParameterIndexOnly(String firstname);

	// DATAJPA-564
	@Query(value = "select * from (" //
			+ "select u.*, rownum() as RN from (" //
			+ "select * from SD_User ORDER BY ucase(firstname)" //
			+ ") u" //
			+ ") where RN between ?#{ #pageable.offset +1 } and ?#{#pageable.offset + #pageable.pageSize}", //
			countQuery = "select count(u.id) from SD_User u", //
			nativeQuery = true)
	Page<User> findUsersInNativeQueryWithPagination(Pageable pageable);

	// DATAJPA-1140
	@Query("select u from User u where u.firstname =:#{#user.firstname} and u.lastname =:lastname")
	List<User> findUsersByUserFirstnameAsSpELExpressionAndLastnameAsString(@Param("user") User user,
			@Param("lastname") String lastname);

	// DATAJPA-1140
	@Query("select u from User u where u.firstname =:firstname and u.lastname =:#{#user.lastname}")
	List<User> findUsersByFirstnameAsStringAndUserLastnameAsSpELExpression(@Param("firstname") String firstname,
			@Param("user") User user);

	// DATAJPA-1140
	@Query("select u from User u where u.firstname =:#{#user.firstname} and u.lastname =:#{#lastname}")
	List<User> findUsersByUserFirstnameAsSpELExpressionAndLastnameAsFakeSpELExpression(@Param("user") User user,
			@Param("lastname") String lastname);

	// DATAJPA-1140
	@Query("select u from User u where u.firstname =:#{#firstname} and u.lastname =:#{#user.lastname}")
	List<User> findUsersByFirstnameAsFakeSpELExpressionAndUserLastnameAsSpELExpression(
			@Param("firstname") String firstname, @Param("user") User user);

	// DATAJPA-1140
	@Query("select u from User u where u.firstname =:firstname")
	List<User> findUsersByFirstnamePaginated(Pageable page, @Param("firstname") String firstname);

	// DATAJPA-629
	@Query("select u from #{#entityName} u where u.firstname = ?#{[0]} and u.lastname = ?#{[1]}")
	List<User> findUsersByFirstnameForSpELExpressionWithParameterIndexOnlyWithEntityExpression(String firstname,
			String lastname);

	// DATAJPA-606
	List<User> findByAgeIn(Collection<Integer> ages);

	// GH-2013
	Page<User> findByAgeIn(Collection<Integer> ages, Pageable pageable);

	// GH-2013
	Page<User> findByAgeIn(Collection<Integer> ages, PageRequest pageable);

	// DATAJPA-606
	List<User> queryByAgeIn(Integer[] ages);

	// DATAJPA-606
	List<User> queryByAgeInOrFirstname(Integer[] ages, String firstname);

	// DATAJPA-677
	@Query("select u from User u")
	Stream<User> findAllByCustomQueryAndStream();

	// DATAJPA-677
	Stream<User> readAllByFirstnameNotNull();

	// DATAJPA-677
	@Query("select u from User u")
	Stream<User> streamAllPaged(Pageable pageable);

	// DATAJPA-830
	List<User> findByLastnameNotContaining(String part);

	// DATAJPA-829
	List<User> findByRolesContaining(Role role);

	// DATAJPA-829
	List<User> findByRolesNotContaining(Role role);

	// DATAJPA-858
	List<User> findByRolesNameContaining(String name);

	// DATAJPA-1179
	@Query("select u from User u where u.firstname = :#{#firstname} and u.firstname = :#{#firstname}")
	List<User> findUsersByDuplicateSpel(@Param("firstname") String firstname);

	List<RolesAndFirstname> findRolesAndFirstnameBy();

	@Query(value = "FROM User u")
	List<IdOnly> findIdOnly();

	// DATAJPA-1172
	@Query("select u from User u where u.age = :age")
	List<User> findByStringAge(@Param("age") String age);

	// DATAJPA-1185
	<T> Stream<T> findAsStreamByFirstnameLike(String name, Class<T> projectionType);

	// DATAJPA-1185
	<T> List<T> findAsListByFirstnameLike(String name, Class<T> projectionType);

	// DATAJPA-980
	@Query(value = "SELECT firstname, lastname FROM SD_User WHERE id = ?1", nativeQuery = true)
	NameOnly findByNativeQuery(Integer id);

	@NativeQuery("SELECT firstname, lastname FROM SD_User WHERE id = ?1")
	NameOnlyRecord findRecordProjectionByNativeQuery(Integer id);

	// GH-3155
	@NativeQuery(value = "SELECT emailaddress, secondary_email_address FROM SD_User WHERE id = ?1",
			sqlResultSetMapping = "emailDto")
	User.EmailDto findEmailDtoByNativeQuery(Integer id);

	@NativeQuery(value = "SELECT emailaddress, secondary_email_address FROM SD_User WHERE id = ?1")
	EmailOnly findEmailOnlyByNativeQuery(Integer id);

	// DATAJPA-1235
	@Query("SELECT u FROM User u where u.firstname >= ?1 and u.lastname = '000:1'")
	List<User> queryWithIndexedParameterAndColonFollowedByIntegerInString(String firstname);

	/**
	 * TODO: ORDER BY CASE appears to only with Hibernate. The examples attempting to do this through pure JPQL don't
	 * appear to work with Hibernate, so we must set them aside until we can implement HQL.
	 */
	// // DATAJPA-1233
	// @Query(value = "SELECT u FROM User u ORDER BY CASE WHEN (u.firstname >= :name) THEN 0 ELSE 1 END, u.firstname")
	// Page<User> findAllOrderedBySpecialNameSingleParam(@Param("name") String name, Pageable page);
	//
	// // DATAJPA-1233
	// @Query(
	// value = "SELECT u FROM User u WHERE :other = 'x' ORDER BY CASE WHEN (u.firstname >= :name) THEN 0 ELSE 1 END,
	// u.firstname")
	// Page<User> findAllOrderedBySpecialNameMultipleParams(@Param("name") String name, @Param("other") String other,
	// Pageable page);
	//
	// // DATAJPA-1233
	// @Query(
	// value = "SELECT u FROM User u WHERE ?2 = 'x' ORDER BY CASE WHEN (u.firstname >= ?1) THEN 0 ELSE 1 END,
	// u.firstname")
	// Page<User> findAllOrderedBySpecialNameMultipleParamsIndexed(String other, String name, Pageable page);

	// DATAJPA-928
	Page<User> findByNativeNamedQueryWithPageable(Pageable pageable);

	// DATAJPA-928
	@Query(value = "SELECT firstname FROM SD_User ORDER BY UCASE(firstname)", countQuery = "SELECT count(*) FROM SD_User",
			nativeQuery = true)
	Page<String> findByNativeQueryWithPageable(@Param("pageable") Pageable pageable);

	// DATAJPA-1273
	List<NameOnly> findByNamedQueryWithAliasInInvertedOrder();

	// DATAJPA-1301
	@Query("select firstname as firstname, lastname as lastname from User u where u.firstname = 'Oliver'")
	Map<String, Object> findMapWithNullValues();

	// DATAJPA-1307
	@Query(value = "select * from SD_User u where u.emailAddress = ?", nativeQuery = true)
	User findByEmailNativeAddressJdbcStyleParameter(String emailAddress);

	// DATAJPA-1334
	List<NameOnlyDto> findByNamedQueryWithConstructorExpression();

	// DATAJPA-1519
	@Query("select u from User u where u.lastname like %?#{escape([0])}% escape ?#{escapeCharacter()}")
	List<User> findContainingEscaped(String namePart);

	// GH-3619
	@Query("select u from User u where u.lastname like ?${query.lastname:empty}")
	List<User> findWithPropertyPlaceholder();

	// DATAJPA-1303
	List<User> findByAttributesIgnoreCaseIn(Collection<String> attributes);

	// DATAJPA-1303
	List<User> findByAttributesIgnoreCaseNotIn(Collection<String> attributes);

	// DATAJPA-1303
	Page<User> findByAttributesIgnoreCaseIn(Pageable pageable, String... attributes);

	// #2363
	List<NameOnlyDto> findAllDtoProjectedBy();

	// GH-2408
	List<NameOnly> findAllInterfaceProjectedBy();

	// GH-2045, GH-425
	@Query("select concat(?1,u.id,?2) as id from #{#entityName} u")
	List<String> findAllAndSortByFunctionResultPositionalParameter(
			@Param("positionalParameter1") String positionalParameter1,
			@Param("positionalParameter2") String positionalParameter2, Sort sort);

	// GH-2045, GH-425
	@Query("select concat(:namedParameter1,u.id,:namedParameter2) as id from #{#entityName} u")
	List<String> findAllAndSortByFunctionResultNamedParameter(@Param("namedParameter1") String namedParameter1,
			@Param("namedParameter2") String namedParameter2, Sort sort);

	// GH-2555
	@Modifying(clearAutomatically = true)
	@Query(value = "update SD_User u set u.active = false where u.id = :userId", nativeQuery = true)
	void setActiveToFalseWithModifyingNative(@Param("userId") int userId);

	// GH-2578
	@Query(value = "SELECT u.firstname from SD_User u where u.age < 32 " //
			+ "except " //
			+ "SELECT u.firstname from SD_User u where u.age >= 32 ", nativeQuery = true)
	List<String> findWithSimpleExceptNative();

	// GH-2578
	@Query(value = "SELECT u.firstname from SD_User u where u.age < 32 " //
			+ "union " //
			+ "SELECT u.firstname from SD_User u where u.age >= 32 ", nativeQuery = true)
	List<String> findWithSimpleUnionNative();

	// GH-2578
	@Query(value = "SELECT u.firstname from (select * from SD_User u where u.age < 32) u " //
			+ "except " //
			+ "SELECT u.firstname from SD_User u where u.age >= 32 ", nativeQuery = true)
	List<String> findWithComplexExceptNative();

	// GH-2578
	@Query(value = "VALUES (1)", nativeQuery = true)
	List<Integer> valuesStatementNative();

	// GH-2578
	@Query(value = "with sample_data as ( Select * from SD_User u where u.age > 30  ) \n select * from sample_data",
			nativeQuery = true)
	List<User> withNativeStatement();

	// GH-2578
	@Query(value = "with sample_data as ( Select * from SD_User u where u.age > 30  ), \n " //
			+ "another as ( Select * from SD_User u) \n " //
			+ "select lower(s.firstname) as lowFirst from sample_data as s,another as a where s.firstname = a.firstname ",
			nativeQuery = true)
	List<String> complexWithNativeStatement();

	// GH-2607
	List<User> findByAttributesContains(String attribute);

	// GH-2593
	@Modifying
	@Query(
			value = "INSERT INTO SD_User(id,active,age,firstname,lastname,emailAddress,DTYPE) VALUES (9999,true,23,'Diego','K','dk@email.com','User')",
			nativeQuery = true)
	void insertNewUserWithNativeQuery();

	// GH-2593
	@Modifying
	@Query(
			value = "INSERT INTO SD_User(id,active,age,firstname,lastname,emailAddress,DTYPE) VALUES (9999,true,23,'Diego',:lastname,'dk@email.com','User')",
			nativeQuery = true)
	void insertNewUserWithParamNativeQuery(@Param("lastname") String lastname);

	// GH-2641
	@Modifying(clearAutomatically = true)
	@Query(
			value = "merge into sd_user " + "using (select id from sd_user where age < 30) request "
					+ "on (sd_user.id = request.id) " + "when matched then " + "    update set sd_user.age = 30",
			nativeQuery = true)
	int mergeNativeStatement();

	// DATAJPA-1713, GH-2008
	@Query("select u from User u where u.firstname >= (select Min(u0.firstname) from User u0)")
	List<NameOnly> findProjectionBySubselect();

	@Query("select u from User u")
	List<UserExcerpt> findRecordProjection();

	@Query("select u from User u")
	<T> List<T> findRecordProjection(Class<T> projectionType);

	@Query("select u.firstname, u.lastname from User u")
	List<UserExcerpt> findMultiselectRecordProjection();

	@UserRoleCountProjectingQuery
	List<UserRoleCountDtoProjection> dtoProjectionEntityAndAggregatedValue();

	@UserRoleCountProjectingQuery
	Page<UserRoleCountDtoProjection> dtoProjectionEntityAndAggregatedValue(PageRequest page);

	@Query("select u as user, count(r) as roleCount from User u left outer join u.roles r group by u")
	List<UserRoleCountInterfaceProjection> interfaceProjectionEntityAndAggregatedValue();

	@Query("select u as user, count(r) as roleCount from User u left outer join u.roles r group by u")
	List<Map<String, Object>> rawMapProjectionEntityAndAggregatedValue();

	@UserRoleCountProjectingQuery
	<T> List<T> findMultiselectRecordDynamicProjection(Class<T> projectionType);

	Window<User> findBy(OffsetScrollPosition position);

	@Retention(RetentionPolicy.RUNTIME)
	@Query("select u, count(r) from User u left outer join u.roles r group by u")
	@interface UserRoleCountProjectingQuery {}

	interface RolesAndFirstname {

		String getFirstname();

		Set<Role> getRoles();
	}

	interface NameOnly {

		String getFirstname();

		String getLastname();
	}

	interface EmailOnly {
		String getEmailAddress();

		String getSecondaryEmailAddress();
	}

	interface IdOnly {
		int getId();
	}

	record UserExcerpt(String firstname, String lastname) {

	}

	record UserRoleCountDtoProjection(User user, Long roleCount) {}

	interface UserRoleCountInterfaceProjection {
		User getUser();
		Long getRoleCount();
	}

}
