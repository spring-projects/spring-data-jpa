/*
 * Copyright 2008-2019 the original author or authors.
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
package org.springframework.data.jpa.repository.sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.JpaContext;
import org.springframework.util.Assert;

/**
 * Dummy implementation to allow check for invoking a custom implementation.
 *
 * @author Oliver Gierke
 */
public class UserRepositoryImpl implements UserRepositoryCustom {

	private static final Logger LOG = LoggerFactory.getLogger(UserRepositoryImpl.class);

	@Autowired
	public UserRepositoryImpl(JpaContext context) {
		Assert.notNull(context, "JpaContext must not be null!");
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.sample.UserRepositoryCustom#someCustomMethod(org.springframework.data.jpa.domain.sample.User)
	 */
	@Override
	public void someCustomMethod(User u) {
		LOG.debug("Some custom method was invoked!");
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.sample.UserRepositoryCustom#findByOverrridingMethod()
	 */
	@Override
	public void findByOverrridingMethod() {
		LOG.debug("A method overriding a finder was invoked!");
	}
}
