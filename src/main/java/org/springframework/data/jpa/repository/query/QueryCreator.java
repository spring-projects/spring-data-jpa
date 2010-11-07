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
package org.springframework.data.jpa.repository.query;

import static org.springframework.data.jpa.repository.query.QueryUtils.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.repository.utils.JpaClassUtils;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.ParameterOutOfBoundsException;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryCreationException;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.parser.OrderBySource;
import org.springframework.data.repository.query.parser.PartSource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;


/**
 * Class to encapsulate query creation logic for {@link QueryMethod}s.
 * 
 * @author Oliver Gierke
 */
class QueryCreator {

    private static final Logger LOG = LoggerFactory
            .getLogger(QueryCreator.class);
    private static final String INVALID_PARAMETER_SIZE =
            "You have to provide method arguments for each query "
                    + "criteria to construct the query correctly!";
    private static final String AND = "And";
    private static final String OR = "Or";

    private JpaQueryMethod method;


    /**
     * Creates a new {@link QueryCreator} for the given {@link QueryMethod}.
     * 
     * @param queryMethod
     */
    public QueryCreator(JpaQueryMethod queryMethod) {

        Assert.isTrue(!queryMethod.isModifyingQuery());
        this.method = queryMethod;
    }


    /**
     * Constructs a query from the underlying {@link QueryMethod}.
     * 
     * @return the query string
     * @throws QueryCreationException in case the query can't be created
     */
    public String constructQuery() {

        StringBuilder queryBuilder = new StringBuilder();

        Parameters parameters = method.getParameters().getBindableParameters();
        String methodName = method.getName();
        Class<?> domainClass = method.getDomainClass();

        try {
            int parametersBound =
                    doCreateQuery(new PartSource(methodName), domainClass,
                            queryBuilder, parameters);

            if (!method.isCorrectNumberOfParameters(parametersBound)) {
                throw QueryCreationException.create(method,
                        INVALID_PARAMETER_SIZE);
            }
        } catch (ParameterOutOfBoundsException e) {
            throw QueryCreationException.create(method, e);
        }

        String query = queryBuilder.toString();

        LOG.debug("Created query '%s' from method %s", query, method.getName());

        return query;
    }


    private int doCreateQuery(PartSource source, Class<?> domainClass,
            StringBuilder builder, Parameters parameters) {

        builder.append(getQueryString(READ_ALL_QUERY,
                JpaClassUtils.getEntityName(domainClass)));
        builder.append(" where ");

        Iterator<PartSource> orParts = source.getParts(OR);
        int parametersBound = 0;

        while (orParts.hasNext()) {

            Iterator<PartSource> andParts = orParts.next().getParts(AND);

            while (andParts.hasNext()) {
                PartSource andPart = andParts.next();

                JpaQueryPart part =
                        new JpaQueryPart(andPart.cleanedUp(), domainClass);
                Parameter parameter =
                        part.getParameterRequired() ? parameters
                                .getParameter(parametersBound) : null;

                builder.append(part.getQueryPart(parameter));

                if (andParts.hasNext()) {
                    builder.append(" and ");
                }
                parametersBound += part.getNumberOfArguments();
            }

            if (orParts.hasNext()) {
                builder.append(" or ");
            }
        }

        if (source.hasOrderByClause()) {
            builder.append(" ").append(getClause(source.getOrderBySource()));
        }

        return parametersBound;
    }


    /**
     * Returns the final JPA order by clause.
     * 
     * @return
     */
    public String getClause(OrderBySource source) {

        List<String> parts = new ArrayList<String>();

        for (Order order : source.toSort()) {
            parts.add(String.format("x.%s %s", order.getProperty(),
                    QueryUtils.toJpaDirection(order)));

        }

        return "order by "
                + StringUtils.collectionToDelimitedString(parts, ", ");
    }
}
