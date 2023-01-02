/*
 * Copyright 2008-2023 the original author or authors.
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

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * JPA entity with an {@link Embedded} set of auditable data.
 *
 * @author Greg Turnquist
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
public class AuditableEntity {

	@Id
	@GeneratedValue //
	private Long id;

	private String data;

	@Embedded //
	private AuditableEmbeddable auditDetails;

	public AuditableEntity() {
		this(null, null, null);
	}

	public AuditableEntity(Long id, String data, AuditableEmbeddable auditDetails) {

		this.id = id;
		this.data = data;
		this.auditDetails = auditDetails;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public AuditableEmbeddable getAuditDetails() {
		return auditDetails;
	}

	public void setAuditDetails(AuditableEmbeddable auditDetails) {
		this.auditDetails = auditDetails;
	}
}
