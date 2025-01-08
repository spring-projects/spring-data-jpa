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
package org.springframework.data.jpa.domain.sample;

import jakarta.persistence.Embeddable;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

/**
 * JPA {@link Embeddable} to test out {@link org.springframework.data.jpa.domain.support.AuditingEntityListener}.
 * 
 * @author Greg Turnquist
 */
@Embeddable
public class AuditableEmbeddable {

	@CreatedDate //
	private Instant dateCreated;

	@LastModifiedDate //
	private Instant dateUpdated;

	public Instant getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(Instant dateCreated) {
		this.dateCreated = dateCreated;
	}

	public Instant getDateUpdated() {
		return dateUpdated;
	}

	public void setDateUpdated(Instant dateUpdated) {
		this.dateUpdated = dateUpdated;
	}
}
