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

import javax.persistence.QueryHint;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.sample.SpecialUser;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository interface for {@code User}s.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User>,
		UserRepositoryCustom {

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
	public User findOne(Integer primaryKey);

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
	@Query(value = "SELECT * FROM User WHERE lastname = ?1", nativeQuery = true)
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

	@Query(value = "SELECT 1 FROM User", nativeQuery = true)
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
}
