/*
 * Copyright 2014 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.NamedStoredProcedureQueries;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.StoredProcedureParameter;

import org.springframework.data.jpa.repository.support.JpaEntityMetadata;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A factory class for {@link StoredProcedureAttributes}.
 * 
 * @author Thomas Darimont
 */
public class StoredProcedureAttributeCreator {

	/**
	 * Creates a new {@link StoredProcedureAttributes} from the given {@link Method} and {@link JpaEntityMetadata}.
	 * 
	 * @param method must not be {@literal null}
	 * @param entityMetadata must not be {@literal null}
	 * @return
	 */
	public StoredProcedureAttributes createFrom(Method method, JpaEntityMetadata<?> entityMetadata) {

		Assert.notNull(method, "Method must not be null!");
		Assert.notNull(entityMetadata, "EntityMetadata must not be null!");

		Procedure procedure = method.getAnnotation(Procedure.class);
		Assert.notNull(procedure, "Method must have an @Procedure annotation!");

		NamedStoredProcedureQuery namedStoredProc = tryFindAnnotatedNamedStoredProcedureQuery(method, entityMetadata,
				procedure);

		if (namedStoredProc != null) {
			return newProcedureAttributesFrom(method, namedStoredProc);
		}

		String procedureName = deriveProcedureNameFrom(procedure);
		if (StringUtils.isEmpty(procedureName)) {
			throw new IllegalArgumentException("Could not determine name of procedure for @Procedure annotated method: "
					+ method);
		}

		return new StoredProcedureAttributes(procedureName, null, method.getReturnType(), false);
	}

	/**
	 * @param procedure
	 * @return
	 */
	private String deriveProcedureNameFrom(Procedure procedure) {
		return StringUtils.hasText(procedure.value()) ? procedure.value() : procedure.procedureName();
	}

	/**
	 * @param method
	 * @param namedStoredProc
	 * @return
	 */
	private StoredProcedureAttributes newProcedureAttributesFrom(Method method, NamedStoredProcedureQuery namedStoredProc) {

		String outputParameterName = null;
		Class<?> outputParameterType = null;

		int outputParameterCount = 0;

		for (StoredProcedureParameter param : namedStoredProc.parameters()) {
			switch (param.mode()) {
				case OUT:
				case INOUT:

					if (outputParameterCount > 0) {
						throw new IllegalStateException(
								String.format(
										"Could not create ProcedureAttributes from %s. We currently support only one output parameter!",
										method));
					}

					outputParameterName = param.name();
					outputParameterType = param.type();

					outputParameterCount++;
					break;
				case IN:
				default:
					continue;
			}
		}

		if (outputParameterType == null) {
			outputParameterType = method.getReturnType();
		}

		return new StoredProcedureAttributes(namedStoredProc.name(), outputParameterName, outputParameterType, true);
	}

	/**
	 * @param method must not be {@literal null}.
	 * @param entityMetadata must not be {@literal null}.
	 * @param procedure must not be {@literal null}.
	 * @return
	 */
	private NamedStoredProcedureQuery tryFindAnnotatedNamedStoredProcedureQuery(Method method,
			JpaEntityMetadata<?> entityMetadata, Procedure procedure) {

		Assert.notNull(method, "Method must not be null!");
		Assert.notNull(entityMetadata, "EntityMetadata must not be null!");
		Assert.notNull(procedure, "Procedure must not be null!");

		Class<?> entityType = entityMetadata.getJavaType();

		List<NamedStoredProcedureQuery> queries = collectNamedStoredProcedureQueriesFrom(entityType);

		if (queries.isEmpty()) {
			return null;
		}

		String namedProcedureName = derivedNamedProcedureNameFrom(method, entityMetadata, procedure);

		for (NamedStoredProcedureQuery query : queries) {

			if (query.name().equals(namedProcedureName)) {
				return query;
			}
		}

		return null;
	}

	/**
	 * @param method
	 * @param entityMetadata
	 * @param procedure
	 * @return
	 */
	private String derivedNamedProcedureNameFrom(Method method, JpaEntityMetadata<?> entityMetadata, Procedure procedure) {
		return StringUtils.hasText(procedure.name()) ? procedure.name() : entityMetadata.getEntityName() + "."
				+ method.getName();
	}

	/**
	 * @param entityType
	 * @return
	 */
	private List<NamedStoredProcedureQuery> collectNamedStoredProcedureQueriesFrom(Class<?> entityType) {

		List<NamedStoredProcedureQuery> queries = new ArrayList<NamedStoredProcedureQuery>();

		NamedStoredProcedureQueries namedQueriesAnnotation = entityType.getAnnotation(NamedStoredProcedureQueries.class);
		if (namedQueriesAnnotation != null) {
			queries.addAll(Arrays.asList(namedQueriesAnnotation.value()));
		}

		NamedStoredProcedureQuery namedQueryAnnotation = entityType.getAnnotation(NamedStoredProcedureQuery.class);
		if (namedQueryAnnotation != null) {
			queries.add(namedQueryAnnotation);
		}

		return queries;
	}
}
