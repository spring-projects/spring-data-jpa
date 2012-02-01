/*
 * Copyright 2012 the original author or authors.
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
package org.springframework.data.jpa.domain.support;

import org.joda.time.DateTime;

/**
 * Default {@link DateTimeProvider} simply creating new {@link DateTime} instances for each method call.
 * 
 * @author Oliver Gierke
 */
public enum CurrentDateTimeProvider implements DateTimeProvider {

	INSTANCE;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.domain.support.DateTimeProvider#getDateTime()
	 */
	public DateTime getDateTime() {
		return new DateTime();
	}
}
