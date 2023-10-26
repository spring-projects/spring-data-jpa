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
package org.springframework.data.jpa.domain;

import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;

import java.util.Map;

/**
 * Parameterized specification in the sense of Domain Driven Design.
 *
 * @author Yanming Zhou
 */
public interface ParameterizedSpecification<T> extends Specification<T> {

	long serialVersionUID = 1L;

	/**
	 * Required parameters of the underlying {@link TypedQuery}.
	 *
	 * @return Required parameters of the underlying {@link TypedQuery}, should not be {@literal null} or empty.
	 * @see CriteriaQuery#getParameters()
	 * @see TypedQuery#setParameter(String, Object) 
	 * @since 2.0
	 */
	Map<String, Object> getParameters();
}
