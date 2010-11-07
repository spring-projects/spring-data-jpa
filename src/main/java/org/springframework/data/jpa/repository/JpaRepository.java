package org.springframework.data.jpa.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.Repository;


/**
 * JPA specific extension of {@link Repository}.
 * 
 * @author Oliver Gierke
 */
public interface JpaRepository<T, ID extends Serializable> extends
        PagingAndSortingRepository<T, ID> {

    /**
     * Returns a single entity matching the given {@link Specification}.
     * 
     * @param spec
     * @return
     */
    T findOne(Specification<T> spec);


    /**
     * Returns all entities matching the given {@link Specification}.
     * 
     * @param spec
     * @return
     */
    List<T> findAll(Specification<T> spec);


    /**
     * Returns a {@link Page} of entities matching the given
     * {@link Specification}.
     * 
     * @param spec
     * @param pageable
     * @return
     */
    Page<T> findAll(Specification<T> spec, Pageable pageable);


    /**
     * Flushes all pending changes to the database.
     */
    void flush();


    /**
     * Saves an entity and flushes changes instantly.
     * 
     * @param entity
     * @return the saved entity
     */
    T saveAndFlush(T entity);
}
