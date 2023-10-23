/*
 * Copyright 2023 the original author or authors.
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import jakarta.persistence.QueryHint;
import jakarta.persistence.Tuple;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.util.JpaMetamodel;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.util.Lazy;
import org.springframework.lang.Nullable;

/**
 * High-level flow for creating a JPA query, later to be run by a {@link QueryEngine}.
 *
 * @author Greg Turnquist
 */
abstract class AbstractJpaQueryContext implements QueryContext {

	private final Optional<JpaQueryMethod> method;
	private final EntityManager entityManager;
	private final JpaMetamodel metamodel;
	private final PersistenceProvider provider;
	protected final QueryParameterSetter.QueryMetadataCache metadataCache;
	protected final Lazy<ParameterBinder> parameterBinder;

	private final static Map<Class<?>, String> ENTITY_NAME_CACHE = new HashMap<>();
	private final static Map<String, String> ALIAS_CACHE = new HashMap<>();

	public AbstractJpaQueryContext(Optional<JpaQueryMethod> method, EntityManager entityManager) {

		this.method = method;
		this.entityManager = entityManager;
		this.metamodel = JpaMetamodel.of(entityManager.getMetamodel());
		this.provider = PersistenceProvider.fromEntityManager(entityManager);
		this.metadataCache = new QueryParameterSetter.QueryMetadataCache();
		this.parameterBinder = Lazy.of(this::createBinder);
	}

	@Override
	public Optional<JpaQueryMethod> getQueryMethod() {
		return method;
	}

	/**
	 * This method either returns the {@link JpaQueryMethod} or throws a {@link java.util.NoSuchElementException} in order
	 * to honor the contract of NOT returning a {@literal null}.
	 */
	@Override
	public JpaQueryMethod queryMethod() {
		return method.get();
	}

	public JpaMetamodel getMetamodel() {
		return metamodel;
	}

	public EntityManager getEntityManager() {
		return entityManager;
	}

	/**
	 * This is the fundamental flow that all JPA-based operations will take, whether it's a stored procedure, a custom
	 * finder, an {@literal @Query}-based method, or something else.
	 *
	 * @return
	 */
	@Nullable
	@Override
	public Query createJpaQuery(JpaParametersParameterAccessor accessor) {

		ContextualQuery initialQuery = createQuery(accessor);

		ContextualQuery processedQuery = postProcessQuery(initialQuery, accessor);

		Query jpaQuery = turnIntoJpaQuery(processedQuery, accessor);

		// Query jpaQueryWithSpecs = applySpecifications(jpaQuery, accessor);

		Query jpaQueryWithHints = applyQueryHints(jpaQuery);

		Query jpaQueryWithHintsAndLockMode = applyLockMode(jpaQueryWithHints);

		Query queryToExecute = bindParameters(jpaQueryWithHintsAndLockMode, accessor);

		return queryToExecute;
	}

	/**
	 * Every form of a JPA-based query must produce a string-based query.
	 *
	 * @return
	 */
	protected abstract ContextualQuery createQuery(JpaParametersParameterAccessor accessor);

	/**
	 * Take the original query, apply any transformations needed to make the query runnable.
	 *
	 * @param query
	 * @return modified query
	 */
	protected ContextualQuery postProcessQuery(ContextualQuery query, JpaParametersParameterAccessor accessor) {
		return query;
	}

	/**
	 * Transform a string query into a JPA {@link Query}.
	 *
	 * @param query
	 * @return
	 */
	protected Query turnIntoJpaQuery(ContextualQuery query, JpaParametersParameterAccessor accessor) {

		Class<?> typeToRead = getTypeToRead(queryMethod().getResultProcessor().getReturnedType());

		return entityManager.createQuery(query.getQuery(), typeToRead);
	}

	/**
	 * Extract the property return type for this {@link JpaQueryMethod}.
	 * 
	 * @param returnedType
	 * @return {@link Class} representation
	 */
	@Nullable
	protected Class<?> getTypeToRead(ReturnedType returnedType) {

		if (PersistenceProvider.ECLIPSELINK.equals(provider)) {
			return null;
		}

		return returnedType.isProjecting() && !getMetamodel().isJpaManaged(returnedType.getReturnedType()) //
				? Tuple.class //
				: null;
	}

	@Override
	public Query createJpaCountQuery(JpaParametersParameterAccessor accessor) {
		throw new UnsupportedOperationException(getClass().getSimpleName() + " does not support count queries");
	}

	/**
	 * Create the {@link ParameterBinder} needed to associate arguments with the query.
	 * 
	 * @return
	 */
	protected ParameterBinder createBinder() {
		return ParameterBinderFactory.createBinder(queryMethod().getParameters());
	}

	/**
	 * Apply any {@link QueryHint}s to the {@link Query}.
	 * 
	 * @param query
	 * @return
	 */
	protected Query applyQueryHints(Query query) {

		method.ifPresent(queryMethod -> {

			queryMethod.getHints().forEach(hint -> query.setHint(hint.name(), hint.value()));

			// Apply any meta-attributes that exist
			if (queryMethod.hasQueryMetaAttributes() && provider.getCommentHintKey() != null) {

				query.setHint( //
						provider.getCommentHintKey(),
						provider.getCommentHintValue(queryMethod.getQueryMetaAttributes().getComment()));
			}
		});

		return query;
	}

	/**
	 * Apply the {@link LockModeType} to the {@link Query}.
	 * 
	 * @param query
	 * @return
	 */
	protected Query applyLockMode(Query query) {

		return method //
				.map(JpaQueryMethod::getLockModeType) //
				.map(query::setLockMode) //
				.orElse(query);
	}

	/**
	 * Bind the query arguments to the {@link Query}. NOTE: Not used for count queries.
	 * 
	 * @param query
	 * @param accessor
	 * @return
	 */
	protected Query bindParameters(Query query, JpaParametersParameterAccessor accessor) {
		return query;
	}

	// Utilities

	/**
	 * Extract a JPQL entity name for a given {@link Class}.
	 *
	 * @param clazz
	 * @return
	 */
	String entityName(Class<?> clazz) {

		return ENTITY_NAME_CACHE.computeIfAbsent( //
				clazz, //
				domainType -> getEntityManager().getMetamodel().entity(domainType).getName());
	}

	/**
	 * Extract a JPQL alias for a given {@link Class}.
	 *
	 * @param clazz
	 * @return
	 */
	static String alias(Class<?> clazz) {

		return ALIAS_CACHE.computeIfAbsent( //
				clazz.getSimpleName(), //
				entityType -> entityType.substring(0, 1).toLowerCase());
	}
}
