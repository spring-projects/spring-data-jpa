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

import javax.persistence.EntityManager;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TypedQuery;

import org.springframework.data.jpa.repository.query.JpaParameters.JpaParameter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link AbstractJpaQuery} implementation that inspects a {@link JpaQueryMethod} for the existence of an
 * {@link Procedure} annotation and creates a JPA 2.1 {@link StoredProcedureQuery} from it.
 * 
 * @author Thomas Darimont
 */
public class StoredProcedureJpaQuery extends AbstractJpaQuery {

	private final StoredProcedureAttributes procedureAttributes;

	/**
	 * Creates a new {@link StoredProcedureJpaQuery}.
	 * 
	 * @param method must not be {@literal null}
	 * @param em must not be {@literal null}
	 */
	public StoredProcedureJpaQuery(JpaQueryMethod method, EntityManager em) {

		super(method, em);
		this.procedureAttributes = method.getProcedureAttributes();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.AbstractJpaQuery#createQuery(java.lang.Object[])
	 */
	protected StoredProcedureQuery createQuery(Object[] values) {
		return applyHints(doCreateQuery(values), getQueryMethod());
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.AbstractJpaQuery#doCreateQuery(java.lang.Object[])
	 */
	@Override
	protected StoredProcedureQuery doCreateQuery(Object[] values) {

		StoredProcedureQuery proc = createStoredProcedure();

		return createBinder(values).bind(proc);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.AbstractJpaQuery#doCreateCountQuery(java.lang.Object[])
	 */
	@Override
	protected TypedQuery<Long> doCreateCountQuery(Object[] values) {
		return null;
	}

	/**
	 * Extracts the output value from the given {@link StoredProcedureQuery}.
	 * 
	 * @param storedProcedureQuery must not be {@literal null}.
	 * @return
	 */
	Object extractOutputValue(StoredProcedureQuery storedProcedureQuery) {

		Assert.notNull(storedProcedureQuery, "StoredProcedureQuery must not be null!");

		if (!procedureAttributes.hasReturnValue()) {
			return null;
		}

		if (StringUtils.hasText(procedureAttributes.getOutputParameterName())) {
			return storedProcedureQuery.getOutputParameterValue(procedureAttributes.getOutputParameterName());
		}

		return storedProcedureQuery.getOutputParameterValue(getQueryMethod().getParameters().getNumberOfParameters() + 1);
	}

	/**
	 * Creates a new JPA 2.1 {@link StoredProcedureQuery} from this {@link StoredProcedureJpaQuery}.
	 * 
	 * @return
	 */
	private StoredProcedureQuery createStoredProcedure() {

		if (procedureAttributes.isNamedStoredProcedure()) {
			return newNamedStoredProcedureQuery();
		}

		return newAdhocStoredProcedureQuery();
	}

	/**
	 * Creates a new named {@link StoredProcedureQuery} defined via an {@link NamedStoredProcedureQuery} on an entity.
	 * 
	 * @return
	 */
	private StoredProcedureQuery newNamedStoredProcedureQuery() {
		return getEntityManager().createNamedStoredProcedureQuery(procedureAttributes.getProcedureName());
	}

	/**
	 * Creates a new ad-hoc {@link StoredProcedureQuery} from the given {@link StoredProcedureAttributes}.
	 * 
	 * @return
	 */
	private StoredProcedureQuery newAdhocStoredProcedureQuery() {

		StoredProcedureQuery proc = getEntityManager().createStoredProcedureQuery(procedureAttributes.getProcedureName());

		JpaParameters params = getQueryMethod().getParameters();
		for (JpaParameter param : params) {

			if (!param.isBindable()) {
				continue;
			}

			if (param.isNamedParameter()) {
				proc.registerStoredProcedureParameter(param.getName(), param.getType(), ParameterMode.IN);
			} else {
				proc.registerStoredProcedureParameter(param.getIndex() + 1, param.getType(), ParameterMode.IN);
			}
		}

		if (procedureAttributes.hasReturnValue()) {
			proc.registerStoredProcedureParameter(params.getNumberOfParameters() + 1,
					procedureAttributes.getOutputParameterType(), ParameterMode.OUT);
		}

		return proc;
	}
}
