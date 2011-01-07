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
package org.springframework.data.jpa.repository.query;

import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.util.Assert;


/**
 * JPA specific {@link Part}. Allows creating JPQL snippets via
 * {@link #createQueryPart(Type, String, Parameter)}.
 * 
 * @author Oliver Gierke
 */
class JpaQueryPart extends Part {

    /**
     * @param part
     * @param method
     * @param parameter
     */
    public JpaQueryPart(String part, Class<?> method) {

        super(part, method);
    }


    /**
     * Returns the query part.
     * 
     * @return
     */
    public String getQueryPart(Parameter parameter) {

        return createQueryPart(getType(), getProperty().toDotPath(), parameter);
    }


    /**
     * Create the actual query part for the given property. Creates a simple
     * assignment of the following shape by default. {@code x.$
     * property} ${operator} ${parameterPlaceholder}}.
     * 
     * @param property the actual clean property
     * @param parameters
     * @param index
     * @return
     */
    private String createQueryPart(Type type, String property,
            Parameter parameter) {

        switch (type) {
        case BETWEEN:
            String first = parameter.getPlaceholder();
            String second = parameter.getNext().getPlaceholder();

            return String.format("x.%s between %s and %s", property, first,
                    second);
        case IS_NOT_NULL:
            return String.format("x.%s is not null", property);
        case IS_NULL:
            return String.format("x.%s is null", property);
        default:
            Assert.notNull(parameter);
            return String.format("x.%s %s %s", property, type.getOperator(),
                    parameter.getPlaceholder());
        }
    }
}
