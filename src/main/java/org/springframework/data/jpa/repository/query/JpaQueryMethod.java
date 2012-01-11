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

import static org.springframework.core.annotation.AnnotationUtils.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.LockModeType;
import javax.persistence.QueryHint;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * JPA specific extension of {@link QueryMethod}.
 * 
 * @author Oliver Gierke
 */
public class JpaQueryMethod extends QueryMethod {

	private final QueryExtractor extractor;
	private final Method method;

	/**
	 * Creates a {@link JpaQueryMethod}.
	 * 
	 * @param method must not be {@literal null}
	 * @param extractor must not be {@literal null}
	 * @param metadata must not be {@literal null}
	 */
	public JpaQueryMethod(Method method, RepositoryMetadata metadata, QueryExtractor extractor) {

		super(method, metadata);

		Assert.notNull(method, "Method must not be null!");
		Assert.notNull(extractor, "Query extractor must not be null!");

		this.method = method;
		this.extractor = extractor;

		Assert.isTrue(!(isModifyingQuery() && getParameters().hasSpecialParameter()),
				String.format("Modifying method must not contain %s!", Parameters.TYPES));
	}

	/**
	 * Returns whether the finder is a modifying one.
	 * 
	 * @return
	 */
	@Override
	public boolean isModifyingQuery() {

		return null != method.getAnnotation(Modifying.class);
	}

	/**
	 * Returns all {@link QueryHint}s annotated at this class. Note, that {@link QueryHints}
	 * 
	 * @return
	 */
	List<QueryHint> getHints() {

		List<QueryHint> result = new ArrayList<QueryHint>();

		QueryHints hints = getAnnotation(method, QueryHints.class);
		if (hints != null) {
			result.addAll(Arrays.asList(hints.value()));
		}

		return result;
	}

	/**
	 * Returns the {@link LockModeType} to be used for the query.
	 * 
	 * @return
	 */
	LockModeType getLockModeType() {

		Lock annotation = method.getAnnotation(Lock.class);
		return (LockModeType) AnnotationUtils.getValue(annotation);
	}

	/**
	 * Returns whether the potentially configured {@link QueryHint}s shall be applied when triggering the count query for
	 * pagination.
	 * 
	 * @return
	 */
	boolean applyHintsToCountQuery() {

		QueryHints hints = getAnnotation(method, QueryHints.class);
		return hints != null ? hints.forCounting() : false;
	}

	/**
	 * Returns the {@link QueryExtractor}.
	 * 
	 * @return
	 */
	QueryExtractor getQueryExtractor() {

		return extractor;
	}

	/**
	 * Returns the actual return type of the method.
	 * 
	 * @return
	 */
	Class<?> getReturnType() {

		return method.getReturnType();
	}

	/**
	 * Returns the query string declared in a {@link Query} annotation or {@literal null} if neither the annotation found
	 * nor the attribute was specified.
	 * 
	 * @return
	 */
	String getAnnotatedQuery() {

		String query = getAnnotationValue("value", String.class);
		return StringUtils.hasText(query) ? query : null;
	}

	/**
	 * Returns the countQuery string declared in a {@link Query} annotation or {@literal null} if neither the annotation
	 * found nor the attribute was specified.
	 * 
	 * @return
	 */
	String getCountQuery() {

		String countQuery = getAnnotationValue("countQuery", String.class);
		return StringUtils.hasText(countQuery) ? countQuery : null;
	}

	/**
	 * Returns whether the backing query is a native one.
	 * 
	 * @return
	 */
	boolean isNativeQuery() {
		return getAnnotationValue("nativeQuery", Boolean.class).booleanValue();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.QueryMethod#getNamedQueryName()
	 */
	@Override
	public String getNamedQueryName() {

		String annotatedName = getAnnotationValue("name", String.class);
		return StringUtils.hasText(annotatedName) ? annotatedName : super.getNamedQueryName();
	}

	/**
	 * Returns the name of the {@link NamedQuery} that shall be used for count queries.
	 * 
	 * @return
	 */
	String getNamedCountQueryName() {

		String annotatedName = getAnnotationValue("countName", String.class);
		return StringUtils.hasText(annotatedName) ? annotatedName : getNamedQueryName() + ".count";
	}

	/**
	 * Returns whether we should clear automatically for modifying queries.
	 * 
	 * @return
	 */
	boolean getClearAutomatically() {

		return (Boolean) AnnotationUtils.getValue(method.getAnnotation(Modifying.class), "clearAutomatically");
	}

	/**
	 * Returns the {@link Query} annotation's attribute casted to the given type or default value if no annotation
	 * available.
	 * 
	 * @param attribute
	 * @param type
	 * @param defaultValue
	 * @return
	 */
	private <T> T getAnnotationValue(String attribute, Class<T> type) {

		Query annotation = method.getAnnotation(Query.class);
		Object value = annotation == null ? AnnotationUtils.getDefaultValue(Query.class, attribute) : AnnotationUtils
				.getValue(annotation, attribute);

		return type.cast(value);
	}
}
