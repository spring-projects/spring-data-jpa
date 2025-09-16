/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.data.envers.repository.support;

import org.springframework.data.repository.history.support.RevisionEntityInformation;

/**
 * Envers-specific extension to {@link RevisionEntityInformation}.
 *
 * @author Mark Paluch
 * @since 4.0
 */
public interface EnversRevisionEntityInformation extends RevisionEntityInformation {

	/**
	 * Return the name of the timestamp property (annotated with {@link org.hibernate.envers.RevisionTimestamp}).
	 *
	 * @return the name of the timestamp property,
	 */
	String getRevisionTimestampPropertyName();

}
