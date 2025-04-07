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
import org.springframework.data.jpa.repository.QueryRewriter;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * @author Christoph Strobl
 * @author Mark Paluch
 */
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

	// -------------------------------------------------------------------------
	// Declared Queries
	// -------------------------------------------------------------------------

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

	// -------------------------------------------------------------------------
	// Value Expressions
	// -------------------------------------------------------------------------

	@Query("select u from #{#entityName} u where u.emailAddress = ?1")
	User findTemplatedByEmailAddress(String emailAddress);

	@Query("select u from User u where u.emailAddress = :#{#emailAddress}")
	User findValueExpressionNamedByEmailAddress(String emailAddress);

	@Query("select u from User u where u.emailAddress = ?#{[0]} or u.firstname = ?${user.dir}")
	User findValueExpressionPositionalByEmailAddress(String emailAddress);

	// -------------------------------------------------------------------------
	// Projections: DTO
	// -------------------------------------------------------------------------

	List<UserDtoProjection> findUserProjectionByLastnameStartingWith(String lastname);

	Page<UserDtoProjection> findUserProjectionByLastnameStartingWith(String lastname, Pageable page);

	Names findDtoByEmailAddress(String emailAddress);

	Page<Names> findDtoPageByEmailAddress(String emailAddress, Pageable pageable);

	@Query("select u from User u where u.emailAddress = ?1")
	Names findAnnotatedDtoEmailAddress(String emailAddress);

	@Query("select u from User u where u.emailAddress = ?1")
	Page<Names> findAnnotatedDtoPageByEmailAddress(String emailAddress, Pageable pageable);

	@NativeQuery(value = "SELECT emailaddress, secondary_email_address FROM SD_User WHERE id = ?1",
			sqlResultSetMapping = "emailDto")
	User.EmailDto findEmailDtoByNativeQuery(Integer id);

	@Query(name = "User.findByEmailAddress")
	Names findNamedDtoEmailAddress(String emailAddress);

	// -------------------------------------------------------------------------
	// Projections: Interface
	// -------------------------------------------------------------------------

	EmailOnly findEmailProjectionById(Integer id);

	Page<EmailOnly> findProjectedPageByEmailAddress(String emailAddress, Pageable page);

	Slice<EmailOnly> findProjectedSliceByEmailAddress(String lastname, Pageable page);

	Stream<EmailOnly> streamProjectedByEmailAddress(String lastname);

	@Query("select u from User u where u.emailAddress = ?1")
	EmailOnly findAnnotatedEmailProjectionByEmailAddress(String emailAddress);

	@Query("select u from User u where u.emailAddress = ?1")
	Page<EmailOnly> findAnnotatedProjectedPageByEmailAddress(String emailAddress, Pageable page);

	@NativeQuery(value = "SELECT emailaddress as emailAddress FROM SD_User WHERE id = ?1")
	EmailOnly findEmailProjectionByNativeQuery(Integer id);

	@Query(name = "User.findByEmailAddress")
	EmailOnly findNamedProjectionEmailAddress(String emailAddress);

	// -------------------------------------------------------------------------
	// Modifying
	// -------------------------------------------------------------------------

	User deleteByEmailAddress(String username);

	// cannot generate delete and return a domain object
	@Modifying
	@Query("delete from User u where u.emailAddress = ?1")
	User deleteAnnotatedQueryByEmailAddress(String username);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update User u set u.lastname = ?1")
	int renameAllUsersTo(String lastname);

	// -------------------------------------------------------------------------
	// Native Queries
	// -------------------------------------------------------------------------

	@Query(value = "SELECT firstname FROM SD_User ORDER BY UCASE(firstname)", countQuery = "SELECT count(*) FROM SD_User",
			nativeQuery = true)
	Page<String> findByNativeQueryWithPageable(Pageable pageable);

	// -------------------------------------------------------------------------
	// Named Queries
	// -------------------------------------------------------------------------

	User findByEmailAddress(String emailAddress);

	@Query(name = "User.findByEmailAddress")
	Page<User> findPagedByEmailAddress(Pageable pageable, String emailAddress);

	@Query(name = "User.findByEmailAddress", countQuery = "SELECT CoUnT(u) FROM User u WHERE u.emailAddress = ?1")
	Page<User> findPagedWithCountByEmailAddress(Pageable pageable, String emailAddress);

	@Query(name = "User.findByEmailAddress", countName = "User.findByEmailAddress.count-provided")
	Page<User> findPagedWithNamedCountByEmailAddress(Pageable pageable, String emailAddress);

	// -------------------------------------------------------------------------
	// Query Hints
	// -------------------------------------------------------------------------

	@QueryHints(value = { @QueryHint(name = "jakarta.persistence.cache.storeMode", value = "foo") }, forCounting = false)
	List<User> findHintedByLastname(String lastname);

	@EntityGraph(type = EntityGraph.EntityGraphType.FETCH, value = "User.overview")
	User findWithNamedEntityGraphByFirstname(String firstname);

	@EntityGraph(type = EntityGraph.EntityGraphType.FETCH, attributePaths = { "roles", "manager.roles" })
	User findWithDeclaredEntityGraphByFirstname(String firstname);

	@Query("select u from User u where u.emailAddress = ?1 AND TYPE(u) = ?2")
	<T extends User> T findByEmailAddress(String emailAddress, Class<T> type);

	@Query(value = "select u from PLACEHOLDER u where u.emailAddress = ?1", queryRewriter = MyQueryRewriter.class)
	User findAndApplyQueryRewriter(String emailAddress);

	@Query(value = "select u from OTHER u where u.emailAddress = ?1", queryRewriter = MyQueryRewriter.class)
	Page<User> findAndApplyQueryRewriter(String emailAddress, Pageable pageable);

	// -------------------------------------------------------------------------
	// Unsupported: Procedures
	// -------------------------------------------------------------------------
	@Procedure(name = "User.plus1IO") // Named
	Integer namedProcedure(@Param("arg") Integer arg);

	@Procedure(value = "sp_add") // Stored procedure
	Integer providedProcedure(@Param("arg") Integer arg);

	interface EmailOnly {
		String getEmailAddress();
	}

	record Names(String firstname, String lastname) {
	}

	static class MyQueryRewriter implements QueryRewriter {

		@Override
		public String rewrite(String query, Sort sort) {
			return query.replaceAll("PLACEHOLDER", "User");
		}

		@Override
		public String rewrite(String query, Pageable pageRequest) {
			return query.replaceAll("OTHER", "User");
		}
	}
}
