/*
 * Copyright 2024 the original author or authors.
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
package com.example;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * @author Christoph Strobl
 */
public interface UserRepository extends CrudRepository<User, Integer> {

	List<User> findUserNoArgumentsBy();

	User findOneByEmailAddress(String emailAddress);

	Optional<User> findOptionalOneByEmailAddress(String emailAddress);

	Long countUsersByLastname(String lastname);

	Boolean existsUserByLastname(String lastname);

	List<User> findByLastnameStartingWith(String lastname);

	List<User> findTop2ByLastnameStartingWith(String lastname);

	List<User> findByLastnameStartingWithOrderByEmailAddress(String lastname);

	List<User> findByLastnameStartingWith(String lastname, Limit limit);

	List<User> findByLastnameStartingWith(String lastname, Sort sort);

	List<User> findByLastnameStartingWith(String lastname, Sort sort, Limit limit);

	List<User> findByLastnameStartingWith(String lastname, Pageable page);

	Page<User> findPageOfUsersByLastnameStartingWith(String lastname, Pageable page);

	Slice<User> findSliceOfUserByLastnameStartingWith(String lastname, Pageable page);

	/* Annotated Queries */

	@Query("select u from User u where u.emailAddress = ?1")
	User findAnnotatedQueryByEmailAddress(String username);

	@Query("select u from User u where u.lastname like ?1%")
	List<User> findAnnotatedQueryByLastname(String lastname);

	@Query("select u from User u where u.lastname like :lastname%")
	List<User> findAnnotatedQueryByLastnameParamter(String lastname);

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

	@Query("select u from User u where u.lastname like ?1%")
	Slice<User> findAnnotatedQuerySliceOfUsersByLastname(String lastname, Pageable pageable);

	// modifying

	User deleteByEmailAddress(String username);

	Long deleteReturningDeleteCountByEmailAddress(String username);

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

	List<User> findByLastnameStartingWithOrderByFirstname(String lastname, Limit limit);

	List<User> findByLastname(String lastname, Sort sort);

	List<User> findByLastname(String lastname, Pageable page);

	List<User> findByLastnameOrderByFirstname(String lastname);

	User findByEmailAddress(String emailAddress);
}
