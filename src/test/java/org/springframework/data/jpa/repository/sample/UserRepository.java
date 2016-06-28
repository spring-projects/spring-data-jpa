/*
 * Copyright 2008-2014 the original author or authors.
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
	 * Redeclaration of {@link CrudRepository#findOne(java.io.Serializable)} to change transaction configuration.
	 */
	@Transactional
	User findOne(Integer primaryKey);

	/**
	 * Redeclaration of {@link CrudRepository#delete(java.io.Serializable)}. to make sure the transaction configuration of
	 * the original method is considered if the redeclaration does not carry a {@link Transactional} annotation.
	 * 
	 * @see DATACMNS-649
	 */
	void delete(Integer id);

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
	 * @param username
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

	/**
	 * @see DATAJPA-292
	 */
	@Query("select u from User u where u.firstname like ?1%")
	List<User> findByFirstnameLike(String firstname);

	/**
	 * @see DATAJPA-292
	 */
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
	 * @param firstname
	 * @param lastname
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

	/**
	 * @see DATAJPA-117
	 */
	@Query(value = "SELECT * FROM SD_User WHERE lastname = ?1", nativeQuery = true)
	List<User> findNativeByLastname(String lastname);

	/**
	 * @see DATAJPA-132
	 */
	List<User> findByActiveTrue();

	/**
	 * @see DATAJPA-132
	 */
	List<User> findByActiveFalse();

	/**
	 * Commented out until OpenJPA supports this.
	 */
	// @Query("select u.colleagues from User u where u = ?1")
	// List<User> findColleaguesFor(User user);

	/**
	 * @see DATAJPA-188
	 */
	List<User> findByCreatedAtBefore(Date date);

	/**
	 * @see DATAJPA-188
	 */
	List<User> findByCreatedAtAfter(Date date);

	/**
	 * @see DATAJPA-180
	 */
	List<User> findByFirstnameStartingWith(String firstname);

	/**
	 * @see DATAJPA-180
	 */
	List<User> findByFirstnameEndingWith(String firstname);

	/**
	 * @see DATAJPA-180
	 */
	List<User> findByFirstnameContaining(String firstname);

	@Query(value = "SELECT 1 FROM SD_User", nativeQuery = true)
	List<Integer> findOnesByNativeQuery();

	/**
	 * @see DATAJPA-231
	 */
	long countByLastname(String lastname);

	/**
	 * @see DATAJPA-231
	 */
	int countUsersByFirstname(String firstname);

	/**
	 * @see DATAJPA-920
	 */
	boolean existsByLastname(String lastname);

	/**
	 * @see DATAJPA-391
	 */
	@Query("select u.firstname from User u where u.lastname = ?1")
	List<String> findFirstnamesByLastname(String lastname);

	/**
	 * @see DATAJPA-415
	 */
	Collection<User> findByIdIn(@Param("ids") Integer... ids);

	/**
	 * @see DATAJPA-461
	 */
	@Query("select u from User u where u.id in ?1")
	Collection<User> findByIdsCustomWithPositionalVarArgs(Integer... ids);

	/**
	 * @see DATAJPA-461
	 */
	@Query("select u from User u where u.id in :ids")
	Collection<User> findByIdsCustomWithNamedVarArgs(@Param("ids") Integer... ids);

	/**
	 * @see DATAJPA-415
	 */
	@Modifying
	@Query("update #{#entityName} u set u.active = :activeState where u.id in :ids")
	void updateUserActiveState(@Param("activeState") boolean activeState, @Param("ids") Integer... ids);

	/**
	 * @see DATAJPA-405
	 */
	List<User> findAllByOrderByLastnameAsc();

	/**
	 * @see DATAJPA-454
	 */
	List<User> findByBinaryData(byte[] data);

	/**
	 * @see DATAJPA-486
	 */
	Slice<User> findSliceByLastname(String lastname, Pageable pageable);

	/**
	 * @see DATAJPA-496
	 */
	List<User> findByAttributesIn(Set<String> attributes);

	/**
	 * @see DATAJPA-460
	 */
	Long removeByLastname(String lastname);

	/**
	 * @see DATAJPA-460
	 */
	List<User> deleteByLastname(String lastname);

	/**
	 * @see DATAJPA-505
	 * @see https://issues.apache.org/jira/browse/OPENJPA-2484
	 */
	// @Query(value = "select u.binaryData from User u where u.id = :id")
	// byte[] findBinaryDataByIdJpaQl(@Param("id") Integer id);

	/**
	 * Explicitly mapped to a procedure with name "plus1inout" in database.
	 * 
	 * @see DATAJPA-455
	 */
	@Procedure("plus1inout")
	Integer explicitlyNamedPlus1inout(Integer arg);

	/**
	 * Implicitly mapped to a procedure with name "plus1inout" in database via alias.
	 * 
	 * @see DATAJPA-455
	 */
	@Procedure(procedureName = "plus1inout")
	Integer plus1inout(Integer arg);

	/**
	 * Explicitly mapped to named stored procedure "User.plus1IO" in {@link EntityManager}.
	 * 
	 * @see DATAJPA-455
	 */
	@Procedure(name = "User.plus1IO")
	Integer entityAnnotatedCustomNamedProcedurePlus1IO(@Param("arg") Integer arg);

	/**
	 * Implicitly mapped to named stored procedure "User.plus1" in {@link EntityManager}.
	 * 
	 * @see DATAJPA-455
	 */
	@Procedure
	Integer plus1(@Param("arg") Integer arg);

	/**
	 * @see DATAJPA-456
	 */
	@Query(value = "select u from User u where u.firstname like ?1%", countProjection = "u.firstname")
	Page<User> findAllByFirstnameLike(String firstname, Pageable page);

	/**
	 * @see DATAJPA-456
	 */
	@Query(name = "User.findBySpringDataNamedQuery", countProjection = "u.firstname")
	Page<User> findByNamedQueryAndCountProjection(String firstname, Pageable page);

	/**
	 * @see DATAJPA-551
	 */
	User findFirstByOrderByAgeDesc();

	/**
	 * @see DATAJPA-551
	 */
	User findFirst1ByOrderByAgeDesc();

	/**
	 * @see DATAJPA-551
	 */
	User findTopByOrderByAgeDesc();

	/**
	 * @see DATAJPA-551
	 */
	User findTopByOrderByAgeAsc();

	/**
	 * @see DATAJPA-551
	 */
	User findTop1ByOrderByAgeAsc();

	/**
	 * @see DATAJPA-551
	 */
	List<User> findTop2ByOrderByAgeDesc();

	/**
	 * @see DATAJPA-551
	 */
	List<User> findFirst2ByOrderByAgeDesc();

	/**
	 * @see DATAJPA-551
	 */
	List<User> findFirst2UsersBy(Sort sort);

	/**
	 * @see DATAJPA-551
	 */
	List<User> findTop2UsersBy(Sort sort);

	/**
	 * @see DATAJPA-551
	 */
	Page<User> findFirst3UsersBy(Pageable page);

	/**
	 * @see DATAJPA-551
	 */
	Page<User> findFirst2UsersBy(Pageable page);

	/**
	 * @see DATAJPA-551
	 */
	Slice<User> findTop3UsersBy(Pageable page);

	/**
	 * @see DATAJPA-551
	 */
	Slice<User> findTop2UsersBy(Pageable page);

	/**
	 * @see DATAJPA-506
	 */
	@Query(value = "select u.binaryData from SD_User u where u.id = ?1", nativeQuery = true)
	byte[] findBinaryDataByIdNative(Integer id);

	/**
	 * @see DATAJPA-506
	 */
	@Query("select u from User u where u.emailAddress = ?1")
	Optional<User> findOptionalByEmailAddress(String emailAddress);

	/**
	 * @see DATAJPA-564
	 */
	@Query("select u from User u where u.firstname = ?#{[0]} and u.firstname = ?1 and u.lastname like %?#{[1]}% and u.lastname like %?2%")
	List<User> findByFirstnameAndLastnameWithSpelExpression(String firstname, String lastname);

	/**
	 * @see DATAJPA-564
	 */
	@Query("select u from User u where u.lastname like %:#{[0]}% and u.lastname like %:lastname%")
	List<User> findByLastnameWithSpelExpression(@Param("lastname") String lastname);

	/**
	 * @see DATAJPA-564
	 */
	@Query("select u from User u where u.firstname = ?#{'Oliver'}")
	List<User> findOliverBySpELExpressionWithoutArgumentsWithQuestionmark();

	/**
	 * @see DATAJPA-564
	 */
	@Query("select u from User u where u.firstname = :#{'Oliver'}")
	List<User> findOliverBySpELExpressionWithoutArgumentsWithColon();

	/**
	 * @see DATAJPA-564
	 */
	@Query("select u from User u where u.age = ?#{[0]}")
	List<User> findUsersByAgeForSpELExpressionByIndexedParameter(int age);

	/**
	 * @see DATAJPA-564
	 */
	@Query("select u from User u where u.firstname = :firstname and u.firstname = :#{#firstname}")
	List<User> findUsersByFirstnameForSpELExpression(@Param("firstname") String firstname);

	/**
	 * @see DATAJPA-564
	 */
	@Query("select u from User u where u.emailAddress = ?#{principal.emailAddress}")
	List<User> findCurrentUserWithCustomQuery();

	/**
	 * @see DATAJPA-564
	 */
	@Query("select u from User u where u.firstname = ?1 and u.firstname=?#{[0]} and u.emailAddress = ?#{principal.emailAddress}")
	List<User> findByFirstnameAndCurrentUserWithCustomQuery(String firstname);

	/**
	 * @see DATAJPA-564
	 */
	@Query("select u from User u where u.firstname = :#{#firstname}")
	List<User> findUsersByFirstnameForSpELExpressionWithParameterVariableOnly(@Param("firstname") String firstname);

	/**
	 * @see DATAJPA-564
	 */
	@Query("select u from User u where u.firstname = ?#{[0]}")
	List<User> findUsersByFirstnameForSpELExpressionWithParameterIndexOnly(String firstname);

	/**
	 * @see DATAJPA-564
	 */
	@Query(
			value = "select * from (select rownum() as RN, u.* from SD_User u) where RN between ?#{ #pageable.offset -1} and ?#{#pageable.offset + #pageable.pageSize}",
			countQuery = "select count(u.id) from SD_User u", nativeQuery = true)
	Page<User> findUsersInNativeQueryWithPagination(Pageable pageable);

	/**
	 * @see DATAJPA-629
	 */
	@Query("select u from #{#entityName} u where u.firstname = ?#{[0]} and u.lastname = ?#{[1]}")
	List<User> findUsersByFirstnameForSpELExpressionWithParameterIndexOnlyWithEntityExpression(String firstname,
			String lastname);

	/**
	 * @see DATAJPA-606
	 */
	List<User> findByAgeIn(Collection<Integer> ages);

	/**
	 * @see DATAJPA-606
	 */
	List<User> queryByAgeIn(Integer[] ages);

	/**
	 * @see DATAJPA-606
	 */
	List<User> queryByAgeInOrFirstname(Integer[] ages, String firstname);

	/**
	 * @see DATAJPA-677
	 */
	@Query("select u from User u")
	Stream<User> findAllByCustomQueryAndStream();

	/**
	 * @see DATAJPA-677
	 */
	Stream<User> readAllByFirstnameNotNull();

	/**
	 * @see DATAJPA-677
	 */
	@Query("select u from User u")
	Stream<User> streamAllPaged(Pageable pageable);

	/**
	 * @see DATAJPA-830
	 */
	List<User> findByLastnameNotContaining(String part);

	/**
	 * @see DATAJPA-829
	 */
	List<User> findByRolesContaining(Role role);

	/**
	 * @see DATAJPA-829
	 */
	List<User> findByRolesNotContaining(Role role);

	/**
	 * @see DATAJPA-858
	 */
	List<User> findByRolesNameContaining(String name);
}
