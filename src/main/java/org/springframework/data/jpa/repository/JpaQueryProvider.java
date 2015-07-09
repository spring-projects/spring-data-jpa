/*
 * Copyright 2008-2015 the original author or authors.
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
package org.springframework.data.jpa.repository;

/**
 * Interface that should be implemented by classes that provide JPA queries.
 * 
 * @author Nick Guletskii
 */
public interface JpaQueryProvider {

	/**
	 * This method should return the query string or null if {@link Query#value()} should be used.
	 * 
	 * @see Query#value()
	 */
	public String getQuery();

}
