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

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.SingularAttribute;

import org.springframework.data.repository.core.EntityInformation;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Implementation of {@link EntityInformation} that uses JPA {@link Metamodel} to find the domain class' id field.
 * 
 * @author Oliver Gierke
 */
public class JpaMetamodelEntityInformation<T, ID extends Serializable> extends JpaEntityInformationSupport<T, ID>
		implements JpaEntityInformation<T, ID> {

	private final SingularAttribute<? super T, ?> attribute;

	/**
	 * Creates a new {@link JpaMetamodelEntityInformation} for the given domain class and {@link Metamodel}.
	 * 
	 * @param domainClass
	 * @param metamodel
	 */
	public JpaMetamodelEntityInformation(Class<T> domainClass, Metamodel metamodel) {

		super(domainClass);

		Assert.notNull(metamodel);
		ManagedType<T> type = metamodel.managedType(domainClass);

		if (type == null) {
			throw new IllegalArgumentException("The given domain class can not be found in the given Metamodel!");
		}

		if (!(type instanceof IdentifiableType)) {
			throw new IllegalArgumentException("The given domain class does not contain an id attribute!");
		}

		IdentifiableType<T> identifiableType = (IdentifiableType<T>) type;
		this.attribute = identifiableType.getId(identifiableType.getIdType().getJavaType());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.repository.support.IdAware#getId(java.lang.Object
	 * )
	 */
	@SuppressWarnings("unchecked")
	public ID getId(T entity) {

		return (ID) getMemberValue(attribute.getJavaMember(), entity);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.repository.support.EntityInformation#getIdType()
	 */
	@SuppressWarnings("unchecked")
	public Class<ID> getIdType() {

		return (Class<ID>) attribute.getJavaType();
	}

	/**
	 * Returns the value of the given {@link Member} of the given {@link Object} .
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

		throw new IllegalArgumentException("Given member is neither Field nor Method!");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.jpa.repository.support.JpaEntityMetadata#
	 * getIdAttribute()
	 */
	public SingularAttribute<? super T, ?> getIdAttribute() {

		return attribute;
	}
}
