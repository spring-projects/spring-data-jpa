/*
 * Copyright 2008-2010 the original author or authors.
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
package org.springframework.data.jpa.repository;

import java.io.Serializable;

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
