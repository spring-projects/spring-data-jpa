package org.springframework.data.jpa.repository.sample;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

/**
 * Custom Repository Interface that adjusts the querys of well known repository interface Methods via {@link Query}
 * annotation.
 * 
 * @author Thomas Darimont
 */
public interface RedeclaringRepositoryMethodsRepository extends CrudRepository<User, Long> {

	/**
	 * Should not find any users at all.
	 */
	@Query("SELECT u FROM User u where u.id = -1")
	List<User> findAll();

	/**
	 * Should only find users with the firstname 'Oliver'.
	 * 
	 * @param page
	 * @return
	 */
	@Query("SELECT u FROM User u where u.firstname = 'Oliver'")
	Page<User> findAll(Pageable page);
}
