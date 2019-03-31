/*
 * Copyright 2014-2019 the original author or authors.
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

import java.util.Date;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

/**
 * @author Oliver Gierke
 * @since 1.6
 */
@MappedSuperclass
public class AbstractAnnotatedAuditable {

	private @Id @GeneratedValue Long id;

	private @CreatedBy @ManyToOne AuditableUser createdBy;
	private @CreatedDate @Temporal(TemporalType.TIMESTAMP) Date createAt;

	private @ManyToOne AuditableUser lastModifiedBy;
	private @Temporal(TemporalType.TIMESTAMP) Date lastModifiedAt;

	public Long getId() {
		return id;
	}

	public AuditableUser getCreatedBy() {
		return createdBy;
	}

	public Date getCreateAt() {
		return createAt;
	}

	@LastModifiedBy
	public AuditableUser getLastModifiedBy() {
		return lastModifiedBy;
	}

	@LastModifiedDate
	public Date getLastModifiedAt() {
		return lastModifiedAt;
	}
}
