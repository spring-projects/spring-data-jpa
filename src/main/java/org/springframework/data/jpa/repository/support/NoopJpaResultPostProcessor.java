/*
 * Copyright 2008-2014 the original author or authors.
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

import java.util.List;

/**
 * A {@link JpaResultPostProcessor} that simply returns the results as is.
 * 
 * @author Thomas Darimont
 */
enum NoopJpaResultPostProcessor implements JpaResultPostProcessor {

	INSTANCE;

	/* (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.support.JpaResultPostProcessor#postProcessResult(java.lang.Class, java.lang.Object)
	 */
	@Override
	public <T, R> R postProcessResult(Class<T> domainClass, R entity) {
		return entity;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.support.JpaResultPostProcessor#postProcessResults(java.lang.Class, java.util.List)
	 */
	@Override
	public <T> List<T> postProcessResults(Class<T> domainClass, List<T> results) {
		return results;
	}
}
