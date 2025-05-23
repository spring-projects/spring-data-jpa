/*
 * Copyright 2008-2025 the original author or authors.
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

import jakarta.persistence.Query;
import jakarta.persistence.TypedQueryReference;

import org.jspecify.annotations.Nullable;

/**
 * Interface to hide different implementations to extract the original JPA query string from a {@link Query}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public interface QueryExtractor {

	/**
	 * Reverse engineers the query string from the {@link Query} or a {@link TypedQueryReference} object. This requires
	 * provider specific API as JPA does not provide access to the underlying query string as soon as one has created a
	 * {@link Query} instance of it.
	 *
	 * @param query
	 * @return the query string representing the query or {@literal null} if resolving is not possible.
	 */
	@Nullable
	String extractQueryString(Object query);

	/**
	 * Reverse engineers the query native flag from a {@link Query} or native query as JPA does not provide access to the
	 * underlying query string once a (named) query is constructed.
	 *
	 * @param query
	 * @return {@literal true} if the query is a native one.
	 * @since 4.0
	 */
	boolean isNativeQuery(Object query);

	/**
	 * Returns whether the extractor is able to extract the original query string from a given {@link Query}.
	 *
	 * @return
	 */
	boolean canExtractQuery();

}
