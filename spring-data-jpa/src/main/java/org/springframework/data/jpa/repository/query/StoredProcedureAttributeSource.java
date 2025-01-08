/*
 * Copyright 2014-2025 the original author or authors.
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

import jakarta.persistence.NamedStoredProcedureQueries;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureParameter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * A factory class for {@link StoredProcedureAttributes}.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Diego Diez
 * @author Jeff Sheets
 * @author Gabriel Basilio
 * @author Greg Turnquist
 * @author Thorben Janssen
 * @since 1.6
 */
enum StoredProcedureAttributeSource {

	INSTANCE;

	/**
	 * Creates a new {@link StoredProcedureAttributes} from the given {@link Method} and {@link JpaEntityMetadata}.
	 *
	 * @param method must not be {@literal null}
	 * @param entityMetadata must not be {@literal null}
	 * @return
	 */
	public StoredProcedureAttributes createFrom(Method method, JpaEntityMetadata<?> entityMetadata) {

		Assert.notNull(method, "Method must not be null");
		Assert.notNull(entityMetadata, "EntityMetadata must not be null");

		Procedure procedure = AnnotatedElementUtils.findMergedAnnotation(method, Procedure.class);
		Assert.notNull(procedure, "Method must have an @Procedure annotation");

		NamedStoredProcedureQuery namedStoredProc = tryFindAnnotatedNamedStoredProcedureQuery(method, entityMetadata,
				procedure);

		if (namedStoredProc != null) {
			return newProcedureAttributesFrom(method, namedStoredProc, procedure);
		}

		String procedureName = deriveProcedureNameFrom(method, procedure);
		if (ObjectUtils.isEmpty(procedureName)) {
			throw new IllegalArgumentException(
					"Could not determine name of procedure for @Procedure annotated method: " + method);
		}

		return new StoredProcedureAttributes(procedureName, createOutputProcedureParameterFrom(method, procedure));
	}

	/**
	 * Tries to derive the procedure name from the given {@link Procedure}, falls back to the name of the given
	 * {@link Method}.
	 *
	 * @param method
	 * @param procedure
	 * @return
	 */
	private String deriveProcedureNameFrom(Method method, Procedure procedure) {

		if (StringUtils.hasText(procedure.value())) {
			return procedure.value();
		}

		String procedureName = procedure.procedureName();
		return StringUtils.hasText(procedureName) ? procedureName : method.getName();
	}

	/**
	 * Extract procedure attributes from method and procedure.
	 *
	 * @param method
	 * @param namedStoredProc
	 * @param procedure
	 * @return
	 */
	private StoredProcedureAttributes newProcedureAttributesFrom(Method method, NamedStoredProcedureQuery namedStoredProc,
			Procedure procedure) {

		List<ProcedureParameter> outputParameters;

		if (!procedure.outputParameterName().isEmpty()) {

			// we give the output parameter definition from the @Procedure annotation precedence
			outputParameters = Collections.singletonList(createOutputProcedureParameterFrom(method, procedure));
		} else {

			// try to discover the output parameter
			outputParameters = extractOutputParametersFrom(namedStoredProc);
		}

		return new StoredProcedureAttributes(namedStoredProc.name(), outputParameters, true);
	}

	/**
	 * Create a {@link ProcedureParameter} from relevant {@link Method} and {@link Procedure}.
	 *
	 * @param method
	 * @param procedure
	 * @return
	 */
	private ProcedureParameter createOutputProcedureParameterFrom(Method method, Procedure procedure) {

		return new ProcedureParameter(procedure.outputParameterName(), 1,
				procedure.refCursor() ? ParameterMode.REF_CURSOR : ParameterMode.OUT, method.getReturnType());
	}

	/**
	 * Translate all the {@Link NamedStoredProcedureQuery} parameters into a {@link List} of {@link ProcedureParameter}s.
	 *
	 * @param namedStoredProc
	 * @return
	 */
	private List<ProcedureParameter> extractOutputParametersFrom(NamedStoredProcedureQuery namedStoredProc) {

		List<ProcedureParameter> outputParameters = new ArrayList<>();

		int position = 1;
		for (StoredProcedureParameter param : namedStoredProc.parameters()) {

			switch (param.mode()) {
				case OUT:
				case INOUT:
				case REF_CURSOR:
					outputParameters.add(new ProcedureParameter(param.name(), position, param.mode(), param.type()));
					break;
				case IN:
				default:
					// not an output parameter
			}

			position++;
		}

		return outputParameters;
	}

	/**
	 * @param method must not be {@literal null}.
	 * @param entityMetadata must not be {@literal null}.
	 * @param procedure must not be {@literal null}.
	 * @return
	 */
	@Nullable
	private NamedStoredProcedureQuery tryFindAnnotatedNamedStoredProcedureQuery(Method method,
			JpaEntityMetadata<?> entityMetadata, Procedure procedure) {

		Assert.notNull(method, "Method must not be null");
		Assert.notNull(entityMetadata, "EntityMetadata must not be null");
		Assert.notNull(procedure, "Procedure must not be null");

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
	private String derivedNamedProcedureNameFrom(Method method, JpaEntityMetadata<?> entityMetadata,
			Procedure procedure) {

		return StringUtils.hasText(procedure.name()) //
				? procedure.name() //
				: entityMetadata.getEntityName() + "." + method.getName();
	}

	/**
	 * @param entityType
	 * @return
	 */
	private List<NamedStoredProcedureQuery> collectNamedStoredProcedureQueriesFrom(Class<?> entityType) {

		List<NamedStoredProcedureQuery> queries = new ArrayList<>();

		NamedStoredProcedureQueries namedQueriesAnnotation = AnnotatedElementUtils.findMergedAnnotation(entityType,
				NamedStoredProcedureQueries.class);
		if (namedQueriesAnnotation != null) {
			queries.addAll(Arrays.asList(namedQueriesAnnotation.value()));
		}

		NamedStoredProcedureQuery namedQueryAnnotation = AnnotatedElementUtils.findMergedAnnotation(entityType,
				NamedStoredProcedureQuery.class);
		if (namedQueryAnnotation != null) {
			queries.add(namedQueryAnnotation);
		}

		return queries;
	}
}
