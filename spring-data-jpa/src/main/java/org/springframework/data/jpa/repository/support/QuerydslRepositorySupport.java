/*
 * Copyright 2011-present the original author or authors.
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
package org.springframework.data.jpa.repository.support;

import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Repository;

import com.querydsl.core.types.dsl.PathBuilder;

/**
 * Base class for implementing repositories using Querydsl library.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Maxim Shabanov
 */
@Repository
public abstract non-sealed class QuerydslRepositorySupport<T> extends QuerydslRepositorySupportBase {

    /**
     * Creates a new {@link QuerydslRepositorySupport} instance.
     * <p>
     * This constructor automatically detects the domain type {@code T} by resolving the generic parameter of the
     * concrete subclass.
     * 
     * @throws IllegalArgumentException if the domain type {@code T} cannot be resolved from the generic signature of
     *             the subclass.
     */
    public QuerydslRepositorySupport() {
        super();
    }

    /**
     * Creates a new {@link QuerydslRepositorySupport} instance for the given domain type.
     *
     * @param domainClass must not be {@literal null}.
     */
    public QuerydslRepositorySupport(Class<?> domainClass) {
        super(domainClass);
    }

    /**
     * Returns a type-safe {@link PathBuilder} for the configured domain type {@code T}.
     *
     * @return the Querdsl {@link PathBuilder}.
     */
    @SuppressWarnings("unchecked")
    @Override
    protected PathBuilder<T> getBuilder() {
        return (PathBuilder<T>) super.getBuilder();
    }

    @Override
    Class<?> resolveDomainClass() {

        Class<?> domainClass = ResolvableType.forClass(this.getClass()).as(QuerydslRepositorySupport.class)
                .getGeneric(0).resolve();

        if (domainClass == null) {
            throw new IllegalArgumentException(
                    String.format("Could not resolve generic parameter for %s", this.getClass()));
        }

        return domainClass;
    }
}
