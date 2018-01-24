/*
 * Copyright 2018 the original author or authors.
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

import java.util.Collections;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * NULL-Object pattern implementation for {@link DeclaredQuery}.
 *
 * @author Jens Schauder
 * @since 2.0.3
 */
class EmptyDeclaredQuery implements DeclaredQuery {

	/**
	 * An implementation implementing the NULL-Object pattern for situations where there is no query.
	 */
	static final DeclaredQuery EMPTY_QUERY = new EmptyDeclaredQuery();

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.DeclaredQuery#hasNamedParameter()
	 */
	@Override
	public boolean hasNamedParameter() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.DeclaredQuery#getQueryString()
	 */
	@Override
	public String getQueryString() {
		return "";
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.DeclaredQuery#getAlias()
	 */
	@Override
	public String getAlias() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.DeclaredQuery#hasConstructorExpression()
	 */
	@Override
	public boolean hasConstructorExpression() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.DeclaredQuery#isDefaultProjection()
	 */
	@Override
	public boolean isDefaultProjection() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.DeclaredQuery#getParameterBindings()
	 */
	@Override
	public List<StringQuery.ParameterBinding> getParameterBindings() {
		return Collections.emptyList();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.DeclaredQuery#deriveCountQuery(java.lang.String, java.lang.String)
	 */
	@Override
	public DeclaredQuery deriveCountQuery(@Nullable String countQuery, @Nullable String countQueryProjection) {

		Assert.hasText(countQuery, "CountQuery must not be empty!");

		return DeclaredQuery.of(countQuery);
	}
}
