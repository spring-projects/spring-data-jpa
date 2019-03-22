/*
 * Copyright 2016-2019 the original author or authors.
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
package org.springframework.data.jpa.provider;

import org.hibernate.query.Query;
import org.springframework.lang.Nullable;

/**
 * Utility functions to work with Hibernate. Mostly using reflection to make sure common functionality can be executed
 * against all the Hibernate version we support.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Jens Schauder
 * @since 1.10.2
 * @soundtrack Benny Greb - Soulfood (Live, https://www.youtube.com/watch?v=9_ErMa_CtSw)
 */
public abstract class HibernateUtils {

	private HibernateUtils() {}

	/**
	 * Return the query string of the underlying native Hibernate query.
	 *
	 * @param query
	 * @return
	 */
	@Nullable
	public static String getHibernateQuery(Object query) {

		if (query instanceof Query) {
			return ((Query) query).getQueryString();
		} else {
			throw new IllegalArgumentException("Don't know how to extract the query string from " + query);
		}
	}
}
