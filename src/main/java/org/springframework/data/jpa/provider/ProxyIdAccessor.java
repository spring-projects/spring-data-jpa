/*
 * Copyright 2014-2018 the original author or authors.
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
package org.springframework.data.jpa.provider;

import org.springframework.lang.Nullable;

/**
 * Interface for a persistence provider specific accessor of identifiers held in proxies.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public interface ProxyIdAccessor {

	/**
	 * Returns whether the {@link ProxyIdAccessor} should be used for the given entity. Will inspect the entity to see
	 * whether it is a proxy so that lenient id lookup can be used.
	 *
	 * @param entity must not be {@literal null}.
	 * @return
	 */
	boolean shouldUseAccessorFor(Object entity);

	/**
	 * Returns the identifier of the given entity by leniently inspecting it for the identifier value.
	 *
	 * @param entity must not be {@literal null}.
	 * @return can be {@literal null}.
	 */
	@Nullable
	Object getIdentifierFrom(Object entity);
}
