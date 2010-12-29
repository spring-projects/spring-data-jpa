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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.repository.QueryDslPredicateExecutor;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.impl.JPAQuery;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.Expression;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.PathBuilder;


/**
 * QueryDsl specific extension of {@link SimpleJpaRepository} which adds
 * implementation for {@link QueryDslPredicateExecutor}.
 * 
 * @author Oliver Gierke
 */
public class QueryDslJpaRepository<T, ID extends Serializable> extends
        SimpleJpaRepository<T, ID> implements QueryDslPredicateExecutor<T> {

    private final EntityManager em;
    private final EntityPath<T> path;
    private final PathBuilder<T> builder;


    /**
     * Creates a new {@link QueryDslJpaRepository} from the given domain class
     * and {@link EntityManager}. This will use the
     * {@link SimpleEntityPathResolver} to translate the given domain class into
     * an {@link EntityPath}.
     * 
     * @param domainClass
     * @param entityManager
     */
    public QueryDslJpaRepository(JpaEntityInformation<T, ID> entityMetadata,
            EntityManager entityManager) {

        this(entityMetadata, entityManager, SimpleEntityPathResolver.INSTANCE);
    }


    /**
     * Creates a new {@link QueryDslJpaRepository} from the given domain class
     * and {@link EntityManager} and uses the given {@link EntityPathResolver}
     * to translate the domain class into an {@link EntityPath}.
     * 
     * @param domainClass
     * @param entityManager
     * @param resolver
     */
    public QueryDslJpaRepository(JpaEntityInformation<T, ID> entityMetadata,
            EntityManager entityManager, EntityPathResolver resolver) {

        super(entityMetadata, entityManager);
        this.em = entityManager;
        this.path = resolver.createPath(entityMetadata.getJavaType());
        this.builder = new PathBuilder<T>(path.getType(), path.getMetadata());
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.jpa.repository.querydsl.
     * QueryDslSpecificationExecutor#findOne(com.mysema.query.types.Predicate)
     */
    public T findOne(Predicate predicate) {

        return createQuery(predicate).uniqueResult(path);
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.jpa.repository.querydsl.
     * QueryDslSpecificationExecutor#findAll(com.mysema.query.types.Predicate)
     */
    public List<T> findAll(Predicate predicate) {

        return createQuery(predicate).list(path);
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.jpa.repository.querydsl.
     * QueryDslSpecificationExecutor#findAll(com.mysema.query.types.Predicate,
     * com.mysema.query.types.OrderSpecifier<?>[])
     */
    public List<T> findAll(Predicate predicate, OrderSpecifier<?>... orders) {

        return createQuery(predicate).orderBy(orders).list(path);
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.jpa.repository.querydsl.
     * QueryDslSpecificationExecutor#findAll(com.mysema.query.types.Predicate,
     * org.springframework.data.domain.Pageable)
     */
    public Page<T> findAll(Predicate predicate, Pageable pageable) {

        JPQLQuery countQuery = createQuery(predicate);
        JPQLQuery query = applyPagination(createQuery(predicate), pageable);

        return new PageImpl<T>(query.list(path), pageable, countQuery.count());
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.jpa.repository.querydsl.
     * QueryDslSpecificationExecutor#count(com.mysema.query.types.Predicate)
     */
    public Long count(Predicate predicate) {

        return createQuery(predicate).count();
    }


    /**
     * Creates a new {@link JPQLQuery} for the given {@link Predicate}.
     * 
     * @param predicate
     * @return
     */
    private JPQLQuery createQuery(Predicate... predicate) {

        return new JPAQuery(em).from(path).where(predicate);
    }


    /**
     * Applies the given {@link Pageable} to the given {@link JPQLQuery}.
     * 
     * @param query
     * @param pageable
     * @return
     */
    private JPQLQuery applyPagination(JPQLQuery query, Pageable pageable) {

        if (pageable == null) {
            return query;
        }

        query.offset(pageable.getOffset());
        query.limit(pageable.getPageSize());

        return applySorting(query, pageable.getSort());
    }


    /**
     * Applies sorting to the given {@link JPQLQuery}.
     * 
     * @param query
     * @param sort
     * @return
     */
    private JPQLQuery applySorting(JPQLQuery query, Sort sort) {

        if (sort == null) {
            return query;
        }

        for (Order order : sort) {
            query.orderBy(toOrder(order));
        }

        return query;
    }


    /**
     * Transforms a plain {@link Order} into a QueryDsl specific
     * {@link OrderSpecifier}.
     * 
     * @param order
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private OrderSpecifier<?> toOrder(Order order) {

        Expression<Object> property = builder.get(order.getProperty());

        return new OrderSpecifier(
                order.isAscending() ? com.mysema.query.types.Order.ASC
                        : com.mysema.query.types.Order.DESC, property);
    }

    /**
     * Strategy interface to abstract the ways to translate an plain domain
     * class into a {@link EntityPath}.
     * 
     * @author Oliver Gierke
     */
    public static interface EntityPathResolver {

        <T> EntityPath<T> createPath(Class<T> domainClass);
    }

    /**
     * Simple implementation of {@link EntityPathResolver} to lookup a query
     * class by reflection and using the static field of the same type.
     * 
     * @author Oliver Gierke
     */
    static enum SimpleEntityPathResolver implements EntityPathResolver {

        INSTANCE;

        private static final String NO_CLASS_FOUND_TEMPLATE =
                "Did not find a query class %s for domain class %s!";
        private static final String NO_FIELD_FOUND_TEMPLATE =
                "Did not find a static field of the same type in %s!";


        /**
         * Creates an {@link EntityPath} instance for the given domain class.
         * Tries to lookup a class matching the naming convention (prepend Q to
         * the simple name of the class, same package) and find a static field
         * of the same type in it.
         * 
         * @param domainClass
         * @return
         */
        @SuppressWarnings("unchecked")
        public <T> EntityPath<T> createPath(Class<T> domainClass) {

            String pathClassName = getQueryClassName(domainClass);

            try {
                Class<?> pathClass =
                        ClassUtils.forName(pathClassName,
                                QueryDslJpaRepository.class.getClassLoader());
                Field field = getStaticFieldOfType(pathClass);

                if (field == null) {
                    throw new IllegalStateException(String.format(
                            NO_FIELD_FOUND_TEMPLATE, pathClass));
                } else {
                    return (EntityPath<T>) ReflectionUtils
                            .getField(field, null);
                }

            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(String.format(
                        NO_CLASS_FOUND_TEMPLATE, pathClassName,
                        domainClass.getName()), e);
            }
        }


        /**
         * Returns the first static field of the given type inside the given
         * type.
         * 
         * @param type
         * @return
         */
        private Field getStaticFieldOfType(Class<?> type) {

            for (Field field : type.getDeclaredFields()) {

                boolean isStatic = Modifier.isStatic(field.getModifiers());
                boolean hasSameType = type.equals(field.getType());

                if (isStatic && hasSameType) {
                    return field;
                }
            }

            Class<?> superclass = type.getSuperclass();
            return Object.class.equals(superclass) ? null
                    : getStaticFieldOfType(superclass);
        }


        /**
         * Returns the name of the query class for the given domain class.
         * 
         * @param domainClass
         * @return
         */
        private String getQueryClassName(Class<?> domainClass) {

            String simpleClassName = ClassUtils.getShortName(domainClass);
            return String.format("%s.Q%s%s",
                    domainClass.getPackage().getName(),
                    getClassBase(simpleClassName), domainClass.getSimpleName());
        }


        /**
         * Analyzes the short class name and potentially returns the outer
         * class.
         * 
         * @param shortName
         * @return
         */
        private String getClassBase(String shortName) {

            String[] parts = shortName.split("\\.");

            if (parts.length < 2) {
                return "";
            }

            return parts[0] + "_";
        }
    }
}
