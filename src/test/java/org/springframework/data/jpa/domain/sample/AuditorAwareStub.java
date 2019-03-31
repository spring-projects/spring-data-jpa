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
package org.springframework.data.jpa.domain.sample;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.sample.AuditableUserRepository;
import org.springframework.util.Assert;

/**
 * Stub implementation for {@link AuditorAware}. Returns {@literal null} for the current auditor.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class AuditorAwareStub implements AuditorAware<AuditableUser> {

	@SuppressWarnings("unused") //
	private final AuditableUserRepository repository;

	private AuditableUser auditor;

	public AuditorAwareStub(AuditableUserRepository repository) {

		Assert.notNull(repository, "AuditableUserRepository must not be null!");
		this.repository = repository;
	}

	public void setAuditor(AuditableUser auditor) {
		this.auditor = auditor;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.AuditorAware#getCurrentAuditor()
	 */
	@Override
	public Optional<AuditableUser> getCurrentAuditor() {
		return Optional.ofNullable(auditor);
	}
}
