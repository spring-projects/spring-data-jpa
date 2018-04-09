/*
 * Copyright 2012-2018 the original author or authors.
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
package org.springframework.data.jpa.mapping;

import org.springframework.data.mapping.PersistentProperty;

/**
 * Interface for a JPA-specific {@link PersistentProperty}.
 *
 * @author Oliver Gierke
 * @since 1.3
 */
public interface JpaPersistentProperty extends PersistentProperty<JpaPersistentProperty> {

	/**
	 * Return whether the property is considered embeddable.
	 * 
	 * @return
	 * @since 2.1
	 */
	boolean isEmbeddable();
}
