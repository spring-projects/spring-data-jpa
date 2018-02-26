/*
 * Copyright 2008-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository.sample;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.QueryHint;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.domain.sample.SpecialUser;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Optional;

/**
 * Repository interface for {@code User}s.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Kevin Peters
 */
public interface UserRepository
		extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User>, UserRepositoryCustom {

	/**
	 * Retrieve users by their lastname. The finder {@literal User.findByLastname} is declared in
	 * {@literal META-INF/orm.xml} .
	 *
	 * @param lastname
	 * @return all users with the given lastname
	 */
	@QueryHints({ @QueryHint(name = "foo", value = "bar") })
	List<User> findByLastname(String lastname);

	/**
	 * Redeclaration of {@link CrudRepository#findById(java.io.Serializable)} to change transaction configuration.
	 */
	@Transactional
	java.util.Optional<User> findById(Integer primaryKey);

	/**
	 * Redeclaration of {@link CrudRepository#deleteById(java.io.Serializable)}. to make sure the transaction
	 * configuration of the original method is considered if the redeclaration does not carry a {@link Transactional}
	 * annotation.
	 */
	void deleteById(Integer id); // DATACMNS-649

	/**
	 * Retrieve users by their email address. The finder {@literal User.findByEmailAddress} is declared as annotation at
	 * {@code User}.
	 *
	 * @param emailAddress
	 * @return the user with the given email address
	 */
	User findByEmailAddress(String emailAddress);

	@Query("select u from User u left outer join u.manager as manager")
	Page<User> findAllPaged(Pageable pageable);

	/**
	 * Retrieves users by the given email and lastname. Acts as a dummy method declaration to test finder query creation.
	 *
	 * @param emailAddress
	 * @param lastname
	 * @return the user with the given email address and lastname
	 */
	User findByEmailAddressAndLastname(String emailAddress, String lastname);

	/**
	 * Retrieves users by email address and lastname or firstname. Acts as a dummy method declaration to test finder query
	 * creation.
	 *
	 * @param emailAddress
	 * @param lastname
	 * @param username
	 * @return the users with the given email address and lastname or the given firstname
	 */
	List<User> findByEmailAddressAndLastnameOrFirstname(String emailAddress, String lastname, String username);

	/**
	 * Retrieves a user by its username using the query annotated to the method.
	 *
	 * @param emailAddress
	 * @return
	 */
	@Query("select u from User u where u.emailAddress = ?1")
	@Transactional(readOnly = true)
	User findByAnnotatedQuery(String emailAddress);

	/**
	 * Method to directly create query from and adding a {@link Pageable} parameter to be regarded on query execution.
	 *
	 * @param pageable
	 * @param lastname
	 * @return
	 */
	Page<User> findByLastname(Pageable pageable, String lastname);

	/**
	 * Method to directly create query from and adding a {@link Pageable} parameter to be regarded on query execution.
	 * Just returns the queried {@link Page}'s contents.
	 *
	 * @param firstname
	 * @param pageable
	 * @return
	 */
	List<User> findByFirstname(String firstname, Pageable pageable);

	Page<User> findByFirstnameIn(Pageable pageable, String... firstnames);

	List<User> findByFirstnameNotIn(Collection<String> firstnames);

	// DATAJPA-292
	@Query("select u from User u where u.firstname like ?1%")
	List<User> findByFirstnameLike(String firstname);

	// DATAJPA-292
	@Query("select u from User u where u.firstname like :firstname%")
	List<User> findByFirstnameLikeNamed(@Param("firstname") String firstname);

	/**
	 * Manipulating query to set all {@link User}'s names to the given one.
	 *
	 * @param lastname
	 */
	@Modifying
	@Query("update User u set u.lastname = ?1")
	void renameAllUsersTo(String lastname);

	@Query("select count(u) from User u where u.firstname = ?1")
	Long countWithFirstname(String firstname);

	/**
	 * Method where parameters will be applied by name. Note that the order of the parameters is then not crucial anymore.
	 *
	 * @param foo
	 * @param bar
	 * @return
	 */
	@Query("select u from User u where u.lastname = :lastname or u.firstname = :firstname")
	List<User> findByLastnameOrFirstname(@Param("firstname") String foo, @Param("lastname") String bar);

	@Query("select u from User u where u.lastname = :lastname or u.firstname = :firstname")
	List<User> findByLastnameOrFirstnameUnannotated(String firstname, String lastname);

	/**
	 * Method to check query creation and named parameter usage go well hand in hand.
	 *
	 * @param lastname
	 * @param firstname
	 * @return
	 */
	List<User> findByFirstnameOrLastname(@Param("lastname") String lastname, @Param("firstname") String firstname);

	List<User> findByLastnameLikeOrderByFirstnameDesc(String lastname);

	List<User> findByLastnameNotLike(String lastname);

	List<User> findByLastnameNot(String lastname);

	List<User> findByManagerLastname(String name);

	List<User> findByColleaguesLastname(String lastname);

	List<User> findByLastnameNotNull();

	List<User> findByLastnameNull();

	List<User> findByEmailAddressLike(String email, Sort sort);

	List<SpecialUser> findSpecialUsersByLastname(String lastname);

	List<User> findBySpringDataNamedQuery(String lastname);

	List<User> findByLastnameIgnoringCase(String lastname);

	Page<User> findByLastnameIgnoringCase(Pageable pageable, String lastname);

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
	 * Explicitly mapped to named stored procedure "User.plus1IO" in {@link EntityManager}.
	 */
	@Procedure(name = "User.plus1IO") // DATAJPA-455
	Integer entityAnnotatedCustomNamedProcedurePlus1IO(@Param("arg") Integer arg);

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
	@Query(value = "select u.binaryData from SD_User u where u.id = ?1", nativeQuery = true)
	byte[] findBinaryDataByIdNative(Integer id);

	// DATAJPA-506
	@Query("select u from User u where u.emailAddress = ?1")
	Optional<User> findOptionalByEmailAddress(String emailAddress);

	// DATAJPA-564
	@Query("select u from User u where u.firstname = ?#{[0]} and u.firstname = ?1 and u.lastname like %?#{[1]}% and u.lastname like %?2%")
	List<User> findByFirstnameAndLastnameWithSpelExpression(String firstname, String lastname);

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
	@Query(
			value = "select * from (select rownum() as RN, u.* from SD_User u) where RN between ?#{ #pageable.offset -1} and ?#{#pageable.offset + #pageable.pageSize}",
			countQuery = "select count(u.id) from SD_User u", nativeQuery = true)
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

	// DATAJPA-1248
	@Query(value = "SELECT emailaddress FROM SD_User WHERE id = ?1", nativeQuery = true)
	EmailOnly findEmailOnlyByNativeQuery(Integer id);

	// DATAJPA-1235
	@Query("SELECT u FROM User u where u.firstname >= ?1 and u.lastname = '000:1'")
	List<User> queryWithIndexedParameterAndColonFollowedByIntegerInString(String firstname);

	// DATAJPA-1233
	@Query(value = "SELECT u FROM User u ORDER BY CASE WHEN (u.firstname  >= :name) THEN 0 ELSE 1 END, u.firstname")
	Page<User> findAllOrderedBySpecialNameSingleParam(@Param("name") String name, Pageable page);

	// DATAJPA-1233
	@Query(value = "SELECT u FROM User u WHERE :other = 'x' ORDER BY CASE WHEN (u.firstname  >= :name) THEN 0 ELSE 1 END, u.firstname")
	Page<User> findAllOrderedBySpecialNameMultipleParams(@Param("name") String name, @Param("other") String other, Pageable page);

	// DATAJPA-1233
	@Query(value = "SELECT u FROM User u WHERE ?2 = 'x' ORDER BY CASE WHEN (u.firstname  >= ?1) THEN 0 ELSE 1 END, u.firstname")
	Page<User> findAllOrderedBySpecialNameMultipleParamsIndexed(String name, String other, Pageable page);

	// DATAJPA-928
	Page<User> findByNativeNamedQueryWithPageable(Pageable pageable);

	// DATAJPA-928
	@Query(value = "SELECT firstname FROM SD_User ORDER BY UCASE(firstname)", countQuery = "SELECT count(*) FROM SD_User", nativeQuery = true)
	Page<String> findByNativeQueryWithPageable(@Param("pageable") Pageable pageable);

	// DATAJPA-1273
	List<NameOnly> findByNamedQueryWithAliasInInvertedOrder();

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
	}
}
