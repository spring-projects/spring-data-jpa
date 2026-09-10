/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.data.jpa.util;

import java.util.List;

/**
 * Exposes methods that used through reflection.
 * <p>
 * Reflective methods can be introspected by components that e.g. need to register reflective hints while keeping a
 * single reference that performs lookup.
 *
 * @author Mark Paluch
 * @since 4.2
 */
public interface ReflectiveMethods {

	/**
	 * Returns the list of {@link ReflectiveMethod methods}.
	 */
	List<ReflectiveMethod> getReflectiveMethods();

}
