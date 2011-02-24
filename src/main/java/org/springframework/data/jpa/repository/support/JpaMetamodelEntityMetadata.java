/*
 * Copyright 2011 the original author or authors.
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

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.SingularAttribute;

import org.springframework.data.repository.support.AbstractEntityMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;


/**
 * Implementation of {@link EntityInformation} that uses JPA {@link Metamodel}
 * to find the domain class' id field.
 * 
 * @author Oliver Gierke
 */
public class JpaMetamodelEntityMetadata<T> extends AbstractEntityMetadata<T> {

    private final Member member;


    /**
     * Creates a new {@link JpaMetamodelEntityMetadata} for the given domain
     * class and {@link Metamodel}.
     * 
     * @param domainClass
     * @param metamodel
     */
    public JpaMetamodelEntityMetadata(Class<T> domainClass, Metamodel metamodel) {

        super(domainClass);

        Assert.notNull(metamodel);
        EntityType<?> type = metamodel.entity(domainClass);
        SingularAttribute<?, ?> idAttribute =
                type.getId(type.getIdType().getJavaType());
        this.member = idAttribute.getJavaMember();
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.support.IdAware#getId(java.lang.Object
     * )
     */
    public Object getId(Object entity) {

        return getMemberValue(member, entity);
    }


    /**
     * Returns the value of the given {@link Member} of the given {@link Object}
     * .
     * 
     * @param member
     * @param source
     * @return
     */
    private static Object getMemberValue(Member member, Object source) {

        if (member instanceof Field) {
            Field field = (Field) member;
            ReflectionUtils.makeAccessible(field);
            return ReflectionUtils.getField(field, source);
        } else if (member instanceof Method) {
            Method method = (Method) member;
            ReflectionUtils.makeAccessible(method);
            return ReflectionUtils.invokeMethod(method, source);
        }

        throw new IllegalArgumentException(
                "Given member is neither Field nor Method!");
    }
}
