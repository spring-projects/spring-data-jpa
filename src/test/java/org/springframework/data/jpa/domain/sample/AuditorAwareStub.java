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
package org.springframework.data.jpa.domain.sample;

import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.sample.AuditableUserRepository;
import org.springframework.util.Assert;

/**
 * Stub implementation for {@link AuditorAware}. Returns {@literal null} for the current auditor.
 * 
 * @author Oliver Gierke
 */
public class AuditorAwareStub implements AuditorAware<AuditableUser> {

	@SuppressWarnings("unused")
	private final AuditableUserRepository repository;
	private AuditableUser auditor;

	public AuditorAwareStub(AuditableUserRepository repository) {

		Assert.notNull(repository);
		this.repository = repository;
	}

	public void setAuditor(AuditableUser auditor) {

		this.auditor = auditor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.domain.AuditorAware#getCurrentAuditor()
	 */
	public AuditableUser getCurrentAuditor() {

		return auditor;
	}
}
