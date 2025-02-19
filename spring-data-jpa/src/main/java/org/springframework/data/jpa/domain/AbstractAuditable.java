/*
 * Copyright 2008-2025 the original author or authors.
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
package org.springframework.data.jpa.domain;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.jspecify.annotations.NullUnmarked;
import org.springframework.data.domain.Auditable;

import org.jspecify.annotations.Nullable;

/**
 * Abstract base class for auditable entities. Stores the audition values in persistent fields.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 * @param <U> the auditing type. Typically some kind of user.
 * @param <PK> the type of the auditing type's identifier.
 */
@MappedSuperclass
@SuppressWarnings("NullAway") // querydsl does not work with jspecify -> 'Did not find type @org.jspecify.annotations.Nullable...'
public abstract class AbstractAuditable<U, PK extends Serializable> extends AbstractPersistable<PK>
		implements Auditable<U, PK, LocalDateTime> {

	@ManyToOne //
	private  U createdBy;

	private  Instant createdDate;

	@ManyToOne //
	private U lastModifiedBy;

	private Instant lastModifiedDate;

	@Override
	public Optional<U> getCreatedBy() {
		return Optional.ofNullable(createdBy);
	}

	@Override
	public void setCreatedBy(@Nullable U createdBy) {
		this.createdBy = createdBy;
	}

	@Override
	public Optional<LocalDateTime> getCreatedDate() {
		return null == createdDate ? Optional.empty()
				: Optional.of(LocalDateTime.ofInstant(createdDate, ZoneId.systemDefault()));
	}

	@Override
	public void setCreatedDate(LocalDateTime createdDate) {
		this.createdDate = createdDate.atZone(ZoneId.systemDefault()).toInstant();
	}

	@Override
	public Optional<U> getLastModifiedBy() {
		return Optional.ofNullable(lastModifiedBy);
	}

	@Override
	public void setLastModifiedBy(@Nullable U lastModifiedBy) {
		this.lastModifiedBy = lastModifiedBy;
	}

	@Override
	public Optional<LocalDateTime> getLastModifiedDate() {
		return null == lastModifiedDate ? Optional.empty()
				: Optional.of(LocalDateTime.ofInstant(lastModifiedDate, ZoneId.systemDefault()));
	}

	@Override
	public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate.atZone(ZoneId.systemDefault()).toInstant();
	}
}
