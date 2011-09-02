/*
 * Copyright 2008-2011 the original author or authors.
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
package org.springframework.data.jpa.repository.sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.sample.User;

/**
 * Dummy implementation to allow check for invoking a custom implementation.
 * 
 * @author Oliver Gierke
 */
public class UserRepositoryImpl implements UserRepositoryCustom {

	private static final Logger LOG = LoggerFactory.getLogger(UserRepositoryImpl.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.jpa.repository.sample.UserRepositoryCustom#
	 * someCustomMethod(org.springframework.data.jpa.domain.sample.User)
	 */
	public void someCustomMethod(User u) {

		LOG.debug("Some custom method was invoked!");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.jpa.repository.sample.UserRepositoryCustom#
	 * findByOverrridingMethod()
	 */
	public void findByOverrridingMethod() {

		LOG.debug("A method overriding a finder was invoked!");
	}
}
