/*
 * Copyright 2008-2011 the original author or authors.
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
package org.springframework.data.jpa.repository.support;

import java.io.Serializable;

import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.support.IsNewAware;
import org.springframework.data.repository.support.PersistableEntityInformation;
import org.springframework.data.repository.support.RepositorySupport;


/**
 * Base class to implement JPA CRUD repository implementations. Will use a
 * {@link JpaAnnotationEntityInformation} by default but prefer a
 * {@link PersistableEntityInformation} in case the type to be handled
 * implements {@link Persistable}.
 * 
 * @author Oliver Gierke
 */
public abstract class JpaRepositorySupport<T, ID extends Serializable> extends
        RepositorySupport<T, ID> implements JpaRepository<T, ID> {

    /**
     * @param domainClass
     */
    public JpaRepositorySupport(Class<T> domainClass) {

        super(domainClass);
    }


    @Override
    protected IsNewAware createIsNewStrategy(Class<?> domainClass) {

        if (Persistable.class.isAssignableFrom(domainClass)) {
            return new PersistableEntityInformation();
        } else {
            return new JpaAnnotationEntityInformation(domainClass);
        }
    }
}