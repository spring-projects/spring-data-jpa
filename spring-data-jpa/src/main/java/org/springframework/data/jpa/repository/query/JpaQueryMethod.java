/*
 * Copyright 2008-2023 the original author or authors.
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

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Meta;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.jpa.repository.QueryRewriter;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.util.QueryExecutionConverters;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
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
 * @author Réda Housni Alaoui
 * @author Greg Turnquist
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
	private final Class<?> returnType;

	private @Nullable StoredProcedureAttributes storedProcedureAttributes;
	private final Lazy<LockModeType> lockModeType;
	private final Lazy<QueryHints> queryHints;
	private final Lazy<JpaEntityGraph> jpaEntityGraph;
	private final Lazy<Modifying> modifying;
	private final Lazy<Boolean> isNativeQuery;
	private final Lazy<Boolean> isCollectionQuery;
	private final Lazy<Boolean> isProcedureQuery;
	private final Lazy<JpaEntityMetadata<?>> entityMetadata;
	private final Map<Class<? extends Annotation>, Optional<Annotation>> annotationCache;

	/**
	 * Creates a {@link JpaQueryMethod}.
	 *
	 * @param method must not be {@literal null}
	 * @param metadata must not be {@literal null}
	 * @param factory must not be {@literal null}
	 * @param extractor must not be {@literal null}
	 */
	protected JpaQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
			QueryExtractor extractor) {

		super(method, metadata, factory);

		Assert.notNull(method, "Method must not be null");
		Assert.notNull(extractor, "Query extractor must not be null");

		this.method = method;
		this.returnType = potentiallyUnwrapReturnTypeFor(metadata, method);
		this.extractor = extractor;
		this.lockModeType = Lazy
				.of(() -> (LockModeType) Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(method, Lock.class)) //
						.map(AnnotationUtils::getValue) //
						.orElse(null));

		this.queryHints = Lazy.of(() -> AnnotatedElementUtils.findMergedAnnotation(method, QueryHints.class));
		this.modifying = Lazy.of(() -> AnnotatedElementUtils.findMergedAnnotation(method, Modifying.class));
		this.jpaEntityGraph = Lazy.of(() -> {

			EntityGraph entityGraph = AnnotatedElementUtils.findMergedAnnotation(method, EntityGraph.class);

			if (entityGraph == null) {
				return null;
			}

			return new JpaEntityGraph(entityGraph, getNamedQueryName());
		});
		this.isNativeQuery = Lazy.of(() -> getAnnotationValue("nativeQuery", Boolean.class));
		this.isCollectionQuery = Lazy.of(() -> super.isCollectionQuery() && !NATIVE_ARRAY_TYPES.contains(this.returnType));
		this.isProcedureQuery = Lazy.of(() -> AnnotationUtils.findAnnotation(method, Procedure.class) != null);
		this.entityMetadata = Lazy.of(() -> new DefaultJpaEntityMetadata<>(getDomainClass()));
		this.annotationCache = new ConcurrentReferenceHashMap<>();

		Assert.isTrue(!(isModifyingQuery() && getParameters().hasSpecialParameter()),
				String.format("Modifying method must not contain %s", Parameters.TYPES));
		assertParameterNamesInAnnotatedQuery();
	}

	private static Class<?> potentiallyUnwrapReturnTypeFor(RepositoryMetadata metadata, Method method) {

		TypeInformation<?> returnType = metadata.getReturnType(method);

		while (QueryExecutionConverters.supports(returnType.getType())
				|| QueryExecutionConverters.supportsUnwrapping(returnType.getType())) {
			returnType = returnType.getRequiredComponentType();
		}

		return returnType.getType();
	}

	private void assertParameterNamesInAnnotatedQuery() {

		String annotatedQuery = getAnnotatedQuery();

		if (!DeclaredQuery.of(annotatedQuery, this.isNativeQuery.get()).hasNamedParameter()) {
			return;
		}

		for (Parameter parameter : getParameters()) {

			if (!parameter.isNamedParameter()) {
				continue;
			}

			if (!StringUtils.hasText(annotatedQuery)
					|| !annotatedQuery.contains(String.format(":%s", parameter.getName().get()))
							&& !annotatedQuery.contains(String.format("#%s", parameter.getName().get()))) {
				throw new IllegalStateException(
						String.format("Using named parameters for method %s but parameter '%s' not found in annotated query '%s'",
								method, parameter.getName(), annotatedQuery));
			}
		}
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public JpaEntityMetadata<?> getEntityInformation() {
		return this.entityMetadata.get();
	}

	/**
	 * Returns whether the finder is a modifying one.
	 *
	 * @return
	 */
	@Override
	public boolean isModifyingQuery() {
		return modifying.getNullable() != null;
	}

	@SuppressWarnings("unchecked")
	private <A extends Annotation> Optional<A> doFindAnnotation(Class<A> annotationType) {

		return (Optional<A>) this.annotationCache.computeIfAbsent(annotationType,
				it -> Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(method, it)));
	}

	/**
	 * Returns all {@link QueryHint}s annotated at this class. Note, that {@link QueryHints}
	 *
	 * @return
	 */
	List<QueryHint> getHints() {

		QueryHints hints = this.queryHints.getNullable();
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
		return lockModeType.getNullable();
	}

	/**
	 * Returns the {@link EntityGraph} to be used for the query.
	 *
	 * @return
	 * @since 1.6
	 */
	@Nullable
	JpaEntityGraph getEntityGraph() {
		return jpaEntityGraph.getNullable();
	}

	/**
	 * Returns whether the potentially configured {@link QueryHint}s shall be applied when triggering the count query for
	 * pagination.
	 *
	 * @return
	 */
	boolean applyHintsToCountQuery() {

		QueryHints hints = this.queryHints.getNullable();
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
		return returnType;
	}

	/**
	 * @return return true if {@link Meta} annotation is available.
	 * @since 3.0
	 */
	public boolean hasQueryMetaAttributes() {
		return getMetaAnnotation() != null;
	}

	/**
	 * Returns the {@link Meta} annotation that is applied to the method or {@code null} if not available.
	 *
	 * @return
	 * @since 3.0
	 */
	@Nullable
	Meta getMetaAnnotation() {
		return doFindAnnotation(Meta.class).orElse(null);
	}

	/**
	 * Returns the {@link org.springframework.data.jpa.repository.query.Meta} attributes to be applied.
	 *
	 * @return never {@literal null}.
	 * @since 1.6
	 */
	public org.springframework.data.jpa.repository.query.Meta getQueryMetaAttributes() {

		Meta meta = getMetaAnnotation();
		if (meta == null) {
			return new org.springframework.data.jpa.repository.query.Meta();
		}

		org.springframework.data.jpa.repository.query.Meta metaAttributes = new org.springframework.data.jpa.repository.query.Meta();

		if (StringUtils.hasText(meta.comment())) {
			metaAttributes.setComment(meta.comment());
		}

		return metaAttributes;
	}

	/**
	 * Returns the query string declared in a {@link Query} annotation or {@literal null} if neither the annotation found
	 * nor the attribute was specified.
	 *
	 * @return
	 */
	@Nullable
	public String getAnnotatedQuery() {

		String query = getAnnotationValue("value", String.class);
		return StringUtils.hasText(query) ? query : null;
	}

	/**
	 * @return {@code true} if this method is annotated with {@code  @Query(name=…)}.
	 */
	boolean hasAnnotatedQueryName() {
		return StringUtils.hasText(getAnnotationValue("name", String.class));
	}

	/**
	 * Returns the required query string declared in a {@link Query} annotation or throws {@link IllegalStateException} if
	 * neither the annotation found nor the attribute was specified.
	 *
	 * @return
	 * @throws IllegalStateException if no {@link Query} annotation is present or the query is empty.
	 * @since 2.0
	 */
	public String getRequiredAnnotatedQuery() throws IllegalStateException {

		String query = getAnnotatedQuery();

		if (query != null) {
			return query;
		}

		throw new IllegalStateException(String.format("No annotated query found for query method %s", getName()));
	}

	/**
	 * Returns the countQuery string declared in a {@link Query} annotation or {@literal null} if neither the annotation
	 * found nor the attribute was specified.
	 *
	 * @return
	 */
	@Nullable
	public String getCountQuery() {

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
		return this.isNativeQuery.get();
	}

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

	@Override
	protected JpaParameters createParameters(Method method) {
		return new JpaParameters(method);
	}

	@Override
	public JpaParameters getParameters() {
		return (JpaParameters) super.getParameters();
	}

	@Override
	public boolean isCollectionQuery() {
		return this.isCollectionQuery.get();
	}

	/**
	 * Return {@literal true} if the method contains a {@link Procedure} annotation.
	 *
	 * @return
	 */
	public boolean isProcedureQuery() {
		return this.isProcedureQuery.get();
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

	/**
	 * Returns the {@link QueryRewriter} type.
	 *
	 * @return type of the {@link QueryRewriter}
	 * @since 3.0
	 */
	public Class<? extends QueryRewriter> getQueryRewriter() {
		return getMergedOrDefaultAnnotationValue("queryRewriter", Query.class, Class.class);
	}
}
