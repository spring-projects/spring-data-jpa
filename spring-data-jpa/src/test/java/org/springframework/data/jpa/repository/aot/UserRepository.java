/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.data.jpa.repository.aot;

import jakarta.persistence.QueryHint;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.CrudRepository;

/**
 * @author Christoph Strobl
 * @author Mark Paluch
 */
// TODO: Querydsl, query by example
interface UserRepository extends CrudRepository<User, Integer> {

	List<User> findUserNoArgumentsBy();

	User findOneByEmailAddress(String emailAddress);

	Optional<User> findOptionalOneByEmailAddress(String emailAddress);

	Long countUsersByLastname(String lastname);

	boolean existsUserByLastname(String lastname);

	List<User> findByLastnameStartingWith(String lastname);

	List<User> findTop2ByLastnameStartingWith(String lastname);

	List<User> findByLastnameStartingWithOrderByEmailAddress(String lastname);

	List<User> findByLastnameStartingWith(String lastname, Limit limit);

	List<User> findByLastnameStartingWith(String lastname, Sort sort);

	List<User> findByLastnameStartingWith(String lastname, Sort sort, Limit limit);

	List<User> findByLastnameStartingWith(String lastname, Pageable page);

	Page<User> findPageOfUsersByLastnameStartingWith(String lastname, Pageable page);

	Slice<User> findSliceOfUserByLastnameStartingWith(String lastname, Pageable page);

	Stream<User> streamByLastnameLike(String lastname);

	/* Annotated Queries */

	@Query("select u from User u where u.emailAddress = ?1")
	User findAnnotatedQueryByEmailAddress(String username);

	@Query("select u from User u where u.lastname like ?1%")
	List<User> findAnnotatedQueryByLastname(String lastname);

	@Query("select u from User u where u.lastname like :lastname%")
	List<User> findAnnotatedQueryByLastnameParameter(String lastname);

	@Query("select u from User u where u.lastname like :lastname% or u.lastname like %:lastname")
	List<User> findAnnotatedLikeStartsEnds(String lastname);

	@Query("""
			select u
			from User u
			where u.lastname LIKE ?1%""")
	List<User> findAnnotatedMultilineQueryByLastname(String username);

	@Query("select u from User u where u.lastname like ?1%")
	List<User> findAnnotatedQueryByLastname(String lastname, Limit limit);

	@Query("select u from User u where u.lastname like ?1%")
	List<User> findAnnotatedQueryByLastname(String lastname, Sort sort);

	@Query("select u from User u where u.lastname like ?1%")
	List<User> findAnnotatedQueryByLastname(String lastname, Limit limit, Sort sort);

	@Query("select u from User u where u.lastname like ?1%")
	List<User> findAnnotatedQueryByLastname(String lastname, Pageable pageable);

	@Query("select u from User u where u.lastname like ?1%")
	Page<User> findAnnotatedQueryPageOfUsersByLastname(String lastname, Pageable pageable);

	@Query("select u from User u where u.lastname like ?1% ORDER BY u.lastname")
	Page<User> findAnnotatedQueryPageWithStaticSort(String lastname, Pageable pageable);

	@Query("select u from User u where u.lastname like ?1%")
	Slice<User> findAnnotatedQuerySliceOfUsersByLastname(String lastname, Pageable pageable);

	// Value Expressions

	@Query("select u from #{#entityName} u where u.emailAddress = ?1")
	User findTemplatedByEmailAddress(String emailAddress);

	@Query("select u from User u where u.emailAddress = :#{#emailAddress}")
	User findValueExpressionNamedByEmailAddress(String emailAddress);

	@Query("select u from User u where u.emailAddress = ?#{[0]} or u.firstname = ?${user.dir}")
	User findValueExpressionPositionalByEmailAddress(String emailAddress);

	@NativeQuery(value = "SELECT emailaddress, secondary_email_address FROM SD_User WHERE id = ?1",
			sqlResultSetMapping = "emailDto")
	User.EmailDto findEmailDtoByNativeQuery(Integer id);

	// modifying

	User deleteByEmailAddress(String username);

	// cannot generate delete and return a domain object
	@Modifying
	@Query("delete from User u where u.emailAddress = ?1")
	User deleteAnnotatedQueryByEmailAddress(String username);

	// native queries

	@Query(value = "SELECT firstname FROM SD_User ORDER BY UCASE(firstname)", countQuery = "SELECT count(*) FROM SD_User",
			nativeQuery = true)
	Page<String> findByNativeQueryWithPageable(Pageable pageable);

	// projections

	List<UserDtoProjection> findUserProjectionByLastnameStartingWith(String lastname);

	Page<UserDtoProjection> findUserProjectionByLastnameStartingWith(String lastname, Pageable page);

	// old ones

	@Query("select u from User u where u.firstname = ?1")
	List<User> findAllUsingAnnotatedJpqlQuery(String firstname);

	List<User> findByLastname(String lastname);

	@QueryHints(value = { @QueryHint(name = "jakarta.persistence.cache.storeMode", value = "foo") }, forCounting = false)
	List<User> findHintedByLastname(String lastname);

	@EntityGraph(type = EntityGraph.EntityGraphType.FETCH, value = "User.overview")
	User findWithNamedEntityGraphByFirstname(String firstname);

	@EntityGraph(type = EntityGraph.EntityGraphType.FETCH, attributePaths = { "roles", "manager.roles" })
	User findWithDeclaredEntityGraphByFirstname(String firstname);

	List<User> findByLastnameStartingWithOrderByFirstname(String lastname, Limit limit);

	List<User> findByLastname(String lastname, Sort sort);

	List<User> findByLastname(String lastname, Pageable page);

	List<User> findByLastnameOrderByFirstname(String lastname);

	/**
	 * Retrieve users by their email address. The finder {@literal User.findByEmailAddress} is declared as annotation at
	 * {@code User}.
	 */
	User findByEmailAddress(String emailAddress);

	@Query(name = "User.findByEmailAddress")
	Page<User> findPagedByEmailAddress(Pageable pageable, String emailAddress);

	@Query(name = "User.findByEmailAddress", countQuery = "SELECT CoUnT(u) FROM User u WHERE u.emailAddress = ?1")
	Page<User> findPagedWithCountByEmailAddress(Pageable pageable, String emailAddress);

	@Query(name = "User.findByEmailAddress", countName = "User.findByEmailAddress.count-provided")
	Page<User> findPagedWithNamedCountByEmailAddress(Pageable pageable, String emailAddress);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update User u set u.lastname = ?1")
	int renameAllUsersTo(String lastname);

}
