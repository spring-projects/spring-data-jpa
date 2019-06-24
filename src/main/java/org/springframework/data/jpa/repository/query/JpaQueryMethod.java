/*
 * Copyright 2008-2019 the original author or authors.
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
package org.springframework.data.jpa.repository.query;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.persistence.LockModeType;
import javax.persistence.QueryHint;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * JPA specific extension of {@link QueryMethod}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @author Nicolas Cirigliano
 * @author Mark Paluch
 * @author Сергей Цыпанов
 */
public class JpaQueryMethod extends QueryMethod {

	/**
	 * @see <a href=
	 *      "https://download.oracle.com/otn-pub/jcp/persistence-2.0-fr-eval-oth-JSpec/persistence-2_0-final-spec.pdf">JPA
	 *      2.0 Specification 2.2 Persistent Fields and Properties Page 23 - Top paragraph.</a>
	 */
	private static final Set<Class<?>> NATIVE_ARRAY_TYPES;
	private static final StoredProcedureAttributeSource storedProcedureAttributeSource = StoredProcedureAttributeSource.INSTANCE;

	static {

		Set<Class<?>> types = new HashSet<>();
		types.add(byte[].class);
		types.add(Byte[].class);
		types.add(char[].class);
		types.add(Character[].class);

		NATIVE_ARRAY_TYPES = Collections.unmodifiableSet(types);
	}

	private final QueryExtractor extractor;
	private final Method method;

	private @Nullable StoredProcedureAttributes storedProcedureAttributes;

	/**
	 * Creates a {@link JpaQueryMethod}.
	 *
	 * @param method must not be {@literal null}
	 * @param metadata must not be {@literal null}
	 * @param factory must not be {@literal null}
	 * @param extractor must not be {@literal null}
	 */
	public JpaQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
			QueryExtractor extractor) {

		super(method, metadata, factory);

		Assert.notNull(method, "Method must not be null!");
		Assert.notNull(extractor, "Query extractor must not be null!");

		this.method = method;
		this.extractor = extractor;

		Assert.isTrue(!(isModifyingQuery() && getParameters().hasSpecialParameter()),
				String.format("Modifying method must not contain %s!", Parameters.TYPES));
		assertParameterNamesInAnnotatedQuery();
	}

	private void assertParameterNamesInAnnotatedQuery() {

		String annotatedQuery = getAnnotatedQuery();

		if (!DeclaredQuery.of(annotatedQuery).hasNamedParameter()) {
			return;
		}

		for (Parameter parameter : getParameters()) {

			if (!parameter.isNamedParameter()) {
				continue;
			}

			if (StringUtils.isEmpty(annotatedQuery)
					|| !annotatedQuery.contains(String.format(":%s", parameter.getName().get()))
							&& !annotatedQuery.contains(String.format("#%s", parameter.getName().get()))) {
				throw new IllegalStateException(
						String.format("Using named parameters for method %s but parameter '%s' not found in annotated query '%s'!",
								method, parameter.getName(), annotatedQuery));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.QueryMethod#getEntityInformation()
	 */
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public JpaEntityMetadata<?> getEntityInformation() {
		return new DefaultJpaEntityMetadata(getDomainClass());
	}

	/**
	 * Returns whether the finder is a modifying one.
	 *
	 * @return
	 */
	@Override
	public boolean isModifyingQuery() {

		return null != AnnotationUtils.findAnnotation(method, Modifying.class);
	}

	/**
	 * Returns all {@link QueryHint}s annotated at this class. Note, that {@link QueryHints}
	 *
	 * @return
	 */
	List<QueryHint> getHints() {

		QueryHints hints = AnnotatedElementUtils.findMergedAnnotation(method, QueryHints.class);
		if (hints != null) {
			return Arrays.asList(hints.value());
		}

		return Collections.emptyList();
	}

	/**
	 * Returns the {@link LockModeType} to be used for the query.
	 *
	 * @return
	 */
	@Nullable
	LockModeType getLockModeType() {

		return (LockModeType) Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(method, Lock.class)) //
				.map(AnnotationUtils::getValue) //
				.orElse(null);
	}

	/**
	 * Returns the {@link EntityGraph} to be used for the query.
	 *
	 * @return
	 * @since 1.6
	 */
	@Nullable
	JpaEntityGraph getEntityGraph() {

		EntityGraph annotation = AnnotatedElementUtils.findMergedAnnotation(method, EntityGraph.class);
		return annotation == null ? null : new JpaEntityGraph(annotation, getNamedQueryName());
	}

	/**
	 * Returns whether the potentially configured {@link QueryHint}s shall be applied when triggering the count query for
	 * pagination.
	 *
	 * @return
	 */
	boolean applyHintsToCountQuery() {

		QueryHints hints = AnnotatedElementUtils.findMergedAnnotation(method, QueryHints.class);
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
	@Nullable
	String getAnnotatedQuery() {

		String query = getAnnotationValue("value", String.class);
		return StringUtils.hasText(query) ? query : null;
	}

	/**
	 * Returns the required query string declared in a {@link Query} annotation or throws {@link IllegalStateException} if
	 * neither the annotation found nor the attribute was specified.
	 *
	 * @return
	 * @throws IllegalStateException if no {@link Query} annotation is present or the query is empty.
	 * @since 2.0
	 */
	String getRequiredAnnotatedQuery() throws IllegalStateException {

		String query = getAnnotatedQuery();

		if (query != null) {
			return query;
		}

		throw new IllegalStateException(String.format("No annotated query found for query method %s!", getName()));
	}

	/**
	 * Returns the countQuery string declared in a {@link Query} annotation or {@literal null} if neither the annotation
	 * found nor the attribute was specified.
	 *
	 * @return
	 */
	@Nullable
	String getCountQuery() {

		String countQuery = getAnnotationValue("countQuery", String.class);
		return StringUtils.hasText(countQuery) ? countQuery : null;
	}

	/**
	 * Returns the count query projection string declared in a {@link Query} annotation or {@literal null} if neither the
	 * annotation found nor the attribute was specified.
	 *
	 * @return
	 * @since 1.6
	 */
	@Nullable
	String getCountQueryProjection() {

		String countProjection = getAnnotationValue("countProjection", String.class);
		return StringUtils.hasText(countProjection) ? countProjection : null;
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
	 * Returns whether we should flush automatically for modifying queries.
	 * 
	 * @return whether we should flush automatically.
	 */
	boolean getFlushAutomatically() {
		return getMergedOrDefaultAnnotationValue("flushAutomatically", Modifying.class, Boolean.class);
	}

	/**
	 * Returns whether we should clear automatically for modifying queries.
	 *
	 * @return whether we should clear automatically.
	 */
	boolean getClearAutomatically() {
		return getMergedOrDefaultAnnotationValue("clearAutomatically", Modifying.class, Boolean.class);
	}

	/**
	 * Returns the {@link Query} annotation's attribute casted to the given type or default value if no annotation
	 * available.
	 *
	 * @param attribute
	 * @param type
	 * @return
	 */
	private <T> T getAnnotationValue(String attribute, Class<T> type) {
		return getMergedOrDefaultAnnotationValue(attribute, Query.class, type);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T> T getMergedOrDefaultAnnotationValue(String attribute, Class annotationType, Class<T> targetType) {

		Annotation annotation = AnnotatedElementUtils.findMergedAnnotation(method, annotationType);
		if (annotation == null) {
			return targetType.cast(AnnotationUtils.getDefaultValue(annotationType, attribute));
		}

		return targetType.cast(AnnotationUtils.getValue(annotation, attribute));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.QueryMethod#createParameters(java.lang.reflect.Method)
	 */
	@Override
	protected JpaParameters createParameters(Method method) {
		return new JpaParameters(method);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.QueryMethod#getParameters()
	 */
	@Override
	public JpaParameters getParameters() {
		return (JpaParameters) super.getParameters();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.QueryMethod#isCollectionQuery()
	 */
	@Override
	public boolean isCollectionQuery() {
		return super.isCollectionQuery() && !NATIVE_ARRAY_TYPES.contains(method.getReturnType());
	}

	/**
	 * Return {@literal true} if the method contains a {@link Procedure} annotation.
	 *
	 * @return
	 */
	public boolean isProcedureQuery() {
		return AnnotationUtils.findAnnotation(method, Procedure.class) != null;
	}

	/**
	 * Returns a new {@link StoredProcedureAttributes} representing the stored procedure meta-data for this
	 * {@link JpaQueryMethod}.
	 *
	 * @return
	 */
	StoredProcedureAttributes getProcedureAttributes() {

		if (storedProcedureAttributes == null) {
			this.storedProcedureAttributes = storedProcedureAttributeSource.createFrom(method, getEntityInformation());
		}

		return storedProcedureAttributes;
	}
}
