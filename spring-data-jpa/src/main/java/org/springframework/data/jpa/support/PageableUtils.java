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
package org.springframework.data.jpa.support;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Pageable;

/**
 * Provide a set of utility methods to support {@link Pageable}s.
 *
 * @author Greg Turnquist
 * @since 3.0
 */
public final class PageableUtils {

	private PageableUtils() {
		throw new IllegalStateException("Cannot instantiate a utility class!");
	}

	/**
	 * Convert a {@link Pageable}'s offset value from {@link Long} to {@link Integer} to support JPA spec methods.
	 *
	 * @param pageable
	 * @return integer
	 */
	public static int getOffsetAsInteger(Pageable pageable) {

		if (pageable.getOffset() > Integer.MAX_VALUE) {
			throw new InvalidDataAccessApiUsageException("Page offset exceeds Integer.MAX_VALUE (" + Integer.MAX_VALUE + ")");
		}

		return Math.toIntExact(pageable.getOffset());
	}
}
