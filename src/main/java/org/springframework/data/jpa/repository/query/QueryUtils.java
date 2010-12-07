/*
 * Copyright 2008-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.data.jpa.repository.query;

import static java.util.regex.Pattern.*;
import static org.springframework.data.jpa.repository.utils.JpaClassUtils.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.util.Assert;


/**
 * Simple utility class to create JPA queries.
 * 
 * @author Oliver Gierke
 */
public abstract class QueryUtils {

    public static final String COUNT_QUERY_STRING =
            "select count(%s) from %s x";

    public static final String DELETE_ALL_QUERY_STRING = "delete from %s x";
    public static final String READ_ALL_QUERY = "select x from %s x";
    private static final String DEFAULT_ALIAS = "x";
    private static final String COUNT_REPLACEMENT = "select count($3$5) $4$5$6";

    private static final Pattern ALIAS_MATCH;
    private static final Pattern COUNT_MATCH;

    private static final String IDENTIFIER = "[\\p{L}._$]+";
    private static final String IDENTIFIER_GROUP = String.format("(%s)",
            IDENTIFIER);

    static {

        StringBuilder builder = new StringBuilder();
        builder.append("(?<=from)"); // from as starting delimiter
        builder.append("(?: )+"); // at least one space separating
        builder.append(IDENTIFIER_GROUP); // Entity name, can be qualified (any
        builder.append("(?: as)*"); // exclude possible "as" keyword
        builder.append("(?: )+"); // at least one space separating
        builder.append("(\\w*)"); // the actual alias

        ALIAS_MATCH = compile(builder.toString(), CASE_INSENSITIVE);

        builder = new StringBuilder();
        builder.append("(select\\s+((distinct )?.+?)\\s+)?(from\\s+");
        builder.append(IDENTIFIER);
        builder.append("(?:\\s+as)?\\s+)");
        builder.append(IDENTIFIER_GROUP);
        builder.append("(.*)");

        COUNT_MATCH = compile(builder.toString(), CASE_INSENSITIVE);
    }


    /**
     * Private constructor to prevent instantiation.
     */
    private QueryUtils() {

    }


    /**
     * Returns the query string for the given class.
     * 
     * @return
     */
    public static String getQueryString(String template, Class<?> clazz) {

        if (null == clazz) {
            throw new IllegalArgumentException("Class must not be null!");
        }

        return getQueryString(template, getEntityName(clazz));
    }


    /**
     * Returns the query string for the given class name.
     * 
     * @param template
     * @param clazzName
     * @return
     */
    private static String getQueryString(String template, String clazzName) {

        Assert.hasText(clazzName, "Classname must not be null or empty!");

        return String.format(template, clazzName);
    }


    /**
     * Adds {@literal order by} clause to the JPQL query. Uses the
     * {@link #DEFAULT_ALIAS} to bind the sorting property to.
     * 
     * @param query
     * @param alias
     * @param sort
     * @return
     */
    public static String applySorting(String query, Sort sort) {

        return applySorting(query, sort, DEFAULT_ALIAS);
    }


    /**
     * Adds {@literal order by} clause to the JPQL query.
     * 
     * @param query
     * @param sort
     * @param alias
     * @return
     */
    public static String applySorting(String query, Sort sort, String alias) {

        Assert.hasText(query);

        if (null == sort) {
            return query;
        }

        StringBuilder builder = new StringBuilder(query);
        builder.append(" order by");

        for (Order order : sort) {
            builder.append(String.format(" %s.%s %s,", alias,
                    order.getProperty(), toJpaDirection(order)));
        }

        builder.deleteCharAt(builder.length() - 1);

        return builder.toString();
    }


    public static String toJpaDirection(Order order) {

        return order.getDirection().name().toLowerCase(Locale.US);
    }


    /**
     * Resolves the alias for the entity to be retrieved from the given JPA
     * query.
     * 
     * @param query
     * @return
     */
    public static String detectAlias(String query) {

        Matcher matcher = ALIAS_MATCH.matcher(query);

        return matcher.find() ? matcher.group(2) : null;
    }


    /**
     * Creates a where-clause referencing the given entities and appends it to
     * the given query string. Binds the given entities to the query.
     * 
     * @param <T>
     * @param queryString
     * @param entities
     * @param entityManager
     * @return
     */
    public static <T> Query applyAndBind(String queryString,
            Iterable<T> entities, EntityManager entityManager) {

        Assert.notNull(queryString);
        Assert.notNull(entities);
        Assert.notNull(entityManager);

        Iterator<T> iterator = entities.iterator();

        if (!iterator.hasNext()) {
            return entityManager.createQuery(queryString);
        }

        String alias = detectAlias(queryString);
        StringBuilder builder = new StringBuilder(queryString);
        builder.append(" where");

        int i = 0;

        while (iterator.hasNext()) {

            iterator.next();

            builder.append(String.format(" %s = ?%d", alias, ++i));

            if (iterator.hasNext()) {
                builder.append(" or");
            }
        }

        Query query = entityManager.createQuery(builder.toString());

        iterator = entities.iterator();
        i = 0;

        while (iterator.hasNext()) {
            query.setParameter(++i, iterator.next());
        }

        return query;
    }


    /**
     * Creates a count projected query from the given orginal query.
     * 
     * @param originalQuery must not be {@literal null} or empty
     * @return
     */
    public static String createCountQueryFor(String originalQuery) {

        Assert.hasText(originalQuery);

        Matcher matcher = COUNT_MATCH.matcher(originalQuery);
        return matcher.replaceFirst(COUNT_REPLACEMENT);
    }


    /**
     * Returns whether the given {@link Query} contains named parameters.
     * 
     * @param query
     * @return
     */
    public static boolean hasNamedParameter(Query query) {

        for (Parameter<?> parameter : query.getParameters()) {
            if (parameter.getName() != null) {
                return true;
            }
        }

        return false;
    }


    /**
     * Turns the given {@link Sort} into
     * {@link javax.persistence.criteria.Order}s.
     * 
     * @param sort
     * @param root
     * @param cb
     * @return
     */
    public static List<javax.persistence.criteria.Order> toOrders(Sort sort,
            Root<?> root, CriteriaBuilder cb) {

        List<javax.persistence.criteria.Order> orders =
                new ArrayList<javax.persistence.criteria.Order>();

        if (sort == null) {
            return orders;
        }

        for (org.springframework.data.domain.Sort.Order order : sort) {
            orders.add(toJpaOrder(order, root, cb));
        }

        return orders;
    }


    /**
     * Creates a criteria API {@link javax.persistence.criteria.Order} from the
     * given {@link Order}.
     * 
     * @param order
     * @param root
     * @param cb
     * @return
     */
    private static javax.persistence.criteria.Order toJpaOrder(Order order,
            Root<?> root, CriteriaBuilder cb) {

        Expression<?> expression = root.get(order.getProperty());
        return order.isAscending() ? cb.asc(expression) : cb.desc(expression);
    }
}
